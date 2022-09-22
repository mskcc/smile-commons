package org.mskcc.smile.commons;

/**
 *
 * @author ochoaa
 */
public interface JsonComparator {
    Boolean isConsistentUpdates(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields,
            Boolean isUpdateMetadata) throws Exception;
}
