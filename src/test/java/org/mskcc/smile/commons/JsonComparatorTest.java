package org.mskcc.smile.commons;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mskcc.smile.commons.config.MockDataConfig;
import org.mskcc.smile.commons.model.MockJsonTestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author ochoaa
 */
@ContextConfiguration(classes = MockDataConfig.class)
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
    private Map<String, MockJsonTestData> mockedRequestJsonDataMap;

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
        Assert.assertNotNull(mockedRequestJsonDataMap);

        for (Map.Entry<String, String> entry : requestJsonDataIdMap.entrySet()) {
            Assert.assertTrue(mockedRequestJsonDataMap.containsKey(entry.getKey()));
            Assert.assertTrue(mockedRequestJsonDataMap.containsKey(entry.getValue()));
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
            MockJsonTestData incomingRequest = mockedRequestJsonDataMap.get(incomingRequestId);
            MockJsonTestData publishedRequest = mockedRequestJsonDataMap.get(publishedRequestId);

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
                mockedRequestJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        MockJsonTestData publishedRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonNullValues");
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
                mockedRequestJsonDataMap.get("mockIncomingRequest4JsonNullOrEmptyValues");
        MockJsonTestData publishedRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest4JsonNullOrEmptyValues");
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
                mockedRequestJsonDataMap.get("mockIncomingRequest1JsonDataWith2T2N");
        Assert.assertFalse(incomingRequest.getJsonString().contains("sampleAliases"));
        MockJsonTestData publishedRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
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
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWithLibUpdates");
        MockJsonTestData targetRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistent(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testUpdatesComparatorRequestMetadata() throws Exception {
        // create json with request-level metadata and another with updates
        MockJsonTestData referenceRequest =
                mockedRequestJsonDataMap.get("mockUpdatedPublishedRequest1Metadata");
        MockJsonTestData targetRequest =
                mockedRequestJsonDataMap.get("mockUpdatedPublishedRequest1MetadataWithInvalidUpdates");
        Boolean igoConsistencyCheckStatus = jsonComparator.isConsistentIgoUpdates(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Boolean dashboardConsistencyCheckStatus = jsonComparator.isConsistentDashboardUpdates(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertTrue(igoConsistencyCheckStatus);
        Assert.assertFalse(dashboardConsistencyCheckStatus);
    }

    @Test
    public void testUpdatesComparatorSampleMetadata() throws Exception {
        // create json with sample-level metadata and another with updates
        MockJsonTestData referenceRequest =
                mockedRequestJsonDataMap.get("mockUpdatedPublishedSampleMetadata");
        MockJsonTestData targetRequest =
                mockedRequestJsonDataMap.get("mockUpdatedPublishedSampleMetadataWithInvalidUpdates");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentIgoUpdates(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertTrue(consistencyCheckStatus);
    }

    @Test
    public void testValidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWithLibUpdates");
        MockJsonTestData targetRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentIgoUpdates(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertFalse(consistencyCheckStatus);
    }

    @Test
    public void testInvalidUpdatesComparatorWholeRequest() throws Exception {
        MockJsonTestData referenceRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWithInvalidUpdates");
        MockJsonTestData targetRequest =
                mockedRequestJsonDataMap.get("mockPublishedRequest1JsonDataWith2T2N");
        Boolean consistencyCheckStatus = jsonComparator.isConsistentIgoUpdates(
                referenceRequest.getJsonString(), targetRequest.getJsonString());
        Assert.assertTrue(consistencyCheckStatus);

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
