module psarc {
    requires atlantafx.base;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;

    exports sh.adelessfox.psarc to javafx.graphics;
}