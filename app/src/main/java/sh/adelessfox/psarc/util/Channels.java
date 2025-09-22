package sh.adelessfox.psarc.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public final class Channels {
    private Channels() {
    }

    public static ByteChannel byteBufferChannel(ByteBuffer buffer) {
        return new ByteBufferChannel(buffer);
    }

    private static final class ByteBufferChannel implements ByteChannel {
        private final ByteBuffer buffer;
        private boolean closed;

        ByteBufferChannel(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            ensureOpen();
            return transfer(buffer, dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            ensureOpen();
            return transfer(src, buffer);
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() {
            closed = true;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Channel is closed");
            }
        }

        private static int transfer(ByteBuffer src, ByteBuffer dst) {
            if (!src.hasRemaining()) {
                return -1;
            }

            int remaining = Math.min(src.remaining(), dst.remaining());
            int srcPos = src.position();
            int dstPos = dst.position();

            dst.put(dstPos, src, srcPos, remaining);
            dst.position(dstPos + remaining);
            src.position(srcPos + remaining);

            return remaining;
        }
    }
}
