package sh.adelessfox.psarc.archive.psarc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.archive.ArchiveWriter;
import sh.adelessfox.psarc.hashing.HashFunction;
import sh.adelessfox.psarc.util.type.FourCC;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.nio.file.StandardOpenOption.*;

public class PsarcArchiveWriter implements ArchiveWriter<PsarcAssetId> {
    private static final Logger log = LoggerFactory.getLogger(PsarcArchiveWriter.class);

    private static final FourCC COMPRESSION = FourCC.of("zlib");
    private static final short VERSION_MAJOR = 1;
    private static final short VERSION_MINOR = 4;
    private static final int BLOCK_SIZE = 65536;

    private final SortedMap<PsarcAssetId, AssetSource> assets = new TreeMap<>();
    private final SeekableByteChannel channel;

    public PsarcArchiveWriter(Path path) throws IOException {
        this.channel = FileChannel.open(path, WRITE, CREATE, TRUNCATE_EXISTING);
    }

    @Override
    public void add(PsarcAssetId id, AssetSource publisher) {
        if (assets.putIfAbsent(id, publisher) != null) {
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
    public void close() throws IOException {
        var manifest = AssetSources.ofByteBuffer(buildManifest().asByteBuffer());

        int manifestBlocks = computeSizeInBlocks(manifest);
        int assetsBlocks = assets.values().stream().mapToInt(this::computeSizeInBlocks).sum();
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
        tocEntries.add(writeAsset(null, manifest, tocBlockBuffer, tocBlockSizesShortBuffer));

        // Write the rest of assets
        for (var entry : assets.entrySet()) {
            tocEntries.add(writeAsset(entry.getKey(), entry.getValue(), tocBlockBuffer, tocBlockSizesShortBuffer));
        }

        // Write toc
        channel.position(0);
        writeToc(tocEntries, tocSize, tocBlockSizesBuffer);

        // We're done
        channel.close();
    }

    private PsarcEntry writeAsset(PsarcAssetId id, AssetSource source, ByteBuffer currentBlock, ShortBuffer blockSizes) throws IOException {
        log.debug("Writing {} ({} bytes)", id != null ? id : "manifest", source.size());

        var firstBlock = blockSizes.position();
        var position = channel.position();
        var size = source.size();

        try (ReadableByteChannel src = source.open()) {
            long transferred = 0;
            while (transferred < size) {
                long read = src.read(currentBlock);
                if (read < 0) {
                    throw new EOFException();
                }
                transferred += read;
                if (currentBlock.remaining() == 0) {
                    writeBlock(currentBlock, blockSizes);
                }
            }
            writeBlock(currentBlock, blockSizes);
        }

        byte[] hash;
        if (id != null) {
            hash = HashFunction.md5().hash(id.name()).asArray();
        } else {
            hash = new byte[16];
        }

        return new PsarcEntry(hash, firstBlock, size, position);
    }

    private void writeBlock(ByteBuffer block, ShortBuffer blockSizes) throws IOException {
        int position = block.position();
        if (position > 0) {
            block.flip();
            write(block);
            block.clear();
            blockSizes.put((short) position);
        }
    }

    private void writeToc(List<PsarcEntry> entries, int tocSize, ByteBuffer tocBlocks) throws IOException {
        var header = new PsarcHeader(
            PsarcHeader.PSAR,
            VERSION_MAJOR,
            VERSION_MINOR,
            COMPRESSION,
            tocSize,
            PsarcEntry.BYTES,
            entries.size(),
            BLOCK_SIZE,
            PsarcHeader.FLAGS_ABSOLUTE
        );

        // Write header
        write(header.toByteBuffer());

        // Write toc entries
        for (PsarcEntry entry : entries) {
            write(entry.toByteBuffer());
        }

        // Write block sizes
        write(tocBlocks);
    }

    private void write(ByteBuffer buffer) throws IOException {
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

    private int computeSizeInBlocks(AssetSource source) {
        return Math.toIntExact((source.size() + BLOCK_SIZE - 1) / BLOCK_SIZE);
    }
}
