package sh.adelessfox.psarc.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

final class SequenceBinaryReader extends BufferedBinaryReader {
    private final NavigableMap<Long, BinaryReader> readers = new TreeMap<>(Long::compareUnsigned);
    private BinaryReader reader;

    SequenceBinaryReader(List<? extends BinaryReader> readers) {
        super(readers.stream().mapToLong(BinaryReader::size).sum());
        long offset = 0;
        for (BinaryReader reader : readers) {
            this.readers.put(offset, reader);
            offset += reader.size();
        }
    }

    @Override
    protected void readImpl(ByteBuffer dst) throws IOException {
        long position = position();
        while (dst.hasRemaining()) {
            if (reader == null || reader.isDrained()) {
                positionImpl(position);
            }
            if (reader.isDrained()) {
                throw new EOFException();
            }
            int read = Math.min(Math.toIntExact(reader.remaining()), dst.remaining());
            reader.readBytes(dst.array(), dst.position(), read);
            dst.position(dst.position() + read);
            position += read;
        }
    }

    @Override
    protected void positionImpl(long position) throws IOException {
        var entry = readers.floorEntry(position);
        var base = (long) entry.getKey();
        reader = entry.getValue();
        reader.position(position - base);
    }

    @Override
    public void close() throws IOException {
        for (BinaryReader reader : readers.values()) {
            reader.close();
        }
    }
}
