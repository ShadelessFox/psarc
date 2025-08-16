package sh.adelessfox.psarc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;
import sh.adelessfox.psarc.ui.StructuredTreeItem;
import sh.adelessfox.psarc.ui.TreeStructure;
import sh.adelessfox.psarc.util.FilePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main extends Application {
    public void start(Stage stage) {
        PsarcArchive archive;

        try {
            archive = new PsarcArchive(Path.of("D:/PlayStation Games/Until Dawn TEST70002/USRDIR/data_ps3.psarc"), ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var structure = ArchiveStructure.fromArchive(archive);

        var view = new TreeView<Element>();
        view.setRoot(new StructuredTreeItem<>(structure));
        view.setShowRoot(false);

        stage.setTitle("My JavaFX Application");
        stage.setScene(new Scene(view));
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private record ArchiveStructure(
        PsarcArchive archive,
        TreeMap<FilePath, PsarcAsset> paths
    ) implements TreeStructure<Element> {
        static ArchiveStructure fromArchive(PsarcArchive archive) {
            TreeMap<FilePath, PsarcAsset> files = archive.getAll()
                .collect(Collectors.toMap(
                    (PsarcAsset asset) -> FilePath.of(asset.id().name().split("/")),
                    Function.identity(),
                    (a, _) -> {
                        throw new IllegalStateException("Duplicate asset: " + a);
                    },
                    TreeMap::new
                ));

            return new ArchiveStructure(archive, files);
        }

        @Override
        public Element getRoot() {
            return new Element.Folder(null, FilePath.of());
        }

        @Override
        public List<? extends Element> getChildren(Element parent) {
            var files = paths.subMap(parent.path(), parent.path().concat("*"));
            var children = new HashMap<FilePath, Element>();

            for (FilePath prefix : getCommonPrefixes(files.keySet(), parent.path().length())) {
                if (parent.path().equals(prefix)) {
                    continue;
                }
                children.computeIfAbsent(prefix, p -> new Element.Folder(parent, p));
            }

            files.forEach((path, asset) -> {
                if (path.length() == parent.path().length() + 1) {
                    children.computeIfAbsent(path, f -> new Element.File(parent, f, asset));
                }
            });

            return children.values().stream()
                .sorted(Comparator
                    .comparingInt((Element path) -> hasChildren(path) ? -1 : 1)
                    .thenComparing(Element::toString))
                .toList();
        }

        @Override
        public boolean hasChildren(Element parent) {
            return paths.ceilingKey(parent.path()) != parent.path();
        }

        private static List<FilePath> getCommonPrefixes(Collection<FilePath> paths, int offset) {
            return paths.stream()
                .collect(Collectors.groupingBy(p -> p.get(offset)))
                .values().stream()
                .map(p -> getCommonPrefix(p, offset))
                .toList();
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

    private sealed interface Element {
        FilePath path();

        record File(Element parent, FilePath path, PsarcAsset asset) implements Element {
            @Override
            public String toString() {
                return path.last();
            }
        }

        record Folder(Element parent, FilePath path) implements Element {
            @Override
            public String toString() {
                if (parent instanceof Folder folder) {
                    return path.subpath(folder.path.length()).full("\u2009/\u2009");
                } else if (path.length() > 0) {
                    return path.last();
                } else {
                    return "<root>";
                }
            }
        }
    }
}