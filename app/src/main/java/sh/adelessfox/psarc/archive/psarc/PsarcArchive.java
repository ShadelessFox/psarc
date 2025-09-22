package sh.adelessfox.psarc.archive.psarc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.util.Filenames;
import wtf.reversed.toolbox.compress.Decompressor;
import wtf.reversed.toolbox.hash.HashCode;
import wtf.reversed.toolbox.hash.HashFunction;
import wtf.reversed.toolbox.io.BinaryReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PsarcArchive implements Archive<PsarcAssetId, PsarcAsset> {
    private static final Logger log = LoggerFactory.getLogger(PsarcArchive.class);

    private final BinaryReader reader;
    private final Decompressor decompressor;

    private final PsarcHeader header;
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

        this.header = PsarcHeader.read(reader);
        this.decompressor = switch (header.compression().toString()) {
            case "zlib" -> Decompressor.deflate(false);
            case "lzma" -> Decompressor.lzma();
            case "oodl" -> Decompressor.oodle(findOodleLibrary());
            default -> throw new IOException("Unsupported compression type: " + header.compression());
        };

        var entries = reader.readObjects(header.tocEntries(), PsarcEntry::read);
        this.sizes = reader.readShorts(header.tocSize() - PsarcHeader.BYTES - PsarcEntry.BYTES * header.tocEntries() >> 1);

        var manifest = PsarcManifest.of(read(entries.getFirst()));
        var names = new HashMap<HashCode, String>();

        for (String name : manifest.filenames()) {
            assert !header.isAbsolute() || name.charAt(0) == '/';
            var key = header.isIgnoreCase() ? name.toUpperCase(Locale.ROOT) : name;
            var value = header.isAbsolute() ? name.substring(1) : name;
            names.put(HashFunction.md5().hash(key), value);
        }

        for (int i = 1; i < entries.size(); i++) {
            var entry = entries.get(i);
            var hash = HashCode.ofBytes(entry.hash());
            var name = names.get(hash);
            if (name == null) {
                log.warn("Missing name for entry with hash {}", hash);
                continue;
            }

            var transformedId = PsarcAssetId.of(header.isIgnoreCase() ? name.toUpperCase(Locale.ROOT) : name);
            var originalId = PsarcAssetId.of(name);
            var asset = new PsarcAsset(originalId, entry.blockOffset(), entry.uncompressedSize(), entry.fileOffset());

            var existingAsset = assets.putIfAbsent(transformedId, asset);
            if (existingAsset != null) {
                if (existingAsset.equals(asset)) {
                    log.warn("Found a duplicate entry for asset {}", name);
                } else {
                    log.error("Found a duplicate entry for asset {} that points to another location in the archive", name);
                }
            }
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

    private ByteBuffer read(PsarcEntry entry) throws IOException {
        return read(entry.uncompressedSize(), entry.blockOffset(), entry.fileOffset());
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
        String filename = path.getFileName().toString();
        String basename = filename.substring(0, filename.lastIndexOf('.'));
        Pattern pattern = Pattern.compile(Pattern.quote(basename) + "\\.[0-9]+");

        try (Stream<Path> stream = Files.list(path.getParent())) {
            return stream
                .filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                .sorted(Path::compareTo)
                .toList();
        }
    }

    private static Path findOodleLibrary() throws IOException {
        var matcher = FileSystems.getDefault().getPathMatcher("glob:**/oo2core_*_win64.dll");
        var root = Path.of("").toAbsolutePath();
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                .filter(matcher::matches)
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("Couldn't find a suitable Oodle library file. Please obtain oo2core_X_win64.dll and put it in " + root.toAbsolutePath()));
        }
    }
}
