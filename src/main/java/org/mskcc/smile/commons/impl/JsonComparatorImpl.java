package org.mskcc.smile.commons.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    public Boolean isConsistent(String referenceJson, String targetJson) throws Exception {
        return isConsistent(referenceJson, targetJson,  DEFAULT_IGNORED_FIELDS);
    }

    @Override
    public Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields)
            throws Exception {
        Boolean consistencyCheckStatus = Boolean.TRUE;

        // filter reference and target request jsons and compare
        String filteredReferenceJson = standardizeAndFilterRequestJson(referenceJson, ignoredFields);
        String filteredTargetJson = standardizeAndFilterRequestJson(targetJson, ignoredFields);
        if (!isMatchingJsons(filteredReferenceJson, filteredTargetJson)) {
            consistencyCheckStatus = Boolean.FALSE;
        }

        // filter reference and target sample list jsons and compare if applicable
        if (jsonHasSamplesField(referenceJson) || jsonHasSamplesField(targetJson)) {
            String filteredReferenceSamplesJson =
                    standardizeAndFilterRequestSamplesJson(referenceJson, ignoredFields);
            String filteredTargetSamplesJson =
                    standardizeAndFilterRequestSamplesJson(targetJson, ignoredFields);
            if (!isMatchingJsons(filteredReferenceSamplesJson, filteredTargetSamplesJson)) {
                consistencyCheckStatus = Boolean.FALSE;
            }
        }
        return consistencyCheckStatus;
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
    private String standardizeAndFilterRequestJson(String jsonString, String[] ignoredFields)
            throws JsonProcessingException {
        JsonNode unfilteredJsonNode = mapper.readTree(jsonString);
        JsonNode stdJsonNode = standardizeJsonProperties(
                (ObjectNode) unfilteredJsonNode, STD_IGO_REQUEST_JSON_PROPS_MAP);
        JsonNode stdFilteredJsonNode = filterJsonNode((ObjectNode) stdJsonNode, ignoredFields);
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
    private String standardizeAndFilterRequestSamplesJson(String jsonString, String[] ignoredFields)
            throws JsonProcessingException {
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
            JsonNode stdFilteredSampleNode = filterJsonNode((ObjectNode) stdSampleNode, ignoredFields);

            String sid = stdFilteredSampleNode.get("primaryId").toString();
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
    private JsonNode filterJsonNode(ObjectNode node, String[] ignoredFields) {
        List<String> fieldsToRemove = new ArrayList<>();
        // if ignored fields is not null then add to list of fields to remove
        if (ignoredFields != null) {
            fieldsToRemove.addAll(Arrays.asList(ignoredFields));
        }

        // append list of fields to remove from node that contain null or empty values
        Iterator<String> itr = node.fieldNames();
        while (itr.hasNext()) {
            String field = itr.next();
            String value = node.get(field).asText();
            if (Strings.isNullOrEmpty(value) || value.equalsIgnoreCase("null")) {
                fieldsToRemove.add(field);
            }
        }

        // remove compiled fields to remove list from input node
        // and return cleaned up node
        if (!fieldsToRemove.isEmpty()) {
            for (String field : fieldsToRemove) {
                node.remove(field);
            }
        }

        return node;
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
