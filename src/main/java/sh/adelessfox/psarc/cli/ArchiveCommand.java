package sh.adelessfox.psarc.cli;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;

abstract class ArchiveCommand implements Callable<Void> {
    @Parameters(index = "0", arity = "1", description = "The file to process.")
    protected Path input;

    @Option(names = "--order", description = "Byte order of the file")
    protected ByteOrder order = ByteOrder.BIG_ENDIAN;

    @Override
    public Void call() throws IOException {
        try (var archive = new PsarcArchive(input, order)) {
            process(archive, archive.getAll());
            return null;
        }
    }

    protected abstract void process(PsarcArchive archive, Collection<PsarcAsset> assets) throws IOException;
}
