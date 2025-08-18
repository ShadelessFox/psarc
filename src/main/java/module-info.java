module psarc.app {
    requires atlantafx.base;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.google.gson;
    requires devtoolsfx.gui;
    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;

    exports sh.adelessfox.psarc to javafx.graphics;
    exports sh.adelessfox.psarc.archive;
    exports sh.adelessfox.psarc.settings to javafx.graphics;

    opens sh.adelessfox.psarc.settings to com.google.gson;
}