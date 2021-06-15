package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

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
        for (Preservation enumValue : values()) {
            nameToPreservation.put(enumValue.name, enumValue);
        }
    }

    private final String name;

    Preservation(String name) {
        this.name = name;
    }

    public static Preservation fromString(String name) {
        if (!nameToPreservation.containsKey(name))
            throw new RuntimeException(format("Unsupported %s: %s", Preservation.class.getName(), name));

        return nameToPreservation.get(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
