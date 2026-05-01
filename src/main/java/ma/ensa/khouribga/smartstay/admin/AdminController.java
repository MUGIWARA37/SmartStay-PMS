package ma.ensa.khouribga.smartstay.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.ThemeManager;
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

    // Dashboard charts
    @FXML private PieChart roomStatusChart;
    @FXML private BarChart<String, Number> clientChart;
    @FXML private CategoryAxis clientXAxis;
    @FXML private NumberAxis   clientYAxis;
    @FXML private PieChart staffStatusChart;

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

    @FXML private Button btnThemeToggle;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Room selectedRoom; private VBox selectedRoomCard;
    private Reservation selectedRes; private VBox selectedResCard;
    private Payroll selectedPayroll; private VBox selectedPayrollCard;
    private User selectedStaff; private VBox selectedStaffCard;

    @FXML
    public void initialize() {
        try { SessionManager.requireRole(User.Role.ADMIN); }
        catch (Exception e) { Platform.runLater(() -> Navigator.goToLogin(statAvailable)); return; }
        updateThemeButton();
        setupFilters();
        loadOverview();
        loadAllRooms();
        loadAllReservations();
        loadAllPayroll();
        loadAllStaff();
        setupRevenueYearPicker();
    }

    // ── Theme ────────────────────────────────────────────────────────────────
    @FXML public void handleThemeToggle() {
        ThemeManager.toggle();
        updateThemeButton();
        Platform.runLater(this::refreshChartStyles);
    }
    private void updateThemeButton() {
        if (btnThemeToggle != null) btnThemeToggle.setText(ThemeManager.getToggleLabel());
    }

    // ── Overview + Dashboard Charts ──────────────────────────────────────────
    @FXML public void refreshOverview() { loadOverview(); }

    private void loadOverview() {
        new Thread(() -> {
            try {
                int[] counts = RoomDao.countByStatus();
                List<Reservation> active = ReservationDao.findActive();
                List<User> allUsers = UserDao.findAll();
                Map<String, Integer> monthlyCheckIns = fetchMonthlyCheckIns(LocalDate.now().getYear());
                Platform.runLater(() -> {
                    if (statAvailable != null) statAvailable.setText(String.valueOf(counts[Room.Status.AVAILABLE.ordinal()]));
                    if (statOccupied  != null) statOccupied .setText(String.valueOf(counts[Room.Status.OCCUPIED.ordinal()]));
                    if (statMaint     != null) statMaint    .setText(String.valueOf(counts[Room.Status.MAINTENANCE.ordinal()]));
                    if (statActiveRes != null) statActiveRes.setText(String.valueOf(active.size()));
                    buildRoomStatusChart(counts);
                    buildClientChart(monthlyCheckIns);
                    buildStaffStatusChart(allUsers);
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void buildRoomStatusChart(int[] counts) {
        if (roomStatusChart == null) return;
        roomStatusChart.getData().clear();
        int available   = counts[Room.Status.AVAILABLE.ordinal()];
        int occupied    = counts[Room.Status.OCCUPIED.ordinal()];
        int cleaning    = counts.length > 2 ? counts[2] : 0;
        int maintenance = counts[Room.Status.MAINTENANCE.ordinal()];
        roomStatusChart.getData().addAll(
            new PieChart.Data("Available ("   + available   + ")", Math.max(available,   0.001)),
            new PieChart.Data("Occupied ("    + occupied    + ")", Math.max(occupied,     0.001)),
            new PieChart.Data("Cleaning ("    + cleaning    + ")", Math.max(cleaning,     0.001)),
            new PieChart.Data("Maintenance (" + maintenance + ")", Math.max(maintenance,  0.001))
        );
        String[] colors = {"#2ecc71", "#e74c3c", "#3498db", "#f1c40f"};
        Platform.runLater(() -> {
            var list = roomStatusChart.getData();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getNode() != null)
                    list.get(i).getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
            }
            styleChart(roomStatusChart);
        });
    }

    private void buildClientChart(Map<String, Integer> data) {
        if (clientChart == null) return;
        clientChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Check-ins");
        data.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        clientChart.getData().add(series);
        Platform.runLater(() -> {
            for (var d : series.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle(
                        "-fx-bar-fill: linear-gradient(to top, #8b0000, #c0392b);" +
                        "-fx-background-radius: 4; -fx-border-radius: 4;");
            }
            styleChart(clientChart);
        });
    }

    private void buildStaffStatusChart(List<User> users) {
        if (staffStatusChart == null) return;
        staffStatusChart.getData().clear();
        long active   = users.stream().filter(User::isActive).count();
        long inactive = users.stream().filter(u -> !u.isActive()).count();
        staffStatusChart.getData().addAll(
            new PieChart.Data("Active ("   + active   + ")", Math.max(active,   0.001)),
            new PieChart.Data("Inactive (" + inactive + ")", Math.max(inactive,  0.001))
        );
        Platform.runLater(() -> {
            var list = staffStatusChart.getData();
            if (list.size() >= 2) {
                if (list.get(0).getNode() != null) list.get(0).getNode().setStyle("-fx-pie-color: #2ecc71;");
                if (list.get(1).getNode() != null) list.get(1).getNode().setStyle("-fx-pie-color: #e74c3c;");
            }
            styleChart(staffStatusChart);
        });
    }

    private void styleChart(Chart c) { if (c != null) c.setStyle("-fx-background-color: transparent;"); }
    private void refreshChartStyles() { styleChart(roomStatusChart); styleChart(clientChart); styleChart(staffStatusChart); styleChart(revenueChart); }

    private Map<String, Integer> fetchMonthlyCheckIns(int year) throws Exception {
        String sql = "SELECT MONTH(check_in_date) AS m, COUNT(*) AS cnt FROM reservations WHERE YEAR(check_in_date) = ? AND status IN ('CHECKED_IN','CHECKED_OUT') GROUP BY MONTH(check_in_date) ORDER BY MONTH(check_in_date)";
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Month m : Month.values()) result.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0);
        try (Connection conn = ma.ensa.khouribga.smartstay.db.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(Month.of(rs.getInt("m")).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), rs.getInt("cnt"));
            }
        }
        return result;
    }

    // ── Filters ──────────────────────────────────────────────────────────────
    private void setupFilters() {
        if (roomStatusFilter != null) { roomStatusFilter.setItems(FXCollections.observableArrayList(Room.Status.values())); roomStatusFilter.setOnAction(e -> filterRooms()); }
        if (roomStatusEdit   != null) roomStatusEdit.setItems(FXCollections.observableArrayList(Room.Status.values()));
        if (resStatusFilter  != null) { resStatusFilter.setItems(FXCollections.observableArrayList("ALL","PENDING","CONFIRMED","CHECKED_IN","CHECKED_OUT","CANCELLED")); resStatusFilter.setValue("ALL"); resStatusFilter.setOnAction(e -> filterReservations()); }
        if (payPeriodStart   != null) payPeriodStart.setValue(LocalDate.now().withDayOfMonth(1));
        if (payPeriodEnd     != null) payPeriodEnd.setValue(LocalDate.now());
        if (staffRoleFilter  != null) { staffRoleFilter.setItems(FXCollections.observableArrayList("ALL","STAFF","ADMIN","CLIENT")); staffRoleFilter.setValue("ALL"); staffRoleFilter.setOnAction(e -> filterStaff()); }
        if (staffStatusFilter != null) { staffStatusFilter.setItems(FXCollections.observableArrayList("ALL","ACTIVE","INACTIVE")); staffStatusFilter.setValue("ALL"); staffStatusFilter.setOnAction(e -> filterStaff()); }
    }

    @FXML public void goToProfile(MouseEvent event) { Navigator.navigateTo((Node) event.getSource(), Navigator.ADMIN_PROFILE); }
    @FXML public void handleLogout(ActionEvent event) { SessionManager.logout(); Navigator.goToLogin(statAvailable); }

    // ── Rooms ─────────────────────────────────────────────────────────────────
    @FXML public void loadAllRooms() { new Thread(() -> { try { List<Room> rooms = RoomDao.findAll(); Platform.runLater(() -> populateRoomGrid(rooms)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }
    @FXML public void filterRooms() { if (roomStatusFilter.getValue() == null) { loadAllRooms(); return; } new Thread(() -> { try { List<Room> rooms = RoomDao.findByStatus(roomStatusFilter.getValue()); Platform.runLater(() -> populateRoomGrid(rooms)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }
    
    private void populateRoomGrid(List<Room> rooms) {
        if (roomGrid == null) return;
        roomGrid.getChildren().clear(); selectedRoom = null; selectedRoomCard = null;
        for (Room room : rooms) {
            VBox card = new VBox(8); card.getStyleClass().add("data-card");
            HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
            Label lblNum = new Label("Room " + room.getRoomNumber()); lblNum.getStyleClass().add("card-header-text");
            Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
            header.getChildren().addAll(lblNum, s, createBadge(room.getStatus().toString()));
            Label lblType = label("Type: " + room.getTypeName()); Label lblFloor = label("Floor: " + room.getFloor());
            card.getChildren().addAll(header, new Separator(), lblType, lblFloor);
            if (room.getNotes() != null && !room.getNotes().isEmpty()) { Label n = new Label("Notes: " + room.getNotes()); n.getStyleClass().add("card-detail-text"); n.setStyle("-fx-text-fill: #e74c3c;"); card.getChildren().add(n); }
            card.setOnMouseClicked(e -> { if (selectedRoomCard != null) selectedRoomCard.getStyleClass().remove("selected-card"); card.getStyleClass().add("selected-card"); selectedRoomCard = card; selectedRoom = room; });
            roomGrid.getChildren().add(card);
        }
    }

    @FXML public void updateRoomStatus() {
        if (selectedRoom == null || roomStatusEdit.getValue() == null) { showAlert("Select a room card and a new status."); return; }
        new Thread(() -> { try { RoomDao.updateStatus(selectedRoom.getId(), roomStatusEdit.getValue()); Platform.runLater(this::loadAllRooms); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start();
    }

    // ── Reservations ──────────────────────────────────────────────────────────
    @FXML public void loadAllReservations() { new Thread(() -> { try { List<Reservation> list = ReservationDao.findAll(); Platform.runLater(() -> populateResGrid(list)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }
    @FXML public void filterReservations() { if (resStatusFilter.getValue() == null || "ALL".equals(resStatusFilter.getValue())) { loadAllReservations(); return; } new Thread(() -> { try { List<Reservation> list = ReservationDao.findByStatus(Reservation.Status.valueOf(resStatusFilter.getValue())); Platform.runLater(() -> populateResGrid(list)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }

    private void populateResGrid(List<Reservation> list) {
        if (resGrid == null) return;
        resGrid.getChildren().clear(); selectedRes = null; selectedResCard = null;
        for (Reservation res : list) {
            VBox card = new VBox(8); card.getStyleClass().add("data-card");
            HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(res.getReservationCode()); lbl.getStyleClass().add("card-header-text");
            Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
            header.getChildren().addAll(lbl, s, createBadge(res.getStatus().toString()));
            card.getChildren().addAll(header, new Separator(), label("Guest: " + res.getGuestFullName()), label("Room: " + res.getRoomNumber()), label("Dates: " + res.getCheckInDate().format(DATE_FMT) + " to " + res.getCheckOutDate().format(DATE_FMT)));
            card.setOnMouseClicked(e -> { if (selectedResCard != null) selectedResCard.getStyleClass().remove("selected-card"); card.getStyleClass().add("selected-card"); selectedResCard = card; selectedRes = res; });
            resGrid.getChildren().add(card);
        }
    }

    @FXML public void doCheckIn() { if (selectedRes == null) { showAlert("Select a reservation card."); return; } new Thread(() -> { try { ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_IN); Platform.runLater(this::loadAllReservations); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }
    @FXML public void doCheckOut() { if (selectedRes == null) { showAlert("Select a reservation card."); return; } new Thread(() -> { try { ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_OUT); Platform.runLater(this::loadAllReservations); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }

    // ── Payroll ───────────────────────────────────────────────────────────────
    @FXML public void loadAllPayroll() { new Thread(() -> { try { List<Payroll> list = PayrollDao.findAll(); Platform.runLater(() -> populatePayrollGrid(list)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }

    private void populatePayrollGrid(List<Payroll> list) {
        if (payrollGrid == null) return;
        payrollGrid.getChildren().clear(); selectedPayroll = null; selectedPayrollCard = null;
        for (Payroll pay : list) {
            VBox card = new VBox(8); card.getStyleClass().add("data-card");
            HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(pay.getStaffUsername()); lbl.getStyleClass().add("card-header-text");
            Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
            header.getChildren().addAll(lbl, s, createBadge(pay.getStatus().toString()));
            Label net = new Label(String.format("Net Salary: %.2f MAD", pay.getNetSalary())); net.getStyleClass().add("card-detail-text"); net.setStyle("-fx-text-fill: #c5a059; -fx-font-weight: bold;");
            card.getChildren().addAll(header, new Separator(), label("Position: " + pay.getStaffPosition()), label("Period: " + pay.getPeriodStart().format(DATE_FMT) + " - " + pay.getPeriodEnd().format(DATE_FMT)), net);
            card.setOnMouseClicked(e -> { if (selectedPayrollCard != null) selectedPayrollCard.getStyleClass().remove("selected-card"); card.getStyleClass().add("selected-card"); selectedPayrollCard = card; selectedPayroll = pay; });
            payrollGrid.getChildren().add(card);
        }
    }

    @FXML public void generatePayroll() {
        if (payPeriodStart.getValue() == null || payPeriodEnd.getValue() == null) return;
        LocalDate start = payPeriodStart.getValue(), end = payPeriodEnd.getValue();
        if (!end.isAfter(start)) { showAlert("Select a valid period."); return; }
        new Thread(() -> { try { PayrollDao.generateForPeriod(start, end); Platform.runLater(() -> { loadAllPayroll(); showAlert("Payroll generated."); }); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start();
    }

    @FXML public void markPayrollPaid() {
        if (selectedPayroll == null) { showAlert("Select a payroll card."); return; }
        new Thread(() -> { try { PayrollDao.markPaid(selectedPayroll.getId()); Platform.runLater(this::loadAllPayroll); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start();
    }

    // ── Staff ──────────────────────────────────────────────────────────────────
    @FXML public void loadAllStaff() { new Thread(() -> { try { List<User> users = UserDao.findAll(); Platform.runLater(() -> populateStaffGrid(users)); } catch (Exception ex) { ex.printStackTrace(); } }).start(); }

    @FXML public void filterStaff() {
        String role = staffRoleFilter != null ? staffRoleFilter.getValue() : "ALL";
        String status = staffStatusFilter != null ? staffStatusFilter.getValue() : "ALL";
        new Thread(() -> { try { List<User> filtered = UserDao.findAll().stream().filter(u -> "ALL".equals(role) || u.getRole().name().equals(role)).filter(u -> "ALL".equals(status) || ("ACTIVE".equals(status) && u.isActive()) || ("INACTIVE".equals(status) && !u.isActive())).toList(); Platform.runLater(() -> populateStaffGrid(filtered)); } catch (Exception ex) { ex.printStackTrace(); } }).start();
    }

    private void populateStaffGrid(List<User> users) {
        if (staffGrid == null) return;
        staffGrid.getChildren().clear(); selectedStaff = null; selectedStaffCard = null;
        for (User user : users) {
            VBox card = new VBox(8); card.getStyleClass().add("data-card");
            HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
            StackPane avatar = new StackPane(); avatar.getStyleClass().add("avatar-circle"); avatar.setPrefWidth(38); avatar.setPrefHeight(38);
            String initials = user.getUsername().length() >= 2 ? user.getUsername().substring(0, 2).toUpperCase() : user.getUsername().toUpperCase();
            Label li = new Label(initials); li.getStyleClass().add("avatar-initials"); li.setStyle("-fx-font-size: 14px;"); avatar.getChildren().add(li);
            Label lblName = new Label(user.getUsername()); lblName.getStyleClass().add("card-header-text"); lblName.setStyle("-fx-padding: 0 0 0 10;");
            Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
            header.getChildren().addAll(avatar, lblName, s, user.isActive() ? createBadge("ACTIVE") : createBadge("INACTIVE"));
            Label em = new Label("✉ " + user.getEmail()); em.getStyleClass().add("card-detail-text");
            Label rl = new Label("Role: " + user.getRole().name()); rl.getStyleClass().add("card-detail-text"); rl.setStyle("-fx-text-fill: #c5a059;");
            Label id = new Label("ID: #" + user.getId()); id.getStyleClass().add("card-detail-text"); id.setStyle("-fx-text-fill: #606060; -fx-font-size: 11px;");
            card.getChildren().addAll(header, new Separator(), em, rl, id);
            card.setOnMouseClicked(e -> { if (selectedStaffCard != null) selectedStaffCard.getStyleClass().remove("selected-card"); card.getStyleClass().add("selected-card"); selectedStaffCard = card; selectedStaff = user; });
            staffGrid.getChildren().add(card);
        }
    }

    @FXML public void activateStaff() { if (selectedStaff == null) { showAlert("Select a staff card first."); return; } if (selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already active."); return; } new Thread(() -> { try { UserDao.setActive(selectedStaff.getId(), true); Platform.runLater(() -> { loadAllStaff(); showAlert(selectedStaff.getUsername() + " activated."); }); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }
    @FXML public void deactivateStaff() { if (selectedStaff == null) { showAlert("Select a staff card first."); return; } if (!selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already inactive."); return; } if (selectedStaff.getRole() == User.Role.ADMIN) { showAlert("Cannot deactivate an ADMIN account."); return; } new Thread(() -> { try { UserDao.setActive(selectedStaff.getId(), false); Platform.runLater(() -> { loadAllStaff(); showAlert(selectedStaff.getUsername() + " deactivated."); }); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }

    // ── Revenue Chart ──────────────────────────────────────────────────────────
    private void setupRevenueYearPicker() {
        if (revenueYearPicker == null) return;
        int currentYear = LocalDate.now().getYear();
        revenueYearPicker.setItems(FXCollections.observableArrayList(currentYear - 2, currentYear - 1, currentYear));
        revenueYearPicker.setValue(currentYear);
    }

    @FXML public void loadRevenueChart() {
        if (revenueChart == null || revenueYearPicker.getValue() == null) return;
        int year = revenueYearPicker.getValue();
        new Thread(() -> { try { Map<String, Double> data = fetchMonthlyRevenue(year); Platform.runLater(() -> renderRevenueChart(data, year)); } catch (Exception ex) { ex.printStackTrace(); } }).start();
    }

    private Map<String, Double> fetchMonthlyRevenue(int year) throws Exception {
        String sql = "SELECT MONTH(r.check_out_date) AS month, SUM(DATEDIFF(r.check_out_date, r.check_in_date) * rt.price_per_night) AS total FROM reservations r JOIN rooms rm ON r.room_id = rm.id JOIN room_types rt ON rm.room_type_id = rt.id WHERE r.status IN ('CHECKED_OUT','CHECKED_IN') AND YEAR(r.check_out_date) = ? GROUP BY MONTH(r.check_out_date) ORDER BY MONTH(r.check_out_date)";
        Map<String, Double> result = new LinkedHashMap<>();
        for (Month m : Month.values()) result.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0.0);
        try (Connection conn = ma.ensa.khouribga.smartstay.db.Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.put(Month.of(rs.getInt("month")).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), rs.getDouble("total")); }
        }
        return result;
    }

    private void renderRevenueChart(Map<String, Double> data, int year) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Revenue " + year);
        double[] total = {0};
        data.forEach((k, v) -> { series.getData().add(new XYChart.Data<>(k, v)); total[0] += v; });
        revenueChart.getData().add(series);
        Platform.runLater(() -> {
            for (var d : series.getData()) { if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #c5a059; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0.2, 0, 2);"); }
            if (revenueTotalLabel != null) revenueTotalLabel.setText(String.format("Annual Total: %.2f MAD", total[0]));
            styleChart(revenueChart);
        });
        revenueChart.setStyle("-fx-background-color: transparent; -fx-plot-background-color: rgba(20,20,24,0.7);");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Label label(String text) { Label l = new Label(text); l.getStyleClass().add("card-detail-text"); return l; }

    private Label createBadge(String statusText) {
        Label badge = new Label(statusText); badge.getStyleClass().add("badge");
        if (statusText.equals("AVAILABLE") || statusText.equals("CHECKED_OUT") || statusText.equals("PAID") || statusText.equals("ACTIVE")) badge.getStyleClass().add("badge-available");
        else if (statusText.equals("OCCUPIED") || statusText.equals("CANCELLED") || statusText.equals("INACTIVE")) badge.getStyleClass().add("badge-occupied");
        else if (statusText.equals("MAINTENANCE") || statusText.equals("PENDING")) badge.getStyleClass().add("badge-maintenance");
        else if (statusText.equals("CLEANING") || statusText.equals("CHECKED_IN")) badge.getStyleClass().add("badge-cleaning");
        return badge;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}
