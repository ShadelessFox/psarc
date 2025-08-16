package sh.adelessfox.psarc.archive;

public interface AssetId extends Comparable<AssetId> {
    String fullName();

    @Override
    default int compareTo(AssetId o) {
        return fullName().compareTo(o.fullName());
    }
}
