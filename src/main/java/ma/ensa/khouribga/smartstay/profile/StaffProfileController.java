package ma.ensa.khouribga.smartstay.profile;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Controller for staff_profile.fxml
 * FILE: src/main/java/.../profile/StaffProfileController.java
 */
public class StaffProfileController {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Circle  avatarCircle;
    @FXML private Label   avatarInitials;
    @FXML private Label   fullNameLabel;
    @FXML private Label   positionLabel;
    @FXML private Label   employeeCodeLabel;
    @FXML private Label   departmentLabel;
    @FXML private Label   hireDateLabel;
    @FXML private Label   salaryLabel;

    // ── Contact ───────────────────────────────────────────────────────────────
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField emergencyField;
    @FXML private Label     contactMsg;

    // ── Password ──────────────────────────────────────────────────────────────
    @FXML private PasswordField oldPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label         passMsg;

    // ── Shifts ────────────────────────────────────────────────────────────────
    @FXML private HBox  shiftWeekRow;
    @FXML private Label weekRangeLabel;
    @FXML private Label hoursLoggedLabel;

    // ── Issue report ──────────────────────────────────────────────────────────
    @FXML private TextField issueTitleField;
    @FXML private TextArea  issueDescField;
    @FXML private Label     issueMsg;

    // ── State ─────────────────────────────────────────────────────────────────
    private long   staffProfileId = -1;
    private String currentPasswordHash;
    private String previousRoute = Navigator.RECEPTION; // default back destination

    @FXML
    public void initialize() {
        loadProfile();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public init (called by Navigator before show)
    // ─────────────────────────────────────────────────────────────────────────
    public void setPreviousRoute(String route) {
        this.previousRoute = route;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load profile data
    // ─────────────────────────────────────────────────────────────────────────
    private void loadProfile() {
        User user = SessionManager.getCurrentUser();
        long userId = user.getId();

        Task<Map<String, String>> task = new Task<>() {
            @Override protected Map<String, String> call() throws Exception {
                Map<String, String> data = new LinkedHashMap<>();
                String sql = """
                    SELECT u.email, u.password_hash, u.created_at,
                           sp.id AS sp_id, sp.employee_code, sp.position, sp.department,
                           sp.hire_date, sp.salary_base, sp.emergency_contact,
                           g.first_name, g.last_name, g.phone
                    FROM users u
                    LEFT JOIN staff_profiles sp ON sp.user_id = u.id
                    LEFT JOIN guests g ON g.email = u.email
                    WHERE u.id = ?
                    LIMIT 1
                    """;
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            data.put("email",          rs.getString("email"));
                            data.put("passwordHash",   rs.getString("password_hash"));
                            data.put("spId",           String.valueOf(rs.getLong("sp_id")));
                            data.put("employeeCode",   rs.getString("employee_code"));
                            data.put("position",       rs.getString("position"));
                            data.put("department",     rs.getString("department"));
                            data.put("hireDate",       rs.getString("hire_date"));
                            data.put("salary",         rs.getString("salary_base"));
                            data.put("emergency",      rs.getString("emergency_contact"));
                            data.put("firstName",      rs.getString("first_name"));
                            data.put("lastName",       rs.getString("last_name"));
                            data.put("phone",          rs.getString("phone"));
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
            staffProfileId = Long.parseLong(d.getOrDefault("spId", "-1"));

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
            positionLabel.setText("⚔  " + d.getOrDefault("position", "Staff"));
            employeeCodeLabel.setText("ID: " + d.getOrDefault("employeeCode", "—"));
            departmentLabel.setText("🏯  " + d.getOrDefault("department", "—"));
            hireDateLabel.setText("Hired: " + d.getOrDefault("hireDate", "—"));
            salaryLabel.setText("Base Salary: " + d.getOrDefault("salary", "—") + " MAD");

            emailField.setText(d.getOrDefault("email", ""));
            phoneField.setText(d.getOrDefault("phone", ""));
            emergencyField.setText(d.getOrDefault("emergency", ""));

            loadWeekShifts();
        });

        task.setOnFailed(e -> showMsg(contactMsg, "Failed to load profile.", true));
        Thread t = new Thread(task, "profile-load");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weekly shift schedule
    // ─────────────────────────────────────────────────────────────────────────
    private void loadWeekShifts() {
        if (staffProfileId < 0) return;

        LocalDate today    = LocalDate.now();
        LocalDate monday   = today.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1);
        LocalDate sunday   = monday.plusDays(6);
        weekRangeLabel.setText(monday.format(DateTimeFormatter.ofPattern("MMM d")) +
                               " – " + sunday.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        final long spId = staffProfileId;
        Task<List<ShiftDay>> task = new Task<>() {
            @Override protected List<ShiftDay> call() throws Exception {
                List<ShiftDay> days = new ArrayList<>();
                String sql = """
                    SELECT ssa.assigned_date, s.shift_name, s.start_time, s.end_time,
                           sa.check_in, sa.check_out, sa.status
                    FROM staff_shift_assignments ssa
                    JOIN shifts s ON s.id = ssa.shift_id
                    LEFT JOIN staff_attendance sa ON sa.staff_profile_id = ssa.staff_profile_id
                         AND sa.date = ssa.assigned_date
                    WHERE ssa.staff_profile_id = ?
                      AND ssa.assigned_date BETWEEN ? AND ?
                    ORDER BY ssa.assigned_date
                    """;
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, spId);
                    ps.setDate(2, java.sql.Date.valueOf(monday));
                    ps.setDate(3, java.sql.Date.valueOf(sunday));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ShiftDay sd = new ShiftDay();
                            sd.date      = rs.getDate("assigned_date").toLocalDate();
                            sd.shiftName = rs.getString("shift_name");
                            sd.startTime = rs.getString("start_time");
                            sd.endTime   = rs.getString("end_time");
                            sd.checkIn   = rs.getTimestamp("check_in");
                            sd.checkOut  = rs.getTimestamp("check_out");
                            sd.status    = rs.getString("status");
                            days.add(sd);
                        }
                    }
                }
                return days;
            }
        };

