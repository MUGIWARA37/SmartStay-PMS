package ma.ensa.khouribga.smartstay;

import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import ma.ensa.khouribga.smartstay.VideoBackground;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.Navigator;

public class LandingController {

    @FXML private MediaView bgVideo;

    @FXML
    public void initialize() {
        VideoBackground.register(bgVideo);
    }

    /** "ENTER THE PORTAL" button — navigate to the guest browse screen. */
    @FXML
    public void handleEnter() {
        Navigator.navigateTo(bgVideo, Navigator.HOME);
    }

    @FXML
    public void handleThemeToggle() {
        ThemeManager.toggle();
    }
}