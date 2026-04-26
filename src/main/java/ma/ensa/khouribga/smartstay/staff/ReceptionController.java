package ma.ensa.khouribga.smartstay.staff;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.CleaningDao;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.model.*;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceptionController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<Reservation> resTable;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colGuest;
    @FXML private TableColumn<Reservation, String> colRoom;
    @FXML private TableColumn<Reservation, LocalDate> colIn;
    @FXML private TableColumn<Reservation, LocalDate> colOut;
    @FXML private TableColumn<Reservation, Reservation.Status> colStatus;
    @FXML private TextField walkFirstName;
    @FXML private TextField walkLastName;
    @FXML private TextField walkPassport;
    @FXML private TextField walkEmail;
    @FXML private ComboBox<Room> walkRoomCombo;
    @FXML private DatePicker walkCheckIn;
    @FXML private DatePicker walkCheckOut;
    @FXML private Label walkError;
    @FXML private ComboBox<Room> cleanRoomCombo;
    @FXML private ComboBox<CleaningRequest.Priority> cleanPriority;
    @FXML private TextArea cleanNote;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        try { SessionManager.requireRole(User.Role.STAFF); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        welcomeLabel.setText(SessionManager.getCurrentUser().getUsername());

        colCode.setCellValueFactory(new PropertyValueFactory<>("reservationCode"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestFullName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colIn.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        colOut.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });

        walkCheckIn.setValue(LocalDate.now());
        walkCheckOut.setValue(LocalDate.now().plusDays(1));

        applyRoomCellFactory(walkRoomCombo);
        applyRoomCellFactory(cleanRoomCombo);

        cleanPriority.setItems(FXCollections.observableArrayList(CleaningRequest.Priority.values()));
        cleanPriority.setValue(CleaningRequest.Priority.MEDIUM);

        loadRooms();
        loadReservations();
    }

    private void applyRoomCellFactory(ComboBox<Room> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : "Room " + r.getRoomNumber() + " – " + r.getTypeName());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "Select room" : "Room " + r.getRoomNumber());
            }
        });
    }

    private void loadRooms() {
        new Thread(() -> {
            try {
                List<Room> available = RoomDao.findAvailable();
                List<Room> all       = RoomDao.findAll();
                Platform.runLater(() -> {
                    walkRoomCombo.setItems(FXCollections.observableArrayList(available));
                    cleanRoomCombo.setItems(FXCollections.observableArrayList(all));
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "reception-rooms").start();
    }

    @FXML public void loadReservations() {
        new Thread(() -> {
            try {
                List<Reservation> active = ReservationDao.findActive();
                Platform.runLater(() -> resTable.setItems(FXCollections.observableArrayList(active)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "reception-reservations").start();
    }

    @FXML public void doCheckIn() {
        Reservation sel = resTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a reservation to check in."); return; }
        if (sel.getStatus() == Reservation.Status.CHECKED_IN || sel.getStatus() == Reservation.Status.CHECKED_OUT) {
            showAlert("Reservation is already " + sel.getStatus()); return;
        }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(sel.getId(), sel.getRoomId(), Reservation.Status.CHECKED_IN);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "reception-checkin").start();
    }

    @FXML public void doCheckOut() {
        Reservation sel = resTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a reservation to check out."); return; }
        if (sel.getStatus() != Reservation.Status.CHECKED_IN) { showAlert("Only CHECKED_IN reservations can be checked out."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(sel.getId(), sel.getRoomId(), Reservation.Status.CHECKED_OUT);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "reception-checkout").start();
    }

    @FXML public void doCancel() {
        Reservation sel = resTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a reservation to cancel."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(sel.getId(), sel.getRoomId(), Reservation.Status.CANCELLED);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert("Error: " + ex.getMessage())); }
        }, "reception-cancel").start();
    }

    @FXML public void doWalkIn() {
        walkError.setText("");
        String firstName = walkFirstName.getText().trim();
        String lastName  = walkLastName.getText().trim();
        String passport  = walkPassport.getText().trim();
        String email     = walkEmail.getText().trim();
        Room   room      = walkRoomCombo.getValue();
        LocalDate checkIn  = walkCheckIn.getValue();
        LocalDate checkOut = walkCheckOut.getValue();

        if (firstName.isEmpty() || lastName.isEmpty()) { walkError.setText("Enter guest name."); return; }
        if (passport.isEmpty())   { walkError.setText("Enter passport / ID."); return; }
        if (email.isEmpty())      { walkError.setText("Enter email."); return; }
        if (room == null)         { walkError.setText("Select a room."); return; }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            walkError.setText("Select valid check-in / check-out dates."); return;
        }

        final String fn = firstName, ln = lastName, pp = passport, em = email;
        new Thread(() -> {
            try {
                Guest guest = GuestDao.findByPassport(pp).orElseGet(() -> {
                    Guest g = new Guest(); g.setFirstName(fn); g.setLastName(ln);
                    g.setEmail(em); g.setIdPassportNumber(pp); return g;
                });
                if (guest.getId() == 0) guest.setId(GuestDao.create(guest));

                Reservation res = new Reservation();
                res.setGuestId(guest.getId()); res.setRoomId(room.getId());
                res.setBookedByUserId((int) SessionManager.getCurrentUser().getId());
                res.setCheckInDate(checkIn); res.setCheckOutDate(checkOut);
                res.setAdultsCount(1); res.setChildrenCount(0);
                ReservationDao.create(res);
                ReservationDao.updateStatus(res.getId(), room.getId(), Reservation.Status.CHECKED_IN);

                Platform.runLater(() -> {
                    walkFirstName.clear(); walkLastName.clear(); walkPassport.clear(); walkEmail.clear();
                    walkRoomCombo.setValue(null);
                    loadReservations(); loadRooms();
                    showAlert("Walk-in created and room checked in.");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> walkError.setText("Error: " + ex.getMessage()));
            }
        }, "reception-walkin").start();
    }

    @FXML public void doCleaningRequest() {
        Room room = cleanRoomCombo.getValue();
        if (room == null) { showAlert("Select a room."); return; }
        CleaningRequest req = new CleaningRequest();
        req.setRoomId(room.getId());
        req.setRequestedByUserId((int) SessionManager.getCurrentUser().getId());
        req.setPriority(cleanPriority.getValue() != null ? cleanPriority.getValue() : CleaningRequest.Priority.MEDIUM);
        req.setRequestNote(cleanNote.getText().trim());
        new Thread(() -> {
            try {
                CleaningDao.create(req);
                Platform.runLater(() -> { cleanRoomCombo.setValue(null); cleanNote.clear(); showAlert("Cleaning request submitted."); });
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Error: " + ex.getMessage()));
            }
        }, "reception-cleaning").start();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }

    @FXML public void onLogout() { SessionManager.logout(); Navigator.goToLogin(welcomeLabel); }
}