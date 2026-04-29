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
import ma.ensa.khouribga.smartstay.dao.UserDao;
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

public class LoginController {

    // ── Login FXML bindings ───────────────────────────────────────────────────
    @FXML private MediaView     mediaView;
    @FXML private Pane          particlePane;
    @FXML private VBox          brandStripe;
    @FXML private VBox          loginPanel;
    @FXML private VBox          glassCard;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    // ── Register FXML bindings ────────────────────────────────────────────────
    @FXML private VBox          registerPanel;
    @FXML private TextField     regFirstName;
    @FXML private TextField     regLastName;
    @FXML private TextField     regUsername;
    @FXML private TextField     regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label         regErrorLabel;

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

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML
    public void showRegister() {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        registerPanel.setVisible(true);
        registerPanel.setManaged(true);
        clearRegError();
    }

    @FXML
    public void showLogin() {
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        loginPanel.setVisible(true);
        loginPanel.setManaged(true);
        clearError();
    }

    // ── Responsive layout ─────────────────────────────────────────────────────

    private void bindResponsiveLayout() {
        particlePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;

            mediaView.fitWidthProperty().bind(newScene.widthProperty());
            mediaView.fitHeightProperty().bind(newScene.heightProperty());
            particlePane.prefWidthProperty().bind(newScene.widthProperty());
            particlePane.prefHeightProperty().bind(newScene.heightProperty());

            newScene.widthProperty().addListener((o, oldW, newW) -> {
                double scale = newW.doubleValue() / 1100.0;
                double cardWidth = Math.min(480, Math.max(300, 340 * scale));
                for (VBox panel : new VBox[]{loginPanel, registerPanel}) {
                    panel.setPrefWidth(cardWidth);
                    panel.setMaxWidth(cardWidth);
                    double fontScale = Math.min(1.4, Math.max(0.85, scale));
                    panel.setStyle("-fx-font-size: " + (14 * fontScale) + "px;");
                }
            });

            newScene.heightProperty().addListener((o, oldH, newH) -> {
                double scale = newH.doubleValue() / 700.0;
                double topBot = Math.min(72, Math.max(28, 52 * scale));
                loginPanel.setPadding(new javafx.geometry.Insets(topBot, 50, topBot - 4, 50));
                registerPanel.setPadding(new javafx.geometry.Insets(topBot - 16, 50, topBot - 16, 50));
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

    // ── Register action ───────────────────────────────────────────────────────

    @FXML
    public void onRegister() {
        clearRegError();
        String firstName = regFirstName.getText().trim();
        String lastName  = regLastName.getText().trim();
        String username  = regUsername.getText().trim();
        String email     = regEmail.getText().trim();
        String password  = regPassword.getText();
        String confirm   = regConfirm.getText();

        if (firstName.isEmpty())            { showRegError("First name is required.");           return; }
        if (lastName.isEmpty())             { showRegError("Last name is required.");            return; }
        if (username.isEmpty())             { showRegError("Username is required.");             return; }
        if (username.length() < 3)          { showRegError("Username must be at least 3 characters."); return; }
        if (email.isEmpty() || !email.contains("@")) { showRegError("Please enter a valid email."); return; }
        if (password.length() < 8)          { showRegError("Password must be at least 8 characters."); return; }
        if (!password.equals(confirm))      { showRegError("Passwords do not match.");          return; }

        regFirstName.setDisable(true);
        regLastName.setDisable(true);
        regUsername.setDisable(true);
        regEmail.setDisable(true);
        regPassword.setDisable(true);
        regConfirm.setDisable(true);

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        Thread t = new Thread(() -> {
            try {
                // Check username availability
                boolean taken = checkUsernameTaken(username);
                if (taken) {
                    Platform.runLater(() -> { showRegError("Username already taken. Try another."); resetRegFields(); });
                    return;
                }
                // Insert user
                long userId = UserDao.insert(username, email, hash, User.Role.CLIENT);
                // Insert guest record
                insertGuest(userId, firstName, lastName, email);
                // Auto-login
                User user = findActiveUser(username);
                if (user != null) {
                    SessionManager.setCurrentUser(user);
                    Platform.runLater(() -> {
                        if (particleTimeline != null) particleTimeline.stop();
                        Navigator.navigateTo(regUsername, Navigator.HOME);
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> { showRegError("Registration failed. Please try again."); resetRegFields(); });
            }
        }, "register-thread");
        t.setDaemon(true);
        t.start();
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

    private boolean checkUsernameTaken(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertGuest(long userId, String firstName, String lastName, String email) throws SQLException {
        String sql = """
                INSERT INTO guests (first_name, last_name, email, created_at)
                VALUES (?, ?, ?, NOW())
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.executeUpdate();
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

    private void showError(String msg)    { errorLabel.setText(msg);    errorLabel.setVisible(true); }
    private void clearError()             { errorLabel.setText("");      errorLabel.setVisible(false); }
    private void showRegError(String msg) { regErrorLabel.setText(msg); regErrorLabel.setVisible(true); }
    private void clearRegError()          { regErrorLabel.setText("");   regErrorLabel.setVisible(false); }

    private void resetFields() {
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        passwordField.clear();
        usernameField.requestFocus();
    }

    private void resetRegFields() {
        regFirstName.setDisable(false); regLastName.setDisable(false);
        regUsername.setDisable(false);  regEmail.setDisable(false);
        regPassword.setDisable(false);  regConfirm.setDisable(false);
    }
}