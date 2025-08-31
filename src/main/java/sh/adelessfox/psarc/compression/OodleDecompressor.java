package sh.adelessfox.psarc.compression;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;

final class OodleDecompressor extends Decompressor {
    private static final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/oo2core_*_win64.dll");

    private final Arena arena;
    private final OodleFFM library;

    public OodleDecompressor() throws IOException {
        var root = Path.of("").toAbsolutePath();
        var path = findOodleLibrary(root);

        this.arena = Arena.ofConfined();
        this.library = new OodleFFM(path, arena);
    }

    @Override
    public void decompress(ByteBuffer src, ByteBuffer dst) throws IOException {
        try (var arena = Arena.ofConfined()) {
            var srcSegment = arena.allocate(src.remaining()).copyFrom(MemorySegment.ofBuffer(src));
            var dstSegment = arena.allocate(dst.remaining());

            var result = library.OodleLZ_Decompress(
                srcSegment, srcSegment.byteSize(),
                dstSegment, dstSegment.byteSize(),
                1, 1, 0,
                MemorySegment.NULL, 0,
                MemorySegment.NULL, MemorySegment.NULL,
                MemorySegment.NULL, 0,
                3
            );

            if (result != dst.remaining()) {
                throw new IOException("Error decompressing data");
            }

            MemorySegment.ofBuffer(dst).copyFrom(dstSegment);
        }
    }

    @Override
    public void close() {
        arena.close();
    }

    private static Path findOodleLibrary(Path root) throws IOException {
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                .filter(matcher::matches)
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("Couldn't find a suitable Oodle library file. Please obtain oo2core_X_win64.dll and put it in " + root.toAbsolutePath()));
        }
    }
}
