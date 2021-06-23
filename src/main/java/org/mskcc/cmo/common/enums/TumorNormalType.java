package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public enum TumorNormalType {
    TUMOR("Tumor"),
    NORMAL("Normal");

    private static final Map<String, TumorNormalType> valueToTumorNormalType = new HashMap<>();

    static {
        for (TumorNormalType tumorNormalType : values()) {
            valueToTumorNormalType.put(tumorNormalType.value, tumorNormalType);
        }
    }

    private String value;

    TumorNormalType(String value) {
        this.value = value;
    }

    /**
     * TumorNormalType enum constructor.
     * @param value
     * @return
     */
    public static TumorNormalType getByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Tumor/Normal is empty");
        }
        if (!valueToTumorNormalType.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported Tumor/Normal type: %s", value));
        }
        return valueToTumorNormalType.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
