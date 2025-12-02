package org.mskcc.smile.commons.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public enum SampleType {
    BLOCKS("Blocks"),
    BLOCKS_SLIDES("Blocks/Slides"),
    BLOOD("Blood"),
    BONE_MARROW_BIOPSY("Bone Marrow Biopsy"),
    BUCCAL_SWAB("Buccal Swab"),
    BUFFY_COAT("Buffy Coat"),
    CDNA("cDNA"),
    CDNA_LIBRARY("cDNA Library"),
    CELLS("Cells"),
    CFDNA("cfDNA"),
    CSF("CSF"),
    CURLS_PUNCHES("Curls/Punches"),
    DNA("DNA"),
    DNA_CDNA_LIBRARY("DNA/cDNA Library"),
    DNA_LIBRARY("DNA Library"),
    FINGERNAILS("Fingernails"),
    HMWDNA("hnwDNA"),
    NUCLEI("Nuclei"),
    OTHER("other"),
    PLASMA("Plasma"),
    POOLED_LIBRARY("Pooled Library"),
    RNA("RNA"),
    SALIVA("Saliva"),
    SLIDES("Slides"),
    TISSUE("Tissue"),
    UHMWDNA("uhmwDNA"),
    WHOLE_BLOOD("Whole Blood");
    //STOOL("Stool");

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
