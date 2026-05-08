package ma.ensa.khouribga.smartstay;

import javafx.scene.Scene;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;

/**
 * Global theme manager for SmartStay PMS.
 * Dark  = "Nightly Blue"       — full-moon.mp4  (default)
 * Light = "Sky Blue & Green"   — endless-sky.mp4
 */
public class ThemeManager {

    private static boolean darkMode = true;
    private static final List<Scene> registeredScenes = new ArrayList<>();

    public static void applyToScene(Scene scene) {
        if (scene == null) return;
        registeredScenes.removeIf(s -> s == null);
        if (!registeredScenes.contains(scene)) registeredScenes.add(scene);
        applyTheme(scene);
    }

    public static void toggle() {
        darkMode = !darkMode;
        registeredScenes.removeIf(s -> s == null);
        for (Scene scene : registeredScenes) applyTheme(scene);
        // Swap background videos on ALL registered MediaViews
        VideoBackground.syncAll(darkMode);
    }

    public static boolean isDarkMode() { return darkMode; }

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
            if (!root.getStyleClass().contains("light-mode"))
                root.getStyleClass().add("light-mode");
        }
    }
}
