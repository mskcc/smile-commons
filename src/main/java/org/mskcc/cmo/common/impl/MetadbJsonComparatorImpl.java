package org.mskcc.cmo.common.impl;

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
import org.mskcc.cmo.common.MetadbJsonComparator;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.stereotype.Component;

/**
 *
 * @author ochoaa
 */
@Component
public class MetadbJsonComparatorImpl implements MetadbJsonComparator {
    public final String[] DEFAULT_IGNORED_FIELDS = new String[]{
        "metaDbRequestId",
        "metaDbSampleId",
        "metaDbPatientId",
        "requestJson",
        "samples",
        "importDate"};
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Boolean isConsistent(String referenceJson, String targetJson) throws Exception {
        return isConsistent(referenceJson, targetJson,  DEFAULT_IGNORED_FIELDS);
    }

    @Override
    public Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields)
            throws Exception {
        Boolean consistencyCheckStatus = Boolean.TRUE;

        // filter reference and target request jsons and compare
        String filteredReferenceJson = getFilteredJsonString(referenceJson, ignoredFields);
        String filteredTargetJson = getFilteredJsonString(targetJson, ignoredFields);
        if (!isMatchingJsons(filteredReferenceJson, filteredTargetJson)) {
            consistencyCheckStatus = Boolean.FALSE;
        }

        // filter reference and target sample list jsons and compare if applicable
        if (jsonHasSamplesField(referenceJson) || jsonHasSamplesField(targetJson)) {
            String filteredReferenceSamplesJson =
                    getFilteredRequestSamplesJsonString(referenceJson, ignoredFields);
            String filteredTargetSamplesJson =
                    getFilteredRequestSamplesJsonString(targetJson, ignoredFields);
            if (!isMatchingJsons(filteredReferenceSamplesJson, filteredTargetSamplesJson)) {
                consistencyCheckStatus = Boolean.FALSE;
            }
        }
        return consistencyCheckStatus;
    }

    private String getFilteredJsonString(String jsonString, String[] ignoredFields)
            throws JsonProcessingException {
        JsonNode unfilteredJsonNode = mapper.readTree(jsonString);
        JsonNode filteredJsonNode = filterJsonNode((ObjectNode) unfilteredJsonNode, ignoredFields);
        return mapper.writeValueAsString(filteredJsonNode);
    }

    private String getFilteredRequestSamplesJsonString(String jsonString, String[] ignoredFields)
            throws JsonProcessingException {
        JsonNode unfilteredJsonNode = mapper.readTree(jsonString);
        if (!unfilteredJsonNode.has("samples")) {
            return null;
        }
        Map<String, JsonNode> unorderedSamplesMap = new HashMap<>();

        ArrayNode samplesArrayNode = (ArrayNode) unfilteredJsonNode.get("samples");
        Iterator<JsonNode> itr = samplesArrayNode.elements();
        while (itr.hasNext()) {
            JsonNode n = filterJsonNode((ObjectNode) itr.next(), ignoredFields);
            String sid = n.get("igoId").toString();
            unorderedSamplesMap.put(sid, n);
        }

        LinkedHashMap<String, JsonNode> orderedSamplesMap = new LinkedHashMap<>();
        unorderedSamplesMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(x -> orderedSamplesMap.put(x.getKey(), x.getValue()));

        ArrayNode sortedRequestSamplesArrayNode = mapper.createArrayNode();
        orderedSamplesMap.entrySet().forEach((entry) -> {
            sortedRequestSamplesArrayNode.add(entry.getValue());
        });
        return mapper.writeValueAsString(sortedRequestSamplesArrayNode);
    }

    private Boolean jsonHasSamplesField(String jsonString) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(jsonString);
        return jsonNode.has("samples");
    }

    private Boolean isMatchingJsons(String referenceJson, String targetJson) {
        if (referenceJson == null ? targetJson == null : referenceJson.equals(targetJson)) {
            return Boolean.TRUE;
        }

        try {
            JSONAssert.assertEquals(referenceJson, targetJson, JSONCompareMode.STRICT);
        } catch (JSONException e) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

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
}
