package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;
import static java.lang.String.format;

public enum SampleType {
    TISSUE("Tissue"),
    CELLS("Cells"),
    BLOCKS_SLIDES("Blocks/Slides"),
    BLOOD("Blood"),
    BUFFY_COAT("Buffy Coat"),
    RNA("RNA"),
    DNA("DNA"),
    CFDNA("cfDNA"),
    DNA_LIBRARY("DNA Library"),
    POOLED_LIBRARY("Pooled Library"),
    CDNA("cDNA"),
    CDNA_LIBRARY("cDNA Library"),
    OTHER("other");

    private static final Map<String, SampleType> nameToEnum = new HashMap<>();

    static {
        for (SampleType enumValue : values()) {
            nameToEnum.put(enumValue.name, enumValue);
        }
    }

    private final String name;

    SampleType(String name) {
        this.name = name;
    }

    public static SampleType fromString(String name) {
        if (!nameToEnum.containsKey(name))
            throw new RuntimeException(format("Unsupported %s: %s", SampleType.class.getName(), name));

        return nameToEnum.get(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
