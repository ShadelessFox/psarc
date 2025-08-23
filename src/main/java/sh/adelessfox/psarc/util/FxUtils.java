package sh.adelessfox.psarc.util;

import devtoolsfx.gui.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    public static boolean isPrimaryDoubleClick(MouseEvent event) {
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() % 2 == 0;
    }

    public static <S, T> boolean isDisclosureNode(TreeTableCell<S, T> cell, MouseEvent event) {
        var treeTableView = cell.getTreeTableView();
        var treeColumn = treeTableView.getTreeColumn() == null ? treeTableView.getVisibleLeafColumn(0) : treeTableView.getTreeColumn();
        var column = cell.getTableColumn();

        if (column != treeColumn) {
            return false;
        }

        var disclosureNode = cell.getTableRow().getDisclosureNode();
        if (disclosureNode != null && disclosureNode.isVisible()) {
            double endX = disclosureNode.getBoundsInParent().getMaxX();
            double startX = treeTableView.getVisibleLeafColumns().stream()
                .takeWhile(tc -> tc != treeColumn)
                .mapToDouble(TableColumnBase::getWidth)
                .sum();

            return event.getX() < endX - startX;
        }

        return false;
    }
}
