package ma.ensa.khouribga.smartstay.auth;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import java.util.Random;

public class LoginController {

    @FXML private MediaView mediaView;
    @FXML private StackPane mainStackPane;
    @FXML private Pane animationPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private MediaPlayer mediaPlayer;
    private final Random random = new Random();

    @FXML
    public void initialize() {
        setupVideoBackground();
        startSakuraAnimation();
    }

    private void setupVideoBackground() {
        try {
            var resource = getClass().getResource("/videos/sakura.mp4");
            if (resource != null) {
                Media media = new Media(resource.toExternalForm());
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true);
                mediaPlayer.play();

                // FIX: Force video to scale when window size changes (Full Screen)
                mainStackPane.widthProperty().addListener((obs, oldV, newV) -> 
                    mediaView.setFitWidth(newV.doubleValue()));
                mainStackPane.heightProperty().addListener((obs, oldV, newV) -> 
                    mediaView.setFitHeight(newV.doubleValue()));
            }
        } catch (Exception e) {
            System.err.println("Scaling Error: " + e.getMessage());
        }
    }

    private void startSakuraAnimation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(600), e -> spawnLeaf()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void spawnLeaf() {
        Region leaf = new Region();
        leaf.getStyleClass().add("sakura-leaf");
        double size = 6 + random.nextDouble() * 10;
        leaf.setPrefSize(size, size);
        leaf.setLayoutX(random.nextDouble() * mainStackPane.getWidth());
        leaf.setLayoutY(-30);
        animationPane.getChildren().add(leaf);

        TranslateTransition fall = new TranslateTransition(Duration.seconds(8 + random.nextDouble() * 5), leaf);
        fall.setByY(mainStackPane.getHeight() + 100);
        fall.setByX(random.nextDouble() * 120 - 60);
        leaf.setRotate(random.nextDouble() * 360);
        fall.setOnFinished(e -> animationPane.getChildren().remove(leaf));
        fall.play();
    }

    @FXML
    private void onLogin() {
        if(usernameField.getText().isEmpty()) messageLabel.setText("Identity is required.");
    }
}