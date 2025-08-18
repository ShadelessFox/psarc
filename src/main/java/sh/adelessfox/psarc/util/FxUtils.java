package sh.adelessfox.psarc.util;

import devtoolsfx.gui.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FxUtils {
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private FxUtils() {
    }

    public static void installDevTools(Stage stage, Application application, KeyCombination combination) {
        stage.getScene().getAccelerators().put(combination, () -> GUI.openToolStage(stage, application.getHostServices()));
    }

    public static void installStylesheetHotReload(Scene scene, String stylesheet) {
        executorService.submit(() -> {
            var service = FileSystems.getDefault().newWatchService();
            var folder = Path.of(URI.create(stylesheet)).getParent();
            folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY);

            for (WatchKey key; (key = service.take()) != null; key.reset()) {
                for (WatchEvent<?> _ : key.pollEvents()) {
                    Platform.runLater(() -> scene.getStylesheets().setAll(stylesheet));
                }
            }

            return null;
        });
    }
}
