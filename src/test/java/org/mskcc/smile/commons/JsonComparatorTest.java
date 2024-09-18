package org.mskcc.smile.commons;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mskcc.smile.commons.config.MockDataConfig;
import org.mskcc.smile.commons.model.MockJsonTestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@SpringBootTest(classes = MockDataConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
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
        Assert.assertFalse(isConsistent);
    }

    /**
     * Tests to ensure the mocked request json data map is not null and
     * contains all id's from 'requestJsonDataIdMap'.
     */
    @Test
    public void testMockedRequestJsonDataLoading() {
        Assert.assertNotNull(mockedJsonDataMap);

        for (Map.Entry<String, String> entry : requestJsonDataIdMap.entrySet()) {
            Assert.assertTrue(mockedJsonDataMap.containsKey(entry.getKey()));
            Assert.assertTrue(mockedJsonDataMap.containsKey(entry.getValue()));
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
            Assert.fail(getErrorMessage(errorsMap));
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
        Assert.assertFalse(consistencyCheckStatus);
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
        Assert.assertTrue(consistencyCheckStatus);
    }

    /**
     * Tests that 'sampleAliases' field added to the sample metadata gets ignored.
     * @throws Exception
     */
    @Test
    public void testPublishedSampleAliasesIgnored() throws Exception {
        MockJsonTestData incomingRequest =
                mockedJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        Assert.assertFalse(incomingRequest.getJsonString().contains("sampleAliases"));
        MockJsonTestData publishedRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Assert.assertTrue(publishedRequest.getJsonString().contains("sampleAliases"));
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                incomingRequest.getJsonString(), publishedRequest.getJsonString());
        Assert.assertTrue(consistencyCheckStatus);
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
        Assert.assertFalse(consistencyCheckStatus);
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
        Assert.assertTrue(consistencyCheckStatus);
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
        Assert.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testValidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWithLibUpdates");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testInvalidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWithInvalidUpdates");
        MockJsonTestData targetRequest =
                mockedJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentByIgoProperties(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertTrue(consistencyCheckStatus);

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
        Assert.assertFalse(igoUpdatesOnlyCheck);
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
        Assert.assertTrue(nonIgoUpdatesCheck2);
    }

    /**
     * Tests that the comparator can detect changes in general as well as IGO-specific properties.
     * @throws Exception
     */
    @Test public void testSampleHasMixedIgoAndNonIgoUpdates() throws Exception {
        MockJsonTestData refSample = mockedJsonDataMap.get("samplePreUpdate");
        MockJsonTestData mixedUpdatesSample = mockedJsonDataMap.get("sampleMixedUpdates");
        Boolean mixedUpdatesSampleCheck1 = jsonComparator.isConsistent(
                refSample.getJsonString(), mixedUpdatesSample.getJsonString());
        Assert.assertFalse(mixedUpdatesSampleCheck1);
        Boolean mixedUpdatesSampleCheck2 = jsonComparator.isConsistentByIgoProperties(
                refSample.getJsonString(), mixedUpdatesSample.getJsonString());
        Assert.assertTrue(mixedUpdatesSampleCheck2);
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
        Assert.assertTrue(compareSampleStatus);
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
        Assert.assertFalse(compareSampleStatus);
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
        Assert.assertFalse(compareSampleStatus);
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
        Assert.assertFalse(isConsistent);
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
