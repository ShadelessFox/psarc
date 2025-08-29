package sh.adelessfox.psarc.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class SequenceBinaryReader implements BinaryReader {
    private final ByteBuffer buffer = ByteBuffer.allocate(16384)
        .order(ByteOrder.LITTLE_ENDIAN)
        .limit(0);

    private final NavigableMap<Long, BinaryReader> readers;
    private final long size;
    private long position;
    private BinaryReader reader;

    SequenceBinaryReader(List<? extends BinaryReader> readers) {
        if (readers.isEmpty()) {
            throw new IllegalArgumentException("At least one reader must be provided");
        }

        this.readers = new TreeMap<>(Long::compareUnsigned);
        this.size = readers.stream().mapToLong(BinaryReader::size).sum();

        long offset = 0;
        for (BinaryReader reader : readers) {
            this.readers.put(offset, reader);
            offset += reader.size();
        }
    }

    @Override
    public byte readByte() throws IOException {
        refill(Byte.BYTES);
        return buffer.get();
    }

    @Override
    public short readShort() throws IOException {
        refill(Short.BYTES);
        return buffer.getShort();
    }

    @Override
    public int readInt() throws IOException {
        refill(Integer.BYTES);
        return buffer.getInt();
    }

    @Override
    public long readLong() throws IOException {
        refill(Long.BYTES);
        return buffer.getLong();
    }

    @Override
    public float readFloat() throws IOException {
        refill(Float.BYTES);
        return buffer.getFloat();
    }

    @Override
    public double readDouble() throws IOException {
        refill(Double.BYTES);
        return buffer.getDouble();
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);

        int remaining = buffer.remaining();

        // If we can read the entire array in one go, do so
        if (remaining >= len) {
            buffer.get(dst, off, len);
            return;
        }

        if (remaining > 0) {
            buffer.get(dst, off, remaining);
            off += remaining;
            len -= remaining;
        }

        // If we can fit the remaining bytes in the buffer, do a refill and read
        if (buffer.capacity() >= len) {
            refill();
            buffer.get(dst, off, len);
            return;
        }

        // If we can't fit the remaining bytes in the buffer, read directly into the destination
        long end = position + buffer.position() + len;
        if (end > size) {
            throw new EOFException();
        }

        read(ByteBuffer.wrap(dst, off, len));
        position = end;
        buffer.limit(0);
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public long position() {
        return position + buffer.position();
    }

    @Override
    public void position(long pos) throws IOException {
        Objects.checkIndex(pos, size + 1);

        var reader = peekReader(pos);
        if (reader == this.reader && pos >= position && pos < position + buffer.limit()) {
            buffer.position(Math.toIntExact(pos - position));
        } else {
            buffer.limit(0);
            position = pos;
            positionReader(pos);
        }
    }

    @Override
    public ByteOrder order() {
        return buffer.order();
    }

    @Override
    public BinaryReader order(ByteOrder order) {
        buffer.order(order);
        return this;
    }

    @Override
    public void close() throws IOException {
        for (BinaryReader reader : readers.values()) {
            reader.close();
        }
    }

    private void refill(int count) throws IOException {
        if (buffer.capacity() < count) {
            throw new IllegalArgumentException("Can't refill more bytes than the buffer can hold");
        }
        if (buffer.remaining() < count) {
            refill();
            if (buffer.remaining() < count) {
                throw new EOFException("Expected to read " + count + " bytes, but only " + buffer.remaining() + " bytes are available");
            }
        }
    }

    private void refill() throws IOException {
        long start = position();
        long end = Math.min(start + buffer.capacity(), size);

        position = start;
        buffer.compact();
        buffer.limit(Math.toIntExact(end - start));
        read(buffer);
        buffer.flip();
    }

    private void read(ByteBuffer dst) throws IOException {
        long position = position();
        while (dst.hasRemaining()) {
            if (reader == null || reader.remaining() == 0) {
                positionReader(position);
            }
            if (reader.remaining() == 0) {
                throw new EOFException();
            }
            int read = Math.min(Math.toIntExact(reader.remaining()), dst.remaining());
            reader.readBytes(dst.array(), dst.position(), read);
            dst.position(dst.position() + read);
            position += read;
        }
    }

    private void positionReader(long position) throws IOException {
        var entry = readers.floorEntry(position);
        var base = (long) entry.getKey();
        reader = entry.getValue();
        reader.position(position - base);
    }

    private BinaryReader peekReader(long position) {
        return readers.floorEntry(position).getValue();
    }
}
