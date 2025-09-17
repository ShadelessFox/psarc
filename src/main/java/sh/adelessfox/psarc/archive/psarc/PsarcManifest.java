package sh.adelessfox.psarc.archive.psarc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public record PsarcManifest(List<String> filenames) {
    public static PsarcManifest of(ByteBuffer buffer) {
        var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        var filenames = new String(bytes, StandardCharsets.UTF_8).split("[\n\u0000]");
        return new PsarcManifest(List.of(filenames));
    }

    public ByteBuffer asByteBuffer() {
        var manifest = String.join("\n", filenames);
        return ByteBuffer.wrap(manifest.getBytes(StandardCharsets.UTF_8));
    }
}
