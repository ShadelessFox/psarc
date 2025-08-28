package sh.adelessfox.psarc.compression;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

final class OodleFFM {
    private final MethodHandle OodleLZ_Decompress;

    OodleFFM(Path path, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(path, arena);
        Linker linker = Linker.nativeLinker();

        OodleLZ_Decompress = linker.downcallHandle(
            lookup.findOrThrow("OodleLZ_Decompress"),
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,   // compBuf
                ValueLayout.JAVA_LONG, // compBufSize
                ValueLayout.ADDRESS,   // rawBuf
                ValueLayout.JAVA_LONG, // rawLen
                ValueLayout.JAVA_INT,  // fuzzSafe
                ValueLayout.JAVA_INT,  // checkCRC
                ValueLayout.JAVA_INT,  // verbosity
                ValueLayout.ADDRESS,   // decBufBase
                ValueLayout.JAVA_LONG, // decBufSize
                ValueLayout.ADDRESS,   // fpCallback
                ValueLayout.ADDRESS,   // callbackUserData
                ValueLayout.ADDRESS,   // decoderMemory
                ValueLayout.JAVA_LONG, // decoderMemorySize
                ValueLayout.JAVA_INT   // threadPhase
            )
        );
    }

    public long OodleLZ_Decompress(
        MemorySegment compBuf,
        long compBufSize,
        MemorySegment rawBuf,
        long rawLen,
        int fuzzSafe,
        int checkCRC,
        int verbosity,
        MemorySegment decBufBase,
        long decBufSize,
        MemorySegment fpCallback,
        MemorySegment callbackUserData,
        MemorySegment decoderMemory,
        long decoderMemorySize,
        int threadPhase
    ) {
        try {
            return (long) OodleLZ_Decompress.invokeExact(
                compBuf,
                compBufSize,
                rawBuf,
                rawLen,
                fuzzSafe,
                checkCRC,
                verbosity,
                decBufBase,
                decBufSize,
                fpCallback,
                callbackUserData,
                decoderMemory,
                decoderMemorySize,
                threadPhase
            );
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }
}