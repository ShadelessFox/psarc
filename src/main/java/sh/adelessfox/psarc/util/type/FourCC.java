package sh.adelessfox.psarc.util.type;

import java.nio.charset.StandardCharsets;

public record FourCC(int value) {
    public static FourCC of(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 4) {
            throw new IllegalArgumentException("value must be four ASCII characters long");
        }
        for (byte b : bytes) {
            if (b <= 0) {
                throw new IllegalArgumentException("found a non-ASCII character");
            }
        }
        return of(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    public static FourCC of(byte b1, byte b2, byte b3, byte b4) {
        return new FourCC(b1 << 24 | b2 << 16 | b3 << 8 | b4);
    }

    public static FourCC of(int value) {
        return new FourCC(value);
    }

    @Override
    public String toString() {
        byte[] b = new byte[4];
        b[0] = (byte) (value >> 24);
        b[1] = (byte) (value >> 16);
        b[2] = (byte) (value >> 8);
        b[3] = (byte) (value);
        return new String(b, StandardCharsets.ISO_8859_1);
    }
}
