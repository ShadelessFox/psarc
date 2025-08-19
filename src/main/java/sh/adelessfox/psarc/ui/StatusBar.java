package sh.adelessfox.psarc.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import sh.adelessfox.psarc.util.type.FileCount;
import sh.adelessfox.psarc.util.type.FileSize;

public final class StatusBar extends ToolBar {
    private final ObjectProperty<FileCount> totalFiles = new SimpleObjectProperty<>(this, "totalFiles", FileCount.zero());
    private final ObjectProperty<FileSize> totalSize = new SimpleObjectProperty<>(this, "totalSize", FileSize.zero());

    public StatusBar() {
        Label filesLabel = new Label();
        filesLabel.textProperty().bind(totalFiles.asString());

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(totalSize.asString());

        getStyleClass().add("status-bar");
        getItems().setAll(
            new Separator(Orientation.VERTICAL),
            filesLabel,
            new Separator(Orientation.VERTICAL),
            sizeLabel
        );
    }

    public void setTotalFiles(FileCount totalFiles) {
        this.totalFiles.set(totalFiles);
    }

    public void setTotalSize(FileSize totalSize) {
        this.totalSize.set(totalSize);
    }
}
