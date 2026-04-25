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

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    public void onLogin(ActionEvent event) {
        clearMessage();

        String username = normalize(usernameField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter username and password.");
            return;
        }

        String sql = """
                SELECT id, username, email, password_hash, role, is_active
                FROM users
                WHERE LOWER(username) = LOWER(?) AND is_active = 1
                LIMIT 1
                """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showMessage("Invalid username or password.");
                    return;
                }

                String storedHash = rs.getString("password_hash");
                if (storedHash == null || storedHash.isBlank() || !BCrypt.checkpw(password, storedHash)) {
                    showMessage("Invalid username or password.");
                    return;
                }

                User user = buildUser(rs);
                SessionManager.setCurrentUser(user);

                // optional: clear sensitive field as soon as auth succeeds
                passwordField.clear();

                openNextScene(user);
            }

        } catch (Exception e) {
            // Log detailed error internally, show generic message to user
            e.printStackTrace();
            showMessage("Unable to login right now. Please try again.");
        }
    }

    private User buildUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    private void openNextScene(User user) throws Exception {
        String fxml = switch (user.getRole()) {
            case ADMIN -> "/fxml/admin/admin.fxml";
            case CLIENT -> "/fxml/home/home.fxml";
            case STAFF -> resolveStaffFxml(user.getId());
        };

        URL resource = getClass().getResource(fxml);
        if (resource == null) {
            throw new IllegalStateException("FXML not found: " + fxml);
        }

        Scene next = new Scene(FXMLLoader.load(resource), 1000, 650);
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
                if (!rs.next()) return "/fxml/staff/reception.fxml";

                String position = rs.getString("position");
                if (position == null || position.isBlank()) return "/fxml/staff/reception.fxml";

                return switch (position.trim().toLowerCase()) {
                    case "cleaning" -> "/fxml/staff/cleaning.fxml";
                    case "maintenance" -> "/fxml/staff/maintenance.fxml";
                    default -> "/fxml/staff/reception.fxml";
                };
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void showMessage(String msg) {
        if (messageLabel != null) messageLabel.setText(msg);
    }

    private void clearMessage() {
        showMessage("");
    }
}
