package sh.adelessfox.psarc.archive.psarc;

import sh.adelessfox.psarc.archive.Asset;
import wtf.reversed.toolbox.type.FileSize;

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
