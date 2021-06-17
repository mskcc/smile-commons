package org.mskcc.cmo.common.enums;

import java.util.HashMap;
import java.util.Map;
import static java.lang.String.format;

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
        for (NucleicAcid enumValue : values()) {
            nameToEnum.put(enumValue.name, enumValue);
        }
    }

    private final String name;

    NucleicAcid(String name) {
        this.name = name;
    }

    public static NucleicAcid fromString(String name) {
        if (!nameToEnum.containsKey(name))
            throw new RuntimeException(format("Unsupported %s: %s", NucleicAcid.class.getName(), name));

        return nameToEnum.get(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
