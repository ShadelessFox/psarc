package sh.adelessfox.psarc;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.archive.AssetId;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.ui.StructuredTreeItem;
import sh.adelessfox.psarc.util.Fugue;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardOpenOption.*;

public class Main extends Application {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.launch(args);
    }

    public void start(Stage stage) {
        Path path = Path.of("D:/PlayStation Games/Until Dawn TEST70002/USRDIR/data_ps3.psarc");
        PsarcArchive archive;

        try {
            archive = new PsarcArchive(path, ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var root = new BorderPane();
        root.setTop(buildMenuBar());
        root.setCenter(buildTreeTableView(archive));

        var scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("PSARC Explorer - " + path);
        stage.setScene(scene);
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();
    }

    private static MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("_File");
        Menu helpMenu = new Menu("_Help");

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, helpMenu);

        return menuBar;
    }

    private static <K extends AssetId, V extends Asset<K>> TreeTableView<ArchiveStructure<V>> buildTreeTableView(Archive<K, V> archive) {
        var structure = ArchiveStructure.of(archive);

        var view = new TreeTableView<ArchiveStructure<V>>();
        view.getStyleClass().addAll(Styles.DENSE, Tweaks.EDGE_TO_EDGE);
        view.setRoot(new StructuredTreeItem<>(structure));
        view.setShowRoot(false);
        view.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.getColumns().setAll(buildTreeTableColumns());

        // Drag & drop
        view.setOnDragDetected(event -> {
            var item = view.getSelectionModel().getSelectedItem();
            if (!(item.getValue() instanceof ArchiveStructure.File<V> file)) {
                return;
            }

            File result;

            try {
                result = extractToTemporaryFile(archive, file.asset).toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            Dragboard dragboard = view.startDragAndDrop(TransferMode.MOVE);
            dragboard.setContent(Map.of(DataFormat.FILES, List.of(result)));
            event.consume();
        });

        // Double-click
        view.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() % 2 != 0) {
                return;
            }

            // TODO: Fix double-click on the expander triggering the action
            var item = view.getSelectionModel().getSelectedItem();
            if (!(item.getValue() instanceof ArchiveStructure.File<V> file)) {
                return;
            }

            try {
                Desktop.getDesktop().open(extractToTemporaryFile(archive, file.asset).toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return view;
    }

    private static <K extends AssetId, V extends Asset<K>> Path extractToTemporaryFile(Archive<K, V> archive, V asset) throws IOException {
        var id = asset.id();
        var root = Path.of(System.getProperty("java.io.tmpdir"), "psarc-dnd");
        var path = root.resolve(id.fileName());

        log.debug("Creating a temporary file {}", path);

        if (Files.notExists(root)) {
            Files.createDirectory(root);
        }

        try (var channel = Files.newByteChannel(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
            channel.write(archive.read(id));
        }

        return path;
    }

    private static <T extends Asset<?>> List<TreeTableColumn<ArchiveStructure<T>, ?>> buildTreeTableColumns() {
        var nameColumn = new TreeTableColumn<ArchiveStructure<T>, ArchiveStructure<T>>("Name");
        nameColumn.setReorderable(false);
        nameColumn.setSortable(false);
        nameColumn.setCellValueFactory(features -> features.getValue().valueProperty());
        nameColumn.setCellFactory(_ -> new TreeTableCell<>() {
            @Override
            protected void updateItem(ArchiveStructure<T> item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name);
                    setGraphic(Fugue.getImageView(item instanceof ArchiveStructure.File ? "document" : "folder"));
                }
            }
        });

        var sizeColumn = new TreeTableColumn<ArchiveStructure<T>, ArchiveStructure<T>>("Size");
        sizeColumn.setReorderable(false);
        sizeColumn.setSortable(false);
        sizeColumn.setMinWidth(100);
        sizeColumn.setMaxWidth(100);
        sizeColumn.setCellValueFactory(features -> features.getValue().valueProperty());
        sizeColumn.setCellFactory(_ -> new TreeTableCell<>() {
            @Override
            protected void updateItem(ArchiveStructure<T> item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText(item.size);
                }
            }
        });

        return List.of(nameColumn, sizeColumn);
    }
}