package ma.ensa.khouribga.smartstay;

import javafx.scene.Scene;
import javafx.scene.Parent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Global theme manager for SmartStay PMS.
 * Dark  = "Bloody Samurai" — bloodborne.mp4  (default)
 * Light = "Pink Blossom"   — sakura.mp4
 *
 * Scene references are held weakly so that navigating away does not
 * prevent garbage-collection of old scenes.
 */
public class ThemeManager {

    private static boolean darkMode = true;
    private static final List<WeakReference<Scene>> registeredScenes = new ArrayList<>();

    public static void applyToScene(Scene scene) {
        if (scene == null) return;
        // Remove stale refs and any existing registration for this scene
        registeredScenes.removeIf(ref -> ref.get() == null || ref.get() == scene);
        registeredScenes.add(new WeakReference<>(scene));
        applyTheme(scene);
    }

    public static void toggle() {
        darkMode = !darkMode;
        registeredScenes.removeIf(ref -> ref.get() == null);
        for (WeakReference<Scene> ref : registeredScenes) {
            Scene scene = ref.get();
            if (scene != null) applyTheme(scene);
        }
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