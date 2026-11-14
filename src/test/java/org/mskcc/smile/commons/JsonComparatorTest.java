package org.mskcc.smile.commons;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.commons.config.MockDataConfig;
import org.mskcc.smile.commons.model.MockJsonTestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author ochoaa
 */
@SpringBootTest(classes = MockDataConfig.class)
public class JsonComparatorTest {
    @Autowired
    private JsonComparator jsonComparator;

    private Map<String, String> requestJsonDataIdMap;

    @Autowired
    private void initRequestJsonDataIdMap() {
        this.requestJsonDataIdMap = new HashMap<>();
        requestJsonDataIdMap.put("mockIncomingRequest1JsonDataWith2T2N",
                "mockPublishedRequest1JsonDataWith2T2N");
        requestJsonDataIdMap.put("mockIncomingRequest2aJsonData1N",
                "mockPublishedRequest2aJsonData1N");
        requestJsonDataIdMap.put("mockIncomingRequest2bJsonDataMissing1N",
                "mockPublishedRequest2bJsonDataMissing1N");
        requestJsonDataIdMap.put("mockIncomingRequest3JsonDataPooledNormals",
                "mockPublishedRequest3JsonDataPooledNormals");
    }

    @Autowired
    private Map<String, MockJsonTestData> mockedJsonDataMap;

    /**
     * Simple test to assert failure when request ids are different.
     * @throws Exception
     */
    @Test
    public void testConsistencyCheckFailure() throws Exception {
        String referenceJson = "{\"requestId\":\"mockRequestId\"}";
        String targetJson = "{\"requestId\":\"differentRequestId\"}";
        Boolean isConsistent = jsonComparator.isConsistent(referenceJson, targetJson);
        Assertions.assertFalse(isConsistent);
    }

    /**
     * Tests to ensure the mocked request json data map is not null and
     * contains all id's from 'requestJsonDataIdMap'.
     */
    @Test
    public void testMockedRequestJsonDataLoading() {
        Assertions.assertNotNull(mockedJsonDataMap);

        for (Map.Entry<String, String> entry : requestJsonDataIdMap.entrySet()) {
            Assertions.assertTrue(mockedJsonDataMap.containsKey(entry.getKey()));
            Assertions.assertTrue(mockedJsonDataMap.containsKey(entry.getValue()));
        }
    }

