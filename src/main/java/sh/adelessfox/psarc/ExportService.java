package sh.adelessfox.psarc;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.*;

final class ExportService extends Service<Void> {
    private Path path;
    private PsarcArchive archive;
    private Collection<PsarcAsset> assets;

    @Override
    protected Task<Void> createTask() {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(assets, "assets");

        return new ExportTask();
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setArchive(PsarcArchive archive) {
        this.archive = archive;
    }

    public void setAssets(Collection<PsarcAsset> assets) {
        this.assets = assets;
    }

    private final class ExportTask extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            int index = 0;
            for (PsarcAsset asset : assets) {
                updateProgress(index, assets.size());
                updateMessage("Extracting " + asset.id().fullName());
                exportAsset(asset);
                index += 1;
            }

            updateProgress(assets.size(), assets.size());
            updateMessage("Done");

            return null;
        }

        private void exportAsset(PsarcAsset asset) throws Exception {
            Thread.sleep(1);

            Path target = path.resolve(asset.id().fullName());
            if (Files.exists(target)) {
                return;
            }

            Files.createDirectories(target.getParent());

            try (var channel = Files.newByteChannel(target, WRITE, CREATE, TRUNCATE_EXISTING)) {
                channel.write(archive.read(asset.id()));
            }
        }
    }
}
