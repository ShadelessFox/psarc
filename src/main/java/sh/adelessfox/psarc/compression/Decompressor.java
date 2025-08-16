package sh.adelessfox.psarc.compression;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract sealed class Decompressor
    permits LZ4Decompressor, DeflateDecompressor {

    public static Decompressor lz4() {
        return LZ4Decompressor.LZ4;
    }

    public static Decompressor deflate() {
        return DeflateDecompressor.DEFLATE;
    }

    public abstract void decompress(ByteBuffer src, ByteBuffer dst) throws IOException;

    public void decompress(byte[] src, int srcLen, byte[] dst, int dstLen) throws IOException {
        decompress(src, 0, srcLen, dst, 0, dstLen);
    }

    public void decompress(byte[] src, int srcOff, int srcLen, byte[] dst, int dstOff, int dstLen) throws IOException {
        decompress(ByteBuffer.wrap(src, srcOff, srcLen), ByteBuffer.wrap(dst, dstOff, dstLen));
    }
}
