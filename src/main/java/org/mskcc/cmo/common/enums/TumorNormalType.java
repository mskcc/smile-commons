package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;

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

    public static TumorNormalType getByValue(String value) {
        if(!valueToTumorNormalType.containsKey(value))
            throw new UnsupportedTumorNormalTypeException(String.format("Unsupported Tumor/Normal type: %s", value));
        return valueToTumorNormalType.get(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public static class UnsupportedTumorNormalTypeException extends RuntimeException {
        public UnsupportedTumorNormalTypeException(String message) {
            super(message);
        }
    }
}
