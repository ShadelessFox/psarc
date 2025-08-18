module psarc.app {
    requires atlantafx.base;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.google.gson;
    requires dagger;
    requires devtoolsfx.gui;
    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;

    exports sh.adelessfox.psarc to javafx.graphics;
    exports sh.adelessfox.psarc.ui to javafx.graphics;
    exports sh.adelessfox.psarc.archive;

    opens sh.adelessfox.psarc.settings to com.google.gson;
}