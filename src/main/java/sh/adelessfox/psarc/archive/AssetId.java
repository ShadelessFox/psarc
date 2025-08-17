package sh.adelessfox.psarc.archive;

import sh.adelessfox.psarc.util.FilePath;

public interface AssetId extends Comparable<AssetId> {
    String fullName();

    default String fileName() {
        return toFilePath().last();
    }

    FilePath toFilePath();

    @Override
    default int compareTo(AssetId o) {
        return fullName().compareTo(o.fullName());
    }
}
