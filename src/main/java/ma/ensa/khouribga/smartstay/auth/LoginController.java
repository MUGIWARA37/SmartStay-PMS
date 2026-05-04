package ma.ensa.khouribga.smartstay.auth;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;

import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;
import ma.ensa.khouribga.smartstay.dao.SecurityQuestionDao;
import ma.ensa.khouribga.smartstay.dao.SecurityQuestionDao.Question;
import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.Instant;
import java.util.List;

public class LoginController {

    // ── Background ────────────────────────────────────────────────────────────
    @FXML private MediaView bgVideo;

    // ── Panel containers ──────────────────────────────────────────────────────
    @FXML private VBox loginPanel;
    @FXML private VBox registerPanel;
    @FXML private VBox forgotPanel;
    @FXML private VBox recoveryPanel;

    // ── LOGIN fields ──────────────────────────────────────────────────────────
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;

    // ── REGISTER fields ───────────────────────────────────────────────────────
    @FXML private TextField     txtRegFirst;
    @FXML private TextField     txtRegLast;
    @FXML private TextField     txtRegUser;
    @FXML private TextField     txtRegEmail;
    @FXML private PasswordField txtRegPass;
    @FXML private Label         lblRegError;

    // Security question pickers + answer fields (registration)
    @FXML private ComboBox<String> cbQ1;
    @FXML private ComboBox<String> cbQ2;
    @FXML private ComboBox<String> cbQ3;
    @FXML private TextField txtA1;
    @FXML private TextField txtA2;
    @FXML private TextField txtA3;

    // ── FORGOT fields ─────────────────────────────────────────────────────────
    @FXML private TextField txtForgotUser;
    @FXML private Label     lblForgotError;

    // ── RECOVERY fields ───────────────────────────────────────────────────────
    @FXML private Label         lblRQ1;
    @FXML private Label         lblRQ2;
    @FXML private Label         lblRQ3;
    @FXML private TextField     txtRA1;
    @FXML private TextField     txtRA2;
    @FXML private TextField     txtRA3;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private Label         lblRecoveryError;

