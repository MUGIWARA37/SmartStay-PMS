package ma.ensa.khouribga.smartstay.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    public void onLogin(ActionEvent event) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            return;
        }

        String sql = """
                SELECT id, username, email, password_hash, role, is_active
                FROM users
                WHERE username = ? AND is_active = 1
                LIMIT 1
                """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    messageLabel.setText("Invalid credentials.");
                    return;
                }

                String storedHash = rs.getString("password_hash");
                if (storedHash == null || !BCrypt.checkpw(password, storedHash)) {
                    messageLabel.setText("Invalid credentials.");
                    return;
                }

                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(storedHash);
                user.setRole(User.Role.valueOf(rs.getString("role")));
                user.setActive(rs.getBoolean("is_active"));

                SessionManager.setCurrentUser(user);
                openNextScene(user);
            }

        } catch (Exception e) {
            messageLabel.setText("Login error: " + e.getMessage());
        }
    }

    private void openNextScene(User user) throws Exception {
        String fxml;

        switch (user.getRole()) {
            case ADMIN -> fxml = "/fxml/admin/admin.fxml";
            case CLIENT -> fxml = "/fxml/home/home.fxml";
            case STAFF -> fxml = resolveStaffFxml(user.getId());
            default -> throw new IllegalStateException("Unexpected role: " + user.getRole());
        }

        Scene next = new Scene(FXMLLoader.load(getClass().getResource(fxml)), 1000, 650);
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(next);
        stage.show();
    }

    private String resolveStaffFxml(long userId) throws Exception {
        String sql = "SELECT position FROM staff_profiles WHERE user_id = ? LIMIT 1";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "/fxml/staff/reception.fxml"; // fallback
                }

                String position = rs.getString("position");
                if (position == null) return "/fxml/staff/reception.fxml";

                return switch (position.trim().toLowerCase()) {
                    case "cleaning" -> "/fxml/staff/cleaning.fxml";
                    case "maintenance" -> "/fxml/staff/maintenance.fxml";
                    default -> "/fxml/staff/reception.fxml";
                };
            }
        }
    }
}