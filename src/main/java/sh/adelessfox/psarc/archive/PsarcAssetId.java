package sh.adelessfox.psarc.archive;

public record PsarcAssetId(String name) implements AssetId {
    public static PsarcAssetId of(String name) {
        return new PsarcAssetId(name);
    }

    @Override
    public String fullName() {
        return name;
    }
}
