package sh.adelessfox.psarc.settings;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;

@Module
public interface SettingsModule {
    @Provides
    @Singleton
    static SettingsManager provideSettingsManager() {
        return new SettingsManager("PsarcViewer");
    }

    @Provides
    static Settings provideSettings(SettingsManager manager) {
        return manager.get();
    }
}
