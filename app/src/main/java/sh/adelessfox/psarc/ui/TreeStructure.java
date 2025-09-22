package sh.adelessfox.psarc.ui;

import java.util.List;

public interface TreeStructure<T extends TreeStructure<T>> {
    List<? extends T> getChildren();

    boolean hasChildren();
}