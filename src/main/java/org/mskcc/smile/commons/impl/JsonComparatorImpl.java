package org.mskcc.smile.commons.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.mskcc.smile.commons.JsonComparator;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.stereotype.Component;

/**
 * Decides whether two JSON documents carry the same metadata.
 *
 * <p>Inputs may be in IGO's raw schema or SMILE's universal schema (or one of each). Both are
 * first converted to the universal schema ({@link #toUniversalSchema}), then reduced to a
 * canonical form ({@link #normalizeRecord}) and compared structurally. A "record" is the root
 * document or an element of its {@code samples} array; schema conversion and IGO-ownership
 * projection apply per record, while ignored fields and empty-value removal apply at every depth.
 *
 * @author ochoaa
 */
@Component
public class JsonComparatorImpl implements JsonComparator {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SAMPLES = "samples";
    /** SmileRequest serializes its samples as SmileSample objects under this key. */
    private static final String SMILE_SAMPLES = "smileSampleList";
    /** SmileSample's serialized current metadata (its sampleMetadataList holds the full history). */
    private static final String LATEST_SAMPLE_METADATA = "latestSampleMetadata";

    private static final Set<String> DEFAULT_IGNORED_FIELDS = Set.of(
            "smileRequestId", "smileSampleId", "smilePatientId", "requestJson", "importDate",
            "cmoSampleName", "sampleAliases", "patientAliases", "datasource", "genePanel",
            "additionalProperties", "cmoInfoIgoId", "date", "sampleStatus");

    private static final Set<String> GENERIC_IGNORED_FIELDS = Set.of("date", SAMPLES);

    /** Record-level fields added by the SMILE pipeline rather than IGO; see alignPipelineFields. */
    private static final Set<String> PIPELINE_FIELDS = Set.of("status");

    /*
     * IGO-owned data, as universal-schema paths. Only these are compared when an update arrives
     * from IGO (LIMS); everything else may carry Project Manager edits made via the Dashboard,
     * which IGO never sees and must not override. Keep in sync with smile-server's
     * SmileSample.applyIgoLimsUpdates and SmileRequest.applyIgoRequestMetadataUpdates.
     */
    private static final Set<String> IGO_OWNED_SAMPLE_PATHS = Set.of(
            "primaryId", "species", "genePanel", "baitSet", "igoComplete", "cmoSampleIdFields",
            "qcReports", "libraries", // libraries carry runs, which carry fastqs
            "additionalProperties.altId", "additionalProperties.igoSampleStatus");
    private static final Set<String> IGO_OWNED_REQUEST_PATHS = Set.of(
            "igoRequestId", "genePanel", "strand", "libraryType", "isCmoRequest", "pooledNormals",
            "igoDeliveryDate", "ilabRequestId");

    /** IGO raw property -> universal-schema path (mirrors SampleMetadata(IgoSampleManifest)). */
    private static final Map<String, String> SAMPLE_FIELD_MOVES = Map.of(
            "igoId", "primaryId",
            "requestId", "igoRequestId",
            "cmoSampleClass", "sampleType",
            "specimenType", "sampleClass",
            "oncoTreeCode", "oncotreeCode",
            "altid", "additionalProperties.altId");
    /** IGO raw property -> universal-schema path (mirrors SmileRequest IGO constructors). */
    private static final Map<String, String> REQUEST_FIELD_MOVES = Map.of(
            "projectId", "igoProjectId",
            "requestId", "igoRequestId",
            "recipe", "genePanel",
            "deliveryDate", "igoDeliveryDate");
    private static final Set<String> LONG_FIELDS = Set.of("igoDeliveryDate");

    /**
     * How a comparison treats fields.
     * @param ignored fields dropped at every depth (record level is skipped when igoOwnedOnly)
     * @param igoOwnedOnly compare only IGO-owned paths of each record
     * @param standardize whether to convert IGO raw schema to the universal schema first
     */
    private record Profile(Set<String> ignored, boolean igoOwnedOnly, boolean standardize) {}

    private static final Profile ALL_FIELDS = new Profile(DEFAULT_IGNORED_FIELDS, false, true);
    private static final Profile IGO_OWNED = new Profile(DEFAULT_IGNORED_FIELDS, true, true);
    private static final Profile GENERIC = new Profile(GENERIC_IGNORED_FIELDS, false, false);

