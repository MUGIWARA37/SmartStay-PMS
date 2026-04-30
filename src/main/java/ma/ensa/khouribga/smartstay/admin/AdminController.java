package ma.ensa.khouribga.smartstay.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.*;
import ma.ensa.khouribga.smartstay.model.*;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminController {

    @FXML private Label statAvailable;
    @FXML private Label statOccupied;
    @FXML private Label statCleaning;
    @FXML private Label statMaint;
    @FXML private Label statActiveRes;

    @FXML private FlowPane roomGrid;
    @FXML private FlowPane resGrid;
    @FXML private FlowPane payrollGrid;
    @FXML private FlowPane staffGrid;

    @FXML private ComboBox<Room.Status> roomStatusFilter;
    @FXML private ComboBox<Room.Status> roomStatusEdit;
    @FXML private ComboBox<String> resStatusFilter;
    @FXML private ComboBox<String> staffRoleFilter;
    @FXML private ComboBox<String> staffStatusFilter;
    @FXML private DatePicker payPeriodStart;
    @FXML private DatePicker payPeriodEnd;

    @FXML private BarChart<String, Number> revenueChart;
    @FXML private CategoryAxis revenueXAxis;
    @FXML private NumberAxis revenueYAxis;
    @FXML private ComboBox<Integer> revenueYearPicker;
    @FXML private Label revenueTotalLabel;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Room selectedRoom;
    private VBox selectedRoomCard;
    private Reservation selectedRes;
    private VBox selectedResCard;
    private Payroll selectedPayroll;
    private VBox selectedPayrollCard;
    private User selectedStaff;
    private VBox selectedStaffCard;

    @FXML
    public void initialize() {
        try { SessionManager.requireRole(User.Role.ADMIN); } 
        catch (Exception e) { Platform.runLater(() -> Navigator.goToLogin(statAvailable)); return; }

        setupFilters();
        loadOverview();
        loadAllRooms();
        loadAllReservations();
        loadAllPayroll();
        loadAllStaff();
        setupRevenueYearPicker();
    }

    private void setupFilters() {
        if (roomStatusFilter != null) {
            roomStatusFilter.setItems(FXCollections.observableArrayList(Room.Status.values()));
            roomStatusFilter.setOnAction(e -> filterRooms());
        }
        if (roomStatusEdit != null) {
            roomStatusEdit.setItems(FXCollections.observableArrayList(Room.Status.values()));
        }
        if (resStatusFilter != null) {
            resStatusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"));
            resStatusFilter.setValue("ALL");
            resStatusFilter.setOnAction(e -> filterReservations());
        }
        if (payPeriodStart != null) payPeriodStart.setValue(LocalDate.now().withDayOfMonth(1));
        if (payPeriodEnd != null) payPeriodEnd.setValue(LocalDate.now());

        if (staffRoleFilter != null) {
            staffRoleFilter.setItems(FXCollections.observableArrayList("ALL", "STAFF", "ADMIN", "CLIENT"));
            staffRoleFilter.setValue("ALL");
            staffRoleFilter.setOnAction(e -> filterStaff());
        }
        if (staffStatusFilter != null) {
            staffStatusFilter.setItems(FXCollections.observableArrayList("ALL", "ACTIVE", "INACTIVE"));
            staffStatusFilter.setValue("ALL");
            staffStatusFilter.setOnAction(e -> filterStaff());
        }
    }

    @FXML 
    public void goToProfile(MouseEvent event) {
        Navigator.navigateTo((Node) event.getSource(), Navigator.ADMIN_PROFILE);
    }

    @FXML public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Navigator.goToLogin(statAvailable);
    }

    @FXML public void refreshOverview() { loadOverview(); }

    private void loadOverview() {
        new Thread(() -> {
            try {
                int[] counts = RoomDao.countByStatus();
                List<Reservation> active = ReservationDao.findActive();
                Platform.runLater(() -> {
                    if (statAvailable != null) statAvailable.setText(String.valueOf(counts[Room.Status.AVAILABLE.ordinal()]));
                    if (statOccupied != null) statOccupied.setText(String.valueOf(counts[Room.Status.OCCUPIED.ordinal()]));
                    if (statMaint != null) statMaint.setText(String.valueOf(counts[Room.Status.MAINTENANCE.ordinal()]));
                    if (statActiveRes != null) statActiveRes.setText(String.valueOf(active.size()));
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML public void loadAllRooms() {
        new Thread(() -> {
            try {
                List<Room> rooms = RoomDao.findAll();
                Platform.runLater(() -> populateRoomGrid(rooms));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML public void filterRooms() {
        if (roomStatusFilter.getValue() == null) { loadAllRooms(); return; }
        new Thread(() -> {
            try {
                List<Room> rooms = RoomDao.findByStatus(roomStatusFilter.getValue());
                Platform.runLater(() -> populateRoomGrid(rooms));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void populateRoomGrid(List<Room> rooms) {
        if (roomGrid == null) return;
        roomGrid.getChildren().clear();
        selectedRoom = null;
        selectedRoomCard = null;

        for (Room room : rooms) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblNum = new Label("Room " + room.getRoomNumber());
            lblNum.getStyleClass().add("card-header-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(room.getStatus().toString());
            header.getChildren().addAll(lblNum, spacer, badge);

            Label lblType = new Label("Type: " + room.getTypeName());
            lblType.getStyleClass().add("card-detail-text");
            Label lblFloor = new Label("Floor: " + room.getFloor());
            lblFloor.getStyleClass().add("card-detail-text");
            
            card.getChildren().addAll(header, new Separator(), lblType, lblFloor);
            if (room.getNotes() != null && !room.getNotes().isEmpty()) {
                Label lblNotes = new Label("Notes: " + room.getNotes());
                lblNotes.getStyleClass().add("card-detail-text");
                lblNotes.setStyle("-fx-text-fill: #e74c3c;");
                card.getChildren().add(lblNotes);
            }

            card.setOnMouseClicked(e -> {
                if (selectedRoomCard != null) selectedRoomCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedRoomCard = card;
                selectedRoom = room;
            });

            roomGrid.getChildren().add(card);
        }
    }

    @FXML public void updateRoomStatus() {
        if (selectedRoom == null || roomStatusEdit.getValue() == null) {
            showAlert("Select a room card and a new status from the dropdown."); return;
        }
        new Thread(() -> {
            try {
                RoomDao.updateStatus(selectedRoom.getId(), roomStatusEdit.getValue());
                Platform.runLater(this::loadAllRooms);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    @FXML public void loadAllReservations() {
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findAll();
                Platform.runLater(() -> populateResGrid(list));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML public void filterReservations() {
        if (resStatusFilter.getValue() == null || resStatusFilter.getValue().equals("ALL")) {
            loadAllReservations(); return;
        }
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findByStatus(Reservation.Status.valueOf(resStatusFilter.getValue()));
                Platform.runLater(() -> populateResGrid(list));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void populateResGrid(List<Reservation> list) {
        if (resGrid == null) return;
        resGrid.getChildren().clear();
        selectedRes = null;
        selectedResCard = null;

        for (Reservation res : list) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblCode = new Label(res.getReservationCode());
            lblCode.getStyleClass().add("card-header-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(res.getStatus().toString());
            header.getChildren().addAll(lblCode, spacer, badge);

            Label lblGuest = new Label("Guest: " + res.getGuestFullName());
            lblGuest.getStyleClass().add("card-detail-text");
            Label lblRoom = new Label("Room: " + res.getRoomNumber());
            lblRoom.getStyleClass().add("card-detail-text");
            Label lblDates = new Label("Dates: " + res.getCheckInDate().format(DATE_FMT) + " to " + res.getCheckOutDate().format(DATE_FMT));
            lblDates.getStyleClass().add("card-detail-text");

            card.getChildren().addAll(header, new Separator(), lblGuest, lblRoom, lblDates);

            card.setOnMouseClicked(e -> {
                if (selectedResCard != null) selectedResCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedResCard = card;
                selectedRes = res;
            });

            resGrid.getChildren().add(card);
        }
    }

    @FXML public void doCheckIn() {
        if (selectedRes == null) { showAlert("Select a reservation card."); return; }
        new Thread(() -> {
            try { ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_IN);
                Platform.runLater(this::loadAllReservations); }
            catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    @FXML public void doCheckOut() {
        if (selectedRes == null) { showAlert("Select a reservation card."); return; }
        new Thread(() -> {
            try { ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_OUT);
                Platform.runLater(this::loadAllReservations); }
            catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    @FXML public void loadAllPayroll() {
        new Thread(() -> {
            try {
                List<Payroll> list = PayrollDao.findAll();
                Platform.runLater(() -> populatePayrollGrid(list));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void populatePayrollGrid(List<Payroll> list) {
        if (payrollGrid == null) return;
        payrollGrid.getChildren().clear();
        selectedPayroll = null;
        selectedPayrollCard = null;

        for (Payroll pay : list) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblStaff = new Label(pay.getStaffUsername());
            lblStaff.getStyleClass().add("card-header-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(pay.getStatus().toString());
            header.getChildren().addAll(lblStaff, spacer, badge);

            Label lblPos = new Label("Position: " + pay.getStaffPosition());
            lblPos.getStyleClass().add("card-detail-text");
            Label lblDates = new Label("Period: " + pay.getPeriodStart().format(DATE_FMT) + " - " + pay.getPeriodEnd().format(DATE_FMT));
            lblDates.getStyleClass().add("card-detail-text");
            Label lblNet = new Label(String.format("Net Salary: %.2f MAD", pay.getNetSalary()));
            lblNet.getStyleClass().add("card-detail-text");
            lblNet.setStyle("-fx-text-fill: #c5a059; -fx-font-weight: bold;");

            card.getChildren().addAll(header, new Separator(), lblPos, lblDates, lblNet);

            card.setOnMouseClicked(e -> {
                if (selectedPayrollCard != null) selectedPayrollCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedPayrollCard = card;
                selectedPayroll = pay;
            });

            payrollGrid.getChildren().add(card);
        }
    }

    @FXML public void generatePayroll() {
        if (payPeriodStart.getValue() == null || payPeriodEnd.getValue() == null) return;
        LocalDate start = payPeriodStart.getValue(), end = payPeriodEnd.getValue();
        if (!end.isAfter(start)) { showAlert("Select a valid period."); return; }
        new Thread(() -> {
            try { PayrollDao.generateForPeriod(start, end);
                Platform.runLater(() -> { loadAllPayroll(); showAlert("Payroll generated."); });
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    @FXML public void markPayrollPaid() {
        if (selectedPayroll == null) { showAlert("Select a payroll card."); return; }
        new Thread(() -> {
            try { PayrollDao.markPaid(selectedPayroll.getId());
                Platform.runLater(this::loadAllPayroll);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    // ── Staff Management ──────────────────────────────────────────────────────

    @FXML public void loadAllStaff() {
        new Thread(() -> {
            try {
                List<User> users = UserDao.findAll();
                Platform.runLater(() -> populateStaffGrid(users));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML public void filterStaff() {
        String role = staffRoleFilter != null ? staffRoleFilter.getValue() : "ALL";
        String status = staffStatusFilter != null ? staffStatusFilter.getValue() : "ALL";
        new Thread(() -> {
            try {
                List<User> users = UserDao.findAll();
                List<User> filtered = users.stream()
                    .filter(u -> "ALL".equals(role) || u.getRole().name().equals(role))
                    .filter(u -> "ALL".equals(status)
                            || ("ACTIVE".equals(status) && u.isActive())
                            || ("INACTIVE".equals(status) && !u.isActive()))
                    .toList();
                Platform.runLater(() -> populateStaffGrid(filtered));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void populateStaffGrid(List<User> users) {
        if (staffGrid == null) return;
        staffGrid.getChildren().clear();
        selectedStaff = null;
        selectedStaffCard = null;

        for (User user : users) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);

            // Avatar circle with initials
            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("avatar-circle");
            avatar.setPrefWidth(38); avatar.setPrefHeight(38);
            String initials = user.getUsername().length() >= 2
                    ? user.getUsername().substring(0, 2).toUpperCase()
                    : user.getUsername().toUpperCase();
            Label lblInitials = new Label(initials);
            lblInitials.getStyleClass().add("avatar-initials");
            lblInitials.setStyle("-fx-font-size: 14px;");
            avatar.getChildren().add(lblInitials);

            Label lblName = new Label(user.getUsername());
            lblName.getStyleClass().add("card-header-text");
            lblName.setStyle("-fx-padding: 0 0 0 10;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label statusBadge = user.isActive()
                    ? createBadge("ACTIVE") : createBadge("INACTIVE");
            header.getChildren().addAll(avatar, lblName, spacer, statusBadge);

            Label lblEmail = new Label("✉ " + user.getEmail());
            lblEmail.getStyleClass().add("card-detail-text");

            Label lblRole = new Label("Role: " + user.getRole().name());
            lblRole.getStyleClass().add("card-detail-text");
            lblRole.setStyle("-fx-text-fill: #c5a059;");

            Label lblId = new Label("ID: #" + user.getId());
            lblId.getStyleClass().add("card-detail-text");
            lblId.setStyle("-fx-text-fill: #606060; -fx-font-size: 11px;");

            card.getChildren().addAll(header, new Separator(), lblEmail, lblRole, lblId);

            card.setOnMouseClicked(e -> {
                if (selectedStaffCard != null) selectedStaffCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedStaffCard = card;
                selectedStaff = user;
            });

            staffGrid.getChildren().add(card);
        }
    }

    @FXML public void activateStaff() {
        if (selectedStaff == null) { showAlert("Select a staff card first."); return; }
        if (selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already active."); return; }
        new Thread(() -> {
            try {
                UserDao.setActive(selectedStaff.getId(), true);
                Platform.runLater(() -> { loadAllStaff(); showAlert(selectedStaff.getUsername() + " has been activated."); });
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    @FXML public void deactivateStaff() {
        if (selectedStaff == null) { showAlert("Select a staff card first."); return; }
        if (!selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already inactive."); return; }
        if (selectedStaff.getRole() == User.Role.ADMIN) { showAlert("Cannot deactivate an ADMIN account."); return; }
        new Thread(() -> {
            try {
                UserDao.setActive(selectedStaff.getId(), false);
                Platform.runLater(() -> { loadAllStaff(); showAlert(selectedStaff.getUsername() + " has been deactivated."); });
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

    // ── Revenue Chart ─────────────────────────────────────────────────────────

    private void setupRevenueYearPicker() {
        if (revenueYearPicker == null) return;
        int currentYear = LocalDate.now().getYear();
        revenueYearPicker.setItems(FXCollections.observableArrayList(
                currentYear - 2, currentYear - 1, currentYear));
        revenueYearPicker.setValue(currentYear);
    }

    @FXML public void loadRevenueChart() {
        if (revenueChart == null || revenueYearPicker.getValue() == null) return;
        int year = revenueYearPicker.getValue();

        new Thread(() -> {
            try {
                Map<String, Double> monthlyRevenue = fetchMonthlyRevenue(year);
                Platform.runLater(() -> renderRevenueChart(monthlyRevenue, year));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private Map<String, Double> fetchMonthlyRevenue(int year) throws Exception {
        // Query net_salary totals from payroll as a revenue proxy (paid salaries vs generated)
        // Primary source: reservations × price_per_night × nights stayed (most accurate)
        String sql = """
                SELECT MONTH(r.check_out_date) AS month,
                       SUM(DATEDIFF(r.check_out_date, r.check_in_date) * rt.price_per_night) AS total
                FROM reservations r
                JOIN rooms rm ON r.room_id = rm.id
                JOIN room_types rt ON rm.room_type_id = rt.id
                WHERE r.status IN ('CHECKED_OUT', 'CHECKED_IN')
                  AND YEAR(r.check_out_date) = ?
                GROUP BY MONTH(r.check_out_date)
                ORDER BY MONTH(r.check_out_date)
                """;

        Map<String, Double> result = new LinkedHashMap<>();
        // Pre-fill all months with 0
        for (Month m : Month.values()) {
            result.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0.0);
        }

        try (Connection conn = ma.ensa.khouribga.smartstay.db.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int monthNum = rs.getInt("month");
                    double total = rs.getDouble("total");
                    String monthName = Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    result.put(monthName, total);
                }
            }
        }
        return result;
    }

    private void renderRevenueChart(Map<String, Double> data, int year) {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue " + year);

        double annualTotal = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            XYChart.Data<String, Number> point = new XYChart.Data<>(entry.getKey(), entry.getValue());
            series.getData().add(point);
            annualTotal += entry.getValue();
        }

        revenueChart.getData().add(series);

        // Style bars after they're added to the scene
        double finalAnnualTotal = annualTotal;
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle(
                        "-fx-bar-fill: #c5a059;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-radius: 6;" +
                        "-fx-border-color: rgba(255,255,255,0.15);" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0.2, 0, 2);"
                    );
                }
            }
            if (revenueTotalLabel != null) {
                revenueTotalLabel.setText(String.format("Annual Total: %.2f MAD", finalAnnualTotal));
            }
        });

        // Style chart background
        revenueChart.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-plot-background-color: rgba(20,20,24,0.7);" +
            "-fx-horizontal-grid-lines-visible: true;" +
            "-fx-vertical-grid-lines-visible: false;"
        );
    }

    
    private Label createBadge(String statusText) {
        Label badge = new Label(statusText);
        badge.getStyleClass().add("badge");
        
        if (statusText.equals("AVAILABLE") || statusText.equals("CHECKED_OUT") || statusText.equals("PAID") || statusText.equals("ACTIVE")) {
            badge.getStyleClass().add("badge-available");
        } else if (statusText.equals("OCCUPIED") || statusText.equals("CANCELLED") || statusText.equals("INACTIVE")) {
            badge.getStyleClass().add("badge-occupied");
        } else if (statusText.equals("MAINTENANCE") || statusText.equals("PENDING")) {
            badge.getStyleClass().add("badge-maintenance");
        } else if (statusText.equals("CLEANING") || statusText.equals("CHECKED_IN")) {
            badge.getStyleClass().add("badge-cleaning");
        }
    
        return badge;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}