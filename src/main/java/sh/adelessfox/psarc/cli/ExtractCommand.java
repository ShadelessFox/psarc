package sh.adelessfox.psarc.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import sh.adelessfox.psarc.archive.psarc.PsarcArchive;
import sh.adelessfox.psarc.archive.psarc.PsarcAsset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static java.nio.file.StandardOpenOption.*;

@Command(name = "extract", description = "Extract all entries and place in a specified directory", showDefaultValues = true)
final class ExtractCommand extends ArchiveCommand {
    @Option(names = "--dir", description = "Target directory for output files")
    private Path dir = Path.of("").toAbsolutePath();

    @Override
    protected void process(PsarcArchive archive, Collection<PsarcAsset> assets) throws IOException {
        int index = 1;
        for (PsarcAsset asset : assets) {
            System.out.printf("[%d/%d] Extracting %s%n", index, assets.size(), asset.id().fullName());

            Path path = dir.resolve(asset.id().fullName());
            Files.createDirectories(path.getParent());

            try (var channel = Files.newByteChannel(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
                channel.write(archive.read(asset.id()));
            }

            index += 1;
        }
    }
}
