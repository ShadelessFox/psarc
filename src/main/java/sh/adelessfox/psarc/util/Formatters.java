package sh.adelessfox.psarc.util;

import java.text.MessageFormat;

public final class Formatters {
    private static final MessageFormat SIZE_FORMAT = new MessageFormat("{0,number,#.##} {1,choice,0#B|1#kB|2#mB|3#gB}");
    private static final MessageFormat FILES_FORMAT = new MessageFormat("{0,choice,0#no files|1#one file|1<{0,number,integer} files}");

    private Formatters() {
    }

    public static String formatFiles(long files) {
        return FILES_FORMAT.format(new Object[]{files});
    }

    public static String formatSize(long size) {
        var exp = (int) (Math.log10(size) / Math.log10(1024));
        var rem = (double) size / (1L << 10 * exp);
        return SIZE_FORMAT.format(new Object[]{rem, exp});
    }
}
