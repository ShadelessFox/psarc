package sh.adelessfox.psarc.hashing;

import sh.adelessfox.psarc.util.Arrays;

import java.nio.ByteOrder;
import java.util.HexFormat;

public abstract sealed class HashCode {
    public static HashCode fromBytes(byte[] bytes) {
        return new BytesHashCode(bytes);
    }

    public static HashCode fromInt(int hash) {
        return new IntHashCode(hash);
    }

    public static HashCode fromLong(long hash) {
        return new LongHashCode(hash);
    }

    HashCode() {
    }

    public abstract byte[] asArray();

    public abstract int asInt();

    public abstract long asLong();

    public abstract int bits();

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashCode that && bits() == that.bits() && equalsSameBits(that);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    abstract boolean equalsSameBits(HashCode that);

    private static final class BytesHashCode extends HashCode {
        private final byte[] hash;

        BytesHashCode(byte[] hash) {
            this.hash = java.util.Arrays.copyOf(hash, hash.length);
        }

        @Override
        public byte[] asArray() {
            return java.util.Arrays.copyOf(hash, hash.length);
        }

        @Override
        public int asInt() {
            return Arrays.getInt(hash, 0, ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public long asLong() {
            return Arrays.getLong(hash, 0, ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public int bits() {
            return hash.length * Byte.SIZE;
        }

        @Override
        boolean equalsSameBits(HashCode that) {
            return java.util.Arrays.mismatch(asArray(), that.asArray()) == -1;
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(hash);
        }

        @Override
        public String toString() {
            return HexFormat.of().formatHex(hash);
        }
    }

    private static final class IntHashCode extends HashCode {
        private final int hash;

        IntHashCode(int hash) {
            this.hash = hash;
        }

        @Override
        public byte[] asArray() {
            byte[] bytes = new byte[4];
            bytes[0] = (byte) (hash >>> 24);
            bytes[1] = (byte) (hash >>> 16);
            bytes[2] = (byte) (hash >>> 8);
            bytes[3] = (byte) (hash);
            return bytes;
        }

        @Override
        public int asInt() {
            return hash;
        }

        @Override
        public long asLong() {
            throw new IllegalStateException("This hash code has only 32 bits; cannot be converted to a long");
        }

        @Override
        public int bits() {
            return Integer.SIZE;
        }

        @Override
        boolean equalsSameBits(HashCode that) {
            return hash == that.asInt();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(hash);
        }

        @Override
        public String toString() {
            return HexFormat.of().toHexDigits(hash);
        }
    }

    private static final class LongHashCode extends HashCode {
        private final long hash;

        LongHashCode(long hash) {
            this.hash = hash;
        }

        @Override
        public byte[] asArray() {
            byte[] bytes = new byte[8];
            bytes[0] = (byte) (hash >>> 56);
            bytes[1] = (byte) (hash >>> 48);
            bytes[2] = (byte) (hash >>> 40);
            bytes[3] = (byte) (hash >>> 32);
            bytes[4] = (byte) (hash >>> 24);
            bytes[5] = (byte) (hash >>> 16);
            bytes[6] = (byte) (hash >>> 8);
            bytes[7] = (byte) (hash);
            return bytes;
        }

        @Override
        public int asInt() {
            return (int) hash;
        }

        @Override
        public long asLong() {
            return hash;
        }

        @Override
        public int bits() {
            return Long.SIZE;
        }

        @Override
        boolean equalsSameBits(HashCode that) {
            return hash == that.asLong();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(hash);
        }

        @Override
        public String toString() {
            return HexFormat.of().toHexDigits(hash);
        }
    }
}
