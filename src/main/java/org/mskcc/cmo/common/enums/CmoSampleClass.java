package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public enum CmoSampleClass {
    ADJACENT_NORMAL("Adjacent Normal"),
    ADJACENT_TISSUE("Adjacent Tissue"),
    CELL_FREE("Cell free"),
    LOCAL_RECURRENCE("Local Recurrence"),
    METASTASIS("Metastasis"),
    NORMAL("Normal"),
    OTHER("Other"),
    PRIMARY("Primary"),
    RECURRENCE("Recurrence"),
    TUMOR("Tumor"),
    UNKNOWN_TUMOR("Unknown Tumor");

    private static final Map<String, CmoSampleClass> valueToCmoSampleClass = new HashMap<>();

    static {
        for (CmoSampleClass cmoSampleClass : values()) {
            valueToCmoSampleClass.put(cmoSampleClass.value, cmoSampleClass);
        }
    }

    private String value;

    CmoSampleClass(String value) {
        this.value = value;
    }

    /**
     * CmoSampleClass enum constructor.
     * @param value
     * @return
     */
    public static CmoSampleClass fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("CMO Sample Class is empty");
        }
        if (!valueToCmoSampleClass.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported CMO Sample Class: %s", value));
        }
        return valueToCmoSampleClass.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
