package sh.adelessfox.psarc.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.List;

public final class StructuredTreeItem<T extends TreeStructure<T>> extends TreeItem<T> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;

    public StructuredTreeItem(T structure) {
        super(structure);
    }

    @Override
    public ObservableList<TreeItem<T>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren());
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            isLeaf = !getValue().hasChildren();
        }
        return isLeaf;
    }

    private List<? extends TreeItem<T>> buildChildren() {
        return getValue().getChildren().stream()
            .map(StructuredTreeItem::new)
            .toList();
    }
}
