package sh.adelessfox.psarc.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class Fugue {
    private static final Fugue instance;

    private final Image sheet;
    private final Map<String, Rectangle2D> locations;

    static {
        try {
            instance = load();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Fugue(Image sheet, Map<String, Rectangle2D> locations) {
        this.sheet = sheet;
        this.locations = Map.copyOf(locations);
    }

    public static ImageView getImageView(String name) {
        return instance.getImageView0(name);
    }

    private static Fugue load() throws IOException {
        try (
            InputStream iconsInputStream = Fugue.class.getResourceAsStream("/fugue.png");
            InputStream namesInputStream = Fugue.class.getResourceAsStream("/fugue.txt")
        ) {
            if (namesInputStream == null || iconsInputStream == null) {
                throw new IOException("Failed to load Fugue sheet");
            }

            var sheet = new Image(iconsInputStream);
            var names = new BufferedReader(new InputStreamReader(namesInputStream)).lines().toList();

            var size = 16;
            var stride = (int) sheet.getWidth() / size;

            var locations = new HashMap<String, Rectangle2D>();
            for (int i = 0; i < names.size(); i++) {
                var name = names.get(i);
                var x = i % stride * size;
                var y = i / stride * size;
                locations.put(name, new Rectangle2D(x, y, size, size));
            }

            return new Fugue(sheet, locations);
        }
    }

    private ImageView getImageView0(String name) {
        var location = locations.get(name);
        if (location == null) {
            throw new IllegalArgumentException("Unknown icon: " + name);
        }
        var view = new ImageView(sheet);
        view.setViewport(location);
        view.setSmooth(false);
        return view;
    }
}