package sh.adelessfox.psarc.util;

import javafx.stage.Window;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * A class that enables Mica material for a JavaFX window.
 * <p>
 * <b>Note:</b> it can only be used on Windows.
 */
public final class Mica {
    private static final Arena arena = Arena.ofAuto();

    private static final Method Window_getPeer;
    private static final Method TKStage_getRawHandle;

    private static final int WINDOWS_11_22H2 = 22621;

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

    public static void install(Window window) {
        setEnabled(window, true);
    }

    public static void uninstall(Window window) {
        setEnabled(window, false);
    }

    private static void setEnabled(Window window, boolean enable) {
        long handle = getWindowHandle(window).orElseThrow(() -> new IllegalStateException("Window peer is not available"));

        var modern = getBuildNumber().orElse(0) >= WINDOWS_11_22H2;
        int attr = modern ? Dwmapi.DWMWA_SYSTEMBACKDROP_TYPE : Dwmapi.DWMWA_MICA_EFFECT;
        int value = modern ? (enable ? Dwmapi.DWMSBT_MAINWINDOW : Dwmapi.DWMSBT_AUTO) : (enable ? 1 : 0);

        try (Arena arena = Arena.ofConfined()) {
            var hwnd = MemorySegment.ofAddress(handle);
            var data = arena.allocateFrom(JAVA_INT, value);

            var result = Dwmapi.DwmSetWindowAttribute(hwnd, attr, data, (int) data.byteSize());
            if (result < 0) {
                throw new IllegalStateException("Failed to set DWM attribute: " + result);
            }
        }
    }

    private static OptionalLong getWindowHandle(Window window) {
        try {
            var peer = Window_getPeer.invoke(window);
            if (peer == null) {
                return OptionalLong.empty();
            }
            var handle = TKStage_getRawHandle.invoke(peer);
            return OptionalLong.of((long) handle);
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
        static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
        static final int DWMWA_MICA_EFFECT = 1039;

        static final int DWMSBT_AUTO = 0;
        static final int DWMSBT_MAINWINDOW = 2;

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
