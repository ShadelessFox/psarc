package sh.adelessfox.psarc.compression;

import org.tukaani.xz.LZMAInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

final class LZMADecompressor extends Decompressor {
    static final LZMADecompressor LZMA = new LZMADecompressor();

    @Override
    public void decompress(ByteBuffer src, ByteBuffer dst) throws IOException {
        try (LZMAInputStream is = new LZMAInputStream(new ByteBufferInputStream(src))) {
            while (src.hasRemaining()) {
                int read = is.read(dst.array(), dst.position(), dst.remaining());
                if (read != dst.remaining()) {
                    throw new IOException("Decompression failed");
                }
                dst.position(dst.position() + read);
            }
        }
    }

    private static final class ByteBufferInputStream extends InputStream {
        private final ByteBuffer buffer;

        ByteBufferInputStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            if (buffer.hasRemaining()) {
                return Byte.toUnsignedInt(buffer.get());
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) {
            Objects.checkFromIndexSize(off, len, b.length);
            if (!buffer.hasRemaining()) {
                return -1;
            }
            int read = Math.min(buffer.remaining(), len);
            buffer.get(b, off, read);
            return read;
        }
    }
}