    /** Scalars are equal if textually equal, or if both numeric with the same value. */
    private static final Comparator<JsonNode> SCALAR_COMPARATOR = (a, b) -> {
        if (a.equals(b)) {
            return 0;
        }
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue());
        }
        return 1;
    };

    @Override
    public Boolean isConsistentByIgoProperties(String referenceJson, String targetJson) throws Exception {
        return compare(referenceJson, targetJson, IGO_OWNED);
    }

    @Override
    public Boolean isConsistent(String referenceJson, String targetJson) throws Exception {
        return compare(referenceJson, targetJson, ALL_FIELDS);
    }

    /**
     * Comparison with caller-supplied ignored fields.
     * @param comparisonType "igo" compares only IGO-owned fields; any other value compares all
     */
    @Override
    public Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields,
            String comparisonType) throws Exception {
        Set<String> ignored = (ignoredFields == null)
                ? Set.of() : new HashSet<>(Arrays.asList(ignoredFields));
        return compare(referenceJson, targetJson,
                new Profile(ignored, "igo".equals(comparisonType), true));
    }

    @Override
    public Boolean isConsistentGenericComparison(String referenceJson, String targetJson) throws Exception {
        return compare(referenceJson, targetJson, GENERIC);
    }

    private Boolean compare(String referenceJson, String targetJson, Profile profile)
            throws JsonProcessingException {
        JsonNode ref = normalizeRecord(MAPPER.readTree(referenceJson), profile);
        JsonNode tar = normalizeRecord(MAPPER.readTree(targetJson), profile);
        if (profile.standardize()) {
            alignPipelineFields(ref, tar);
        }
        boolean matches = (ref == null) ? tar == null : tar != null && ref.equals(SCALAR_COMPARATOR, tar);
        if (!matches) {
            printDifferences(ref, tar);
        }
        return matches;
    }

    /**
     * Prints JSONAssert's expected/got report for two normalized documents, so it lists only the
     * differences that count, at their normalized paths. Falls back to printing both documents
     * when JSONAssert cannot describe the mismatch.
     */
    private static void printDifferences(JsonNode ref, JsonNode tar) {
        try {
            JSONAssert.assertEquals(String.valueOf(ref), String.valueOf(tar), JSONCompareMode.STRICT);
        } catch (AssertionError ae) {
            System.out.println(ae.getLocalizedMessage());
            return;
        } catch (JSONException e) {
            // fall through to the plain report
        }
        System.out.println("JSON mismatch\n  reference: " + ref + "\n  target:    " + tar);
    }

    /**
     * Fields such as status are added by the SMILE pipeline (smile-request-filter,
     * smile-label-generator), never by IGO, so raw IGO JSON lacks them. A pipeline field present on
     * only one side of a record pair means "not yet processed", not "different", and is dropped
     * from both; when both sides carry it, it is compared as usual. Samples are paired by position,
     * which matches primary id order since normalizeSamples sorts them.
     */
    private static void alignPipelineFields(JsonNode ref, JsonNode tar) {
        if (!(ref instanceof ObjectNode refRecord) || !(tar instanceof ObjectNode tarRecord)) {
            return;
        }
        for (String field : PIPELINE_FIELDS) {
            if (refRecord.has(field) != tarRecord.has(field)) {
                refRecord.remove(field);
                tarRecord.remove(field);
            }
        }
        JsonNode refSamples = refRecord.path(SAMPLES);
        JsonNode tarSamples = tarRecord.path(SAMPLES);
        for (int i = 0; i < Math.min(refSamples.size(), tarSamples.size()); i++) {
            alignPipelineFields(refSamples.get(i), tarSamples.get(i));
        }
    }

    private JsonNode normalizeRecord(JsonNode node, Profile profile) {
        if (node == null || !node.isObject()) {
            return normalizeValue(node, profile);
        }
        ObjectNode record = profile.standardize()
                ? toUniversalSchema((ObjectNode) node) : (ObjectNode) node;
        ObjectNode out = MAPPER.createObjectNode();
        // raw request JSON uses "samples", a serialized SmileRequest uses "smileSampleList";
        // both normalize to "samples" so either shape can be compared with the other
        JsonNode samples = record.has(SAMPLES) ? record.get(SAMPLES) : record.get(SMILE_SAMPLES);
        if (samples != null && !profile.ignored().contains(SAMPLES)) {
            putIfPresent(out, SAMPLES, normalizeSamples(samples, profile));
        }
        if (profile.igoOwnedOnly()) {
            // projection keyed by path, e.g. "additionalProperties.altId"
            for (String path : isSample(record) ? IGO_OWNED_SAMPLE_PATHS : IGO_OWNED_REQUEST_PATHS) {
                putIfPresent(out, path, normalizeValue(record.at("/" + path.replace('.', '/')), profile));
            }
            return out;
        }
        for (Map.Entry<String, JsonNode> entry : record.properties()) {
            String field = entry.getKey();
            if (!field.equals(SAMPLES) && !field.equals(SMILE_SAMPLES)
                    && !profile.ignored().contains(field)) {
                putIfPresent(out, field, normalizeValue(entry.getValue(), profile));
            }
        }
        return out;
    }

    private static void putIfPresent(ObjectNode node, String field, JsonNode value) {
        if (value != null) {
            node.set(field, value);
        }
    }

    private static boolean isSample(JsonNode record) {
        return record.has("primaryId") || record.has("igoId");
    }

    /**
     * Normalizes each sample as a record and orders them by primary id. A SmileSample element is
     * reduced to its latest metadata, since its metadata history is not part of the request's data.
     */
    private JsonNode normalizeSamples(JsonNode samples, Profile profile) {
        if (!samples.isArray()) {
            return normalizeValue(samples, profile);
        }
        List<JsonNode> sorted = new ArrayList<>();
        samples.forEach(sample -> sorted.add(sample.has(LATEST_SAMPLE_METADATA)
                ? sample.get(LATEST_SAMPLE_METADATA) : sample));
        sorted.sort(Comparator.comparing(JsonComparatorImpl::sampleId));
        ArrayNode out = MAPPER.createArrayNode();
        sorted.forEach(sample -> out.add(normalizeRecord(sample, profile)));
        return out.isEmpty() ? null : out;
    }

    private static String sampleId(JsonNode sample) {
        return sample.path("primaryId").asText(sample.path("igoId").asText(""));
    }

    /** Returns null for null/blank/empty values so the caller can drop the field. */
    private JsonNode normalizeValue(JsonNode value, Profile profile) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isTextual()) {
            String text = value.asText();
            return (StringUtils.isBlank(text) || text.equalsIgnoreCase("null") || text.equals("[]"))
                    ? null : value;
        }
        if (value.isObject()) {
            ObjectNode out = MAPPER.createObjectNode();
            for (Map.Entry<String, JsonNode> entry : value.properties()) {
                if (!profile.ignored().contains(entry.getKey())) {
                    JsonNode child = normalizeValue(entry.getValue(), profile);
                    if (child != null) {
                        out.set(entry.getKey(), child);
                    }
                }
            }
            return out.isEmpty() ? null : out;
        }
        if (value.isArray()) {
            // element positions are significant, so empty elements become nulls rather than vanishing
            ArrayNode out = MAPPER.createArrayNode();
            value.forEach(element -> {
                JsonNode child = normalizeValue(element, profile);
                out.add(child == null ? NullNode.getInstance() : child);
            });
            return out.isEmpty() ? null : out;
        }
        return value;
    }

    /**
     * Converts a record from IGO's raw schema to SMILE's universal schema; universal-schema input
     * passes through unchanged. Which moves apply is chosen by the record's shape (sample if it
     * has a primaryId or igoId) rather than by where it appears in the document.
     */
    private ObjectNode toUniversalSchema(ObjectNode node) {
        ObjectNode out = node.deepCopy();
        boolean sample = isSample(out);
        (sample ? SAMPLE_FIELD_MOVES : REQUEST_FIELD_MOVES).forEach((from, to) -> move(out, from, to));
        if (sample) {
            // IGO's sampleStatus is a string; the universal sampleStatus is a different (object) field
            if (out.path("sampleStatus").isTextual()) {
                move(out, "sampleStatus", "additionalProperties.igoSampleStatus");
            }
            // universal genePanel duplicates IGO's cmoSampleIdFields.recipe
            JsonNode recipe = out.path("cmoSampleIdFields").path("recipe");
            if (!out.hasNonNull("genePanel") && recipe.isValueNode()) {
                out.set("genePanel", recipe);
            }
        }
        return out;
    }

    /** Moves a field to a universal-schema path (at most one level of nesting). */
    private static void move(ObjectNode node, String from, String toPath) {
        JsonNode value = node.remove(from);
        if (value == null) {
            return;
        }
        if (LONG_FIELDS.contains(toPath) && value.isValueNode() && !value.isNull()) {
            value = LongNode.valueOf(value.asLong());
        }
        int dot = toPath.indexOf('.');
        if (dot < 0) {
            node.set(toPath, value);
            return;
        }
        String parentField = toPath.substring(0, dot);
        ObjectNode parent = (node.get(parentField) instanceof ObjectNode existing)
                ? existing : node.putObject(parentField);
        parent.set(toPath.substring(dot + 1), value);
    }
}
