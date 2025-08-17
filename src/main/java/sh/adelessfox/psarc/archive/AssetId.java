package sh.adelessfox.psarc.archive;

import sh.adelessfox.psarc.util.FilePath;

public interface AssetId extends Comparable<AssetId> {
    String fullPath();

    FilePath toFilePath();

    @Override
    default int compareTo(AssetId o) {
        return fullPath().compareTo(o.fullPath());
    }
}
