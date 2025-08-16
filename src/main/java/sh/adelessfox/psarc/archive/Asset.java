package sh.adelessfox.psarc.archive;

public interface Asset<T extends AssetId> extends Comparable<Asset<T>> {
    T id();

    int size();

    @Override
    default int compareTo(Asset o) {
        return id().compareTo(o.id());
    }
}