    // ── State ─────────────────────────────────────────────────────────────────
    /** Loaded from DB — maps combo index → question id */
    private List<Question> allQuestions;
    /** User id resolved during the forgot flow */
    private long recoveryUserId = -1;
    /** Question ids resolved during the forgot flow */
    private long rQ1id, rQ2id, rQ3id;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        VideoBackground.register(bgVideo);
        clearError(lblError);
        loadQuestionsAsync();
    }

    private void loadQuestionsAsync() {
        new Thread(() -> {
            try {
                allQuestions = SecurityQuestionDao.findAll();
                List<String> texts = allQuestions.stream().map(Question::text).toList();
                Platform.runLater(() -> {
                    cbQ1.setItems(FXCollections.observableArrayList(texts));
                    cbQ2.setItems(FXCollections.observableArrayList(texts));
                    cbQ3.setItems(FXCollections.observableArrayList(texts));
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "load-questions").start();
    }

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML public void showLogin() {
        show(loginPanel);
        hide(registerPanel); hide(forgotPanel); hide(recoveryPanel);
        clearError(lblError);
    }

    @FXML public void showRegister() {
        show(registerPanel);
        hide(loginPanel); hide(forgotPanel); hide(recoveryPanel);
        clearError(lblRegError);
    }

    @FXML public void showForgot() {
        show(forgotPanel);
        hide(loginPanel); hide(registerPanel); hide(recoveryPanel);
        clearError(lblForgotError);
        txtForgotUser.clear();
    }

    private void show(VBox p) { p.setVisible(true);  p.setManaged(true);  }
    private void hide(VBox p) { p.setVisible(false); p.setManaged(false); }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @FXML
    public void handleLogin() {
        clearError(lblError);
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        if (username.isEmpty()) { showError(lblError, "Please enter your username."); return; }
        if (password.isEmpty()) { showError(lblError, "Please enter your password."); return; }

        txtUsername.setDisable(true); txtPassword.setDisable(true);
        new Thread(() -> {
            try {
                User user = findActiveUser(username);
                if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
                    Platform.runLater(() -> {
                        showError(lblError, "Invalid username or password.");
                        txtUsername.setDisable(false); txtPassword.setDisable(false);
                        txtPassword.clear();
                    });
                    return;
                }
                updateLastLogin(user.getId());
                SessionManager.setCurrentUser(user);
                Platform.runLater(() -> routeByRole(user.getRole()));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError(lblError, "Connection error. Please try again.");
                    txtUsername.setDisable(false); txtPassword.setDisable(false);
                });
            }
        }, "auth-thread").start();
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @FXML
    public void handleRegister() {
        clearError(lblRegError);
        String firstName = txtRegFirst.getText().trim();
        String lastName  = txtRegLast.getText().trim();
        String username  = txtRegUser.getText().trim();
        String email     = txtRegEmail.getText().trim();
        String password  = txtRegPass.getText();
        String a1 = txtA1.getText().trim();
        String a2 = txtA2.getText().trim();
        String a3 = txtA3.getText().trim();

        // Basic field validation
        if (firstName.isEmpty())              { showError(lblRegError, "First name is required.");            return; }
        if (lastName.isEmpty())               { showError(lblRegError, "Last name is required.");             return; }
        if (username.length() < 3)            { showError(lblRegError, "Username must be at least 3 chars."); return; }
        if (email.isEmpty()||!email.contains("@")) { showError(lblRegError, "Enter a valid email.");         return; }
        if (password.length() < 8)            { showError(lblRegError, "Password must be at least 8 chars."); return; }

        // Security question validation
        if (cbQ1.getValue() == null || cbQ2.getValue() == null || cbQ3.getValue() == null)
            { showError(lblRegError, "Please select all 3 security questions."); return; }
        if (a1.isEmpty() || a2.isEmpty() || a3.isEmpty())
            { showError(lblRegError, "Please answer all 3 security questions."); return; }
        if (cbQ1.getValue().equals(cbQ2.getValue()) ||
            cbQ1.getValue().equals(cbQ3.getValue()) ||
            cbQ2.getValue().equals(cbQ3.getValue()))
            { showError(lblRegError, "Please choose 3 different questions."); return; }

        // Resolve question IDs from selection index
        long q1id = allQuestions.get(cbQ1.getSelectionModel().getSelectedIndex()).id();
        long q2id = allQuestions.get(cbQ2.getSelectionModel().getSelectedIndex()).id();
        long q3id = allQuestions.get(cbQ3.getSelectionModel().getSelectedIndex()).id();

        setRegisterDisabled(true);
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        new Thread(() -> {
            try {
                if (checkUsernameTaken(username)) {
                    Platform.runLater(() -> {
                        showError(lblRegError, "Username already taken. Try another.");
                        setRegisterDisabled(false);
                    });
                    return;
                }
                long userId = UserDao.insert(username, email, hash, User.Role.CLIENT);
                insertGuest(userId, firstName, lastName, email);
                // Save security answers
                SecurityQuestionDao.saveAnswers(userId, q1id, a1, q2id, a2, q3id, a3);

                User user = findActiveUser(username);
                if (user != null) {
                    SessionManager.setCurrentUser(user);
                    Platform.runLater(() -> Navigator.navigateTo(txtRegUser, Navigator.HOME));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showError(lblRegError, "Registration failed: " + ex.getMessage());
                    setRegisterDisabled(false);
                });
            }
        }, "register-thread").start();
    }

    // ── FORGOT — step 1: resolve username ────────────────────────────────────

    @FXML
    public void handleForgotNext() {
        clearError(lblForgotError);
        String username = txtForgotUser.getText().trim();
        if (username.isEmpty()) { showError(lblForgotError, "Please enter your username."); return; }

        new Thread(() -> {
            try {
                long uid = SecurityQuestionDao.findUserIdByUsername(username);
                if (uid < 0) {
                    Platform.runLater(() -> showError(lblForgotError, "No account found for that username."));
                    return;
                }
                boolean hasQ = SecurityQuestionDao.hasAnswers(uid);
                if (!hasQ) {
                    Platform.runLater(() -> showError(lblForgotError,
                        "This account has no security questions set up. Contact an administrator."));
                    return;
                }
                List<Question> userQ = SecurityQuestionDao.findByUser(uid);
                Platform.runLater(() -> {
                    recoveryUserId = uid;
                    rQ1id = userQ.get(0).id();
                    rQ2id = userQ.get(1).id();
                    rQ3id = userQ.get(2).id();
                    lblRQ1.setText(userQ.get(0).text());
                    lblRQ2.setText(userQ.get(1).text());
                    lblRQ3.setText(userQ.get(2).text());
                    txtRA1.clear(); txtRA2.clear(); txtRA3.clear();
                    txtNewPass.clear(); txtConfirmPass.clear();
                    clearError(lblRecoveryError);
                    show(recoveryPanel);
                    hide(forgotPanel);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError(lblForgotError, "Error: " + ex.getMessage()));
            }
        }, "forgot-thread").start();
    }

    // ── RECOVERY — step 2: verify answers + reset password ───────────────────

    @FXML
    public void handleRecoverySubmit() {
        clearError(lblRecoveryError);
        String a1 = txtRA1.getText().trim();
        String a2 = txtRA2.getText().trim();
        String a3 = txtRA3.getText().trim();
        String np  = txtNewPass.getText();
        String cp  = txtConfirmPass.getText();

        if (a1.isEmpty() || a2.isEmpty() || a3.isEmpty())
            { showError(lblRecoveryError, "Please answer all 3 questions."); return; }
        if (np.length() < 8)
            { showError(lblRecoveryError, "New password must be at least 8 characters."); return; }
        if (!np.equals(cp))
            { showError(lblRecoveryError, "Passwords do not match."); return; }

        new Thread(() -> {
            try {
                boolean ok = SecurityQuestionDao.verifyAllAnswers(
                    recoveryUserId, rQ1id, a1, rQ2id, a2, rQ3id, a3);
                if (!ok) {
                    Platform.runLater(() -> showError(lblRecoveryError,
                        "One or more answers are incorrect. Please try again."));
                    return;
                }
                SecurityQuestionDao.resetPassword(recoveryUserId, np);
                Platform.runLater(() -> {
                    showLogin();
                    txtUsername.clear(); txtPassword.clear();
                    showError(lblError,
                        "✓ Password reset successfully. You may now sign in.");
                    // Style it green
                    lblError.setStyle("-fx-text-fill: #1e8449;");
                    lblError.setVisible(true); lblError.setManaged(true);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError(lblRecoveryError, "Error: " + ex.getMessage()));
            }
        }, "recovery-thread").start();
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    private void routeByRole(User.Role role) {
        switch (role) {
            case ADMIN  -> Navigator.navigateTo(txtUsername, Navigator.ADMIN);
            case CLIENT -> Navigator.navigateTo(txtUsername, Navigator.HOME);
            case STAFF  -> routeStaffByPosition();
            default     -> showError(lblError, "Unknown role — contact administrator.");
        }
    }

    private void routeStaffByPosition() {
        long userId = SessionManager.getCurrentUser().getId();
        new Thread(() -> {
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT position FROM staff_profiles WHERE user_id = ? LIMIT 1")) {
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
        }, "staff-route").start();
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private User findActiveUser(String username) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, is_active " +
                     "FROM users WHERE username = ? AND is_active = TRUE LIMIT 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRole(User.Role.valueOf(rs.getString("role")));
                u.setActive(rs.getBoolean("is_active"));
                return u;
            }
        }
    }

    private boolean checkUsernameTaken(String username) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertGuest(long userId, String firstName, String lastName, String email)
            throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO guests (first_name, last_name, email, created_at) VALUES (?, ?, ?, NOW())")) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.executeUpdate();
        }
    }

    private void updateLastLogin(long userId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE users SET last_login_at = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[LoginController] last_login_at update failed: " + ex.getMessage());
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #e05c5c;");
        lbl.setVisible(true); lbl.setManaged(true);
    }

    private void clearError(Label lbl) {
        if (lbl == null) return;
        lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false);
    }

    private void setRegisterDisabled(boolean d) {
        txtRegFirst.setDisable(d); txtRegLast.setDisable(d);
        txtRegUser.setDisable(d);  txtRegEmail.setDisable(d);
        txtRegPass.setDisable(d);
        cbQ1.setDisable(d); cbQ2.setDisable(d); cbQ3.setDisable(d);
        txtA1.setDisable(d); txtA2.setDisable(d); txtA3.setDisable(d);
    }

    @FXML public void handleThemeToggle() { ThemeManager.toggle(); }
}