package sh.adelessfox.psarc.util.type;

public final class FileSize extends Amount.OfLong<FileSize> {
    private static final FileSize zero = new FileSize(0);

    private FileSize(long value) {
        super(value);
    }

    public static FileSize zero() {
        return zero;
    }

    public static FileSize ofBytes(long b) {
        if (b < 0) {
            throw new IllegalArgumentException("size must be positive: " + b);
        }
        if (b == 0) {
            return zero;
        }
        return new FileSize(b);
    }

    public static FileSize ofMegabytes(long mb) {
        return ofBytes(mb * 1024);
    }

    public static FileSize ofGigabytes(long gb) {
        return ofBytes(gb * 1024 * 1024);
    }

    @Override
    protected FileSize newInstance(long value) {
        return ofBytes(value);
    }

    @Override
    public String toString() {
        if (value < 1024) {
            return value + " B";
        } else if (value < 1024 * 1024) {
            return String.format("%.2f kB", value / (1024.0));
        } else if (value < 1024 * 1024 * 1024) {
            return String.format("%.2f mB", value / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f gB", value / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
