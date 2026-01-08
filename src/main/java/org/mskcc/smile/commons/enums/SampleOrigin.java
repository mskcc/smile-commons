package org.mskcc.smile.commons.enums;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public enum SampleOrigin {
    BLOCK("Block"),
    BONE_MARROW_ASPIRATE("Bone Marrow Aspirate"),
    BUCCAL_SWAB("Buccal Swab"),
    BUFFY_COAT("Buffy Coat"),
    CELLS("Cells"),
    CELL_PELLET("Cell Pellet"),
    CEREBROSPINAL_FLUID("Cerebrospinal Fluid"),
    CORE_BIOPSY("Core Biopsy"),
    CURLS("Curls"),
    CYTOSPIN("Cytospin"),
    FFPE("FFPE"),
    FINE_NEEDLE_ASPIRATE("Fine Needle Aspirate"),
    FINGERNAILS("Fingernails"),
    FRESH_FROZEN("Fresh or Frozen"),
    ORGANIOD("Organoid"),
    OTHER("Other"),
    PLASMA("Plasma"),
    PUNCH("Punch"),
    RAPID_AUTOPSY_TISSUE("Rapid Autopsy Tissue"),
    SALIVA("Saliva"),
    SLIDES("Slides"),
    SORTED_CELLS("Sorted Cells"),
    TISSUE("Tissue"),
    URINE("Urine"),
    VIABLY_FROZEN_CELLS("Viably Frozen Cells"),
    WHOLE_BLOOD("Whole Blood"),
    STOOL("Stool");

    private static final Map<String, SampleOrigin> valueToSampleOrigin = new HashMap<>();

    static {
        for (SampleOrigin sampleClass : values()) {
            valueToSampleOrigin.put(sampleClass.value, sampleClass);
        }
    }

    private String value;

    SampleOrigin(String value) {
        this.value = value;
    }

    /**
     * SampleOrigin enum constructor.
     * @param value
     * @return
     */
    public static SampleOrigin fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Sample Origin is empty");
        }
        if (!valueToSampleOrigin.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported Sample Origin: %s", value));
        }
        return valueToSampleOrigin.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
