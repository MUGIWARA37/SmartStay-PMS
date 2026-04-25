package ma.ensa.khouribga.smartstay.home;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.net.URL;

public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        try {
            SessionManager.requireLoggedIn();
            SessionManager.requireAnyRole(User.Role.CLIENT, User.Role.ADMIN, User.Role.STAFF);

            User user = SessionManager.getCurrentUser();
            if (user != null) {
                if (welcomeLabel != null) {
                    welcomeLabel.setText("Welcome, " + safe(user.getUsername()) + "!");
                }
                if (roleLabel != null && user.getRole() != null) {
                    roleLabel.setText("Role: " + user.getRole().name());
                }
                if (statusLabel != null) {
                    statusLabel.setText("Connected");
                }
            }
        } catch (Exception e) {
            // If session is invalid, return to login
            goToLogin();
        }
    }

    @FXML
    public void onLogout() {
        SessionManager.logout();
        goToLogin();
    }

    private void goToLogin() {
        try {
            URL loginResource = getClass().getResource("/fxml/auth/login.fxml");
            if (loginResource == null) {
                throw new IllegalStateException("FXML not found: /fxml/auth/login.fxml");
            }

            Stage stage = currentStage();
            if (stage == null) return;

            Scene scene = new Scene(FXMLLoader.load(loginResource), 1000, 650);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (statusLabel != null) {
                statusLabel.setText("Navigation error: " + e.getMessage());
            }
        }
    }

    private Stage currentStage() {
        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            return (Stage) welcomeLabel.getScene().getWindow();
        }
        if (roleLabel != null && roleLabel.getScene() != null) {
            return (Stage) roleLabel.getScene().getWindow();
        }
        if (statusLabel != null && statusLabel.getScene() != null) {
            return (Stage) statusLabel.getScene().getWindow();
        }
        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}