package sh.adelessfox.psarc.util;

import devtoolsfx.gui.GUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FxUtils {
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private FxUtils() {
    }

    public static void showExceptionDialog(Stage stage, String header, Throwable throwable) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);

        var text = new TextArea(sw.toString());
        text.setEditable(false);
        text.setWrapText(false);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(text, Priority.ALWAYS);
        GridPane.setHgrow(text, Priority.ALWAYS);

        var root = new GridPane();
        root.setMaxWidth(Double.MAX_VALUE);
        root.add(new Label("Full stacktrace:"), 0, 0);
        root.add(text, 0, 1);

        var copyToClipboard = new ButtonType("Copy to Clipboard", ButtonData.HELP);

        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("An error occurred");
        alert.setHeaderText(header);
        alert.setContentText(throwable.getLocalizedMessage());
        alert.getDialogPane().setExpandableContent(root);
        alert.getButtonTypes().add(copyToClipboard);
        alert.initOwner(stage);
        alert.resultProperty().addListener((_, _, button) -> {
            if (button == copyToClipboard) {
                var clipboard = Clipboard.getSystemClipboard();
                var content = new ClipboardContent();
                content.putString(sw.toString());
                clipboard.setContent(content);
            }
        });
        alert.show();
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
