package sh.adelessfox.psarc.util.type;

import java.text.NumberFormat;

public final class FileCount extends Amount.OfLong<FileCount> {
    public static final FileCount ZERO = new FileCount(0);

    private FileCount(long value) {
        super(value);
    }

    public static FileCount of(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }
        if (count == 0) {
            return ZERO;
        }
        return new FileCount(count);
    }

    @Override
    protected FileCount newInstance(long value) {
        return of(value);
    }

    @Override
    public String toString() {
        if (value == 0) {
            return "no files";
        } else if (value == 1) {
            return "one file";
        } else {
            return NumberFormat.getIntegerInstance().format(value) + " files";
        }
    }
}
