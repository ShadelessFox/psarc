package sh.adelessfox.psarc.archive;

import sh.adelessfox.psarc.util.Channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public interface ArchiveWriter<T extends AssetId> {
    /**
     * Adds a new asset to the archive.
     *
     * @param id     the id of the asset
     * @param source the asset source
     */
    void add(T id, AssetSource source);

    /**
     * Removes an asset for the given id.
     *
     * @param id id of the asset to remove
     */
    void remove(T id);

    /**
     * Removes all existing assets from the writer.
     */
    void clear();

    /**
     * Writes a new archive to a file.
     *
     * @param path    the path to the archive
     * @param options options specifying how the file is opened
     * @throws IOException if an I/O error has occurred
     */
    void write(Path path, OpenOption... options) throws IOException;

    interface AssetSource {
        ReadableByteChannel open() throws IOException;

        long size();
    }

    final class AssetSources {
        private AssetSources() {
        }

        public static AssetSource ofFile(Path path) throws IOException {
            return new FileAssetSource(path, Files.size(path));
        }

        public static AssetSource ofByteBuffer(ByteBuffer buffer) {
            return new ByteBufferAssetSource(buffer);
        }

        public static AssetSource ofByteArray(byte[] array) {
            return new ByteBufferAssetSource(ByteBuffer.wrap(array));
        }

        private record FileAssetSource(Path path, long size) implements AssetSource {
            @Override
            public ReadableByteChannel open() throws IOException {
                return Files.newByteChannel(path, StandardOpenOption.READ);
            }
        }

        private record ByteBufferAssetSource(ByteBuffer buffer) implements AssetSource {
            @Override
            public ReadableByteChannel open() {
                return Channels.byteBufferChannel(buffer.slice());
            }

            @Override
            public long size() {
                return buffer.limit();
            }
        }
    }
}
