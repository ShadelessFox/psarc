package sh.adelessfox.psarc;

import sh.adelessfox.psarc.archive.Archive;
import sh.adelessfox.psarc.archive.Asset;
import sh.adelessfox.psarc.ui.TreeStructure;
import sh.adelessfox.psarc.util.FilePath;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        var files = new TreeMap<FilePath, T>();
        for (T asset : archive.getAll()) {
            files.put(asset.id().toFilePath(), asset);
        }
        return Folder.of(files, null, FilePath.of());
    }

    static final class File<T extends Asset<?>> extends ArchiveStructure<T> {
        private static final MessageFormat FORMAT = new MessageFormat("{0,number,#.##} {1,choice,0#B|1#kB|2#mB|3#gB}");

        final T asset;

        File(FilePath path, T asset, String name, String size) {
            super(null, path, name, size);
            this.asset = asset;
        }

        static <T extends Asset<?>> File<T> of(FilePath path, T asset) {
            return new File<>(path, asset, path.last(), toDisplaySize(asset.size()));
        }

        @Override
        public List<? extends ArchiveStructure<T>> getChildren() {
            return List.of();
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        private static String toDisplaySize(int size) {
            var exp = (int) (Math.log10(size) / Math.log10(1024)) - 1;
            var rem = (double) size / (1L << 10 * exp);
            return FORMAT.format(new Object[]{rem, exp});
        }
    }

    static final class Folder<T extends Asset<?>> extends ArchiveStructure<T> {
        private static final MessageFormat FORMAT = new MessageFormat("{0,choice,1#{0} file|1<{0} files}");

        Folder(NavigableMap<FilePath, T> paths, FilePath path, String name, String size) {
            super(paths, path, name, size);
        }

        static <T extends Asset<?>> Folder<T> of(NavigableMap<FilePath, T> paths, Folder<T> parent, FilePath path) {
            var files = paths.subMap(path, true, path.concat("*"), true);
            var prefixes = getCommonPrefixes(files.keySet(), path.length());

            var name = parent != null ? toDisplayName(parent, path) : "";
            var size = FORMAT.format(new Object[]{prefixes.size()});

            return new Folder<>(paths, path, name, size);
        }

        @Override
        public List<? extends ArchiveStructure<T>> getChildren() {
            var files = paths.subMap(path, true, path.concat("*"), false);
            var children = new HashMap<FilePath, ArchiveStructure<T>>();

            for (FilePath prefix : getCommonPrefixes(files.keySet(), path.length())) {
                if (path.equals(prefix)) {
                    // Same folder
                    continue;
                }
                children.computeIfAbsent(prefix, p -> Folder.of(files, this, p));
            }

            files.forEach((path, asset) -> {
                if (path.length() == this.path.length() + 1) {
                    // File is a direct child of this folder
                    children.computeIfAbsent(path, f -> File.of(f, asset));
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

        private static Set<FilePath> getCommonPrefixes(Collection<FilePath> paths, int offset) {
            return paths.stream()
                .collect(Collectors.groupingBy(p -> p.get(offset)))
                .values().stream()
                .map(p -> getCommonPrefix(p, offset))
                .collect(Collectors.toSet());
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
    }
}
