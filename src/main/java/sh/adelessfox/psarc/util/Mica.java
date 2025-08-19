package sh.adelessfox.psarc.util;

import javafx.scene.Node;
import javafx.stage.Window;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.foreign.ValueLayout.*;

/**
 * A class that enables Mica material for a JavaFX window.
 * <p>
 * <b>Note:</b> it can only be used on Windows.
 */
public final class Mica {
    private static final Arena arena = Arena.ofAuto();

    private static final Method Window_getPeer;
    private static final Method TKStage_getRawHandle;

    private static final String STYLE_MICA = "mica";

    static {
        try {
            Window_getPeer = Window.class.getDeclaredMethod("getPeer");
            Window_getPeer.setAccessible(true);

            TKStage_getRawHandle = Window_getPeer.getReturnType().getDeclaredMethod("getRawHandle");
            TKStage_getRawHandle.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Mica() {
    }

    public static boolean isMicaSupported() {
        return OperatingSystem.isWindows() && isWin11();
    }

    public static void install(Window window) {
        setEnabled(window, true);
    }

    public static void uninstall(Window window) {
        setEnabled(window, false);
    }

    public static void installStyle(Node node) {
        if (isMicaSupported() && !node.getStyleClass().contains(STYLE_MICA)) {
            node.getStyleClass().add(STYLE_MICA);
        }
    }

    public static void uninstallStyle(Node node) {
        if (isMicaSupported()) {
            node.getStyleClass().remove(STYLE_MICA);
        }
    }

    private static void setEnabled(Window window, boolean enable) {
        if (!isMicaSupported()) {
            throw new UnsupportedOperationException("Mica is not supported on the current platform");
        }

        var hwnd = getWindowHandle(window)
            .orElseThrow(() -> new IllegalStateException("Window peer is not available"));

        int attr;
        int value;

        if (isWin11_22H2()) {
            attr = Dwmapi.DWMWA_SYSTEMBACKDROP_TYPE;
            value = enable ? Dwmapi.DWMSBT_TABBEDWINDOW : Dwmapi.DWMSBT_DISABLE;
        } else {
            attr = Dwmapi.DWMWA_MICA_EFFECT;
            value = enable ? Dwmapi.DWMSBT_MAINWINDOW : 0;
        }

        try (Arena arena = Arena.ofConfined()) {
            var buffer = arena.allocateFrom(JAVA_INT, value);
            var result = Dwmapi.DwmSetWindowAttribute(hwnd, attr, buffer, (int) buffer.byteSize());
            if (result < 0) {
                throw new IllegalStateException("Failed to set DWM attribute: " + result);
            }
        }
    }

    private static Optional<MemorySegment> getWindowHandle(Window window) {
        try {
            var peer = Window_getPeer.invoke(window);
            if (peer == null) {
                return Optional.empty();
            }
            var handle = (long) TKStage_getRawHandle.invoke(peer);
            return Optional.of(MemorySegment.ofAddress(handle));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static OptionalInt getBuildNumber() {
        try (Arena arena = Arena.ofConfined()) {
            // OSVERSIONINFOEX
            MemorySegment info = arena.allocate(156);

            // dwOSVersionInfoSize
            info.set(JAVA_INT, 0, (int) info.byteSize());

            if (Ntdll.RtlGetVersion(info) >= 0) {
                // dwBuildNumber
                return OptionalInt.of(info.get(JAVA_INT, 12));
            }
        }

        return OptionalInt.empty();
    }

    private static boolean isWin11() {
        return getBuildNumber().orElse(0) >= 22000;
    }

    private static boolean isWin11_22H2() {
        return getBuildNumber().orElse(0) >= 22621;
    }

    private static final class Ntdll {
        private static final MethodHandle RtlGetVersion$MH;

        static {
            var linker = Linker.nativeLinker();
            var lookup = SymbolLookup.libraryLookup("ntdll", arena);

            RtlGetVersion$MH = linker.downcallHandle(
                lookup.findOrThrow("RtlGetVersion"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS)
            );
        }

        static int RtlGetVersion(MemorySegment lpVersionInformation) {
            try {
                return (int) RtlGetVersion$MH.invokeExact(lpVersionInformation);
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
        }
    }

    private static final class Dwmapi {
        private static final MethodHandle DwmSetWindowAttribute$MH;

        static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        static final int DWMWA_MICA_EFFECT = 1029;
        static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;

        // DWM_SYSTEMBACKDROP_TYPE
        static final int DWMSBT_AUTO = 0;            // Auto
        static final int DWMSBT_DISABLE = 1;         // None
        static final int DWMSBT_MAINWINDOW = 2;      // Mica
        static final int DWMSBT_TRANSIENTWINDOW = 3; // Acrylic
        static final int DWMSBT_TABBEDWINDOW = 4;    // Tabbed

        static {
            var linker = Linker.nativeLinker();
            var lookup = SymbolLookup.libraryLookup("dwmapi", arena);

            DwmSetWindowAttribute$MH = linker.downcallHandle(
                lookup.findOrThrow("DwmSetWindowAttribute"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT)
            );
        }

        static int DwmSetWindowAttribute(MemorySegment hwnd, int dwAttribute, MemorySegment pvAttribute, int cbAttribute) {
            try {
                return (int) DwmSetWindowAttribute$MH.invokeExact(hwnd, dwAttribute, pvAttribute, cbAttribute);
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
        }
    }
}
