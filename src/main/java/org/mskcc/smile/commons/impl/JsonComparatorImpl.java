package org.mskcc.smile.commons.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.mskcc.smile.commons.JsonComparator;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class JsonComparatorImpl implements JsonComparator {
    private final ObjectMapper mapper = new ObjectMapper();

    public final String[] DEFAULT_IGNORED_FIELDS = new String[]{
        "smileRequestId",
        "smileSampleId",
        "smilePatientId",
        "requestJson",
        "samples",
        "importDate",
        "cmoSampleName",
        "sampleAliases",
        "datasource",
        "patientAliases",
        "sampleAliases",
        "genePanel",
        "additionalProperties"};

    public final String[] IGO_ACCEPTED_FIELDS = new String[]{
        //RequestMetadata fields
        "deliveryDate",
        "isCmoRequest",
        "libraryType",
        "pooledNormals",
        "genePanel",
        "igoRequestId",
        "igoComplete",
        "igoSampleId",
        "strand",
        // SampleMetadata fields
        "baitSet",
        "cfDNA2dBarcode",
        "recipe",
        "genePanel",
        "primaryId",
        "igoId",
        "barcodeId",
        "barcodeIndex",
        "captureConcentrationNm",
        "captureInputNg",
        "captureName",
        "dnaInputNg",
        "libraryConcentrationNgul",
        "libraryIgoId",
        "libraryVolume",
        "fastqs",
        "flowCellId",
        "flowCellLanes",
        "readLength",
        "runDate",
        "runId",
        "runMode",
        "IGORecommendation",
        "comments",
        "investigatorDecision",
        "qcReportType",
        "libraries",
        "qcReports",
        "status",
        "cmoSampleIdFields",
        "runs"
    };

    private final Map<String, String> STD_IGO_REQUEST_JSON_PROPS_MAP =
            initStandardizedIgoRequestJsonPropsMap();
    private final Map<String, String> STD_IGO_SAMPLE_JSON_PROPS_MAP =
            initStandardizedIgoSampleJsonPropsMap();

    private Map<String, String> initStandardizedIgoRequestJsonPropsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("projectId", "igoProjectId");
        map.put("requestId", "igoRequestId");
        map.put("recipe", "genePanel");
        return map;
    }

    private Map<String, String> initStandardizedIgoSampleJsonPropsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("cmoSampleClass", "sampleType");
        map.put("specimenType", "sampleClass");
        map.put("oncoTreeCode", "oncotreeCode");
        map.put("requestId", "igoRequestId");
        map.put("igoId", "primaryId");
        return map;
    }

    @Override
    public Boolean isConsistentByIgoProperties(String referenceJson, String targetJson) throws Exception {
        return isConsistent(referenceJson, targetJson, DEFAULT_IGNORED_FIELDS, "igo");
    }

    @Override
    public Boolean isConsistent(String referenceJson, String targetJson) throws Exception {
        return isConsistent(referenceJson, targetJson,  DEFAULT_IGNORED_FIELDS, "new");
    }

    /**
     * Accepted values of String comparisonType are new, igo and dashboard
     * @param referenceJson
     * @param targetJson
     * @param ignoredFields
     * @param comparisonType
     * @return Boolean
     * @throws Exception
     */
    @Override
    public Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields,
            String comparisonType) throws Exception {
        Boolean consistencyCheckStatus = Boolean.TRUE;

        // filter reference and target request jsons and compare
        String filteredReferenceJson = standardizeAndFilterRequestJson(referenceJson,
                ignoredFields, comparisonType);
        String filteredTargetJson = standardizeAndFilterRequestJson(targetJson,
                ignoredFields, comparisonType);
        if (!isMatchingJsons(filteredReferenceJson, filteredTargetJson)) {
            consistencyCheckStatus = Boolean.FALSE;
        }

        // checks qcreports, libraries and status (case where sample metadata is compared directly)
        if (jsonHasQcAndOrLibrariesAndOrStatusFields(referenceJson)
                || jsonHasQcAndOrLibrariesAndOrStatusFields(targetJson)) {
            if (!isConsistentSampleMetadata(referenceJson, targetJson, comparisonType)) {
                consistencyCheckStatus = Boolean.FALSE;
            }
        }

        // filter reference and target sample list jsons and compare if applicable
        if (jsonHasSamplesField(referenceJson) || jsonHasSamplesField(targetJson)) {
            String filteredReferenceSamplesJson =
                    standardizeAndFilterRequestSamplesJson(referenceJson, ignoredFields, comparisonType);
            String filteredTargetSamplesJson =
                    standardizeAndFilterRequestSamplesJson(targetJson, ignoredFields, comparisonType);

            if (!isMatchingJsons(filteredReferenceSamplesJson, filteredTargetSamplesJson)) {
                consistencyCheckStatus = Boolean.FALSE;
            }

            JsonNode refSamplesJsonNode = mapper.readTree(referenceJson).get("samples");
            ArrayNode refSamplesArrayNode = (ArrayNode) refSamplesJsonNode;
            Iterator<JsonNode> itrRef = refSamplesArrayNode.elements();

            // Iterating through a list of samples from referenceJson
            while (itrRef.hasNext()) {
                JsonNode refSampleNode = itrRef.next();
                String primaryId = findPrimaryIdFromJsonNode(refSampleNode);
                if (primaryId != null) {
                    JsonNode tarSampleNode = findSampleNodeFromSampleArray(targetJson, primaryId);
                    // Compares status, libraries and qcReports.
                    // Runs still need to be addressed
                    if (!isConsistentSampleMetadata(refSampleNode, tarSampleNode, comparisonType)) {
                        consistencyCheckStatus = Boolean.FALSE;
                    }
                }
            }
        }
        return consistencyCheckStatus;
    }

    private Boolean isConsistentSampleMetadata(String referenceJson, String targetJson,
            String comparisonType) throws JsonProcessingException {
        return isConsistentSampleMetadata(mapper.readTree(referenceJson),
                mapper.readTree(targetJson), comparisonType);
    }

    private Boolean isConsistentSampleMetadata(JsonNode referenceNode,
            JsonNode targetNode, String comparisonType) throws JsonProcessingException {
        if (!isMatchingJsonByFieldName(referenceNode, targetNode, "qcReports", comparisonType)
                            || !isMatchingJsonByFieldName(referenceNode, targetNode,
                                    "libraries", comparisonType)
                            || !isMatchingJsonByFieldName(referenceNode, targetNode,
                                            "status", comparisonType)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private JsonNode findSampleNodeFromSampleArray(String targetJson, String primaryId)
            throws JsonMappingException, JsonProcessingException {
        JsonNode tarSamplesJsonNode = mapper.readTree(targetJson).get("samples");
        ArrayNode tarSamplesArrayNode = (ArrayNode) tarSamplesJsonNode;
        Iterator<JsonNode> itrTar = tarSamplesArrayNode.elements();

        while (itrTar.hasNext()) {
            JsonNode sampleNode = itrTar.next();
            String tarPrimaryId =  findPrimaryIdFromJsonNode(sampleNode);
            if (primaryId.equals(tarPrimaryId)) {
                return sampleNode;
            }
        }
        return null;
    }

    private Boolean isMatchingJsonByFieldName(JsonNode refNode, JsonNode tarNode,
            String fieldName, String comparisonType) throws JsonProcessingException {
        // Case 1: target and reference have the field
        if (refNode.has(fieldName) && tarNode.has(fieldName)) {
            // Case 1a: target and reference have an empty field
            if (refNode.get(fieldName).isEmpty() && refNode.get(fieldName).isEmpty()) {
                return Boolean.TRUE;
            }

            // Case 1b: target or reference have an empty field
            if (refNode.get(fieldName).isEmpty() || refNode.get(fieldName).isEmpty()) {
                return Boolean.FALSE;
            }

            // Case 1c: filter the ref and target jsons first then compare
            JsonNode unfilteredRefNode = convertToMapJsonNode(fieldName, refNode);
            JsonNode unfilteredTarNode = convertToMapJsonNode(fieldName, tarNode);

            JsonNode filteredRefNode = filterJsonNode(
                    (ObjectNode) unfilteredRefNode, DEFAULT_IGNORED_FIELDS, comparisonType);
            JsonNode filteredTarNode = filterJsonNode(
                    (ObjectNode) unfilteredTarNode, DEFAULT_IGNORED_FIELDS, comparisonType);
            if (!isMatchingJsons(mapper.writeValueAsString(filteredRefNode),
                    mapper.writeValueAsString(filteredTarNode))) {
                return Boolean.FALSE;
            }

            if (fieldName.equals("libraries")) {
                ArrayNode librariesRefArrayNode = (ArrayNode) refNode.get(fieldName);
                Iterator<JsonNode> itrLibRef = librariesRefArrayNode.elements();

                ArrayNode librariesTarArrayNode = (ArrayNode) tarNode.get(fieldName);
                Iterator<JsonNode> itrLibTar = librariesTarArrayNode.elements();

                if (librariesRefArrayNode.size() != librariesTarArrayNode.size()) {
                    return Boolean.FALSE;
                }

                // Assumption: corresponding library elements from ref and tar are in the same index
                while (itrLibRef.hasNext() && itrLibTar.hasNext()) {
                    JsonNode refLibNext = itrLibRef.next();
                    JsonNode tarLibNext = itrLibTar.next();

                    if (!isMatchingJsonByFieldName(refLibNext,
                            tarLibNext, "runs", comparisonType)) {
                        return Boolean.FALSE;
                    }
                }
            }
            return Boolean.TRUE;
        }
        // Case 2: target and reference do not have the field
        if (!refNode.has(fieldName) && !tarNode.has(fieldName)) {
            return Boolean.TRUE;
        }
        // Case 3: One of them is missing the field
        return Boolean.FALSE;
    }

    /**
     * Helper function to treat possible array node json comparisons as instances of regular JSONs.
     * @param fieldName
     * @param node
     * @return JsonNode
     * @throws JsonProcessingException
     */
    private JsonNode convertToMapJsonNode(String fieldName, JsonNode node) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        if (!node.has(fieldName)) {
            return null;
        }
        map.put(fieldName, node.get(fieldName).toString());
        String convertedMapAsString = mapper.writeValueAsString(map);
        return mapper.readTree(convertedMapAsString);
    }

    /**
     * Helps find primaryId or igoId from sampleMetadata Node
     * @param sampleNode
     * @return
     */
    private String findPrimaryIdFromJsonNode(JsonNode sampleNode) {
        return (sampleNode.get("primaryId") == null)
                ? sampleNode.get("igoId").toString() : sampleNode.get("primaryId").toString();
    }

    /**
     * Given an input jsonString and an array of ignoredFields, returns a JSON
     * with (1) the fields to ignore removed, (2) json fields with null or empty values
     * removed, and (3) standardize json property names.
     * @param jsonString
     * @param ignoredFields
     * @return String
     * @throws JsonProcessingException
     */
    private String standardizeAndFilterRequestJson(String jsonString, String[] ignoredFields,
            String comparisonType) throws JsonProcessingException {
        JsonNode unfilteredJsonNode = mapper.readTree(jsonString);
        JsonNode stdJsonNode = standardizeJsonProperties(
                (ObjectNode) unfilteredJsonNode, STD_IGO_REQUEST_JSON_PROPS_MAP);
        JsonNode stdFilteredJsonNode = filterJsonNode((ObjectNode) stdJsonNode,
                ignoredFields, comparisonType);
        return mapper.writeValueAsString(stdFilteredJsonNode);
    }

    /**
     * Given an input jsonString and an array of ignoredFields, returns a JSON
     * with (1) the fields to ignore removed, (2) json fields with null or empty values
     * removed, and (3) standardize json property names.
     * @param jsonString
     * @param ignoredFields
     * @return String
     * @throws JsonProcessingException
     */
    private String standardizeAndFilterRequestSamplesJson(String jsonString, String[] ignoredFields,
            String comparisonType) throws JsonProcessingException {
        if (!jsonHasSamplesField(jsonString)) {
            return null;
        }
        JsonNode unfilteredJsonNode = mapper.readTree(jsonString);
        Map<String, JsonNode> unorderedSamplesMap = new HashMap<>();

        // iterate through array of sample json nodes and (1) standardize the json
        // props and (2) filter and remove null/empty values
        ArrayNode samplesArrayNode = (ArrayNode) unfilteredJsonNode.get("samples");
        Iterator<JsonNode> itr = samplesArrayNode.elements();
        while (itr.hasNext()) {
            JsonNode stdSampleNode = standardizeJsonProperties(
                    (ObjectNode) itr.next(), STD_IGO_SAMPLE_JSON_PROPS_MAP);
            JsonNode stdFilteredSampleNode = filterJsonNode((ObjectNode) stdSampleNode,
                    ignoredFields, comparisonType);

            String sid = findPrimaryIdFromJsonNode(stdFilteredSampleNode);
            unorderedSamplesMap.put(sid, stdFilteredSampleNode);
        }
        // order array of sample nodes by their primary id
        LinkedHashMap<String, JsonNode> orderedSamplesMap = new LinkedHashMap<>();
        unorderedSamplesMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(x -> orderedSamplesMap.put(x.getKey(), x.getValue()));

        // create ordered array of request sample json nodes and return as string
        ArrayNode sortedRequestSamplesArrayNode = mapper.createArrayNode();
        orderedSamplesMap.entrySet().forEach((entry) -> {
            sortedRequestSamplesArrayNode.add(entry.getValue());
        });
        return mapper.writeValueAsString(sortedRequestSamplesArrayNode);
    }

    private Boolean jsonHasQcAndOrLibrariesAndOrStatusFields(String jsonString)
            throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonString);
        return jsonNode.has("libraries") || jsonNode.has("qcReports") || jsonNode.has("status");
    }

    /**
     * Helper function to return a Boolean based on presence of 'samples' field in
     * the input JSON.
     * @param jsonString
     * @return Boolean
     * @throws JsonProcessingException
     */
    private Boolean jsonHasSamplesField(String jsonString) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonString);
        return jsonNode.has("samples");
    }

    /**
     * Returns Boolean based on results of JSONAssert.
     * @param referenceJson
     * @param targetJson
     * @return
     */
    private Boolean isMatchingJsons(String referenceJson, String targetJson) {
        if (referenceJson == null ? targetJson == null : referenceJson.equals(targetJson)) {
            return Boolean.TRUE;
        }

        try {
            JSONAssert.assertEquals(referenceJson, targetJson, JSONCompareMode.STRICT);
        } catch (JSONException e) {
            return Boolean.FALSE;
        } catch (AssertionError ae) {
            System.out.println(ae.getLocalizedMessage());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Returns a JsonNode with the given json properties matching 'ignoredFields'
     * removed from the node as well as any properties with null or empty values.
     * @param node
     * @param ignoredFields
     * @return JsonNode
     */
    private JsonNode filterJsonNode(ObjectNode node, String[] ignoredFields,
            String comparisonType) throws JsonProcessingException {
        List<String> fieldsToRemove = new ArrayList<>();
        List<String> igoUpdatesRejectedFields = Arrays.asList(IGO_ACCEPTED_FIELDS);

        // if ignored fields is not null then add to list of fields to remove
        if (ignoredFields != null) {
            fieldsToRemove.addAll(Arrays.asList(ignoredFields));
        }

        JsonNode modifiedQcReportsNode = null;
        JsonNode modifiedLibrariesNode = null;
        JsonNode modifiedStatusNode = null; // this is for special case handling
        // append list of fields to remove from node that contain null or empty values
        Iterator<String> itr = node.fieldNames();
        while (itr.hasNext()) {
            String field = itr.next();
            String value = node.get(field).asText();

            // special handling for metadata updates
            if (comparisonType.equals("igo")
                    && !igoUpdatesRejectedFields.contains(field)) {
                fieldsToRemove.add(field);
            }

            if (Strings.isNullOrEmpty(value) || value.equalsIgnoreCase("null")
                    || value.equalsIgnoreCase("[]")) {
                fieldsToRemove.add(field);
            } else if (field.equals("libraries")) {
                // special handling for libraries
                modifiedLibrariesNode = filterArrayNodeChildren(value, comparisonType);
            } else if (field.equals("qcReports")) {
                // special handling for qcReports
                modifiedQcReportsNode = filterArrayNodeChildren(value, comparisonType);
            } else if (field.equals("status")) {
                // special handling for status
                modifiedStatusNode = filterArrayNodeChildren(value, comparisonType);
            }
        }

        // remove compiled fields to remove list from input node
        // and return cleaned up node
        if (!fieldsToRemove.isEmpty()) {
            for (String field : fieldsToRemove) {
                node.remove(field);
            }
        }

        // update the modified libraries node if not null
        if (modifiedLibrariesNode != null) {
            node.remove("libraries");
            node.put("libraries", modifiedLibrariesNode);
        }
        if (modifiedQcReportsNode != null) {
            node.remove("qcReports");
            node.put("qcReports", modifiedQcReportsNode);
        }
        if (modifiedStatusNode != null) {
            node.remove("status");
            node.put("status", modifiedStatusNode);
        }
        return node;
    }

    /**
     * Given a parent node as a string, returns a filtered JsonNode.
     * This is special case handling specific to 'libraries' and other
     * child properties of samples that are actually array nodes
     * @param parentNode
     * @return JsonNode
     * @throws JsonProcessingException
     */
    private JsonNode filterArrayNodeChildren(String parentNode, String comparisonType)
            throws JsonProcessingException {
        List<Object> filteredParentNode = new ArrayList<>();
        List<Object> childrenObjects = mapper.readValue(parentNode, List.class);
        for (Object childObj : childrenObjects) {
            String childObjString = mapper.writeValueAsString(childObj);
            JsonNode filteredChildNode = filterJsonNode((ObjectNode)
                    mapper.readTree(childObjString), DEFAULT_IGNORED_FIELDS, comparisonType);
            filteredParentNode.add(filteredChildNode);
        }
        return mapper.readTree(mapper.writeValueAsString(filteredParentNode));
    }

    /**
     * Returns a JsonNode with standardized properties based on the provided jsonPropsMap.
     * @param node
     * @param jsonPropsMap
     * @return JsonNode
     */
    private JsonNode standardizeJsonProperties(ObjectNode node, Map<String, String> jsonPropsMap) {
        // append list of fields that need to be updated
        List<String> fieldsToUpdate = new ArrayList<>();

        Iterator<String> itr = node.fieldNames();
        while (itr.hasNext()) {
            String field = itr.next();
            if (jsonPropsMap.containsKey(field)) {
                fieldsToUpdate.add(field);
            }
        }
        // updating and removal needs to be done separately from iteration above
        // to avoid a java.util.ConcurrentModificationException
        for (String field : fieldsToUpdate) {
            String value = node.get(field).asText();
            String stdJsonProp = jsonPropsMap.get(field);
            node.put(stdJsonProp, value);
            node.remove(field);
        }
        return node;
    }
}
