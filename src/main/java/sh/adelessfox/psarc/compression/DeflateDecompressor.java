package sh.adelessfox.psarc.compression;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class DeflateDecompressor extends Decompressor {
    static final DeflateDecompressor DEFLATE = new DeflateDecompressor();

    @Override
    public void decompress(ByteBuffer src, ByteBuffer dst) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(src);

        while (!inflater.finished()) {
            try {
                int count = inflater.inflate(dst);
                if (count == 0) {
                    assert !src.hasRemaining();
                    break;
                }
            } catch (DataFormatException e) {
                throw new IOException("Invalid compressed data", e);
            }
        }

        inflater.end();
    }
}
