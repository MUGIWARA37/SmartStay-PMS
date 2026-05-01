package ma.ensa.khouribga.smartstay.profile;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for client_profile.fxml
 * FILE: src/main/java/.../profile/ClientProfileController.java
 */
public class ClientProfileController {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private MediaView bgMediaView;
    @FXML private Circle  avatarCircle;
    @FXML private Label   avatarInitials;
    @FXML private Label   fullNameLabel;
    @FXML private Label   roleLabel;
    @FXML private Label   memberSinceLabel;

    // ── Contact ───────────────────────────────────────────────────────────────
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField nationalityField;
    @FXML private Label     contactMsg;

    // ── Password ──────────────────────────────────────────────────────────────
    @FXML private PasswordField oldPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label         passMsg;

    private String currentPasswordHash;
    private long   guestId = -1;

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        loadProfile();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load
    // ─────────────────────────────────────────────────────────────────────────
    private void loadProfile() {
        User user   = SessionManager.getCurrentUser();
        long userId = user.getId();

        Task<Map<String, String>> task = new Task<>() {
            @Override protected Map<String, String> call() throws Exception {
                Map<String, String> data = new LinkedHashMap<>();
                String sql = """
                    SELECT u.email, u.password_hash, u.created_at,
                           g.id AS g_id, g.first_name, g.last_name, g.phone, g.nationality
                    FROM users u
                    LEFT JOIN guests g ON g.email = u.email
                    WHERE u.id = ?
                    LIMIT 1
                    """;
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            data.put("email",       rs.getString("email"));
                            data.put("passwordHash",rs.getString("password_hash"));
                            data.put("createdAt",   rs.getString("created_at"));
                            data.put("gId",         String.valueOf(rs.getLong("g_id")));
                            data.put("firstName",   rs.getString("first_name"));
                            data.put("lastName",    rs.getString("last_name"));
                            data.put("phone",       rs.getString("phone"));
                            data.put("nationality", rs.getString("nationality"));
                        }
                    }
                }
                return data;
            }
        };

        task.setOnSucceeded(e -> {
            Map<String, String> d = task.getValue();
            if (d.isEmpty()) return;

            currentPasswordHash = d.get("passwordHash");
            guestId = Long.parseLong(d.getOrDefault("gId", "-1"));

            String firstName = d.getOrDefault("firstName", "");
            String lastName  = d.getOrDefault("lastName", "");
            String fullName  = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = SessionManager.getCurrentUser().getUsername();

            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.charAt(0);
            if (!lastName.isEmpty())  initials += lastName.charAt(0);
            if (initials.isEmpty())   initials = fullName.substring(0, Math.min(2, fullName.length())).toUpperCase();

            avatarInitials.setText(initials.toUpperCase());
            fullNameLabel.setText(fullName);
            roleLabel.setText("🏯  Guest / Client");

            String created = d.getOrDefault("createdAt", "");
            if (!created.isEmpty()) {
                memberSinceLabel.setText("Member since: " + created.substring(0, 10));
            }

            emailField.setText(d.getOrDefault("email", ""));
            phoneField.setText(d.getOrDefault("phone", ""));
            nationalityField.setText(d.getOrDefault("nationality", ""));
        });

        task.setOnFailed(e -> showMsg(contactMsg, "Failed to load profile.", true));
        Thread t = new Thread(task, "client-profile-load");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save contact info
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void saveContactInfo() {
        String email       = emailField.getText().trim();
        String phone       = phoneField.getText().trim();
        String nationality = nationalityField.getText().trim();

        if (email.isEmpty() || !email.contains("@")) {
            showMsg(contactMsg, "Please enter a valid email address.", true); return;
        }

        long userId      = SessionManager.getCurrentUser().getId();
        final long gId   = guestId;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                try (Connection conn = Database.getConnection()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET email = ? WHERE id = ?")) {
                        ps.setString(1, email);
                        ps.setLong(2, userId);
                        ps.executeUpdate();
                    }
                    if (gId > 0) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE guests SET phone = ?, nationality = ?, email = ? WHERE id = ?")) {
                            ps.setString(1, phone);
                            ps.setString(2, nationality);
                            ps.setString(3, email);
                            ps.setLong(4, gId);
                            ps.executeUpdate();
                        }
                    }
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> showMsg(contactMsg, "✓ Contact info saved.", false));
        task.setOnFailed(e  -> showMsg(contactMsg, "Error saving contact info.", true));
        new Thread(task, "client-save-contact").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Change password
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void changePassword() {
        String oldPass = oldPassField.getText();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showMsg(passMsg, "All password fields are required.", true); return;
        }
        if (!BCrypt.checkpw(oldPass, currentPasswordHash)) {
            showMsg(passMsg, "Current password is incorrect.", true); return;
        }
        if (newPass.length() < 8) {
            showMsg(passMsg, "New password must be at least 8 characters.", true); return;
        }
        if (!newPass.equals(confirm)) {
            showMsg(passMsg, "New passwords do not match.", true); return;
        }

        String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
        long userId    = SessionManager.getCurrentUser().getId();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users SET password_hash = ? WHERE id = ?")) {
                    ps.setString(1, newHash);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            currentPasswordHash = newHash;
            oldPassField.clear(); newPassField.clear(); confirmPassField.clear();
            showMsg(passMsg, "✓ Password updated successfully.", false);
        });
        task.setOnFailed(e -> showMsg(passMsg, "Error updating password.", true));
        new Thread(task, "client-change-pass").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void goBack() {
        Navigator.navigateTo(emailField, Navigator.HOME);
    }

    @FXML
    public void onLogout() {
        SessionManager.logout();
        Navigator.goToLogin(emailField);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void showMsg(Label lbl, String msg, boolean isError) {
        Platform.runLater(() -> {
            lbl.setText(msg);
            lbl.setVisible(true);
            lbl.setStyle(isError ? "-fx-text-fill: #e05c5c;" : "-fx-text-fill: #4caf82;");
        });
    }

    @FXML
    public void handleThemeToggle() {
        ma.ensa.khouribga.smartstay.ThemeManager.toggle();
    }

}