package sh.adelessfox.psarc.archive;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Stream;

public interface Archive<K extends AssetId, V extends Asset<K>> extends Closeable {
    Optional<V> get(K key);

    Stream<V> getAll();

    ByteBuffer read(K key) throws IOException;
}
