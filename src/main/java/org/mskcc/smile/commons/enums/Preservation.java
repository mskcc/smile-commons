package org.mskcc.smile.commons.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public enum Preservation {
    FROZEN("Frozen"),
    FFPE("FFPE"),
    EDTA_STRECK("EDTA-Streck"),
    FRESH("Fresh"),
    OCT("OCT"),
    TRIZOL("Trizol"),
    DMSO_VIABLYFROZEN("DMSO-ViablyFrozen");

    private static final Map<String, Preservation> nameToPreservation = new HashMap<>();

    static {
        for (Preservation preservation : values()) {
            nameToPreservation.put(preservation.value, preservation);
        }
    }

    private final String value;

    Preservation(String value) {
        this.value = value;
    }

    /**
     * Preservation enum constructor.
     * @param value
     * @return
     */
    public static Preservation fromString(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Preservation type is empty");
        }
        if (!nameToPreservation.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported Preservation type: %s", value));
        }
        return nameToPreservation.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
