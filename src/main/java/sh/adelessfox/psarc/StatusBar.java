package sh.adelessfox.psarc;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import sh.adelessfox.psarc.util.Formatters;

final class StatusBar extends ToolBar {
    private final LongProperty totalFiles = new SimpleLongProperty(this, "totalFiles");
    private final LongProperty totalSize = new SimpleLongProperty(this, "totalSize");

    public StatusBar() {
        Label filesLabel = new Label();
        filesLabel.textProperty().bind(totalFiles.map(x -> Formatters.formatFiles(x.longValue())));

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(totalSize.map(x -> Formatters.formatSize(x.longValue())));

        getStyleClass().add("status-bar");
        getItems().setAll(
            new Separator(Orientation.VERTICAL),
            filesLabel,
            new Separator(Orientation.VERTICAL),
            sizeLabel
        );
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles.set(totalFiles);
    }

    public void setTotalSize(long totalSize) {
        this.totalSize.set(totalSize);
    }
}
