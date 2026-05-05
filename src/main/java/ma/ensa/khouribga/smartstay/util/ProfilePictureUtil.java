package ma.ensa.khouribga.smartstay.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.*;

public class ProfilePictureUtil {

    /** Directory where profile pictures are stored in the user's home. */
    private static final Path STORE = Path.of(
            System.getProperty("user.home"), ".smartstay", "profile_pics");

    static {
        try { Files.createDirectories(STORE); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // ── File chooser ──────────────────────────────────────────────────────────

    /**
     * Opens a native file chooser filtered to images.
     * Copies the chosen file into the persistent store and returns the absolute path.
     * Returns null if the user cancelled.
     */
    public static String chooseAndSave(Window owner, String username) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Profile Picture");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return null;

        try {
            String name = chosen.getName();
            String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
            Path dest = STORE.resolve(username + "_" + System.currentTimeMillis() + ext);
            Files.copy(chosen.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Avatar rendering ──────────────────────────────────────────────────────

    /**
     * Applies a circular-clipped photo to the given StackPane (avatar holder).
     * Hides initials labels when a valid picture is found.
     * No-ops gracefully if path is null, blank, or file is missing.
     */
    public static void applyToAvatar(StackPane avatarPane, String picturePath) {
        if (picturePath == null || picturePath.isBlank()) return;
        File f = new File(picturePath);
        if (!f.exists()) return;

        try {
            double size = avatarPane.getPrefWidth() > 0 ? avatarPane.getPrefWidth() : 64;
            Image img = new Image(f.toURI().toString(), size, size, false, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(false);

            // Circular clip
            Circle clip = new Circle(size / 2, size / 2, size / 2);
            iv.setClip(clip);

            // Replace any existing ImageView, hide initials labels
            avatarPane.getChildren().removeIf(n -> n instanceof ImageView);
            avatarPane.getChildren().add(0, iv);
            avatarPane.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.control.Label)
                .forEach(n -> n.setVisible(false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a standalone circular ImageView for preview use (e.g. registration).
     * Returns null if the path is invalid or file is missing.
     */
    public static ImageView buildCircularPreview(String picturePath, double size) {
        if (picturePath == null || picturePath.isBlank()) return null;
        File f = new File(picturePath);
        if (!f.exists()) return null;
        try {
            Image img = new Image(f.toURI().toString(), size, size, false, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(false);
            iv.setClip(new Circle(size / 2, size / 2, size / 2));
            return iv;
        } catch (Exception e) { return null; }
    }
}
