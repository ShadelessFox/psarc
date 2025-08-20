package sh.adelessfox.psarc.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Command(name = "list", description = "Print the names of all the entries in the archive", showDefaultValues = true)
final class ListCommand extends ArchiveCommand {
    @Option(names = "--verbose", description = "Listing prints entry sizes")
    private boolean verbose;

    @Option(names = "--output", description = "Target file to write the output to")
    private Path output;

    @Override
    protected void process(PsarcArchive archive, Collection<PsarcAsset> assets) throws IOException {
        PrintStream ps;
        if (output != null) {
            ps = new PrintStream(Files.newOutputStream(output));
        } else {
            ps = System.out;
        }
        try (ps) {
            if (verbose) {
                ps.printf("%10s %10s %10s %s%n", "Offset", "Size", "Compressed", "Entry");
            }
            for (PsarcAsset asset : assets) {
                if (verbose) {
                    ps.printf("%10d %10d %10d %s%n", asset.fileOffset(), asset.uncompressedSize(), asset.size().toBytes(), asset.id().fullName());
                } else {
                    ps.println(asset.id().fullName());
                }
            }
        }
    }
}