    /**
     * Tests if the incoming request jsons are consistent with their corresponding
     * published request json counterparts.
     * @throws Exception
     */
    @Test
    public void testAllRequestJsonsForConsistency() throws Exception {
        Map<String, String> errorsMap = new HashMap<>();
        for (Map.Entry<String, String> entry : requestJsonDataIdMap.entrySet()) {
            String incomingRequestId = entry.getKey();
            String publishedRequestId = entry.getValue();
            MockJsonTestData incomingRequest = mockedJsonDataMap.get(incomingRequestId);
            MockJsonTestData publishedRequest = mockedJsonDataMap.get(publishedRequestId);

            try {
                Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                        incomingRequest.getJsonString(), publishedRequest.getJsonString());
                if (!consistencyCheckStatus) {
                    errorsMap.put(incomingRequestId,
                            "Request did not pass consistency check but no exception was caught.");
                }
            } catch (Exception e) {
                errorsMap.put(incomingRequestId, e.getMessage());
            }
        }
        // if any errors caught then print report and fail test
        if (!errorsMap.isEmpty()) {
            Assertions.fail(getErrorMessage(errorsMap));
        }
    }

    /**
     * Test for handling of null fields in the published request json but not the incoming request json.
     * @throws Exception
     */
    @Test
    public void testNullJsonFieldHandlingInPublishedRequest() throws Exception {
        MockJsonTestData incomingRequest =
                mockedJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        MockJsonTestData publishedRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonNullValues");
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                incomingRequest.getJsonString(), publishedRequest.getJsonString());
        Assertions.assertFalse(consistencyCheckStatus);
    }

    /**
     * Test for handling of fields that are a mix of null or empty strings in
     * the incoming json and published request json.
     * The consistency checker is expected to pass since the same fields in the
     * jsons being compared have been set to either a null or empty string even
     * though they are not exactly the same (i.e., null and empty strings are treated the same).
     */
    @Test
    public void testNullOrEmptyJsonFieldHandlingInIncomingAndPublishedRequest() throws Exception {
        MockJsonTestData incomingRequest =
                mockedJsonDataMap.get("mockIncomingRequest4JsonNullOrEmptyValues");
        MockJsonTestData publishedRequest =
                mockedJsonDataMap.get("mockPublishedRequest4JsonNullOrEmptyValues");
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                incomingRequest.getJsonString(), publishedRequest.getJsonString());
        Assertions.assertTrue(consistencyCheckStatus);
    }

    /**
     * Tests that 'sampleAliases' field added to the sample metadata gets ignored.
     * @throws Exception
     */
    @Test
    public void testPublishedSampleAliasesIgnored() throws Exception {
        MockJsonTestData incomingRequest =
                mockedJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        Assertions.assertFalse(incomingRequest.getJsonString().contains("sampleAliases"));
        MockJsonTestData publishedRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Assertions.assertTrue(publishedRequest.getJsonString().contains("sampleAliases"));
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                incomingRequest.getJsonString(), publishedRequest.getJsonString());
        Assertions.assertTrue(consistencyCheckStatus);
    }

    /**
     * Test if the comparator recognizes updates in libraries, qcReports and runs
     * @throws Exception
     */
    @Test
    public void testSubSampleMetadataJsonUpdates() throws Exception {
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWithLibUpdates");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assertions.assertFalse(consistencyCheckStatus);
    }

    /**
     * Test if the comparator recognizes consistent sampleMetadata
     * @throws Exception
     */
    @Test
    public void testUpdatesComparatorSampleMetadata() throws Exception {
        // create json with sample-level metadata and another with updates
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockUpdatedPublishedSampleMetadata");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockUpdatedPublishedSampleMetadataWithInvalidUpdates");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assertions.assertTrue(consistencyCheckStatus);
    }

    /**
     * Test if the comparator recognizes invalid runs updates in sampleMetadata
     * @throws Exception
     */
    @Test
    public void testInvalidRunsUpdatesComparatorSampleMetadata() throws Exception {
        // create json with sample-level metadata and another with updates
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockUpdatedPublishedSampleMetadata");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockUpdatedPublishedSampleMetadataWithInvalidRunsUpdates");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assertions.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testValidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWithLibUpdates");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assertions.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testInvalidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWithInvalidUpdates");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assertions.assertTrue(consistencyCheckStatus);

    }

    /**
     * Tests that sample has changes in IGO-specific properties.
     * @throws Exception
     */
    @Test public void testSampleHasIgoUpdates() throws Exception {
        MockJsonTestData refSample = mockedJsonDataMap.get("samplePreUpdate");
        MockJsonTestData igoUpdateSample = mockedJsonDataMap.get("sampleLimsUpdatesOnly");
        Boolean igoUpdatesOnlyCheck = jsonComparator.isConsistentByIgoProperties(
                refSample.getJsonString(), igoUpdateSample.getJsonString());
        Assertions.assertFalse(igoUpdatesOnlyCheck);
    }

    /**
     * Tests that comparator can identify changes in IGO-specific properties.
     * @throws Exception
     */
    @Test public void testSampleHasNonIgoUpdates() throws Exception {
        MockJsonTestData refSample = mockedJsonDataMap.get("samplePreUpdate");
        MockJsonTestData nonIgoUpdateSample = mockedJsonDataMap.get("sampleNonLimsUpdates");
        Boolean nonIgoUpdatesCheck2 = jsonComparator.isConsistentByIgoProperties(
                refSample.getJsonString(), nonIgoUpdateSample.getJsonString());
        Assertions.assertTrue(nonIgoUpdatesCheck2);
    }

    /**
     * Tests that the comparator can detect changes in general as well as IGO-specific properties.
     * The fixture changes cmoPatientId (Dashboard-owned) and cmoSampleIdFields.recipe (IGO-owned,
     * the source of genePanel), so both checks must report an update.
     * @throws Exception
     */
    @Test public void testSampleHasMixedIgoAndNonIgoUpdates() throws Exception {
        MockJsonTestData refSample = mockedJsonDataMap.get("samplePreUpdate");
        MockJsonTestData mixedUpdatesSample = mockedJsonDataMap.get("sampleMixedUpdates");
        Boolean mixedUpdatesSampleCheck1 = jsonComparator.isConsistent(
                refSample.getJsonString(), mixedUpdatesSample.getJsonString());
        Assertions.assertFalse(mixedUpdatesSampleCheck1);
        Boolean mixedUpdatesSampleCheck2 = jsonComparator.isConsistentByIgoProperties(
                refSample.getJsonString(), mixedUpdatesSample.getJsonString());
        Assertions.assertFalse(mixedUpdatesSampleCheck2);
    }

    /**
     * Tests that the comparator can detect changes in general as well as IGO-specific properties.
     * @throws Exception
     */
    @Test public void testCompareSampleStatus() throws Exception {
        MockJsonTestData refJson = mockedJsonDataMap.get("mockIncomingRequest1JsonWithSampleStatus");
        MockJsonTestData tarJson = mockedJsonDataMap.get("mockPublishingRequest1JsonWithSampleStatus");
        Boolean compareSampleStatus = jsonComparator.isConsistent(
                refJson.getJsonString(), tarJson.getJsonString());
        Assertions.assertTrue(compareSampleStatus);
    }

    /**
     * Tests that the comparator can detect changes in general as well as IGO-specific properties.
     * @throws Exception
     */
    @Test public void testCompareInvalidSampleStatus() throws Exception {
        MockJsonTestData refJson = mockedJsonDataMap.get(
                "mockIncomingRequest1JsonWithSampleStatus");
        MockJsonTestData tarJson = mockedJsonDataMap.get(
                "mockPublishingRequest1JsonInvalidSampleStatus");
        Boolean compareSampleStatus = jsonComparator.isConsistent(
                refJson.getJsonString(), tarJson.getJsonString());
        Assertions.assertFalse(compareSampleStatus);
    }

    /**
     * Tests that the comparator can detect changes in general as well as IGO-specific properties.
     * @throws Exception
     */
    @Test public void testCompareMissingSampleStatus() throws Exception {
        MockJsonTestData refJson = mockedJsonDataMap.get(
                "mockIncomingRequest1JsonWithSampleStatus");
        MockJsonTestData tarJson = mockedJsonDataMap.get(
                "mockPublishingRequest1JsonMissingSampleStatus");
        Boolean compareSampleStatus = jsonComparator.isConsistent(
                refJson.getJsonString(), tarJson.getJsonString());
        Assertions.assertFalse(compareSampleStatus);
    }

    /**
     * Tests that the comparator can detect changes in generic jsons that do not undergo
     * as much filtering and standardization as request jsons or sample jsons.
     * @throws Exception
     */
    @Test
    public void testUpdateCohortCompleteData() throws Exception {
        MockJsonTestData refJson = mockedJsonDataMap.get(
                "mockCohortCompleteCCSPPPQQQQ");
        MockJsonTestData tarJson = mockedJsonDataMap.get(
                "mockCohortCompleteCCSPPPQQQQUpdated");
        Boolean isConsistent = jsonComparator.isConsistentGenericComparison(refJson.getJsonString(),
                tarJson.getJsonString());
        Assertions.assertFalse(isConsistent);
    }

    /**
     * Tests that IGO-specific updates are recognized (ilabRequestId, deliveryDate) at the request level
     * as well as the sample level (sampleStatus).
     * @throws Exception
     */
    @Test public void testNewIgoSupportedFieldsComparison() throws Exception {
        MockJsonTestData igoSchema = mockedJsonDataMap.get(
                "mockIgoSchemaUpdatedRequest");
        MockJsonTestData universalSchema = mockedJsonDataMap.get(
                "mockPublishedIgoSchemaUpdatedRequest");

        // test new igo request/sample with universal schema from smile (deliveryDate v igoDeliveryDate)
        Assertions.assertTrue(jsonComparator.isConsistent(igoSchema.getJsonString(),
                universalSchema.getJsonString()));
        Assertions.assertTrue(jsonComparator.isConsistentByIgoProperties(igoSchema.getJsonString(),
                universalSchema.getJsonString()));

        // test that update to igoSampleStatus is recognized by both IGO and non-IGO consistency checks
        MockJsonTestData universalSchemaIgoUpdates = mockedJsonDataMap.get(
                "mockPublishedIgoSchemaUpdatedRequestWithIgoDataChanges");
        Assertions.assertFalse(jsonComparator.isConsistent(universalSchema.getJsonString(),
                universalSchemaIgoUpdates.getJsonString()));
        Assertions.assertFalse(jsonComparator.isConsistentByIgoProperties(universalSchema.getJsonString(),
                universalSchemaIgoUpdates.getJsonString()));

        // test that update to a non-IGO property is recognized but not for the IGO-specific consistency check
        MockJsonTestData universalSchemaNonIgoUpdates = mockedJsonDataMap.get(
                "mockPublishedIgoSchemaUpdatedRequestWithNonIgoDataChanges");
        Assertions.assertFalse(jsonComparator.isConsistent(universalSchema.getJsonString(),
                universalSchemaNonIgoUpdates.getJsonString()));
        Assertions.assertTrue(jsonComparator.isConsistentByIgoProperties(universalSchema.getJsonString(),
                universalSchemaNonIgoUpdates.getJsonString()));

        // test that updates to both IGO and non-IGO props are recognized by both types of consistency checks
        MockJsonTestData universalSchemaMixedUpdates = mockedJsonDataMap.get(
                "mockPublishedIgoSchemaUpdatedRequestWithMixedDataChanges");
        Assertions.assertFalse(jsonComparator.isConsistent(universalSchema.getJsonString(),
                universalSchemaMixedUpdates.getJsonString()));
        Assertions.assertFalse(jsonComparator.isConsistentByIgoProperties(universalSchema.getJsonString(),
                universalSchemaMixedUpdates.getJsonString()));
    }

    // IGO raw schema: igoId, top-level altid and sampleStatus string
    private static final String IGO_RAW_SAMPLE = "{\"igoId\":\"S1\",\"cmoPatientId\":\"C-IGO\","
            + "\"altid\":\"ALT-1\",\"sampleStatus\":\"PASSED\","
            + "\"cmoSampleIdFields\":{\"recipe\":\"WES\"},"
            + "\"libraries\":[{\"barcodeId\":\"B1\",\"runs\":[{\"runId\":\"R1\",\"fastqs\":[\"a.fq\"]}]}]}";
    // same sample in SMILE's universal schema, with a Project Manager cmoPatientId correction
    private static final String UNIVERSAL_SAMPLE = "{\"primaryId\":\"S1\",\"cmoPatientId\":\"C-PM\","
            + "\"genePanel\":\"WES\",\"cmoSampleIdFields\":{\"recipe\":\"WES\"},"
            + "\"additionalProperties\":{\"altId\":\"ALT-1\",\"igoSampleStatus\":\"PASSED\","
            + "\"isCmoSample\":\"true\"},"
            + "\"libraries\":[{\"barcodeId\":\"B1\",\"runs\":[{\"runId\":\"R1\",\"fastqs\":[\"a.fq\"]}]}]}";

    /**
     * IGO raw and universal schemas carrying the same IGO-owned data are consistent, and a
     * Dashboard edit to a non-IGO field (cmoPatientId) is not treated as an IGO update.
     */
    @Test
    public void testIgoRawVsUniversalSchemaIgnoresDashboardEdits() throws Exception {
        Assertions.assertTrue(jsonComparator.isConsistentByIgoProperties(
                IGO_RAW_SAMPLE, UNIVERSAL_SAMPLE));
        Assertions.assertFalse(jsonComparator.isConsistent(IGO_RAW_SAMPLE, UNIVERSAL_SAMPLE));
    }

    /**
     * ALT ID is top-level in IGO's schema and nested under additionalProperties in the universal
     * schema; a change from IGO must be detected.
     */
    @Test
    public void testIgoAltIdUpdateDetectedAcrossSchemas() throws Exception {
        String igoUpdate = IGO_RAW_SAMPLE.replace("ALT-1", "ALT-2");
        Assertions.assertFalse(jsonComparator.isConsistentByIgoProperties(igoUpdate, UNIVERSAL_SAMPLE));
    }

    /**
     * Fastq paths (nested under libraries[].runs[]) are IGO-owned; a change must be detected.
     */
    @Test
    public void testIgoFastqUpdateDetectedAcrossSchemas() throws Exception {
        String igoUpdate = IGO_RAW_SAMPLE.replace("a.fq", "b.fq");
        Assertions.assertFalse(jsonComparator.isConsistentByIgoProperties(igoUpdate, UNIVERSAL_SAMPLE));
    }

    /**
     * Raw IGO JSON never carries status (added by smile-request-filter/smile-label-generator), so
     * status present on only one side is not a difference; status present on both is compared.
     */
    @Test
    public void testPipelineStatusComparedOnlyWhenBothSidesHaveIt() throws Exception {
        String rawIgo = "{\"requestId\":\"R\",\"strand\":\"x\",\"samples\":[{\"igoId\":\"A\"}]}";
        String validated = "{\"igoRequestId\":\"R\",\"strand\":\"x\","
                + "\"status\":{\"validationStatus\":true},"
                + "\"samples\":[{\"primaryId\":\"A\",\"status\":{\"validationStatus\":true}}]}";
        Assertions.assertTrue(jsonComparator.isConsistent(rawIgo, validated));
        Assertions.assertTrue(jsonComparator.isConsistent(validated, rawIgo));
        String revalidated = validated.replaceFirst("true", "false");
        Assertions.assertFalse(jsonComparator.isConsistent(validated, revalidated));
    }

    /**
     * A serialized SmileRequest carries its samples as SmileSample objects under smileSampleList.
     * Samples are compared (by latest metadata, in primary id order), so list order and metadata
     * history do not matter but a change to a sample's current metadata does.
     */
    @Test
    public void testSmileRequestSamplesComparedByLatestMetadata() throws Exception {
        String existing = "{\"igoRequestId\":\"R\",\"smileSampleList\":["
                + "{\"smileSampleId\":\"u2\",\"latestSampleMetadata\":{\"primaryId\":\"B\",\"sex\":\"M\"},"
                + "\"sampleMetadataList\":[{\"primaryId\":\"B\",\"sex\":\"F\"},"
                + "{\"primaryId\":\"B\",\"sex\":\"M\"}]},"
                + "{\"smileSampleId\":\"u1\",\"latestSampleMetadata\":{\"primaryId\":\"A\",\"sex\":\"F\"},"
                + "\"sampleMetadataList\":[{\"primaryId\":\"A\",\"sex\":\"F\"}]}]}";
        String incoming = "{\"igoRequestId\":\"R\",\"smileSampleList\":["
                + "{\"latestSampleMetadata\":{\"primaryId\":\"A\",\"sex\":\"F\"},"
                + "\"sampleMetadataList\":[{\"primaryId\":\"A\",\"sex\":\"F\"}]},"
                + "{\"latestSampleMetadata\":{\"primaryId\":\"B\",\"sex\":\"M\"},"
                + "\"sampleMetadataList\":[{\"primaryId\":\"B\",\"sex\":\"M\"}]}]}";
        Assertions.assertTrue(jsonComparator.isConsistent(existing, incoming));

        String incomingSampleUpdate = incoming.replace(
                "\"latestSampleMetadata\":{\"primaryId\":\"B\",\"sex\":\"M\"}",
                "\"latestSampleMetadata\":{\"primaryId\":\"B\",\"sex\":\"F\"}");
        Assertions.assertNotEquals(incoming, incomingSampleUpdate);
        Assertions.assertFalse(jsonComparator.isConsistent(existing, incomingSampleUpdate));

        String incomingMissingSample = "{\"igoRequestId\":\"R\",\"smileSampleList\":["
                + "{\"latestSampleMetadata\":{\"primaryId\":\"A\",\"sex\":\"F\"}}]}";
        Assertions.assertFalse(jsonComparator.isConsistent(existing, incomingMissingSample));
    }

    /**
     * Mismatched sample lists are reported as inconsistent rather than throwing.
     */
    @Test
    public void testMismatchedSampleListsAreInconsistent() throws Exception {
        String twoSamples = "{\"requestId\":\"R\",\"samples\":[{\"igoId\":\"A\"},{\"igoId\":\"B\"}]}";
        String oneSample = "{\"requestId\":\"R\",\"samples\":[{\"igoId\":\"A\"}]}";
        String noSamples = "{\"requestId\":\"R\"}";
        Assertions.assertFalse(jsonComparator.isConsistent(twoSamples, oneSample));
        Assertions.assertFalse(jsonComparator.isConsistent(noSamples, oneSample));
        Assertions.assertFalse(jsonComparator.isConsistentByIgoProperties(twoSamples, oneSample));
    }

    private String getErrorMessage(Map<String, String> errorsMap) {
        StringBuilder builder = new StringBuilder();
        builder.append("\nConsistencyCheckerUtil failures summary:\n");
        for (Map.Entry<String, String> entry : errorsMap.entrySet()) {
            builder.append("\n\tRequest id: ")
                    .append(entry.getKey())
                    .append("\n")
                    .append(entry.getValue())
                    .append("\n");
        }
        return builder.toString();
    }
}
