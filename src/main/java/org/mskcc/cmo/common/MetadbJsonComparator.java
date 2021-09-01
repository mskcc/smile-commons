package org.mskcc.cmo.common;

/**
 *
 * @author ochoaa
 */
public interface MetadbJsonComparator {
    Boolean isConsistent(String referenceJson, String targetJson) throws Exception;
    Boolean isConsistent(String referenceJson, String targetJson, String[] ignoredFields) throws Exception;
}
