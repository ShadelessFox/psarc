package sh.adelessfox.psarc.settings;

import java.nio.file.Path;
import java.util.List;

public final class Settings {
    private final Setting<List<Path>> recentPaths = new Setting<>();
    private final Setting<Path> lastDirectory = new Setting<>();

    Settings() {
    }

    public Setting<List<Path>> recentPaths() {
        return recentPaths;
    }

    public Setting<Path> lastDirectory() {
        return lastDirectory;
    }
}
