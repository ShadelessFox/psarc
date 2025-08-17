package sh.adelessfox.psarc;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.archive.AssetId;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.ui.StructuredTreeItem;
import sh.adelessfox.psarc.util.Fugue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;

public class Main extends Application {
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

        var menu = buildMenuBar();
        var view = buildTreeTableView(archive);
        var root = new VBox(menu, view);
        VBox.setVgrow(view, Priority.ALWAYS);

        stage.setTitle("PSARC Explorer - " + path);
        stage.setScene(new Scene(root));
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();
    }

    private static MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("_File");
        Menu aboutMenu = new Menu("_About");

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, aboutMenu);

        return menuBar;
    }

    private static <K extends AssetId, V extends Asset<K>> TreeTableView<ArchiveStructure<V>> buildTreeTableView(Archive<K, V> archive) {
        var structure = ArchiveStructure.of(archive);

        var view = new TreeTableView<ArchiveStructure<V>>();
        view.getStyleClass().add(Styles.DENSE);
        view.setRoot(new StructuredTreeItem<>(structure));
        view.setShowRoot(false);
        view.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.getColumns().setAll(buildTreeTableColumns());


        return view;
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