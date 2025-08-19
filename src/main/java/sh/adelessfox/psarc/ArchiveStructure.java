package sh.adelessfox.psarc;

import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.ui.TreeStructure;
import sh.adelessfox.psarc.util.FilePath;
import sh.adelessfox.psarc.util.type.FileCount;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

sealed abstract class ArchiveStructure<T extends Asset<?>> implements TreeStructure<ArchiveStructure<T>> {
    final NavigableMap<FilePath, T> paths;
    final FilePath path;
    final String name;
    final String size;

    private ArchiveStructure(NavigableMap<FilePath, T> paths, FilePath path, String name, String size) {
        this.paths = paths;
        this.path = path;
        this.name = name;
        this.size = size;
    }

    static <T extends Asset<?>> ArchiveStructure<T> of(Archive<?, T> archive) {
        var paths = new TreeMap<FilePath, T>();
        for (T asset : archive.getAll()) {
            paths.put(asset.id().toFilePath(), asset);
        }
        return Folder.of(paths, null, FilePath.of());
    }

    static final class File<T extends Asset<?>> extends ArchiveStructure<T> {
        final T asset;

        File(FilePath path, T asset, String name, String size) {
            super(null, path, name, size);
            this.asset = asset;
        }

        static <T extends Asset<?>> File<T> of(FilePath path, T asset) {
            return new File<>(path, asset, path.last(), asset.size().toString());
        }

        @Override
        public List<? extends ArchiveStructure<T>> getChildren() {
            return List.of();
        }

        @Override
        public boolean hasChildren() {
            return false;
        }
    }

    static final class Folder<T extends Asset<?>> extends ArchiveStructure<T> {
        Folder(NavigableMap<FilePath, T> paths, FilePath path, String name, String size) {
            super(paths, path, name, size);
        }

        static <T extends Asset<?>> Folder<T> of(NavigableMap<FilePath, T> paths, Folder<T> parent, FilePath path) {
            var children = FileCount.of(collectChildren(paths, path).count());
            var name = parent != null ? toDisplayName(parent, path) : "";
            return new Folder<>(paths, path, name, children.toString());
        }

        @Override
        public List<? extends ArchiveStructure<T>> getChildren() {
            var children = new HashMap<FilePath, ArchiveStructure<T>>();

            collectChildren(paths, path).forEach(child -> {
                if (child.asset() != null) {
                    children.computeIfAbsent(child.path(), path -> File.of(path, child.asset()));
                } else {
                    children.computeIfAbsent(child.path(), path -> Folder.of(paths, this, path));
                }
            });

            return children.values().stream()
                .sorted(Comparator
                    .comparingInt((ArchiveStructure<T> e) -> e.hasChildren() ? -1 : 1)
                    .thenComparing((ArchiveStructure<T> e) -> e.name))
                .toList();
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        private static String toDisplayName(Folder<?> parent, FilePath path) {
            return path.subpath(parent.path.length()).full("\u2009/\u2009");
        }

        private static <T extends Asset<?>> Stream<PathWithAsset<T>> collectChildren(NavigableMap<FilePath, T> paths, FilePath path) {
            var children = paths.subMap(path, true, path.concat("*"), false);
            return Stream.concat(
                collectFolders(children, path),
                collectFiles(children, path)
            );
        }

        private static <T extends Asset<?>> Stream<PathWithAsset<T>> collectFolders(NavigableMap<FilePath, T> paths, FilePath path) {
            return collectCommonPrefixes(paths.keySet(), path)
                .map(p -> new PathWithAsset<>(p, null));
        }

        private static <T extends Asset<?>> Stream<PathWithAsset<T>> collectFiles(NavigableMap<FilePath, T> paths, FilePath path) {
            return paths.entrySet().stream()
                .filter(entry -> path.length() + 1 == entry.getKey().length())
                .map(entry -> new PathWithAsset<>(entry.getKey(), entry.getValue()));
        }

        private static Stream<FilePath> collectCommonPrefixes(Collection<FilePath> paths, FilePath path) {
            int offset = path.length();
            return paths.stream()
                .collect(Collectors.groupingBy(p -> p.get(offset)))
                .values().stream()
                .map(p -> getCommonPrefix(p, offset))
                .distinct()
                .filter(p -> !p.equals(path));
        }

        private static FilePath getCommonPrefix(List<FilePath> paths, int offset) {
            var path = paths.getFirst();
            int position = Math.min(offset, path.length() - 1);

            for (; position < path.length() - 1; position++) {
                for (FilePath other : paths) {
                    if (other.length() < position || !path.get(position).equals(other.get(position))) {
                        return path.subpath(0, position);
                    }
                }
            }

            return path.subpath(0, position);
        }

        private record PathWithAsset<T extends Asset<?>>(FilePath path, T asset) {
        }
    }
}
