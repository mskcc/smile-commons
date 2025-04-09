package org.mskcc.smile.commons.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

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
    DNA_CDNA_LIBRARY("DNA/cDNA Library"),
    OTHER("other");

    private static final Map<String, SampleType> valueToSampleType = new HashMap<>();

    static {
        for (SampleType sampleType : values()) {
            valueToSampleType.put(sampleType.value, sampleType);
        }
    }

    private final String value;

    SampleType(String value) {
        this.value = value;
    }

    /**
     * SampleType enum constructor.
     * @param value
     * @return
     */
    public static SampleType fromString(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Sample Type is empty");
        }
        if (!valueToSampleType.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported Sample Type: %s", value));
        }
        return valueToSampleType.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
