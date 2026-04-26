package ma.ensa.khouribga.smartstay.auth;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

/**
 * Controller for login.fxml.
 * FILE LOCATION: src/main/java/ma/ensa/khouribga/smartstay/auth/LoginController.java
 */
public class LoginController {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private MediaView     mediaView;
    @FXML private Pane          particlePane;
    @FXML private VBox          glassCard;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private static final Random RNG = new Random();
    private Timeline particleTimeline;

    // ── Initialize ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        startVideoBackground();
        startSakuraParticles();
        bindResponsiveLayout();
        clearError();
    }

    // ── Responsive layout bindings ────────────────────────────────────────────

    /**
     * Binds the video and glass card to the scene size so everything
     * scales correctly when the window is maximised or resized.
     */
    private void bindResponsiveLayout() {
        // Wait until the scene is attached, then bind
        particlePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;

            // Bind video to full scene size
            mediaView.fitWidthProperty().bind(newScene.widthProperty());
            mediaView.fitHeightProperty().bind(newScene.heightProperty());

            // Bind particle pane to full scene size
            particlePane.prefWidthProperty().bind(newScene.widthProperty());
            particlePane.prefHeightProperty().bind(newScene.heightProperty());

            // Scale the glass card proportionally: base 340px at 1100px wide
            // clamp between 300 and 480
            newScene.widthProperty().addListener((o, oldW, newW) -> {
                double scale = newW.doubleValue() / 1100.0;
                double cardWidth = Math.min(480, Math.max(300, 340 * scale));
                glassCard.setPrefWidth(cardWidth);
                glassCard.setMaxWidth(cardWidth);

                // Scale font sizes via inline style on the card root
                double fontScale = Math.min(1.4, Math.max(0.85, scale));
                glassCard.setStyle("-fx-font-size: " + (14 * fontScale) + "px;");
            });

            newScene.heightProperty().addListener((o, oldH, newH) -> {
                double scale = newH.doubleValue() / 700.0;
                double topBot = Math.min(72, Math.max(32, 52 * scale));
                glassCard.setPadding(new javafx.geometry.Insets(topBot, 50, topBot - 4, 50));
            });
        });
    }

    // ── Video background ──────────────────────────────────────────────────────

    private void startVideoBackground() {
        URL videoUrl = getClass().getResource("/videos/sakura.mp4");
        if (videoUrl == null) return;

        Media media = new Media(videoUrl.toExternalForm());
        MediaPlayer player = new MediaPlayer(media);
        player.setAutoPlay(true);
        player.setMute(true);
        player.setCycleCount(MediaPlayer.INDEFINITE);

        mediaView.setMediaPlayer(player);
        mediaView.setPreserveRatio(false);
    }

    // ── Sakura particles ──────────────────────────────────────────────────────

    private void startSakuraParticles() {
        particleTimeline = new Timeline(new KeyFrame(Duration.millis(600), e -> spawnLeaf()));
        particleTimeline.setCycleCount(Timeline.INDEFINITE);
        particleTimeline.play();
    }

    private void spawnLeaf() {
        Region leaf = new Region();

        double paneWidth = particlePane.getWidth();
        if (paneWidth <= 0) paneWidth = 1100;

        double startX = RNG.nextDouble() * paneWidth;
        leaf.setLayoutX(startX);
        leaf.setLayoutY(-20);
        particlePane.getChildren().add(leaf);

        double fallDistance = particlePane.getHeight() + 40;
        double duration     = 4000 + RNG.nextDouble() * 4000;
        double drift        = (RNG.nextDouble() - 0.5) * 160;

        Timeline fall = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(leaf.layoutYProperty(), -20),
                new KeyValue(leaf.layoutXProperty(), startX),
                new KeyValue(leaf.opacityProperty(), 0.85),
                new KeyValue(leaf.rotateProperty(), RNG.nextDouble() * 60 - 30)
            ),
            new KeyFrame(Duration.millis(duration),
                new KeyValue(leaf.layoutYProperty(), fallDistance),
                new KeyValue(leaf.layoutXProperty(), startX + drift),
                new KeyValue(leaf.opacityProperty(), 0.0),
                new KeyValue(leaf.rotateProperty(), RNG.nextDouble() * 360)
            )
        );
        fall.setOnFinished(ev -> particlePane.getChildren().remove(leaf));
        fall.play();
    }

    // ── Login action ──────────────────────────────────────────────────────────

    @FXML
    public void onLogin() {
        clearError();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) { showError("Please enter your username."); return; }
        if (password.isEmpty()) { showError("Please enter your password."); return; }

        usernameField.setDisable(true);
        passwordField.setDisable(true);

        Thread authThread = new Thread(() -> {
            try {
                User user = findActiveUser(username);
                if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
                    Platform.runLater(() -> { showError("Invalid username or password."); resetFields(); });
                    return;
                }
                updateLastLogin(user.getId());
                SessionManager.setCurrentUser(user);
                Platform.runLater(() -> routeByRole(user.getRole()));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> { showError("Connection error. Please try again."); resetFields(); });
            }
        }, "auth-thread");

        authThread.setDaemon(true);
        authThread.start();
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private User findActiveUser(String username) throws SQLException {
        String sql = """
                SELECT id, username, email, password_hash, role, is_active
                FROM users
                WHERE username = ? AND is_active = TRUE
                LIMIT 1
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(User.Role.valueOf(rs.getString("role")));
                user.setActive(rs.getBoolean("is_active"));
                return user;
            }
        }
    }

    private void updateLastLogin(long userId) {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[LoginController] last_login_at update failed: " + ex.getMessage());
        }
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    private void routeByRole(User.Role role) {
        if (particleTimeline != null) particleTimeline.stop();
        switch (role) {
            case ADMIN  -> Navigator.navigateTo(usernameField, Navigator.ADMIN);
            case CLIENT -> Navigator.navigateTo(usernameField, Navigator.HOME);
            case STAFF  -> routeStaffByPosition();
            default     -> { showError("Unknown role — contact administrator."); resetFields(); }
        }
    }

    private void routeStaffByPosition() {
        long userId = SessionManager.getCurrentUser().getId();
        String sql  = "SELECT position FROM staff_profiles WHERE user_id = ? LIMIT 1";
        Thread t = new Thread(() -> {
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    String pos = rs.next() ? rs.getString("position").toLowerCase() : "";
                    Platform.runLater(() -> Navigator.navigateTo(usernameField, switch (pos) {
                        case "reception"   -> Navigator.RECEPTION;
                        case "cleaning"    -> Navigator.CLEANING;
                        case "maintenance" -> Navigator.MAINTENANCE;
                        default            -> Navigator.RECEPTION;
                    }));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> Navigator.navigateTo(usernameField, Navigator.RECEPTION));
            }
        }, "staff-route-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError()          { errorLabel.setText("");  errorLabel.setVisible(false); }
    private void resetFields() {
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        passwordField.clear();
        usernameField.requestFocus();
    }
}