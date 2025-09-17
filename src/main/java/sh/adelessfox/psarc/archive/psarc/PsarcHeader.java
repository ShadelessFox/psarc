package sh.adelessfox.psarc.archive.psarc;

import sh.adelessfox.psarc.io.BinaryReader;
import sh.adelessfox.psarc.util.type.FourCC;

import java.io.IOException;
import java.nio.ByteBuffer;

public record PsarcHeader(
    FourCC magic,
    short major,
    short minor,
    FourCC compression,
    int tocSize,
    int tocEntrySize,
    int tocEntries,
    int blockSize,
    int flags
) {
    public static final FourCC PSAR = FourCC.of("PSAR");
    public static final int BYTES = 32;

    public static final int FLAGS_IGNORE_CASE = 0x01;
    public static final int FLAGS_ABSOLUTE = 0x02;

    public static PsarcHeader read(BinaryReader reader) throws IOException {
        var magic = FourCC.of(reader.readInt());
        var major = reader.readShort();
        var minor = reader.readShort();
        var compression = FourCC.of(reader.readInt());
        var tocSize = reader.readInt();
        var tocEntrySize = reader.readInt();
        var tocEntries = reader.readInt();
        var blockSize = reader.readInt();
        var flags = reader.readInt();

        if (!magic.equals(PSAR)) {
            throw new IOException("Magic expected to be " + PSAR + ", was " + magic);
        }

        if (major != 1) {
            throw new IOException("Unsupported major version: " + major);
        }

        if (minor != 4 && minor != 3) {
            throw new IOException("Unsupported minor version: " + minor);
        }

        if (tocEntrySize != PsarcEntry.BYTES) {
            throw new IOException("TOC entry size expected to be " + PsarcEntry.BYTES + ", was " + tocEntrySize);
        }

        return new PsarcHeader(magic, major, minor, compression, tocSize, tocEntrySize, tocEntries, blockSize, flags);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.allocate(32)
            .putInt(magic.value())
            .putShort(major)
            .putShort(minor)
            .putInt(compression.value())
            .putInt(tocSize)
            .putInt(tocEntrySize)
            .putInt(tocEntries)
            .putInt(blockSize)
            .putInt(flags)
            .flip();
    }

    public boolean isIgnoreCase() {
        return (flags & FLAGS_IGNORE_CASE) == FLAGS_IGNORE_CASE;
    }

    public boolean isAbsolute() {
        return (flags & FLAGS_ABSOLUTE) == FLAGS_ABSOLUTE;
    }
}
