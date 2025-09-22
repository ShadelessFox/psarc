package sh.adelessfox.psarc.archive.psarc;

import wtf.reversed.toolbox.collect.Bytes;
import wtf.reversed.toolbox.io.BinaryReader;

import java.io.IOException;
import java.nio.ByteBuffer;

public record PsarcEntry(
    Bytes hash,
    int blockOffset,
    long uncompressedSize,
    long fileOffset
) {
    public static final int BYTES = 30;

    public static PsarcEntry read(BinaryReader reader) throws IOException {
        var hash = Bytes.wrap(reader.readBytes(16));
        var blockOffset = reader.readInt();
        var uncompressedSize = Integer.toUnsignedLong(reader.readInt()) << 8 | reader.readByte() & 0xff;
        var fileOffset = Integer.toUnsignedLong(reader.readInt()) << 8 | reader.readByte() & 0xff;

        return new PsarcEntry(hash, blockOffset, uncompressedSize, fileOffset);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.allocate(BYTES)
            .put(hash.asBuffer())
            .putInt(blockOffset)
            .putInt((int) (uncompressedSize >>> 8)).put((byte) uncompressedSize)
            .putInt((int) (fileOffset >>> 8)).put((byte) fileOffset)
            .flip();
    }
}
