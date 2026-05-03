package ma.ensa.khouribga.smartstay.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;
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

    @FXML private MediaView bgMediaView;
    @FXML private Label statAvailable;
    @FXML private Label statOccupied;
    @FXML private Label statCleaning;
    @FXML private Label statMaint;
    @FXML private Label statActiveRes;
    @FXML private VBox  liveResRows;
    @FXML private ProgressBar staffActiveBar;

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
    @FXML private DatePicker resDateFrom;
    @FXML private DatePicker resDateTo;
    @FXML private ComboBox<String> staffRoleFilter;
    @FXML private ComboBox<String> staffStatusFilter;
    @FXML private Label            lblSelectedStaff;
    @FXML private ComboBox<String> shiftPicker;
    @FXML private DatePicker       shiftDatePicker;
    @FXML private TextField        shiftNotes;
    @FXML private DatePicker payPeriodStart;
    @FXML private DatePicker payPeriodEnd;

    @FXML private BarChart<String, Number> revenueChart;
    @FXML private CategoryAxis revenueXAxis;
    @FXML private NumberAxis revenueYAxis;
    @FXML private ComboBox<Integer> revenueYearPicker;
    @FXML private Label revenueTotalLabel;

    @FXML private Button btnThemeToggle;

    @FXML private TabPane mainTabPane;
    @FXML private Label   headerTabLabel;

    @FXML private Button navBtnOverview;
    @FXML private Button navBtnRooms;
    @FXML private Button navBtnReservations;
    @FXML private Button navBtnPayroll;
    @FXML private Button navBtnStaff;
    @FXML private Button navBtnRevenue;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Room selectedRoom; private VBox selectedRoomCard;
    private Reservation selectedRes; private VBox selectedResCard;
    private Payroll selectedPayroll; private VBox selectedPayrollCard;
    private User selectedStaff; private VBox selectedStaffCard;

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        try { SessionManager.requireRole(User.Role.ADMIN); }
        catch (Exception e) { Platform.runLater(() -> Navigator.goToLogin(statAvailable)); return; }
        updateThemeButton();
        setupFilters();
        loadOverview();
        loadLiveReservations();
        loadAllRooms();
        loadAllReservations();
        loadAllPayroll();
        loadAllStaff();
        setupRevenueYearPicker();
        selectTab(0);
    }

    // ── Sidebar Navigation ───────────────────────────────────────────────────
    /** All nav buttons in sidebar order — used to reset active style. */
    private java.util.List<Button> sideNavButtons() {
        return java.util.List.of(
            navBtnOverview, navBtnRooms, navBtnReservations,
            navBtnPayroll, navBtnStaff, navBtnRevenue
        );
    }

    /** Tab labels matching Stitch sidebar names. */
    private static final String[] TAB_LABELS = {
        "Overview", "Room Mastery", "Guest Scrolls",
        "Financial Katana", "Zen Inventory", "Revenue Chart"
    };

    /**
     * Central tab switcher. Updates the active nav button style and the
     * header breadcrumb label, then selects the correct TabPane tab.
     */
    private void selectTab(int index) {
        java.util.List<Button> btns = sideNavButtons();
        for (int i = 0; i < btns.size(); i++) {
            Button b = btns.get(i);
            b.getStyleClass().remove("nav-button-active");
            if (!b.getStyleClass().contains("nav-button"))
                b.getStyleClass().add("nav-button");
            if (i == index) {
                b.getStyleClass().remove("nav-button");
                b.getStyleClass().add("nav-button-active");
            }
        }
        if (headerTabLabel != null)
            headerTabLabel.setText(TAB_LABELS[index]);
        mainTabPane.getSelectionModel().select(index);
    }

    @FXML public void navToOverview()     { selectTab(0); refreshOverview(); }
    @FXML public void navToRooms()        { selectTab(1); loadAllRooms(); }
    @FXML public void navToReservations() { selectTab(2); loadAllReservations(); }
    @FXML public void navToPayroll()      { selectTab(3); loadAllPayroll(); }
    @FXML public void navToStaff()        { selectTab(4); loadAllStaff(); }
    @FXML public void navToRevenue()      { selectTab(5); }

    // ── Theme ────────────────────────────────────────────────────────────────
    @FXML public void handleThemeToggle() {
        ThemeManager.toggle();
        updateThemeButton();
        Platform.runLater(this::refreshChartStyles);
    }
    @FXML public void handleThemeToggleClick(javafx.scene.input.MouseEvent e) { handleThemeToggle(); }
    private void updateThemeButton() {
        if (btnThemeToggle != null) btnThemeToggle.setText(ThemeManager.getToggleLabel());
    }

    // ── Overview + Dashboard Charts ──────────────────────────────────────────
    @FXML public void refreshOverview() { loadOverview(); loadLiveReservations(); }

    private void loadOverview() {
        new Thread(() -> {
            try {
                int[] counts = RoomDao.countByStatus();
                List<Reservation> active = ReservationDao.findActive();
                List<User> allUsers = UserDao.findAll();
                Map<String, Integer> monthlyCheckIns = fetchMonthlyCheckIns(LocalDate.now().getYear());
                Platform.runLater(() -> {
                    // Card 1: Total Rooms
                    int total = counts[Room.Status.AVAILABLE.ordinal()] + counts[Room.Status.OCCUPIED.ordinal()]
                              + (counts.length > 2 ? counts[2] : 0) + counts[Room.Status.MAINTENANCE.ordinal()];
                    if (statAvailable != null) statAvailable.setText(String.valueOf(total));
                    // Card 2: Active Guests (occupied rooms)
                    if (statOccupied  != null) statOccupied.setText(String.valueOf(counts[Room.Status.OCCUPIED.ordinal()]));
                    // Card 3: Today's check-ins (active reservations)
                    if (statActiveRes != null) statActiveRes.setText(String.valueOf(active.size()));
                    // Card 4: Monthly revenue placeholder — update via revenue query
                    long activeStaff = allUsers.stream().filter(User::isActive).count();
                    if (statCleaning  != null) statCleaning.setText(activeStaff + " Staff");
                    if (staffActiveBar != null) staffActiveBar.setProgress(allUsers.isEmpty() ? 0 : (double) activeStaff / allUsers.size());
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

    private void loadLiveReservations() {
        if (liveResRows == null) return;
        new Thread(() -> {
            try {
                List<Reservation> recent = ReservationDao.findActive();
                Platform.runLater(() -> {
                    liveResRows.getChildren().clear();
                    int shown = Math.min(recent.size(), 6);
                    for (int i = 0; i < shown; i++) {
                        Reservation r = recent.get(i);
                        HBox row = new HBox();
                        row.setStyle("-fx-padding:14 24;" +
                            (i % 2 == 0 ? "" : "-fx-background-color:rgba(255,255,255,0.02);") +
                            "-fx-border-color:rgba(255,255,255,0.04);-fx-border-width:0 0 1 0;");
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        // Avatar + name
                        String name = r.getGuestFullName();
                        String initials = name.length() >= 2 ? name.substring(0,2).toUpperCase() : name.toUpperCase();
                        StackPane avatar = new StackPane();
                        avatar.setPrefSize(28, 28);
                        avatar.setMinSize(28, 28);
                        avatar.setStyle("-fx-background-color:rgba(197,160,89,0.15);-fx-background-radius:14;" +
                            "-fx-border-color:rgba(197,160,89,0.3);-fx-border-radius:14;-fx-border-width:1;");
                        Label av = new Label(initials);
                        av.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#c5a059;");
                        avatar.getChildren().add(av);
                        HBox nameBox = new HBox(8, avatar, new Label(name));
                        nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        ((Label)nameBox.getChildren().get(1)).setStyle("-fx-font-size:12px;-fx-text-fill:#e0e0e0;");
                        nameBox.setMinWidth(200);

                        Label room  = new Label(r.getRoomNumber());   room.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;"); room.setMinWidth(180);
                        Label dates = new Label(r.getCheckInDate().format(DATE_FMT) + " – " + r.getCheckOutDate().format(DATE_FMT));
                        dates.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;"); HBox.setHgrow(dates, javafx.scene.layout.Priority.ALWAYS); dates.setMinWidth(180);

                        Label price = new Label(String.format("$%.2f", r.getBaseTotal()));
                        price.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#fff;"); price.setMinWidth(120);

                        // Status badge
                        String status = r.getStatus().toString();
                        String badgeColor = switch (status) {
                            case "CONFIRMED"  -> "rgba(30,132,73,0.2); -fx-text-fill:#1e8449;";
                            case "CHECKED_IN" -> "rgba(52,152,219,0.2); -fx-text-fill:#3498db;";
                            case "CANCELLED"  -> "rgba(231,76,60,0.2); -fx-text-fill:#e74c3c;";
                            default           -> "rgba(197,160,89,0.2); -fx-text-fill:#c5a059;";
                        };
                        Label badge = new Label(status.replace("_", " "));
                        badge.setStyle("-fx-background-color:" + badgeColor +
                            ";-fx-background-radius:12;-fx-font-size:9px;" +
                            "-fx-font-weight:bold;-fx-padding:3 10;");
                        badge.setMinWidth(100);

                        row.getChildren().addAll(nameBox, room, dates, price, badge);
                        liveResRows.getChildren().add(row);
                    }
                    if (recent.isEmpty()) {
                        Label empty = new Label("No active reservations");
                        empty.setStyle("-fx-text-fill:#666;-fx-font-size:12px;-fx-padding:20 24;");
                        liveResRows.getChildren().add(empty);
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private Map<String, Integer> fetchMonthlyCheckIns(int year) throws Exception {
        String sql = "SELECT MONTH(check_in_date) AS m, COUNT(*) AS cnt FROM reservations WHERE YEAR(check_in_date) = ? AND status IN ('CHECKED_IN','CHECKED_OUT') GROUP BY MONTH(check_in_date) ORDER BY m";
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

    // ── Filters ───────────────────────────────────────────────────────────────
    private void setupFilters() {
        if (roomStatusFilter != null) { roomStatusFilter.setItems(FXCollections.observableArrayList(Room.Status.values())); roomStatusFilter.setOnAction(e -> filterRooms()); }
        if (roomStatusEdit   != null) roomStatusEdit.setItems(FXCollections.observableArrayList(Room.Status.values()));
        if (resStatusFilter  != null) { resStatusFilter.setItems(FXCollections.observableArrayList("ALL","PENDING","CONFIRMED","CHECKED_IN","CHECKED_OUT","CANCELLED")); resStatusFilter.setValue("ALL"); resStatusFilter.setOnAction(e -> filterReservations()); }
        if (resDateFrom != null) { resDateFrom.setValue(LocalDate.now().withDayOfYear(1)); resDateFrom.setOnAction(e -> filterReservations()); }
        if (resDateTo   != null) { resDateTo.setValue(LocalDate.now().withMonth(12).withDayOfMonth(31)); resDateTo.setOnAction(e -> filterReservations()); }
        if (payPeriodStart   != null) payPeriodStart.setValue(LocalDate.now().withDayOfMonth(1));
        if (payPeriodEnd     != null) payPeriodEnd.setValue(LocalDate.now());
        if (staffRoleFilter  != null) { staffRoleFilter.setItems(FXCollections.observableArrayList("ALL","STAFF","ADMIN","CLIENT")); staffRoleFilter.setValue("ALL"); staffRoleFilter.setOnAction(e -> filterStaff()); }
        if (staffStatusFilter != null) { staffStatusFilter.setItems(FXCollections.observableArrayList("ALL","ACTIVE","INACTIVE")); staffStatusFilter.setValue("ALL"); staffStatusFilter.setOnAction(e -> filterStaff()); }
        if (shiftDatePicker != null) shiftDatePicker.setValue(LocalDate.now());
        if (shiftPicker != null) new Thread(() -> {
            try (var conn = ma.ensa.khouribga.smartstay.db.Database.getConnection();
                 var ps = conn.prepareStatement("SELECT shift_name, start_time, end_time FROM shifts ORDER BY start_time");
                 var rs = ps.executeQuery()) {
                var names = new java.util.ArrayList<String>();
                while (rs.next()) names.add(rs.getString("shift_name") + "  (" + rs.getString("start_time").substring(0,5) + " – " + rs.getString("end_time").substring(0,5) + ")");
                Platform.runLater(() -> shiftPicker.setItems(FXCollections.observableArrayList(names)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML public void goToProfile(MouseEvent event) { Navigator.navigateTo((Node) event.getSource(), Navigator.ADMIN_PROFILE); }
    @FXML public void handleLogout(ActionEvent event) { SessionManager.logout(); Navigator.goToLogin(statAvailable); }

    // ── Rooms ────────────────────────────────────────────────────────────────
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
    @FXML public void loadAllReservations() {
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        LocalDate yearEnd   = LocalDate.now().withMonth(12).withDayOfMonth(31);
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findByCheckInRange(yearStart, yearEnd);
                Platform.runLater(() -> populateResGrid(list));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }
    @FXML public void filterReservations() {
        LocalDate from   = (resDateFrom != null && resDateFrom.getValue() != null)
                           ? resDateFrom.getValue() : LocalDate.now().withDayOfYear(1);
        LocalDate to     = (resDateTo   != null && resDateTo.getValue()   != null)
                           ? resDateTo.getValue()   : LocalDate.now().withMonth(12).withDayOfMonth(31);
        String statusVal = (resStatusFilter != null) ? resStatusFilter.getValue() : "ALL";
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findByCheckInRange(from, to);
                if (statusVal != null && !"ALL".equals(statusVal)) {
                    Reservation.Status filter = Reservation.Status.valueOf(statusVal);
                    list = list.stream().filter(r -> r.getStatus() == filter).toList();
                }
                final List<Reservation> result = list;
                Platform.runLater(() -> populateResGrid(result));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

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

    @FXML public void doCancelReservation() {
        if (selectedRes == null) { showAlert("Select a reservation card."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CANCELLED);
                Platform.runLater(this::filterReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }).start();
    }

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
            card.getChildren().addAll(header, new Separator(), label("Position: " + pay.getStaffPosition()), label("Period: " + pay.getPeriodStart().format(DATE_FMT) + " - " + pay.getPeriodEnd().format(DATE_FMT)));
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

    // ── Staff ────────────────────────────────────────────────────────────────
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
            VBox card = new VBox(0);
            card.getStyleClass().add("data-card");
            card.setPrefWidth(340);
            card.setMinWidth(300);
            card.setMaxWidth(400);

            // ── Header row ────────────────────────────────────────────────────
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-padding: 16 16 12 16;");

            String initials = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
            StackPane avatar = new StackPane();
            avatar.setPrefSize(48, 48); avatar.setMinSize(48, 48);
            avatar.getStyleClass().add("avatar-circle");
            Label li = new Label(initials);
            li.getStyleClass().add("avatar-initials");
            li.setStyle("-fx-font-size: 17px;");
            avatar.getChildren().add(li);

            VBox nameCol = new VBox(4); HBox.setHgrow(nameCol, Priority.ALWAYS);
            Label lblName = new Label(user.getUsername());
            lblName.getStyleClass().add("card-header-text");
            lblName.setStyle("-fx-font-size: 15px;");
            Label lblRoleBadge = createBadge(user.getRole().name());
            nameCol.getChildren().addAll(lblName, lblRoleBadge);

            Label statusBadge = user.isActive() ? createBadge("ACTIVE") : createBadge("INACTIVE");
            header.getChildren().addAll(avatar, nameCol, statusBadge);

            // ── Gold divider ─────────────────────────────────────────────────
            Region divider = new Region();
            divider.setMaxWidth(Double.MAX_VALUE);
            divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color: rgba(197,160,89,0.18);");

            // ── Detail body ───────────────────────────────────────────────────
            VBox body = new VBox(9);
            body.setStyle("-fx-padding: 14 16 16 16;");

            // Email
            HBox emailRow = detailRow("✉", user.getEmail(), "#a0a0a0");
            // ID
            HBox idRow    = detailRow("#", "User ID: " + user.getId(), "#606060");

            // Staff profile fields (loaded async per card)
            Label lblPosition   = detailLabel("position", "Loading…", "#c5a059");
            Label lblDept       = detailLabel("dept",     "—", "#888");
            Label lblEmpCode    = detailLabel("code",     "—", "#888");
            Label lblHire       = detailLabel("hire",     "—", "#888");
            Label lblSalary     = detailLabel("salary",   "—", "#1e8449");
            Label lblShift      = detailLabel("shift",    "—", "#3498db");

            HBox posRow    = detailRow("🎯", "", "#c5a059");  posRow.getChildren().add(1, lblPosition);
            HBox deptRow   = detailRow("🏛", "", "#888");     deptRow.getChildren().add(1, lblDept);
            HBox codeRow   = detailRow("🪪", "", "#888");     codeRow.getChildren().add(1, lblEmpCode);
            HBox hireRow   = detailRow("📅", "", "#888");     hireRow.getChildren().add(1, lblHire);
            HBox salaryRow = detailRow("💴", "", "#1e8449");  salaryRow.getChildren().add(1, lblSalary);
            HBox shiftRow  = detailRow("⏰", "", "#3498db");  shiftRow.getChildren().add(1, lblShift);

            body.getChildren().addAll(emailRow, idRow, new Separator(),
                posRow, deptRow, codeRow, hireRow, salaryRow, shiftRow);

            card.getChildren().addAll(header, divider, body);
            card.setOnMouseClicked(e -> {
                if (selectedStaffCard != null) selectedStaffCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedStaffCard = card; selectedStaff = user;
                if (lblSelectedStaff != null)
                    lblSelectedStaff.setText(user.getUsername().toUpperCase() + "  ·  " + user.getRole());
            });
            staffGrid.getChildren().add(card);

            // Async enrich from staff_profiles + shift assignment
            new Thread(() -> {
                try (var conn = ma.ensa.khouribga.smartstay.db.Database.getConnection()) {
                    String sql = """
                        SELECT sp.position, sp.department, sp.employee_code,
                               sp.hire_date, sp.salary_base,
                               s.shift_name, s.start_time, s.end_time
                        FROM staff_profiles sp
                        LEFT JOIN staff_shift_assignments ssa
                               ON ssa.staff_profile_id = sp.id
                               AND ssa.assigned_date = CURDATE()
                        LEFT JOIN shifts s ON s.id = ssa.shift_id
                        WHERE sp.user_id = ?
                        LIMIT 1
                    """;
                    try (var ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, user.getId());
                        try (var rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String pos      = rs.getString("position");
                                String dept     = rs.getString("department");
                                String code     = rs.getString("employee_code");
                                String hire     = rs.getString("hire_date");
                                double salary   = rs.getDouble("salary_base");
                                String sName    = rs.getString("shift_name");
                                String sStart   = rs.getString("start_time");
                                String sEnd     = rs.getString("end_time");
                                Platform.runLater(() -> {
                                    lblPosition.setText(pos  != null ? pos  : "No position");
                                    lblDept    .setText(dept != null ? dept : "No department");
                                    lblEmpCode .setText(code != null ? code : "N/A");
                                    lblHire    .setText("Hired: " + (hire != null ? hire : "unknown"));
                                    lblSalary  .setText(String.format("%.2f MAD / month", salary));
                                    lblShift   .setText(sName != null
                                        ? sName + "  (" + sStart.substring(0,5) + " – " + sEnd.substring(0,5) + ")"
                                        : "No shift today");
                                });
                            } else {
                                Platform.runLater(() -> {
                                    lblPosition.setText("No staff profile");
                                    lblDept.setText("—"); lblEmpCode.setText("—");
                                    lblHire.setText("—"); lblSalary.setText("—");
                                    lblShift.setText("—");
                                });
                            }
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        }
    }

    /** Icon + text row helper */
    private HBox detailRow(String icon, String text, String color) {
        Label ico  = new Label(icon); ico.setStyle("-fx-font-size:13px; -fx-min-width:20;");
        Label lbl  = new Label(text); lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + color + ";");
        HBox row   = new HBox(8, ico, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Standalone label used when the text is set async */
    private Label detailLabel(String id, String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:" + color + ";");
        return l;
    }

    @FXML public void activateStaff() { if (selectedStaff == null) { showAlert("Select a staff card first."); return; } if (selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already active."); return; } new Thread(() -> { try { UserDao.setActive(selectedStaff.getId(), true); Platform.runLater(this::loadAllStaff); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }
    @FXML public void deactivateStaff() { if (selectedStaff == null) { showAlert("Select a staff card first."); return; } if (!selectedStaff.isActive()) { showAlert(selectedStaff.getUsername() + " is already inactive."); return; } new Thread(() -> { try { UserDao.setActive(selectedStaff.getId(), false); Platform.runLater(this::loadAllStaff); } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); } }).start(); }

    @FXML public void assignShift() {
        if (selectedStaff == null) { showAlert("Select a staff card first."); return; }
        String shiftEntry = shiftPicker != null ? shiftPicker.getValue() : null;
        if (shiftEntry == null || shiftEntry.isBlank()) { showAlert("Please select a shift to assign."); return; }
        LocalDate date = shiftDatePicker != null ? shiftDatePicker.getValue() : LocalDate.now();
        if (date == null) { showAlert("Please select a date for the shift."); return; }
        // Extract just the shift_name part (before the "  (" time display)
        String shiftName = shiftEntry.contains("  (") ? shiftEntry.substring(0, shiftEntry.indexOf("  (")).trim() : shiftEntry.trim();
        String notes = shiftNotes != null ? shiftNotes.getText().trim() : "";
        long adminId = SessionManager.getCurrentUser().getId();
        final long staffUserId = selectedStaff.getId();
        final LocalDate assignDate = date;
        new Thread(() -> {
            try (var conn = ma.ensa.khouribga.smartstay.db.Database.getConnection()) {
                // Resolve staff_profile_id from user id
                long staffProfileId = -1;
                try (var ps = conn.prepareStatement("SELECT id FROM staff_profiles WHERE user_id = ?")) {
                    ps.setLong(1, staffUserId);
                    try (var rs = ps.executeQuery()) { if (rs.next()) staffProfileId = rs.getLong("id"); }
                }
                if (staffProfileId < 0) { Platform.runLater(() -> showAlert("No staff profile found for this user.")); return; }
                // Resolve shift_id by name
                long shiftId = -1;
                try (var ps = conn.prepareStatement("SELECT id FROM shifts WHERE shift_name = ?")) {
                    ps.setString(1, shiftName);
                    try (var rs = ps.executeQuery()) { if (rs.next()) shiftId = rs.getLong("id"); }
                }
                if (shiftId < 0) { Platform.runLater(() -> showAlert("Shift not found in database.")); return; }
                // Insert assignment (replace existing on same date for this staff)
                String sql = """
                    INSERT INTO staff_shift_assignments (staff_profile_id, shift_id, assigned_date, assigned_by_user_id, notes)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE shift_id = VALUES(shift_id), assigned_by_user_id = VALUES(assigned_by_user_id), notes = VALUES(notes)
                    """;
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, staffProfileId);
                    ps.setLong(2, shiftId);
                    ps.setDate(3, java.sql.Date.valueOf(assignDate));
                    ps.setLong(4, adminId);
                    ps.setString(5, notes.isEmpty() ? null : notes);
                    ps.executeUpdate();
                }
                final String msg = "Shift '" + shiftName + "' assigned to " + selectedStaff.getUsername() + " on " + assignDate;
                Platform.runLater(() -> {
                    if (shiftNotes != null) shiftNotes.clear();
                    showAlert(msg);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert("Error assigning shift: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Revenue Chart ────────────────────────────────────────────────────────
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
        String sql = "SELECT MONTH(r.check_out_date) AS month, SUM(DATEDIFF(r.check_out_date, r.check_in_date) * rt.price_per_night) AS total FROM reservations r JOIN rooms rm ON r.room_id = rm.id JOIN room_types rt ON rm.room_type_id = rt.id WHERE YEAR(r.check_out_date) = ? GROUP BY MONTH(r.check_out_date) ORDER BY month";
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
            for (var d : series.getData()) { if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #c5a059; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(255,255,255,0.15);"); }
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