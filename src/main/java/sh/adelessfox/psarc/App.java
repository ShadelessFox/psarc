package sh.adelessfox.psarc;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.archive.AssetId;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.settings.Settings;
import sh.adelessfox.psarc.ui.StatusBar;
import sh.adelessfox.psarc.ui.StructuredTreeItem;
import sh.adelessfox.psarc.util.Filenames;
import sh.adelessfox.psarc.util.Fugue;
import sh.adelessfox.psarc.util.FxUtils;
import sh.adelessfox.psarc.util.Mica;
import sh.adelessfox.psarc.util.type.FileCount;
import sh.adelessfox.psarc.util.type.FileSize;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

import static java.nio.file.StandardOpenOption.*;

public class App extends Application {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final String TITLE = "PSARC Viewer";

    private final ObjectProperty<Path> path = new SimpleObjectProperty<>(this, "path");
    private final ObjectProperty<Archive<?, ?>> archive = new SimpleObjectProperty<>(this, "archive");

    private Stage stage;
    private Settings settings;

    public static void main(String[] args) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.launch(args);
    }

    public void start(Stage stage) {
        this.stage = stage;

        var component = DaggerAppComponent.create();
        this.settings = component.settings();

        var root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(buildTreeTableView());
        root.setBottom(buildStatusBar());
        Mica.installStyle(root);

        var scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.initStyle(StageStyle.UNIFIED);
        stage.setScene(scene);
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();

        stage.titleProperty().bind(
            Bindings.when(path.isNotNull())
                .then(Bindings.format("%s - %s", TITLE, path))
                .otherwise(TITLE)
        );

        if (Mica.isMicaSupported()) {
            Mica.install(stage);
        }

        FxUtils.installDevTools(stage, this, KeyCombination.keyCombination("Shift+Ctrl+Alt+X"));
        FxUtils.installStylesheetHotReload(scene, scene.getStylesheets().getFirst());
    }

    public void setPath(Path path) {
        this.path.set(path);
    }

    public void setArchive(Archive<?, ?> archive) {
        this.archive.set(archive);
    }

    public void addRecentPath(Path path) {
        var recentPaths = settings.recentPaths().compute(ArrayList::new);
        recentPaths.remove(path);
        recentPaths.addFirst(path);
    }

    private void chooseArchive() {
        var chooser = new FileChooser();
        chooser.setTitle("Choose archive to load");
        chooser.setInitialDirectory(settings.lastDirectory().map(Path::toFile).orElse(null));
        chooser.getExtensionFilters().add(new ExtensionFilter("PlayStation Archive", "*.psarc"));

        var file = chooser.showOpenDialog(stage);
        if (file != null) {
            var path = file.toPath();
            settings.lastDirectory().set(path.getParent());
            loadArchive(path);
        }
    }

    private void loadArchive(Path path) {
        if (path.equals(this.path.get())) {
            return;
        }
        try {
            setArchive(new PsarcArchive(path, ByteOrder.BIG_ENDIAN));
            setPath(path);
            addRecentPath(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void extractArchive() {
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("A viewer for PlayStation Archive (PSARC) files.");
        alert.initOwner(stage);
        alert.initStyle(StageStyle.UNIFIED);
        alert.show();

        if (Mica.isMicaSupported()) {
            Mica.install(stage);
        }
    }

    private ToolBar buildToolBar() {
        SplitMenuButton openButton = new SplitMenuButton("_Open\u2026", Fugue.getImageView("folder-open-document"));
        openButton.setTooltip(new Tooltip("Open an archive"));
        openButton.setOnAction(_ -> chooseArchive());
        openButton.setOnShowing(_ -> {
            List<MenuItem> items = new ArrayList<>();
            settings.recentPaths().ifPresent(paths -> {
                for (Path path : paths) {
                    MenuItem item = new MenuItem(path.toString());
                    item.setOnAction(_ -> loadArchive(path));
                    items.add(item);
                }
                if (!paths.isEmpty()) {
                    MenuItem item = new MenuItem("Clear recent file list");
                    item.setOnAction(_ -> settings.recentPaths().set(null));
                    items.add(new SeparatorMenuItem());
                    items.add(item);
                }
            });
            if (items.isEmpty()) {
                MenuItem item = new MenuItem("No recent files");
                item.setDisable(true);
                items.add(item);
            }
            openButton.getItems().setAll(items);
        });

        Button extractButton = new Button("_Extract\u2026", Fugue.getImageView("folder-export"));
        extractButton.setTooltip(new Tooltip("Extract all files"));
        extractButton.setOnAction(_ -> extractArchive());
        extractButton.disableProperty().bind(archive.isNull());

        Button aboutButton = new Button("About", Fugue.getImageView("question-white"));
        aboutButton.setOnAction(_ -> showAboutDialog());

        ToolBar toolBar = new ToolBar(openButton, extractButton, new Spacer(), aboutButton);
        Mica.installStyle(toolBar);

        return toolBar;
    }

    private StatusBar buildStatusBar() {
        StatusBar statusBar = new StatusBar();

        archive.addListener((_, _, newValue) -> {
            if (newValue == null) {
                statusBar.setTotalFiles(FileCount.ZERO);
                statusBar.setTotalSize(FileSize.ZERO);
                return;
            }

            var statistics = newValue.getAll().stream()
                .mapToLong(value -> value.size().toBytes())
                .summaryStatistics();

            statusBar.setTotalFiles(FileCount.of(statistics.getCount()));
            statusBar.setTotalSize(FileSize.ofBytes(statistics.getSum()));
        });

        return statusBar;
    }

    private <K extends AssetId, V extends Asset<K>> TreeTableView<ArchiveStructure<V>> buildTreeTableView() {
        var view = new TreeTableView<ArchiveStructure<V>>();
        view.getStyleClass().addAll(Styles.BG_DEFAULT, Styles.DENSE, Tweaks.EDGE_TO_EDGE);
        view.setShowRoot(false);
        view.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.getColumns().setAll(buildTreeTableColumns());

        archive.addListener((_, _, newValue) -> {
            if (newValue == null) {
                view.setRoot(null);
            } else {
                @SuppressWarnings("unchecked")
                Archive<?, V> archive = (Archive<?, V>) newValue;
                view.setRoot(new StructuredTreeItem<>(ArchiveStructure.of(archive)));
            }
        });

        view.setOnDragDetected(event -> {
            var item = view.getSelectionModel().getSelectedItem();
            if (!(item.getValue() instanceof ArchiveStructure.File<V> file)) {
                return;
            }

            File result;

            @SuppressWarnings("unchecked")
            Archive<K, V> archive = (Archive<K, V>) this.archive.get();

            try {
                result = extractToTemporaryFile(archive, file.asset).toFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            Dragboard dragboard = view.startDragAndDrop(TransferMode.MOVE);
            dragboard.setContent(Map.of(DataFormat.FILES, List.of(result)));
            event.consume();
        });

        view.setOnDragOver(event -> {
            var dragboard = event.getDragboard();
            if (dragboard.hasFiles() && dragboard.getFiles().size() == 1) {
                var path = dragboard.getFiles().getFirst().toPath();
                var extension = Filenames.getExtension(path);
                if (extension.equalsIgnoreCase("psarc")) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        view.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            var path = dragboard.getFiles().getFirst().toPath();
            loadArchive(path);
            event.consume();
        });

        view.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() % 2 != 0) {
                return;
            }

            // TODO: Fix double-click on the expander triggering the action
            var item = view.getSelectionModel().getSelectedItem();
            if (!(item.getValue() instanceof ArchiveStructure.File<V> file)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Archive<K, V> archive = (Archive<K, V>) this.archive.get();

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