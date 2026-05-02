package ma.ensa.khouribga.smartstay.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;

import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.VideoBackground;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Guest;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private MediaView     bgVideo;
    @FXML private VBox          loginPanel;
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;

    @FXML private VBox          registerPanel;
    @FXML private TextField     txtRegFirst;
    @FXML private TextField     txtRegLast;
    @FXML private TextField     txtRegUser;
    @FXML private TextField     txtRegEmail;
    @FXML private PasswordField txtRegPass;

    @FXML
    public void initialize() {
        VideoBackground.register(bgVideo);
        clearError();
    }

    @FXML
    public void toggleRegister() {
        boolean isLoginVisible = loginPanel.isVisible();
        loginPanel.setVisible(!isLoginVisible);
        loginPanel.setManaged(!isLoginVisible);
        registerPanel.setVisible(isLoginVisible);
        registerPanel.setManaged(isLoginVisible);
        clearError();
    }

    @FXML
    public void handleLogin() {
        clearError();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        
        if (username.isEmpty()) { showError("Please enter your username."); return; }
        if (password.isEmpty()) { showError("Please enter your password."); return; }

        txtUsername.setDisable(true);
        txtPassword.setDisable(true);

        Thread authThread = new Thread(() -> {
            try {
                User user = UserDao.findByUsername(username).orElse(null);
                if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
                    Platform.runLater(() -> { showError("Invalid username or password."); resetFields(); });
                    return;
                }
                UserDao.updateLastLogin(user.getId());
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

    @FXML
    public void handleRegister() {
        String firstName = txtRegFirst.getText().trim();
        String lastName  = txtRegLast.getText().trim();
        String username  = txtRegUser.getText().trim();
        String email     = txtRegEmail.getText().trim();
        String password  = txtRegPass.getText();

        if (firstName.isEmpty())            { showRegError("First name is required.");           return; }
        if (lastName.isEmpty())             { showRegError("Last name is required.");            return; }
        if (username.isEmpty())             { showRegError("Username is required.");             return; }
        if (username.length() < 3)          { showRegError("Username must be at least 3 characters."); return; }
        if (email.isEmpty() || !email.contains("@")) { showRegError("Please enter a valid email."); return; }
        if (password.length() < 8)          { showRegError("Password must be at least 8 characters."); return; }

        txtRegFirst.setDisable(true);
        txtRegLast.setDisable(true);
        txtRegUser.setDisable(true);
        txtRegEmail.setDisable(true);
        txtRegPass.setDisable(true);

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        Thread t = new Thread(() -> {
            try {
                boolean taken = UserDao.existsByUsername(username);
                if (taken) {
                    Platform.runLater(() -> { showRegError("Username already taken. Try another."); resetRegFields(); });
                    return;
                }
                UserDao.insert(username, email, hash, User.Role.CLIENT);
                Guest g = new Guest();
                g.setFirstName(firstName);
                g.setLastName(lastName);
                g.setEmail(email);
                GuestDao.create(g);
                User user = UserDao.findByUsername(username).orElse(null);
                if (user != null) {
                    SessionManager.setCurrentUser(user);
                    Platform.runLater(() -> Navigator.navigateTo(txtRegUser, Navigator.HOME));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> { showRegError("Registration failed. Please try again."); resetRegFields(); });
            }
        }, "register-thread");
        t.setDaemon(true);
        t.start();
    }

    private void routeByRole(User.Role role) {
        switch (role) {
            case ADMIN  -> Navigator.navigateTo(txtUsername, Navigator.ADMIN);
            case CLIENT -> Navigator.navigateTo(txtUsername, Navigator.HOME);
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
                    Platform.runLater(() -> Navigator.navigateTo(txtUsername, switch (pos) {
                        case "reception"   -> Navigator.RECEPTION;
                        case "cleaning"    -> Navigator.CLEANING;
                        case "maintenance" -> Navigator.MAINTENANCE;
                        default            -> Navigator.RECEPTION;
                    }));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> Navigator.navigateTo(txtUsername, Navigator.RECEPTION));
            }
        }, "staff-route-thread");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) { 
        if(lblError != null) { lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true); }
    }
    
    private void clearError() { 
        if(lblError != null) { lblError.setText(""); lblError.setVisible(false); lblError.setManaged(false); }
    }
    
    private void showRegError(String msg) { 
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Registration Error");
        alert.setHeaderText("Could not complete registration");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void resetFields() {
        txtUsername.setDisable(false); txtPassword.setDisable(false);
        txtPassword.clear(); txtUsername.requestFocus();
    }

    private void resetRegFields() {
        txtRegFirst.setDisable(false); txtRegLast.setDisable(false);
        txtRegUser.setDisable(false);  txtRegEmail.setDisable(false);
        txtRegPass.setDisable(false);  
    }

    @FXML
    public void handleThemeToggle() {
        ma.ensa.khouribga.smartstay.ThemeManager.toggle();
    }

}