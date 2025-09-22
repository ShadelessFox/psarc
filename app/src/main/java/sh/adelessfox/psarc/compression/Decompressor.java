package sh.adelessfox.psarc.compression;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract sealed class Decompressor implements Closeable
    permits DeflateDecompressor, LZMADecompressor, OodleDecompressor {

    public static Decompressor lzma() {
        return LZMADecompressor.LZMA;
    }

    public static Decompressor deflate() {
        return DeflateDecompressor.DEFLATE;
    }

    public static Decompressor oodle() throws IOException {
        return new OodleDecompressor();
    }

    public abstract void decompress(ByteBuffer src, ByteBuffer dst) throws IOException;

    public void decompress(byte[] src, int srcLen, byte[] dst, int dstLen) throws IOException {
        decompress(src, 0, srcLen, dst, 0, dstLen);
    }

    public void decompress(byte[] src, int srcOff, int srcLen, byte[] dst, int dstOff, int dstLen) throws IOException {
        decompress(ByteBuffer.wrap(src, srcOff, srcLen), ByteBuffer.wrap(dst, dstOff, dstLen));
    }

    @Override
    public void close() {
        // do nothing by default
    }
}
