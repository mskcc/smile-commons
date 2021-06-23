package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ochoaa
 */
public enum NucleicAcid {
    CFDNA("cfDNA"),
    DNA("DNA"),
    RNA("RNA"),
    DNA_AND_RNA("DNA and RNA");

    private static final Map<String, NucleicAcid> nameToEnum = new HashMap<>();

    static {
        for (NucleicAcid nucAcid : values()) {
            nameToEnum.put(nucAcid.value, nucAcid);
        }
    }

    private final String value;

    NucleicAcid(String value) {
        this.value = value;
    }

    /**
     * NucleicAcid enum constructor.
     * @param value
     * @return
     */
    public static NucleicAcid fromString(String value) {
        if (!nameToEnum.containsKey(value)) {
            throw new RuntimeException(String.format("Unsupported Nucleic Acid: %s", value));
        }
        return nameToEnum.get(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
