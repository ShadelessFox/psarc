package sh.adelessfox.psarc.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.util.Filenames;
import wtf.reversed.toolbox.compression.Decompressor;
import wtf.reversed.toolbox.hashing.HashCode;
import wtf.reversed.toolbox.hashing.HashFunction;
import wtf.reversed.toolbox.io.BinaryReader;
import wtf.reversed.toolbox.type.FourCC;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PsarcArchive implements Archive<PsarcAssetId, PsarcAsset> {
    private static final Logger log = LoggerFactory.getLogger(PsarcArchive.class);

    private final BinaryReader reader;
    private final Decompressor decompressor;

    private final Header header;
    private final short[] sizes;
    private final SortedMap<PsarcAssetId, PsarcAsset> assets = new TreeMap<>();

    public PsarcArchive(Path path) throws IOException {
        if (isSplit(path)) {
            List<Path> parts = findParts(path);
            List<BinaryReader> readers = new ArrayList<>(parts.size());

            try {
                for (Path part : parts) {
                    readers.add(BinaryReader.open(part));
                }
            } catch (IOException e) {
                for (BinaryReader reader : readers) {
                    reader.close();
                }
                throw e;
            }

            this.reader = BinaryReader.of(readers).order(ByteOrder.BIG_ENDIAN);
        } else {
            this.reader = BinaryReader.open(path).order(ByteOrder.BIG_ENDIAN);
        }

        this.header = Header.read(reader);
        this.decompressor = switch (header.compression().toString()) {
            case "zlib" -> Decompressor.deflate();
            case "lzma" -> Decompressor.lzma();
            case "oodl" -> Decompressor.oodle(findOodleLibrary());
            default -> throw new IOException("Unsupported compression type: " + header.compression());
        };

        var entries = reader.readObjects(header.tocEntries, Entry::read);
        this.sizes = reader.readShorts(header.tocSize - Header.BYTES - Entry.BYTES * header.tocEntries >> 1);

        var manifest = readManifest(entries.getFirst());
        var names = new HashMap<HashCode, String>();

        for (String name : manifest) {
            assert !header.isAbsolute() || name.charAt(0) == '/';
            var key = header.isIgnoreCase() ? name.toUpperCase(Locale.ROOT) : name;
            var value = header.isAbsolute() ? name.substring(1) : name;
            names.put(HashFunction.md5().hash(key), value);
        }

        for (int i = 1; i < entries.size(); i++) {
            var entry = entries.get(i);
            var hash = HashCode.ofBytes(entry.hash);
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
        return Optional.ofNullable(assets.get(transformId(key)));
    }

    @Override
    public Collection<PsarcAsset> getAll() {
        return assets.values();
    }

    @Override
    public ByteBuffer read(PsarcAssetId key) throws IOException {
        var asset = get(key).orElseThrow(FileNotFoundException::new);
        return read(asset.uncompressedSize(), asset.blockOffset(), asset.fileOffset());
    }

    @Override
    public void close() throws IOException {
        reader.close();
        decompressor.close();
    }

    private PsarcAssetId transformId(PsarcAssetId id) {
        return header.isIgnoreCase() ? PsarcAssetId.of(id.name().toUpperCase(Locale.ROOT)) : id;
    }

    private String[] readManifest(Entry entry) throws IOException {
        byte[] manifest = read(entry.uncompressedSize, entry.blockOffset, entry.fileOffset).array();
        return new String(manifest, StandardCharsets.UTF_8).split("[\n\u0000]");
    }

    private ByteBuffer read(long uncompressedSize, int index, long blockOffset) throws IOException {
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
                int length = Math.min(output.remaining(), header.blockSize());
                reader.readBytes(buffer, 0, size);
                decompressor.decompress(buffer, 0, size, output.array(), output.position(), length);
                output.position(output.position() + length);
            }
        }

        return output.position(0);
    }

    private static boolean isSplit(Path path) {
        return Filenames.getExtension(path).matches("[0-9]+");
    }

    private static List<Path> findParts(Path path) throws IOException {
        var filename = path.getFileName().toString();
        var basename = filename.substring(0, filename.lastIndexOf('.'));
        var pattern = Pattern.compile(Pattern.quote(basename) + "\\.[0-9]+");

        try (Stream<Path> stream = Files.list(path.getParent())) {
            return stream
                .filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                .sorted(Path::compareTo)
                .toList();
        }
    }

    private static Path findOodleLibrary() throws IOException {
        var root = Path.of("").toAbsolutePath();
        var pattern = Pattern.compile("oo2core_.*?_win64\\.dll");

        try (Stream<Path> stream = Files.list(root)) {
            return stream
                .filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("Couldn't find a suitable Oodle library file. Please obtain oo2core_X_win64.dll and put it in " + root.toAbsolutePath()));
        }
    }

    private record Header(
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
        static final FourCC MAGIC = FourCC.of("PSAR");
        static final int BYTES = 32;

        private static final int FLAGS_IGNORE_CASE = 0x01;
        private static final int FLAGS_ABSOLUTE = 0x02;

        static Header read(BinaryReader reader) throws IOException {
            var magic = FourCC.of(reader.readInt());
            var major = reader.readShort();
            var minor = reader.readShort();
            var compression = FourCC.of(reader.readInt());
            var tocSize = reader.readInt();
            var tocEntrySize = reader.readInt();
            var tocEntries = reader.readInt();
            var blockSize = reader.readInt();
            var flags = reader.readInt();

            if (!magic.equals(MAGIC)) {
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
