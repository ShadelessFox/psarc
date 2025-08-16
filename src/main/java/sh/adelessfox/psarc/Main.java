package sh.adelessfox.psarc;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sh.adelessfox.psarc.archive.PsarcArchive;
import sh.adelessfox.psarc.archive.PsarcAsset;
import sh.adelessfox.psarc.ui.StructuredTreeItem;
import sh.adelessfox.psarc.ui.TreeStructure;
import sh.adelessfox.psarc.util.FilePath;
import sh.adelessfox.psarc.util.Fugue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main extends Application {
    public static void main(String[] args) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.launch(args);
    }

    public void start(Stage stage) {
        Path path = Path.of("D:/PlayStation Games/Until Dawn TEST70002/USRDIR/data_ps3.psarc");
        PsarcArchive archive;

        try {
            archive = new PsarcArchive(path, ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var menu = buildMenuBar();
        var view = buildTreeTableView(archive);
        var root = new VBox(menu, view);
        VBox.setVgrow(view, Priority.ALWAYS);

        stage.setTitle("PSARC Explorer - " + path);
        stage.setScene(new Scene(root));
        stage.setWidth(750);
        stage.setHeight(720);
        stage.show();
    }

    private static MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("_File");
        Menu aboutMenu = new Menu("_About");

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, aboutMenu);

        return menuBar;
    }

    private static TreeTableView<Element> buildTreeTableView(PsarcArchive archive) {
        var structure = ArchiveStructure.fromArchive(archive);

        var view = new TreeTableView<Element>();
        view.getStyleClass().add(Styles.DENSE);
        view.setRoot(new StructuredTreeItem<>(structure));
        view.setShowRoot(false);
        view.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.getColumns().setAll(buildTreeTableColumns());

        return view;
    }

    private static List<TreeTableColumn<Element, ?>> buildTreeTableColumns() {
        var nameColumn = new TreeTableColumn<Element, Element>("Name");
        nameColumn.setReorderable(false);
        nameColumn.setSortable(false);
        nameColumn.setCellValueFactory(features -> features.getValue().valueProperty());
        nameColumn.setCellFactory(_ -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name);
                    setGraphic(Fugue.getImageView(item instanceof Element.File ? "document" : "folder"));
                }
            }
        });

        var sizeColumn = new TreeTableColumn<Element, Element>("Size");
        sizeColumn.setReorderable(false);
        sizeColumn.setSortable(false);
        sizeColumn.setMinWidth(100);
        sizeColumn.setMaxWidth(100);
        sizeColumn.setCellValueFactory(features -> features.getValue().valueProperty());
        sizeColumn.setCellFactory(_ -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText(item.size);
                }
            }
        });

        return List.of(nameColumn, sizeColumn);
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
            return new Element.Folder(null, FilePath.of(), 0);
        }

        @Override
        public List<? extends Element> getChildren(Element parent) {
            var files = paths.subMap(parent.path, parent.path.concat("*"));
            var children = new HashMap<FilePath, Element>();

            for (FilePath prefix : getCommonPrefixes(files.keySet(), parent.path.length())) {
                if (parent.path.equals(prefix)) {
                    continue;
                }
                children.computeIfAbsent(prefix, p -> {
                    var files1 = paths.subMap(p, p.concat("*"));
                    var prefixes1 = getCommonPrefixes(files1.keySet(), p.length());
                    return new Element.Folder(parent, p, prefixes1.size());
                });
            }

            files.forEach((path, asset) -> {
                if (path.length() == parent.path.length() + 1) {
                    children.computeIfAbsent(path, f -> new Element.File(f, asset));
                }
            });

            return children.values().stream()
                .sorted(Comparator
                    .comparingInt((Element e) -> hasChildren(e) ? -1 : 1)
                    .thenComparing((Element e) -> e.name))
                .toList();
        }

        @Override
        public boolean hasChildren(Element parent) {
            return paths.ceilingKey(parent.path) != parent.path;
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

    private static sealed class Element {
        final FilePath path;

        final String name;
        final String size;

        Element(FilePath path, String name, String size) {
            this.path = path;
            this.name = name;
            this.size = size;
        }

        static final class File extends Element {
            final PsarcAsset asset;

            File(FilePath path, PsarcAsset asset) {
                super(path, path.last(), toDisplaySize(asset.size()));
                this.asset = asset;
            }

            private static String toDisplaySize(int size) {
                double value = size;
                int base = 0;
                while (value >= 1024 && base < 6) {
                    value /= 1024;
                    base += 1;
                }
                var unit = switch (base) {
                    case 0 -> "B";
                    case 1 -> "kB";
                    case 2 -> "mB";
                    case 3 -> "gB";
                    case 4 -> "tB";
                    case 5 -> "pB";
                    case 6 -> "eB";
                    default -> throw new IllegalStateException();
                };
                return "%.2f %s".formatted(value, unit);
            }
        }

        static final class Folder extends Element {
            Folder(Element parent, FilePath path, int elements) {
                super(path, toDisplayString(parent, path), "%d items".formatted(elements));
            }

            private static String toDisplayString(Element parent, FilePath path) {
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