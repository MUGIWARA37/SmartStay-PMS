package ma.ensa.khouribga.smartstay.profile;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.UserDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class StaffProfileController {

    @FXML private MediaView bgMediaView;
    @FXML private Label lblInitials;
    @FXML private Label lblFullName;
    @FXML private Label lblRole;
    @FXML private Label lblPosition;
    @FXML private Label lblDepartment;
    @FXML private Label lblEmpCode;
    @FXML private Label lblHireDate;
    @FXML private Label lblSalary;

    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmergency;

    @FXML private PasswordField txtOldPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    @FXML private HBox shiftsContainer;
    @FXML private TextArea txtIssueDesc;

    private User currentUser;
    private String previousRoute = Navigator.LOGIN; // Default fallback

    public void setPreviousRoute(String route) {
        this.previousRoute = route;
    }

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        loadStaffData();
        loadWeeklyShifts();
    }

    @FXML
    public void goBack(ActionEvent event) {
        Navigator.navigateTo((Node) event.getSource(), previousRoute);
    }

    private void loadStaffData() {
        new Thread(() -> {
            String sql = """
                SELECT g.first_name, g.last_name, g.phone,
                       sp.position, sp.department, sp.employee_code, sp.hire_date, sp.salary_base, sp.emergency_contact
                FROM users u
                LEFT JOIN guests g ON u.email = g.email
                LEFT JOIN staff_profiles sp ON u.id = sp.user_id
                WHERE u.id = ?
            """;
            
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                 
                ps.setLong(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String first = rs.getString("first_name");
                        String last = rs.getString("last_name");
                        String name = (first != null && last != null) ? first + " " + last : currentUser.getUsername();
                        
                        String initials = (first != null && !first.isEmpty() && last != null && !last.isEmpty()) 
                                        ? ("" + first.charAt(0) + last.charAt(0)).toUpperCase() 
                                        : currentUser.getUsername().substring(0, Math.min(2, currentUser.getUsername().length())).toUpperCase();

                        final String fName = name;
                        final String fInitials = initials;
                        final String fPos = rs.getString("position");
                        final String fDept = rs.getString("department");
                        final String fCode = rs.getString("employee_code");
                        final String fHire = rs.getString("hire_date");
                        final double fSal = rs.getDouble("salary_base");
                        final String fPhone = rs.getString("phone");
                        final String fEmerg = rs.getString("emergency_contact");

                        Platform.runLater(() -> {
                            lblFullName.setText(fName.toUpperCase());
                            lblInitials.setText(fInitials);
                            lblRole.setText(currentUser.getRole().toString());
                            
                            lblPosition.setText(fPos != null ? fPos.toUpperCase() : "UNASSIGNED");
                            lblDepartment.setText(fDept != null ? fDept.toUpperCase() : "UNKNOWN");
                            lblEmpCode.setText(fCode != null ? fCode : "N/A");
                            lblHireDate.setText(fHire != null ? fHire : "UNKNOWN");
                            lblSalary.setText(String.format("%.2f MAD", fSal));
                            
                            txtEmail.setText(currentUser.getEmail());
                            if (fPhone != null) txtPhone.setText(fPhone);
                            if (fEmerg != null) txtEmergency.setText(fEmerg);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadWeeklyShifts() {
        // Mocking the visual shift cards generation so the UI renders perfectly.
        // In a real scenario, this would query staff_shift_assignments based on current week.
        Platform.runLater(() -> {
            shiftsContainer.getChildren().clear();
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

            String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
            
            for (int i = 0; i < 7; i++) {
                LocalDate date = startOfWeek.plusDays(i);
                boolean isToday = date.equals(today);
                
                VBox card = new VBox(5);
                card.getStyleClass().add("shift-day-card");
                if (isToday) card.getStyleClass().add("shift-day-today");
                card.setAlignment(Pos.CENTER);
                card.setPrefWidth(90);

                Label lblDayName = new Label(days[i]);
                lblDayName.getStyleClass().add("shift-day-name");
                
                Label lblDayNum = new Label(String.valueOf(date.getDayOfMonth()));
                lblDayNum.getStyleClass().add("shift-day-num");

                // Sample data — weekends off
                String shiftName = (i >= 5) ? "DAY OFF" : "MORNING";
                String shiftTime = (i >= 5) ? "---" : "08:00 - 16:00";

                Label lblShift = new Label(shiftName);
                lblShift.getStyleClass().add("shift-name-label");
                if (i >= 5) lblShift.setStyle("-fx-text-fill: #e74c3c;"); // Red text for days off

                Label lblTime = new Label(shiftTime);
                lblTime.getStyleClass().add("shift-time-label");

                card.getChildren().addAll(lblDayName, lblDayNum, new Separator(), lblShift, lblTime);
                shiftsContainer.getChildren().add(card);
            }
        });
    }

    @FXML
    public void updateContact() {
        String newEmail = txtEmail.getText().trim();
        String newPhone = txtPhone.getText().trim();
        String newEmerg = txtEmergency.getText().trim();

        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Data", "Please enter a valid email address.");
            return;
        }

        new Thread(() -> {
            try (Connection conn = Database.getConnection()) {
                // 1. Update user email
                String sqlUser = "UPDATE users SET email = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                    ps.setString(1, newEmail);
                    ps.setLong(2, currentUser.getId());
                    ps.executeUpdate();
                }

                // 2. Update staff emergency contact
                String sqlStaff = "UPDATE staff_profiles SET emergency_contact = ? WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlStaff)) {
                    ps.setString(1, newEmerg);
                    ps.setLong(2, currentUser.getId());
                    ps.executeUpdate();
                }

                // 3. Update guest phone (linked by email)
                String sqlGuest = "UPDATE guests SET phone = ?, email = ? WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlGuest)) {
                    ps.setString(1, newPhone);
                    ps.setString(2, newEmail);
                    ps.setString(3, currentUser.getEmail());
                    ps.executeUpdate();
                }

                currentUser.setEmail(newEmail);
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Success", "Contacts updated successfully."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update contacts."));
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
                    txtOldPass.clear(); txtNewPass.clear(); txtConfirmPass.clear();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Passphrase successfully updated.");
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to alter passphrase."));
            }
        }).start();
    }

    @FXML
    public void reportIssue() {
        String desc = txtIssueDesc.getText().trim();
        if (desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Report", "Please describe the anomaly before submitting.");
            return;
        }

        new Thread(() -> {
            String sql = """
                    INSERT INTO maintenance_requests
                        (room_id, reported_by_user_id, title, priority, status, description)
                    VALUES (1, ?, ?, 'MEDIUM', 'NEW', ?)
                    """;
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, currentUser.getId());
                ps.setString(2, "[APP] Staff Alert: " + currentUser.getUsername());
                ps.setString(3, desc);
                ps.executeUpdate();

                Platform.runLater(() -> {
                    txtIssueDesc.clear();
                    showAlert(Alert.AlertType.INFORMATION, "Report Filed", "Maintenance has been alerted to the anomaly.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Transmission Failed", "Could not send report."));
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
    public void handleThemeToggle() {
        ThemeManager.toggle();
    }

}