package org.mskcc.smile.commons;

/**
 *
 * @author ochoaa
 */
public interface JsonComparator {
    Boolean isConsistentByIgoProperties(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields,
            String comparisonType) throws Exception;
}
