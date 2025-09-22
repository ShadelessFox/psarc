package sh.adelessfox.psarc.archive.psarc;

import wtf.reversed.toolbox.type.FourCC;

import java.util.Optional;

public enum PsarcCompression {
    ZLIB("zlib"),
    LZMA("lzma"),
    OODLE("oodl");

    private final FourCC fourCC;

    PsarcCompression(String fourCC) {
        this.fourCC = FourCC.of(fourCC);
    }

    public static Optional<PsarcCompression> of(FourCC fourCC) {
        for (PsarcCompression value : values()) {
            if (value.fourCC.equals(fourCC)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public FourCC fourCC() {
        return fourCC;
    }
}
