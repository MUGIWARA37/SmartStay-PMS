package ma.ensa.khouribga.smartstay;

import javafx.scene.Scene;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Global theme manager for SmartStay PMS.
 * Dark  = "Bloody Samurai"  (default)
 * Light = "Pink Blossom"
 *
 * Call {@link #applyToScene(Scene)} whenever a new scene is created.
 * Call {@link #toggle()} to switch theme; all registered scenes update instantly.
 */
public class ThemeManager {

    private static boolean darkMode = true;

    // WeakReferences so GC can collect old scenes
    private static final List<Scene> registeredScenes = new ArrayList<>();

    /** Apply the current theme to a newly created scene and register it for future toggles. */
    public static void applyToScene(Scene scene) {
        if (scene == null) return;
        registeredScenes.removeIf(s -> s == null);
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        applyTheme(scene);
    }

    /** Toggle between dark and light mode. Updates all currently registered scenes. */
    public static void toggle() {
        darkMode = !darkMode;
        registeredScenes.removeIf(s -> s == null);
        for (Scene scene : registeredScenes) {
            applyTheme(scene);
        }
    }

    /** @return true if currently in dark (Bloody Samurai) mode */
    public static boolean isDarkMode() {
        return darkMode;
    }

    /** @return the appropriate emoji label for the current mode toggle button */
    public static String getToggleLabel() {
        return darkMode ? "☀ Light" : "🌙 Dark";
    }

    private static void applyTheme(Scene scene) {
        if (scene == null) return;
        Parent root = scene.getRoot();
        if (root == null) return;
        if (darkMode) {
            root.getStyleClass().remove("light-mode");
        } else {
            if (!root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().add("light-mode");
            }
        }
    }
}
