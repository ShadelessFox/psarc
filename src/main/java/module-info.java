module psarc.app {
    requires atlantafx.base;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.google.gson;
    requires dagger;
    requires devtoolsfx.gui;
    requires info.picocli;
    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;

    exports sh.adelessfox.psarc.archive;
    exports sh.adelessfox.psarc.ui;
    exports sh.adelessfox.psarc.util.type;
    exports sh.adelessfox.psarc.util;
    exports sh.adelessfox.psarc;

    opens sh.adelessfox.psarc.settings to com.google.gson;
    opens sh.adelessfox.psarc.cli to info.picocli;
}