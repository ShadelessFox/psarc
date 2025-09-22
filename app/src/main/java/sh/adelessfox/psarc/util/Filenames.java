package sh.adelessfox.psarc.util;

import java.nio.file.Path;

public final class Filenames {
    private Filenames() {
    }

    public static String getExtension(Path path) {
        return getExtension(path.getFileName().toString());
    }

    public static String getExtension(String filename) {
        int index = indexOfExtension(filename);
        return index < 0 ? "" : filename.substring(index + 1);
    }

    private static int indexOfExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        int slashIndex = filename.lastIndexOf('/');
        return slashIndex > dotIndex ? -1 : dotIndex;
    }
}
