package sh.adelessfox.psarc.util.type;

public final class FileSize extends Amount.OfLong<FileSize> {
    public static final FileSize ZERO = new FileSize(0);

    private FileSize(long value) {
        super(value);
    }

    public static FileSize ofBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("size must be positive: " + bytes);
        }
        if (bytes == 0) {
            return ZERO;
        }
        return new FileSize(bytes);
    }

    public long toBytes() {
        return value;
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
            return String.format("%.2f KiB", value / (1024.0));
        } else if (value < 1024 * 1024 * 1024) {
            return String.format("%.2f MiB", value / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GiB", value / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
