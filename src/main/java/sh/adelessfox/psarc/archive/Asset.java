package sh.adelessfox.psarc.archive;

import wtf.reversed.toolbox.type.FileSize;

public interface Asset<T extends AssetId> extends Comparable<Asset<T>> {
    T id();

    FileSize size();

    @Override
    default int compareTo(Asset o) {
        return id().compareTo(o.id());
    }
}
