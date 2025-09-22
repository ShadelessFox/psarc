package sh.adelessfox.psarc;

import dagger.Component;
import jakarta.inject.Singleton;
import sh.adelessfox.psarc.settings.Settings;
import sh.adelessfox.psarc.settings.SettingsModule;

@Singleton
@Component(modules = SettingsModule.class)
interface AppComponent {
    Settings settings();
}
