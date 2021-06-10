package org.mskcc.cmo.common.enums;

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;

public enum SpecimenType {
    BIOPSY("Biopsy"),
    RESECTION("Resection"),
    BLOOD("Blood"),
    CELLLINE("CellLine"),
    CFDNA("cfDNA"),
    EXOSOME("Exosome"),
    PDX("PDX"),
    XENOGRAFT("Xenograft"),
    XENOGRAFTDERIVEDCELLLINE("XenograftDerivedCellLine"),
    RAPIDAUTOPSY("RapidAutopsy"),
    FINGERNAILS("Fingernails"),
    ORGANOID("Organoid"),
    SALIVA("Saliva"),
    OTHER("other");

    private static final Map<String, SpecimenType> valueToSpecimenType = new HashMap<>();

    static {
        for (SpecimenType specimenType : values()) {
            valueToSpecimenType.put(specimenType.value, specimenType);
        }
    }

    private String value;

    SpecimenType(String value) {
        this.value = value;
    }

    public static SpecimenType fromValue(String value) {
        if (StringUtils.isEmpty(value))
            throw new IllegalArgumentException("Specimen Type (CMO Sample Type) is empty");
        if (!valueToSpecimenType.containsKey(value))
            throw new RuntimeException(String.format("Unsupported Specimen Type: %s", value));
        return valueToSpecimenType.get(value);
    }

    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        return value;
    }
}