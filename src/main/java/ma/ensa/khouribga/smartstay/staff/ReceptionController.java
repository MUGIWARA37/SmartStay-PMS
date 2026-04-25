package ma.ensa.khouribga.smartstay.staff;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.net.URL;

public class ReceptionController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        try {
            SessionManager.requireRole(User.Role.STAFF);

            User user = SessionManager.getCurrentUser();
            if (welcomeLabel != null && user != null) {
                welcomeLabel.setText("Reception: " + user.getUsername());
            }
            if (statusLabel != null) {
                statusLabel.setText("Ready");
            }
        } catch (Exception e) {
            SessionManager.logout();
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

            Stage stage = resolveStage();
            if (stage == null) return;

            stage.setScene(new Scene(FXMLLoader.load(loginResource), 1000, 650));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (statusLabel != null) {
                statusLabel.setText("Navigation error: " + e.getMessage());
            }
        }
    }

    private Stage resolveStage() {
        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            return (Stage) welcomeLabel.getScene().getWindow();
        }
        if (statusLabel != null && statusLabel.getScene() != null) {
            return (Stage) statusLabel.getScene().getWindow();
        }
        return null;
    }
}