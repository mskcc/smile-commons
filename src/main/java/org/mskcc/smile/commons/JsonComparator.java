package org.mskcc.smile.commons;

/**
 *
 * @author ochoaa
 */
public interface JsonComparator {
    Boolean isConsistent(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields) throws Exception;
}
