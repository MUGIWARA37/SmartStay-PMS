package ma.ensa.khouribga.smartstay.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.*;
import ma.ensa.khouribga.smartstay.model.*;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {

    @FXML private Label welcomeLabel;

    // ── Overview tab ─────────────────────────────────────────────────────────
    @FXML private Label statAvailable;
    @FXML private Label statOccupied;
    @FXML private Label statCleaning;
    @FXML private Label statMaint;
    @FXML private Label statActiveRes;

    // ── Rooms tab ─────────────────────────────────────────────────────────────
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, String> colRoomNum;
    @FXML private TableColumn<Room, String> colRoomType;
    @FXML private TableColumn<Room, Integer> colRoomFloor;
    @FXML private TableColumn<Room, Room.Status> colRoomStatus;
    @FXML private TableColumn<Room, String> colRoomNotes;
    @FXML private ComboBox<Room.Status> roomStatusFilter;
    @FXML private ComboBox<Room.Status> roomStatusEdit;

    // ── Reservations tab ──────────────────────────────────────────────────────
    @FXML private TableView<Reservation> resTable;
    @FXML private TableColumn<Reservation, String> colResCode;
    @FXML private TableColumn<Reservation, String> colResGuest;
    @FXML private TableColumn<Reservation, String> colResRoom;
    @FXML private TableColumn<Reservation, LocalDate> colResIn;
    @FXML private TableColumn<Reservation, LocalDate> colResOut;
    @FXML private TableColumn<Reservation, Reservation.Status> colResStatus;
    @FXML private ComboBox<String> resStatusFilter;

    // ── Payroll tab ───────────────────────────────────────────────────────────
    @FXML private TableView<Payroll> payrollTable;
    @FXML private TableColumn<Payroll, String> colPayStaff;
    @FXML private TableColumn<Payroll, String> colPayPos;
    @FXML private TableColumn<Payroll, LocalDate> colPayStart;
    @FXML private TableColumn<Payroll, LocalDate> colPayEnd;
    @FXML private TableColumn<Payroll, Double> colPayNet;
    @FXML private TableColumn<Payroll, Payroll.Status> colPayStatus;
    @FXML private DatePicker payPeriodStart;
    @FXML private DatePicker payPeriodEnd;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        try { SessionManager.requireRole(User.Role.ADMIN); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        welcomeLabel.setText("Admin: " + SessionManager.getCurrentUser().getUsername());

        setupRoomsTab();
        setupReservationsTab();
        setupPayrollTab();
        loadOverview();
    }

    // ── Overview ──────────────────────────────────────────────────────────────

    @FXML public void refreshOverview() { loadOverview(); }

    private void loadOverview() {
        new Thread(() -> {
            try {
                int[] counts = RoomDao.countByStatus();
                // counts indexed by Room.Status ordinal: AVAILABLE=0, OCCUPIED=1, MAINTENANCE=2, CLEANING=3
                List<Reservation> active = ReservationDao.findActive();
                Platform.runLater(() -> {
                    if (statAvailable != null) statAvailable.setText(String.valueOf(counts[Room.Status.AVAILABLE.ordinal()]));
                    if (statOccupied  != null) statOccupied.setText(String.valueOf(counts[Room.Status.OCCUPIED.ordinal()]));
                    if (statCleaning  != null) statCleaning.setText(String.valueOf(counts[Room.Status.CLEANING.ordinal()]));
                    if (statMaint     != null) statMaint.setText(String.valueOf(counts[Room.Status.MAINTENANCE.ordinal()]));
                    if (statActiveRes != null) statActiveRes.setText(String.valueOf(active.size()));
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-overview").start();
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    private void setupRoomsTab() {
        if (colRoomNum == null) return;
        colRoomNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("typeName"));
        colRoomFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colRoomStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colRoomNotes != null) colRoomNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        if (roomStatusFilter != null) {
            roomStatusFilter.setItems(FXCollections.observableArrayList(Room.Status.values()));
        }
        if (roomStatusEdit != null) {
            roomStatusEdit.setItems(FXCollections.observableArrayList(Room.Status.values()));
        }
        loadAllRooms();
    }

    @FXML public void loadAllRooms() {
        new Thread(() -> {
            try {
                List<Room> rooms = RoomDao.findAll();
                Platform.runLater(() -> { if (roomTable != null) roomTable.setItems(FXCollections.observableArrayList(rooms)); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-rooms").start();
    }

    @FXML public void filterRooms() {
        if (roomStatusFilter == null || roomStatusFilter.getValue() == null) { loadAllRooms(); return; }
        Room.Status filter = roomStatusFilter.getValue();
        new Thread(() -> {
            try {
                List<Room> rooms = RoomDao.findByStatus(filter);
                Platform.runLater(() -> { if (roomTable != null) roomTable.setItems(FXCollections.observableArrayList(rooms)); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-room-filter").start();
    }

    @FXML public void updateRoomStatus() {
        if (roomTable == null || roomStatusEdit == null) return;
        Room sel = roomTable.getSelectionModel().getSelectedItem();
        Room.Status newStatus = roomStatusEdit.getValue();
        if (sel == null || newStatus == null) { showAlert("Select a room and a new status."); return; }
        new Thread(() -> {
            try {
                RoomDao.updateStatus(sel.getId(), newStatus);
                Platform.runLater(this::loadAllRooms);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Update failed: " + ex.getMessage())); }
        }, "admin-room-update").start();
    }

    // ── Reservations ──────────────────────────────────────────────────────────

    private void setupReservationsTab() {
        if (colResCode == null) return;
        colResCode.setCellValueFactory(new PropertyValueFactory<>("reservationCode"));
        colResGuest.setCellValueFactory(new PropertyValueFactory<>("guestFullName"));
        colResRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colResIn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        colResOut.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        colResStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colResIn.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        colResOut.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });

        if (resStatusFilter != null) {
            resStatusFilter.setItems(FXCollections.observableArrayList(
                    "ALL", "PENDING", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"));
            resStatusFilter.setValue("ALL");
        }
        loadAllReservations();
    }

    @FXML public void loadAllReservations() {
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findAll();
                Platform.runLater(() -> { if (resTable != null) resTable.setItems(FXCollections.observableArrayList(list)); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-res").start();
    }

    @FXML public void filterReservations() {
        if (resStatusFilter == null || resStatusFilter.getValue() == null || resStatusFilter.getValue().equals("ALL")) {
            loadAllReservations(); return;
        }
        Reservation.Status filter = Reservation.Status.valueOf(resStatusFilter.getValue());
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findByStatus(filter);
                Platform.runLater(() -> { if (resTable != null) resTable.setItems(FXCollections.observableArrayList(list)); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-res-filter").start();
    }

    @FXML public void doCheckIn() {
        if (resTable == null) return;
        Reservation sel = resTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a reservation."); return; }
        new Thread(() -> {
            try { ReservationDao.updateStatus(sel.getId(), sel.getRoomId(), Reservation.Status.CHECKED_IN);
                Platform.runLater(this::loadAllReservations); }
            catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "admin-checkin").start();
    }

    @FXML public void doCheckOut() {
        if (resTable == null) return;
        Reservation sel = resTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a reservation."); return; }
        new Thread(() -> {
            try { ReservationDao.updateStatus(sel.getId(), sel.getRoomId(), Reservation.Status.CHECKED_OUT);
                Platform.runLater(this::loadAllReservations); }
            catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "admin-checkout").start();
    }

    // ── Payroll ───────────────────────────────────────────────────────────────

    private void setupPayrollTab() {
        if (colPayStaff == null) return;
        colPayStaff.setCellValueFactory(new PropertyValueFactory<>("staffUsername"));
        colPayPos.setCellValueFactory(new PropertyValueFactory<>("staffPosition"));
        colPayStart.setCellValueFactory(new PropertyValueFactory<>("periodStart"));
        colPayEnd.setCellValueFactory(new PropertyValueFactory<>("periodEnd"));
        colPayNet.setCellValueFactory(new PropertyValueFactory<>("netSalary"));
        colPayStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPayNet.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f MAD", v));
            }
        });

        if (payPeriodStart != null) payPeriodStart.setValue(LocalDate.now().withDayOfMonth(1));
        if (payPeriodEnd   != null) payPeriodEnd.setValue(LocalDate.now());
        loadAllPayroll();
    }

    @FXML public void loadAllPayroll() {
        new Thread(() -> {
            try {
                List<Payroll> list = PayrollDao.findAll();
                Platform.runLater(() -> { if (payrollTable != null) payrollTable.setItems(FXCollections.observableArrayList(list)); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "admin-payroll").start();
    }

    @FXML public void generatePayroll() {
        if (payPeriodStart == null || payPeriodEnd == null) return;
        LocalDate start = payPeriodStart.getValue();
        LocalDate end   = payPeriodEnd.getValue();
        if (start == null || end == null || !end.isAfter(start)) {
            showAlert("Select a valid payroll period."); return;
        }
        new Thread(() -> {
            try {
                PayrollDao.generateForPeriod(start, end);
                Platform.runLater(() -> { loadAllPayroll(); showAlert("Payroll generated for all staff."); });
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Error: " + ex.getMessage()));
            }
        }, "admin-payroll-gen").start();
    }

    @FXML public void markPayrollPaid() {
        if (payrollTable == null) return;
        Payroll sel = payrollTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a payroll record."); return; }
        new Thread(() -> {
            try {
                PayrollDao.markPaid(sel.getId());
                Platform.runLater(this::loadAllPayroll);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "admin-payroll-paid").start();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        Navigator.goToLogin(welcomeLabel);
    }
}