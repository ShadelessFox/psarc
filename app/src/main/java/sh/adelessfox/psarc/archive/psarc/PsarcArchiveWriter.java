package sh.adelessfox.psarc.archive.psarc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.archive.ArchiveWriter;
import sh.adelessfox.psarc.hashing.HashFunction;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;

/**
 * A writer for PSARC archives.
 */
public final class PsarcArchiveWriter implements ArchiveWriter<PsarcAssetId> {
    /**
     * Options that can be specified for PSARC assets.
     */
    public enum PsarcAssetOption implements AssetOption {
        /**
         * Asset will be stored without any compression.
         */
        NO_COMPRESS
    }

    private static final Logger log = LoggerFactory.getLogger(PsarcArchiveWriter.class);

    private static final short VERSION_MAJOR = 1;
    private static final short VERSION_MINOR = 4;
    private static final int BLOCK_SIZE = 65536;

    private final SortedMap<PsarcAssetId, AssetInfo> assets = new TreeMap<>();
    private final PsarcCompression compression;
    private final boolean absolute;
    private final boolean ignoreCase;

    public PsarcArchiveWriter(PsarcCompression compression, boolean absolute, boolean ignoreCase) {
        this.compression = compression;
        this.absolute = absolute;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public void add(PsarcAssetId id, AssetSource source, AssetOption... options) {
        boolean compress = true;
        for (AssetOption option : options) {
            if (option == PsarcAssetOption.NO_COMPRESS) {
                compress = false;
                break;
            }
        }

        var name = computeName(id);
        var info = new AssetInfo(name, source, compress);

        if (assets.putIfAbsent(id, info) != null) {
            throw new IllegalArgumentException("Asset " + id + " was already added to the writer");
        }
    }

    @Override
    public void remove(PsarcAssetId id) {
        if (assets.remove(id) == null) {
            throw new IllegalArgumentException("Asset " + id + " was not present in the writer");
        }
    }

    @Override
    public void clear() {
        assets.clear();
    }

    @Override
    public void write(Path path, OpenOption... options) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(path, options)) {
            var manifest = AssetSources.ofByteBuffer(buildManifest().asByteBuffer());

            int manifestBlocks = computeSizeInBlocks(manifest);
            int assetsBlocks = assets.values().stream()
                .mapToInt(asset -> computeSizeInBlocks(asset.source()))
                .sum();
            int totalBlocks = manifestBlocks + assetsBlocks;

            int tocHeaderSize = PsarcHeader.BYTES;
            int tocEntriesSize = PsarcEntry.BYTES * (assets.size() + 1);
            int tocBlocksSize = totalBlocks * Short.BYTES;
            int tocSize = tocHeaderSize + tocEntriesSize + tocBlocksSize;

            // Write data
            channel.position(tocSize);

            var tocEntries = new ArrayList<PsarcEntry>();
            var tocBlockBuffer = ByteBuffer.allocate(BLOCK_SIZE);
            var tocBlockSizesBuffer = ByteBuffer.allocate(tocBlocksSize);
            var tocBlockSizesShortBuffer = tocBlockSizesBuffer.asShortBuffer();

            // Write manifest
            tocEntries.add(writeAsset(channel, null, manifest, tocBlockBuffer, tocBlockSizesShortBuffer, true));

            // Write assets
            for (var entry : assets.entrySet()) {
                var id = entry.getKey();
                var info = entry.getValue();

                tocEntries.add(writeAsset(
                    channel,
                    id,
                    info.source(),
                    tocBlockBuffer,
                    tocBlockSizesShortBuffer,
                    info.compressed()
                ));
            }

            // Write toc
            channel.position(0);
            writeToc(channel, tocEntries, tocSize, tocBlockSizesBuffer);
        }
    }

    @Override
    public void close() throws IOException {
        assets.clear();
    }

    private PsarcEntry writeAsset(
        SeekableByteChannel channel,
        PsarcAssetId id,
        AssetSource source,
        ByteBuffer block,
        ShortBuffer sizes,
        boolean compress
    ) throws IOException {
        log.debug("Writing {} ({} bytes)", id != null ? id : "manifest", source.size());

        var firstBlock = sizes.position();
        var start = channel.position();
        var size = source.size();

        try (ReadableByteChannel src = source.open()) {
            long transferred = 0;
            while (transferred < size) {
                long read = src.read(block);
                if (read < 0) {
                    throw new EOFException();
                }
                transferred += read;
                if (block.remaining() == 0) {
                    writeAssetBlock(channel, block, sizes, compress);
                }
            }
            writeAssetBlock(channel, block, sizes, compress);
        }

        // TODO: absolute/relative/case-insensitive flags
        byte[] hash;
        if (id != null) {
            hash = HashFunction.md5().hash(id.name()).asArray();
        } else {
            hash = new byte[16];
        }

        return new PsarcEntry(hash, firstBlock, size, start);
    }

    private void writeAssetBlock(
        SeekableByteChannel channel,
        ByteBuffer block,
        ShortBuffer sizes,
        boolean compress
    ) throws IOException {
        // TODO: actually compress
        int position = block.position();
        if (position > 0) {
            block.flip();
            write(channel, block);
            block.clear();
            sizes.put((short) position);
        }
    }

    private void writeToc(
        SeekableByteChannel channel,
        List<PsarcEntry> entries,
        int tocSize,
        ByteBuffer tocBlocks
    ) throws IOException {
        var header = new PsarcHeader(
            PsarcHeader.PSAR,
            VERSION_MAJOR,
            VERSION_MINOR,
            compression,
            tocSize,
            PsarcEntry.BYTES,
            entries.size(),
            BLOCK_SIZE,
            PsarcHeader.FLAGS_ABSOLUTE
        );

        // Write header
        write(channel, header.toByteBuffer());

        // Write toc entries
        for (PsarcEntry entry : entries) {
            write(channel, entry.toByteBuffer());
        }

        // Write block sizes
        write(channel, tocBlocks);
    }

    private void write(SeekableByteChannel channel, ByteBuffer buffer) throws IOException {
        int remaining = buffer.remaining();
        if (channel.write(buffer) != remaining) {
            throw new IOException("Failed to write to the channel");
        }
    }

    private PsarcManifest buildManifest() {
        var filenames = assets.keySet().stream()
            .map(PsarcAssetId::name)
            .toList();
        return new PsarcManifest(filenames);
    }

    private String computeName(PsarcAssetId id) {
        String name = id.name();
        if (ignoreCase) {
            name = name.toUpperCase(Locale.ROOT);
        }
        if (absolute) {
            name = '/' + name;
        }
        return name;
    }

    private int computeSizeInBlocks(AssetSource source) {
        return Math.toIntExact((source.size() + BLOCK_SIZE - 1) / BLOCK_SIZE);
    }

    private record AssetInfo(String name, AssetSource source, boolean compressed) {
    }
}