        task.setOnSucceeded(e -> buildShiftCards(task.getValue(), monday));
        Thread t = new Thread(task, "shift-load");
        t.setDaemon(true);
        t.start();
    }

    private void buildShiftCards(List<ShiftDay> shifts, LocalDate monday) {
        shiftWeekRow.getChildren().clear();
        String[] dayNames = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        Map<LocalDate, ShiftDay> byDate = new LinkedHashMap<>();
        for (ShiftDay sd : shifts) byDate.put(sd.date, sd);

        double totalHours = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            ShiftDay sd   = byDate.get(day);

            VBox card = new VBox(4);
            card.getStyleClass().add("shift-day-card");
            card.setPrefWidth(120);
            card.setMinWidth(100);
            if (day.equals(LocalDate.now())) card.getStyleClass().add("shift-day-today");

            Label dayLabel  = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("shift-day-name");
            Label dateLabel = new Label(day.getDayOfMonth() + "");
            dateLabel.getStyleClass().add("shift-day-num");

            card.getChildren().addAll(dayLabel, dateLabel);

            if (sd != null) {
                Label shiftLbl = new Label(sd.shiftName);
                shiftLbl.getStyleClass().add("shift-name-label");
                Label timeLbl = new Label(sd.startTime.substring(0,5) + "–" + sd.endTime.substring(0,5));
                timeLbl.getStyleClass().add("shift-time-label");
                card.getChildren().addAll(shiftLbl, timeLbl);

                if (sd.checkIn != null && sd.checkOut != null) {
                    double hrs = Duration.between(
                        sd.checkIn.toLocalDateTime(), sd.checkOut.toLocalDateTime()
                    ).toMinutes() / 60.0;
                    totalHours += hrs;
                    Label logged = new Label(String.format("%.1fh logged", hrs));
                    logged.getStyleClass().add("badge-available");
                    card.getChildren().add(logged);
                } else if (sd.status != null) {
                    Label statusLbl = new Label(sd.status);
                    statusLbl.getStyleClass().add("badge-pending");
                    card.getChildren().add(statusLbl);
                }
            } else {
                Label offLbl = new Label("Day off");
                offLbl.getStyleClass().add("label-muted");
                card.getChildren().add(offLbl);
            }

            shiftWeekRow.getChildren().add(card);
        }

        final double finalHours = totalHours;
        Platform.runLater(() ->
            hoursLoggedLabel.setText(String.format("Hours logged this week: %.1f h", finalHours)));
    }

    private static class ShiftDay {
        LocalDate date;
        String shiftName, startTime, endTime, status;
        Timestamp checkIn, checkOut;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save contact info
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void saveContactInfo() {
        String email     = emailField.getText().trim();
        String phone     = phoneField.getText().trim();
        String emergency = emergencyField.getText().trim();

        if (email.isEmpty() || !email.contains("@")) {
            showMsg(contactMsg, "Please enter a valid email address.", true); return;
        }

        long userId = SessionManager.getCurrentUser().getId();
        final long spId = staffProfileId;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                try (Connection conn = Database.getConnection()) {
                    // Update email on users table
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET email = ? WHERE id = ?")) {
                        ps.setString(1, email);
                        ps.setLong(2, userId);
                        ps.executeUpdate();
                    }
                    // Update phone + emergency on staff_profiles
                    if (spId > 0) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE staff_profiles SET emergency_contact = ? WHERE id = ?")) {
                            ps.setString(1, emergency);
                            ps.setLong(2, spId);
                            ps.executeUpdate();
                        }
                    }
                    // Update guest record if exists
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE guests SET phone = ? WHERE email = (SELECT email FROM users WHERE id = ?)")) {
                        ps.setString(1, phone);
                        ps.setLong(2, userId);
                        ps.executeUpdate();
                    }
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> showMsg(contactMsg, "✓ Contact info saved successfully.", false));
        task.setOnFailed(e  -> showMsg(contactMsg, "Error saving contact info.", true));
        new Thread(task, "save-contact").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Change password
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void changePassword() {
        String oldPass  = oldPassField.getText();
        String newPass  = newPassField.getText();
        String confirm  = confirmPassField.getText();

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

        String newHash  = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
        long userId     = SessionManager.getCurrentUser().getId();

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
        new Thread(task, "change-pass").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Submit issue report (logged to maintenance_requests as a special entry)
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void submitIssue() {
        String title = issueTitleField.getText().trim();
        String desc  = issueDescField.getText().trim();
        if (title.isEmpty()) { showMsg(issueMsg, "Please enter an issue title.", true); return; }
        if (desc.isEmpty())  { showMsg(issueMsg, "Please describe the issue.", true); return; }

        long userId = SessionManager.getCurrentUser().getId();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                // We store app issues as maintenance requests on room_id=1 with title prefixed [APP]
                // This reuses existing infrastructure without a new table
                String sql = """
                    INSERT INTO maintenance_requests
                      (room_id, reported_by_user_id, priority, status, title, description, created_at)
                    VALUES (1, ?, 'MEDIUM', 'NEW', ?, ?, NOW())
                    """;
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    ps.setString(2, "[APP] " + title);
                    ps.setString(3, desc);
                    ps.executeUpdate();
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            issueTitleField.clear(); issueDescField.clear();
            showMsg(issueMsg, "✓ Issue reported. Thank you!", false);
        });
        task.setOnFailed(e -> showMsg(issueMsg, "Failed to submit issue report.", true));
        new Thread(task, "issue-submit").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void goBack() {
        Navigator.navigateTo(emailField, previousRoute);
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
}