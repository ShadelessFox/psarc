package sh.adelessfox.psarc.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * A generic source of data.
 * <p>
 * Offers <i>read</i> methods that read values of primitive types,
 * translating them from sequences of bytes in a particular byte
 * order specified by {@link #order()}.
 */
public interface BinaryReader extends Closeable {
    @FunctionalInterface
    interface Mapper<T> {
        T read(BinaryReader reader) throws IOException;
    }

    static BinaryReader open(Path path) throws IOException {
        return new ChannelBinaryReader(Files.newByteChannel(path, StandardOpenOption.READ));
    }

    static BinaryReader of(List<? extends BinaryReader> readers) {
        if (readers.isEmpty()) {
            throw new IllegalArgumentException("At least one reader must be provided");
        }
        return new SequenceBinaryReader(readers);
    }

    byte readByte() throws IOException;

    void readBytes(byte[] dst, int off, int len) throws IOException;

    default byte[] readBytes(int count) throws IOException {
        var dst = new byte[count];
        readBytes(dst, 0, count);
        return dst;
    }

    short readShort() throws IOException;

    default void readShorts(short[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readShort();
        }
    }

    default short[] readShorts(int count) throws IOException {
        var dst = new short[count];
        readShorts(dst, 0, count);
        return dst;
    }

    int readInt() throws IOException;

    default void readInts(int[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readInt();
        }
    }

    default int[] readInts(int count) throws IOException {
        var dst = new int[count];
        readInts(dst, 0, count);
        return dst;
    }

    long readLong() throws IOException;

    default void readLongs(long[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readLong();
        }
    }

    default long[] readLongs(int count) throws IOException {
        var dst = new long[count];
        readLongs(dst, 0, count);
        return dst;
    }

    float readFloat() throws IOException;

    default void readFloats(float[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readFloat();
        }
    }

    default float[] readFloats(int count) throws IOException {
        var dst = new float[count];
        readFloats(dst, 0, count);
        return dst;
    }

    double readDouble() throws IOException;

    default void readDoubles(double[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readDouble();
        }
    }

    default double[] readDoubles(int count) throws IOException {
        var dst = new double[count];
        readDoubles(dst, 0, count);
        return dst;
    }

    default String readString(int length, Charset charset) throws IOException {
        return new String(readBytes(length), charset);
    }

    default String readString(int length) throws IOException {
        return readString(length, StandardCharsets.UTF_8);
    }

    default boolean readByteBoolean() throws IOException {
        var value = readByte();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IOException("Invalid boolean value: " + value);
        };
    }

    default boolean readIntBoolean() throws IOException {
        var value = readInt();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IOException("Invalid boolean value: " + value);
        };
    }

    default <T> T[] readObjects(int count, Mapper<T> mapper, IntFunction<T[]> creator) throws IOException {
        var dst = creator.apply(count);
        for (int i = 0; i < count; i++) {
            dst[i] = mapper.read(this);
        }
        return dst;
    }

    default <T> List<T> readObjects(int count, Mapper<T> mapper) throws IOException {
        var dst = new ArrayList<T>(count);
        for (int i = 0; i < count; i++) {
            dst.add(mapper.read(this));
        }
        return List.copyOf(dst);
    }

    long size();

    long position();

    void position(long position) throws IOException;

    ByteOrder order();

    BinaryReader order(ByteOrder order);

    default long remaining() {
        return size() - position();
    }

    default boolean isDrained() {
        return remaining() == 0;
    }
}
