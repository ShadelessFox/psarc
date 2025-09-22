package sh.adelessfox.psarc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record FilePath(List<String> segments) implements Comparable<FilePath> {
    private static final String ANY_MARK = "*";
    private static final FilePath EMPTY = new FilePath(List.of());
    private static final FilePath ANY = new FilePath(List.of(ANY_MARK));

    public FilePath {
        segments = List.copyOf(segments);
    }

    public static FilePath of(String path, String separator) {
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("separator must not be empty");
        }
        String[] segments = path.split(separator);
        if (segments.length == 1 && segments[0].isEmpty()) {
            return EMPTY;
        } else {
            return new FilePath(List.of(segments));
        }
    }

    public static FilePath of() {
        return EMPTY;
    }

    public static FilePath any() {
        return ANY;
    }

    public FilePath resolve(FilePath other) {
        if (isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }
        List<String> result = new ArrayList<>(length() + other.length());
        result.addAll(segments);
        result.addAll(other.segments);
        return new FilePath(result);
    }

    public FilePath subpath(int beginIndex) {
        return subpath(beginIndex, length());
    }

    public FilePath subpath(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (beginIndex == endIndex) {
            return of();
        } else if (beginIndex == 0 && endIndex == length()) {
            return this;
        } else {
            return new FilePath(segments.subList(beginIndex, endIndex));
        }
    }

    public String get(int index) {
        return segments.get(index);
    }

    public String full() {
        return full("/");
    }

    public String full(String separator) {
        return String.join(separator, segments);
    }

    public String first() {
        return segments.getFirst();
    }

    public String last() {
        return segments.getLast();
    }

    public int length() {
        return segments.size();
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    @Override
    public String toString() {
        return full();
    }

    @Override
    public int compareTo(FilePath o) {
        int length = Math.min(segments.size(), o.segments.size());
        for (int i = 0; i < length; i++) {
            var a = segments.get(i);
            if (a.equals(ANY_MARK)) {
                return 1;
            }

            var b = o.segments.get(i);
            if (b.equals(ANY_MARK)) {
                return -1;
            }

            var value = a.compareTo(b);
            if (value != 0) {
                return value;
            }
        }

        return segments.size() - o.segments.size();
    }
}