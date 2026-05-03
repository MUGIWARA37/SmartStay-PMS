package ma.ensa.khouribga.smartstay;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages looping cinematic video backgrounds across all scenes.
 *
 * Dark  mode → bloodborne.mp4  ("Bloody Samurai")
 * Light mode → sakura.mp4      ("Pink Blossom")
 *
 * MediaView references are held weakly so that navigating away does not
 * prevent garbage-collection of old scenes and their media views.
 *
 * Usage — in each controller's initialize():
 *   VideoBackground.register(bgMediaView);
 *
 * ThemeManager.toggle() automatically calls VideoBackground.syncAll()
 * so every registered MediaView switches video simultaneously.
 */
public class VideoBackground {

    private static final String DARK_VIDEO  = "/videos/bloodborne.mp4";
    private static final String LIGHT_VIDEO = "/videos/sakura.mp4";

    private static final List<WeakReference<MediaView>> views = new ArrayList<>();

    /** Register a MediaView to be managed. Call once per controller init. */
    public static void register(MediaView view) {
        if (view == null) return;
        // Remove stale refs and any existing registration for this view
        views.removeIf(ref -> ref.get() == null || ref.get() == view);
        views.add(new WeakReference<>(view));
        applyVideo(view, ThemeManager.isDarkMode());

        // Bind size to scene when scene becomes available
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                view.fitWidthProperty().bind(newScene.widthProperty());
                view.fitHeightProperty().bind(newScene.heightProperty());
            }
        });
        if (view.getScene() != null) {
            view.fitWidthProperty().bind(view.getScene().widthProperty());
            view.fitHeightProperty().bind(view.getScene().heightProperty());
        }
    }

    /** Called by ThemeManager.toggle() — swaps video on every registered view. */
    public static void syncAll(boolean darkMode) {
        Platform.runLater(() -> {
            views.removeIf(ref -> ref.get() == null);
            for (WeakReference<MediaView> ref : views) {
                MediaView view = ref.get();
                if (view != null) applyVideo(view, darkMode);
            }
        });
    }

    private static void applyVideo(MediaView view, boolean darkMode) {
        String path = darkMode ? DARK_VIDEO : LIGHT_VIDEO;
        URL url = VideoBackground.class.getResource(path);
        if (url == null) {
            // Fallback: if bloodborne.mp4 not found yet, always use sakura
            url = VideoBackground.class.getResource(LIGHT_VIDEO);
        }
        if (url == null) return;

        // Stop old player cleanly
        MediaPlayer old = view.getMediaPlayer();
        if (old != null) {
            old.stop();
            old.dispose();
        }

        Media media = new Media(url.toExternalForm());
        MediaPlayer player = new MediaPlayer(media);
        player.setAutoPlay(true);
        player.setMute(true);
        player.setCycleCount(MediaPlayer.INDEFINITE);
        view.setMediaPlayer(player);
        view.setPreserveRatio(false);
    }
}