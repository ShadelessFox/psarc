package sh.adelessfox.psarc.archive;

import sh.adelessfox.psarc.util.type.FileSize;

public record PsarcAsset(
    PsarcAssetId id,
    int blockOffset,
    long uncompressedSize,
    long fileOffset
) implements Asset<PsarcAssetId> {
    @Override
    public FileSize size() {
        return FileSize.ofBytes(uncompressedSize);
    }
}
