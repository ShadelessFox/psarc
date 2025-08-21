package sh.adelessfox.psarc.ui;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.archive.AssetId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static java.nio.file.StandardOpenOption.*;

public class ExportService<K extends AssetId, V extends Asset<K>> extends Service<Void> {
    private final Path path;
    private final Archive<K, V> archive;
    private final Collection<V> assets;

    public ExportService(Path path, Archive<K, V> archive, Collection<V> assets) {
        this.path = path;
        this.archive = archive;
        this.assets = assets;
    }

    @Override
    protected Task<Void> createTask() {
        return new ExportTask();
    }

    private final class ExportTask extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            int index = 0;
            for (V asset : assets) {
                updateProgress(index, assets.size());
                updateMessage("Extracting " + asset.id().fullName());
                exportAsset(asset);
                index += 1;
            }

            updateProgress(assets.size(), assets.size());
            updateMessage("Done");

            return null;
        }

        private void exportAsset(V asset) throws Exception {
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
