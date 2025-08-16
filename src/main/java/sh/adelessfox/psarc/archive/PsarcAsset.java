package sh.adelessfox.psarc.archive;

public record PsarcAsset(
    PsarcAssetId id,
    int blockOffset,
    long uncompressedSize,
    long fileOffset
) implements Asset<PsarcAssetId> {
    @Override
    public int size() {
        return Math.toIntExact(uncompressedSize);
    }
}
