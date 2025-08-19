package sh.adelessfox.psarc.archive;

import sh.adelessfox.psarc.util.type.FilePath;

public record PsarcAssetId(String name) implements AssetId {
    public static PsarcAssetId of(String name) {
        return new PsarcAssetId(name);
    }

    @Override
    public String fullName() {
        return name;
    }

    @Override
    public FilePath toFilePath() {
        return FilePath.of(name, "/");
    }
}
