package sh.adelessfox.psarc.archive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.adelessfox.psarc.archive.psarc.PsarcArchiveWriter;
import sh.adelessfox.psarc.archive.psarc.PsarcAssetId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static java.nio.file.StandardOpenOption.*;

final class PsarcArchiveWriterTest {
    @Test
    void testNoCompression(@TempDir Path dir) throws IOException {
        var random = new Random(42);
        var writer = new PsarcArchiveWriter();

        for (int i = 0; i < 50; i++) {
            var data = new byte[random.nextInt(1, 1024 * 128)];
            random.nextBytes(data);

            var id = PsarcAssetId.of("/file%d.bin".formatted(i));
            var source = ArchiveWriter.AssetSources.ofByteArray(data);
            writer.add(id, source);
        }

        Path path = dir.resolve("sample_no_compression.psarc");
        writer.write(path, WRITE, TRUNCATE_EXISTING, CREATE);

        byte[] expected;
        byte[] actual = Files.readAllBytes(path);

        try (InputStream is = getClass().getResourceAsStream("/sample_no_compression.psarc")) {
            Assertions.assertNotNull(is, "Can't find sample file");
            expected = is.readAllBytes();
        }

        Assertions.assertArrayEquals(expected, actual);
    }
}
