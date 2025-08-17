package sh.adelessfox.psarc.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.compression.Decompressor;
import sh.adelessfox.psarc.hashing.HashCode;
import sh.adelessfox.psarc.hashing.HashFunction;
import sh.adelessfox.psarc.io.BinaryReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class PsarcArchive implements Archive<PsarcAssetId, PsarcAsset> {
    private static final Logger log = LoggerFactory.getLogger(PsarcArchive.class);

    private static final int COMPRESSION_ZLIB = 'z' << 24 | 'l' << 16 | 'i' << 8 | 'b';

    private final BinaryReader reader;
    private final Decompressor decompressor;

    private final Header header;
    private final short[] sizes;
    private final SortedMap<PsarcAssetId, PsarcAsset> assets = new TreeMap<>();

    public PsarcArchive(Path path, ByteOrder order) throws IOException {
        this.reader = BinaryReader.open(path).order(order);
        this.header = Header.read(reader);
        this.decompressor = switch (header.compression()) {
            case COMPRESSION_ZLIB -> Decompressor.deflate();
            default ->
                throw new IOException("Unsupported PSARC compression type: %08x".formatted(header.compression()));
        };

        var entries = reader.readObjects(header.tocEntries, Entry::read);
        this.sizes = reader.readShorts(header.tocSize - Header.BYTES - Entry.BYTES * header.tocEntries >> 1);

        var manifest = readManifest(entries.getFirst());
        var names = new HashMap<HashCode, String>();

        for (String name : manifest) {
            var key = header.isIgnoreCase() ? name.toUpperCase(Locale.ROOT) : name;
            var hash = HashFunction.md5().hash(key);
            names.put(hash, name);
        }

        for (int i = 1; i < entries.size(); i++) {
            var entry = entries.get(i);
            var hash = HashCode.fromBytes(entry.hash);
            var name = names.get(hash);
            if (name == null) {
                log.warn("Missing name for hash: {}", hash);
                continue;
            }

            var transformedId = PsarcAssetId.of(header.isIgnoreCase() ? name.toUpperCase(Locale.ROOT) : name);
            var originalId = PsarcAssetId.of(name);
            var asset = new PsarcAsset(originalId, entry.blockOffset, entry.uncompressedSize, entry.fileOffset);
            assets.put(transformedId, asset);
        }
    }

    @Override
    public Optional<PsarcAsset> get(PsarcAssetId key) {
        return Optional.empty();
    }

    @Override
    public Collection<PsarcAsset> getAll() {
        return assets.values();
    }

    @Override
    public ByteBuffer read(PsarcAssetId key) throws IOException {
        throw new IOException();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private PsarcAssetId transformId(PsarcAssetId id) {
        return header.isIgnoreCase() ? PsarcAssetId.of(id.name().toUpperCase(Locale.ROOT)) : id;
    }

    private String[] readManifest(Entry entry) throws IOException {
        byte[] manifest = read(entry.uncompressedSize, entry.blockOffset, entry.fileOffset);
        return new String(manifest, StandardCharsets.UTF_8).split("\n");
    }

    private byte[] read(long uncompressedSize, int index, long blockOffset) throws IOException {
        var output = ByteBuffer.allocate(Math.toIntExact(uncompressedSize));
        var buffer = new byte[header.blockSize()];

        reader.position(blockOffset);

        while (output.hasRemaining()) {
            int size = Short.toUnsignedInt(sizes[index++]);
            if (size == 0) {
                reader.readBytes(output.array(), output.position(), header.blockSize());
                output.position(output.position() + header.blockSize());
            } else if (size == uncompressedSize || size == output.remaining()) {
                reader.readBytes(output.array(), output.position(), size);
                output.position(output.position() + size);
            } else {
                reader.readBytes(buffer, 0, size);
                decompressor.decompress(ByteBuffer.wrap(buffer, 0, size), output);
            }
        }

        return output.array();
    }

    private record Header(
        int magic,
        short major,
        short minor,
        int compression,
        int tocSize,
        int tocEntrySize,
        int tocEntries,
        int blockSize,
        int flags
    ) {
        static final int MAGIC = 'P' << 24 | 'S' << 16 | 'A' << 8 | 'R';
        static final int BYTES = 32;

        private static final int FLAGS_IGNORE_CASE = 0x01;
        private static final int FLAGS_ABSOLUTE = 0x02;

        static Header read(BinaryReader reader) throws IOException {
            var magic = reader.readInt();
            var major = reader.readShort();
            var minor = reader.readShort();
            var compression = reader.readInt();
            var tocSize = reader.readInt();
            var tocEntrySize = reader.readInt();
            var tocEntries = reader.readInt();
            var blockSize = reader.readInt();
            var flags = reader.readInt();

            if (magic != MAGIC) {
                throw new IOException("Magic expected to be " + MAGIC + ", was " + magic);
            }

            if (major != 1) {
                throw new IOException("Unsupported major version: " + major);
            }

            if (minor != 4 && minor != 3) {
                throw new IOException("Unsupported minor version: " + minor);
            }

            if (tocEntrySize != Entry.BYTES) {
                throw new IOException("TOC entry size expected to be " + Entry.BYTES + ", was " + tocEntrySize);
            }

            return new Header(magic, major, minor, compression, tocSize, tocEntrySize, tocEntries, blockSize, flags);
        }

        boolean isIgnoreCase() {
            return (flags & FLAGS_IGNORE_CASE) == FLAGS_IGNORE_CASE;
        }

        boolean isAbsolute() {
            return (flags & FLAGS_ABSOLUTE) == FLAGS_ABSOLUTE;
        }
    }

    private record Entry(
        byte[] hash,
        int blockOffset,
        long uncompressedSize,
        long fileOffset
    ) {
        static final int BYTES = 30;

        static Entry read(BinaryReader reader) throws IOException {
            var hash = reader.readBytes(16);
            var blockOffset = reader.readInt();
            var uncompressedSize = Integer.toUnsignedLong(reader.readInt()) << 8 | reader.readByte() & 0xff;
            var fileOffset = Integer.toUnsignedLong(reader.readInt()) << 8 | reader.readByte() & 0xff;

            return new Entry(hash, blockOffset, uncompressedSize, fileOffset);
        }
    }
}
