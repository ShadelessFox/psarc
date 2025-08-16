package sh.adelessfox.psarc.util;

import java.util.Arrays;
import java.util.Objects;

public record FilePath(String[] segments) implements Comparable<FilePath> {
    private static final FilePath empty = new FilePath(new String[0]);

    public static FilePath of(String... segments) {
        return new FilePath(segments);
    }

    public static FilePath of() {
        return empty;
    }

    public FilePath concat(String last) {
        var concat = Arrays.copyOf(segments, segments.length + 1);
        concat[segments.length] = last;
        return new FilePath(concat);
    }

    public FilePath subpath(int beginIndex) {
        return subpath(beginIndex, length());
    }

    public FilePath subpath(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (beginIndex == endIndex) {
            return empty;
        } else if (beginIndex == 0 && endIndex == length()) {
            return this;
        } else {
            return new FilePath(Arrays.copyOfRange(segments, beginIndex, endIndex));
        }
    }

    public String get(int index) {
        return segments[index];
    }

    public String full() {
        return full("/");
    }

    public String full(String separator) {
        return String.join(separator, segments);
    }

    public String first() {
        return segments[0];
    }

    public String last() {
        return segments[segments.length - 1];
    }

    public int length() {
        return segments.length;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof FilePath(String[] segments1) && Arrays.equals(segments, segments1);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(segments);
    }

    @Override
    public String toString() {
        return full();
    }

    @Override
    public int compareTo(FilePath o) {
        int length = Math.min(segments.length, o.segments.length);
        for (int i = 0; i < length; i++) {
            var a = segments[i];
            if (a.equals("*")) {
                return 1;
            }

            var b = o.segments[i];
            if (b.equals("*")) {
                return -1;
            }

            var value = a.compareTo(b);
            if (value != 0) {
                return value;
            }
        }

        return segments.length - o.segments.length;
    }
}