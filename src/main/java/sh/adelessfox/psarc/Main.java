package sh.adelessfox.psarc;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;

public class Main extends Application {
    public void start(Stage stage) {
        PsarcArchive archive;

        try {
            archive = new PsarcArchive(Path.of("D:/PlayStation Games/Until Dawn TEST70002/USRDIR/data_ps3.psarc"), ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        SortedList<PsarcAsset> items = new SortedList<>(FXCollections.observableArrayList(archive.getAll().toList()));
        TableView<PsarcAsset> view = new TableView<>(items);

        items.comparatorProperty().bind(view.comparatorProperty());

        TableColumn<PsarcAsset, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(x -> new ReadOnlyStringWrapper(x.getValue().id().name()));
        nameColumn.setReorderable(false);

        TableColumn<PsarcAsset, Integer> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue().size()));
        sizeColumn.setReorderable(false);
        sizeColumn.setMinWidth(100);
        sizeColumn.setMaxWidth(100);

        view.getColumns().setAll(List.of(nameColumn, sizeColumn));
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Scene scene = new Scene(view);

        stage.setTitle("My JavaFX Application");
        stage.setScene(scene);
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}