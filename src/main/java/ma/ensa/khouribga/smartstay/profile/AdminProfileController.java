package ma.ensa.khouribga.smartstay.profile;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;
import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import ma.ensa.khouribga.smartstay.util.ProfilePictureUtil;
import ma.ensa.khouribga.smartstay.util.SidebarToggleUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminProfileController {

    @FXML private MediaView bgMediaView;
    @FXML private VBox sidebar;
    @FXML private Button btnSidebarToggle;
    @FXML private StackPane heroAvatarPane;
    @FXML private StackPane sidebarAvatarPane;
    @FXML private Label lblInitials;
    @FXML private Label lblUsername;
    @FXML private Label lblSessionUser;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtOldPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    // Expandable card bodies
    @FXML private VBox bodyProfile;
    @FXML private VBox bodyComms;
    @FXML private VBox bodyAuth;
    @FXML private VBox bodySession;

    // Arrow indicators
    @FXML private Label arrowProfile;
    @FXML private Label arrowComms;
    @FXML private Label arrowAuth;
    @FXML private Label arrowSession;

    private User currentUser;
    private boolean sidebarCollapsed = false;

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        SidebarToggleUtil.initialize(sidebar, btnSidebarToggle);
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(() -> Navigator.goToLogin(lblUsername));
            return;
        }

        String uname = currentUser.getUsername();
        lblUsername.setText(uname.toUpperCase());
        lblInitials.setText(uname.length() >= 2 ? uname.substring(0, 2).toUpperCase() : uname.toUpperCase());
        txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        if (lblSessionUser != null)
            lblSessionUser.setText("Logged in as: " + uname);
        // Load profile picture into both avatar panes
        ProfilePictureUtil.applyToAvatar(heroAvatarPane,    currentUser.getProfilePicture());
        ProfilePictureUtil.applyToAvatar(sidebarAvatarPane, currentUser.getProfilePicture());
    }

    // ── Change profile picture ────────────────────────────────────────────────

    @FXML public void changeProfilePicture(MouseEvent e) {
        e.consume(); // don't propagate to card toggle
        String path = ProfilePictureUtil.chooseAndSave(
            heroAvatarPane.getScene().getWindow(), currentUser.getUsername());
        if (path == null) return;
        new Thread(() -> {
            try {
                UserDao.updateProfilePicture(currentUser.getId(), path);
                currentUser.setProfilePicture(path);
                Platform.runLater(() -> {
                    ProfilePictureUtil.applyToAvatar(heroAvatarPane,    path);
                    ProfilePictureUtil.applyToAvatar(sidebarAvatarPane, path);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Could not save profile picture."));
            }
        }).start();
    }

    // ── Expand / Collapse ─────────────────────────────────────────────────────

    @FXML public void toggleProfile(MouseEvent e) { toggle(bodyProfile, arrowProfile); }
    @FXML public void toggleComms(MouseEvent e)   { toggle(bodyComms,   arrowComms);   }
    @FXML public void toggleAuth(MouseEvent e)    { toggle(bodyAuth,    arrowAuth);    }
    @FXML public void toggleSession(MouseEvent e) { toggle(bodySession, arrowSession); }

    /**
     * Animate a card body open or closed.
     * Uses managed/visible + a prefHeight timeline for a smooth slide effect.
     */
    private void toggle(VBox body, Label arrow) {
        boolean opening = !body.isVisible();

        if (opening) {
            // Make visible first so layout can measure it
            body.setVisible(true);
            body.setManaged(true);
            body.setOpacity(0);
            body.setPrefHeight(0);

            // Let JavaFX compute natural height, then animate to it
            Platform.runLater(() -> {
                double target = body.prefHeight(-1);
                if (target <= 0) target = 200; // fallback

                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(body.prefHeightProperty(), 0),
                        new KeyValue(body.opacityProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(260),
                        new KeyValue(body.prefHeightProperty(), target),
                        new KeyValue(body.opacityProperty(), 1.0)
                    )
                );
                tl.setOnFinished(ev -> body.setPrefHeight(Region.USE_COMPUTED_SIZE));
                tl.play();
            });
            arrow.setText("▾");
            arrow.setStyle("-fx-font-size:18px; -fx-text-fill:#c5a059; -fx-font-weight:bold;");
        } else {
            double current = body.getHeight();
            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(body.prefHeightProperty(), current),
                    new KeyValue(body.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(220),
                    new KeyValue(body.prefHeightProperty(), 0),
                    new KeyValue(body.opacityProperty(), 0)
                )
            );
            tl.setOnFinished(ev -> {
                body.setVisible(false);
                body.setManaged(false);
                body.setPrefHeight(Region.USE_COMPUTED_SIZE);
            });
            tl.play();
            arrow.setText("▸");
            arrow.setStyle("-fx-font-size:18px; -fx-text-fill:#c5a059; -fx-font-weight:bold;");
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @FXML public void goBack(ActionEvent event) {
        Navigator.navigateTo((Node) event.getSource(), Navigator.ADMIN);
    }

    // ── Update Email ──────────────────────────────────────────────────────────

    @FXML public void updateContact() {
        String newEmail = txtEmail.getText().trim();
        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Data", "Please enter a valid email address.");
            return;
        }
        new Thread(() -> {
            String sql = "UPDATE users SET email = ? WHERE id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newEmail);
                ps.setLong(2, currentUser.getId());
                ps.executeUpdate();
                currentUser.setEmail(newEmail);
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "Communications link updated successfully."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update email."));
            }
        }).start();
    }

    // ── Update Password ───────────────────────────────────────────────────────

    @FXML public void updatePassword() {
        String oldPass     = txtOldPass.getText();
        String newPass     = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Incomplete Form", "Please fill out all password fields.");
            return;
        }
        if (newPass.length() < 8) {
            showAlert(Alert.AlertType.ERROR, "Weak Password", "New passphrase must be at least 8 characters.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Mismatch", "New passphrases do not match.");
            return;
        }

        new Thread(() -> {
            try {
                String sqlFetch = "SELECT password_hash FROM users WHERE id = ?";
                String currentHash = "";
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlFetch)) {
                    ps.setLong(1, currentUser.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) currentHash = rs.getString("password_hash");
                    }
                }
                if (!BCrypt.checkpw(oldPass, currentHash)) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Current passphrase is incorrect."));
                    return;
                }
                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
                String sqlUpdate = "UPDATE users SET password_hash = ? WHERE id = ?";
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, newHash);
                    ps.setLong(2, currentUser.getId());
                    ps.executeUpdate();
                }
                currentUser.setPasswordHash(newHash);
                Platform.runLater(() -> {
                    txtOldPass.clear(); txtNewPass.clear(); txtConfirmPass.clear();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Master passphrase has been altered.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to alter passphrase."));
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML public void handleThemeToggle() {
        ThemeManager.toggle();
    }

    @FXML public void toggleSidebar() {
        sidebarCollapsed = SidebarToggleUtil.toggle(sidebar, btnSidebarToggle, sidebarCollapsed);
    }

    @FXML public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Navigator.goToLogin((Node) event.getSource());
    }
}
