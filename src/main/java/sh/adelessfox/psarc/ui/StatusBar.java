package sh.adelessfox.psarc.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import sh.adelessfox.psarc.util.type.FileCount;
import sh.adelessfox.psarc.util.type.FileSize;

public final class StatusBar extends HBox {
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    private final StringProperty message = new SimpleStringProperty(this, "message");
    private final ObjectProperty<FileCount> totalFiles = new SimpleObjectProperty<>(this, "totalFiles", FileCount.ZERO);
    private final ObjectProperty<FileSize> totalSize = new SimpleObjectProperty<>(this, "totalSize", FileSize.ZERO);

    public StatusBar() {
        BorderPane graphicHolder = new BorderPane();
        graphicHolder.centerProperty().bind(graphic);

        Tooltip messageTooltip = new Tooltip();
        messageTooltip.textProperty().bind(message);

        Label messageLabel = new Label();
        messageLabel.textProperty().bind(message);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.tooltipProperty().bind(Bindings.when(message.isNotEmpty()).then(messageTooltip).otherwise((Tooltip) null));
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        Label filesLabel = new Label();
        filesLabel.textProperty().bind(totalFiles.asString());
        filesLabel.setMinWidth(USE_PREF_SIZE);

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(totalSize.asString());
        sizeLabel.setMinWidth(USE_PREF_SIZE);

        getStyleClass().add("status-bar");
        getChildren().setAll(
            graphicHolder,
            messageLabel,
            new Separator(Orientation.VERTICAL),
            filesLabel,
            new Separator(Orientation.VERTICAL),
            sizeLabel
        );
    }

    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public void setTotalFiles(FileCount totalFiles) {
        this.totalFiles.set(totalFiles);
    }

    public void setTotalSize(FileSize totalSize) {
        this.totalSize.set(totalSize);
    }
}
