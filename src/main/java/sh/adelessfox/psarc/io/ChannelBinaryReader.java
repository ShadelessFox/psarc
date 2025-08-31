package sh.adelessfox.psarc.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

final class ChannelBinaryReader extends BufferedBinaryReader {
    private final SeekableByteChannel channel;

    ChannelBinaryReader(SeekableByteChannel channel) throws IOException {
        super(channel.size());
        this.channel = channel;
    }

    @Override
    protected void readImpl(ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            if (channel.read(dst) < 0) {
                throw new EOFException();
            }
        }
    }

    @Override
    protected void positionImpl(long position) throws IOException {
        channel.position(position);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
