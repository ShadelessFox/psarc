package sh.adelessfox.psarc.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.List;

public final class StructuredTreeItem<T> extends TreeItem<T> {
    private final TreeStructure<T> structure;
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;

    public StructuredTreeItem(TreeStructure<T> structure, T value) {
        super(value);
        this.structure = structure;
    }

    public StructuredTreeItem(TreeStructure<T> structure) {
        this(structure, structure.getRoot());
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
            isLeaf = !structure.hasChildren(getValue());
        }
        return isLeaf;
    }

    private List<? extends TreeItem<T>> buildChildren() {
        return structure.getChildren(getValue()).stream()
            .map(child -> new StructuredTreeItem<>(structure, child))
            .toList();
    }
}
