package ma.ensa.khouribga.smartstay.profile;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

public class AdminProfileController {

    @FXML private MediaView bgMediaView;
    @FXML private Label lblInitials;
    @FXML private Label lblUsername;
    @FXML private TextField txtEmail;
    
    @FXML private PasswordField txtOldPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    private User currentUser;

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(() -> Navigator.goToLogin(lblUsername));
            return;
        }

        // Set up the static text
        lblUsername.setText(currentUser.getUsername().toUpperCase());
        
        // Generate initials (First two letters of username)
        String uname = currentUser.getUsername();
        if (uname.length() >= 2) {
            lblInitials.setText(uname.substring(0, 2).toUpperCase());
        } else {
            lblInitials.setText(uname.toUpperCase());
        }

        // Load the email
        txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
    }

    @FXML
    public void goBack(ActionEvent event) {
        Navigator.navigateTo((Node) event.getSource(), Navigator.ADMIN);
    }

    @FXML
    public void updateContact() {
        String newEmail = txtEmail.getText().trim();
        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Data", "Please enter a valid email address.");
            return;
        }

        new Thread(() -> {
            try {
                UserDao.updateEmail(currentUser.getId(), newEmail);
                currentUser.setEmail(newEmail);
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "Communications link updated successfully."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update email."));
            }
        }).start();
    }

    @FXML
    public void updatePassword() {
        String oldPass = txtOldPass.getText();
        String newPass = txtNewPass.getText();
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
                String currentHash = UserDao.findById(currentUser.getId())
                        .map(User::getPasswordHash)
                        .orElse("");

                if (!BCrypt.checkpw(oldPass, currentHash)) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Current passphrase is incorrect."));
                    return;
                }

                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
                UserDao.updatePasswordHash(currentUser.getId(), newHash);

                currentUser.setPasswordHash(newHash);
                Platform.runLater(() -> {
                    txtOldPass.clear();
                    txtNewPass.clear();
                    txtConfirmPass.clear();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Master passphrase has been altered. Keep it secret, keep it safe.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to alter passphrase."));
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Navigator.goToLogin((Node) event.getSource());
    }

    @FXML
    public void handleThemeToggle() {
        ThemeManager.toggle();
    }

}