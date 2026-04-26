package ma.ensa.khouribga.smartstay.guest;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.model.Guest;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RoomDetailController {

    // Room info
    @FXML private Label welcomeLabel;
    @FXML private Label roomNumberLabel;
    @FXML private Label roomTypeLabel;
    @FXML private Label roomFloorLabel;
    @FXML private Label roomMaxLabel;
    @FXML private Label roomPriceLabel;
    @FXML private Label amenitiesLabel;
    @FXML private Label descLabel;

    // Booking form
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> adultsSpinner;
    @FXML private Spinner<Integer> childrenSpinner;
    @FXML private Label totalLabel;

    // Guest form
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField passportField;
    @FXML private TextField nationalityField;
    @FXML private TextArea  specialRequests;

    @FXML private Label errorLabel;

    private Room room;
    private Reservation pendingReservation;

    // ── Called by HomeController before showing this screen ──────────────────
    public void setRoom(Room room) {
        this.room = room;
        roomNumberLabel.setText("Room " + room.getRoomNumber());
        roomTypeLabel.setText(room.getTypeName());
        roomFloorLabel.setText(String.valueOf(room.getFloor()));
        roomMaxLabel.setText(room.getMaxOccupancy() + " guests");
        roomPriceLabel.setText(String.format("%.2f MAD", room.getPricePerNight()));
        amenitiesLabel.setText(room.getAmenities() == null ? "—" : room.getAmenities());
        descLabel.setText(room.getTypeDescription() == null ? "—" : room.getTypeDescription());
    }

    @FXML
    public void initialize() {
        try { SessionManager.requireLoggedIn(); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        welcomeLabel.setText(SessionManager.getCurrentUser().getUsername());

        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));

        // Set default dates
        checkInPicker.setValue(LocalDate.now().plusDays(1));
        checkOutPicker.setValue(LocalDate.now().plusDays(3));
        updateTotal();

        checkInPicker.valueProperty().addListener((o, ov, nv) -> updateTotal());
        checkOutPicker.valueProperty().addListener((o, ov, nv) -> updateTotal());
    }

    private void updateTotal() {
        if (room == null || checkInPicker.getValue() == null || checkOutPicker.getValue() == null) return;
        long nights = ChronoUnit.DAYS.between(checkInPicker.getValue(), checkOutPicker.getValue());
        if (nights <= 0) { totalLabel.setText("—"); return; }
        double total = nights * room.getPricePerNight();
        totalLabel.setText(String.format("%.2f MAD", total));
    }

    @FXML
    public void onBack() {
        Navigator.navigateTo(welcomeLabel, Navigator.HOME);
    }

    @FXML
    public void onBookNow() {
        errorLabel.setText("");
        if (room == null) return;

        // Validate dates
        LocalDate checkIn  = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn == null || checkOut == null) { showError("Please select check-in and check-out dates."); return; }
        if (!checkOut.isAfter(checkIn))          { showError("Check-out must be after check-in."); return; }
        if (checkIn.isBefore(LocalDate.now()))   { showError("Check-in cannot be in the past."); return; }

        // Validate guest form
        String firstName  = firstNameField.getText().trim();
        String lastName   = lastNameField.getText().trim();
        String passport   = passportField.getText().trim();
        String email      = emailField.getText().trim();
        if (firstName.isEmpty() || lastName.isEmpty()) { showError("Please enter guest first and last name."); return; }
        if (passport.isEmpty())                        { showError("Please enter passport / ID number."); return; }
        if (email.isEmpty())                           { showError("Please enter a valid email."); return; }

        new Thread(() -> {
            try {
                // Look up or create guest
                Guest guest = GuestDao.findByPassport(passport).orElseGet(() -> {
                    Guest g = new Guest();
                    g.setFirstName(firstName);
                    g.setLastName(lastName);
                    g.setEmail(email);
                    g.setPhone(phoneField.getText().trim());
                    g.setNationality(nationalityField.getText().trim());
                    g.setIdPassportNumber(passport);
                    return g;
                });

                if (guest.getId() == 0) {
                    // New guest — insert
                    int guestId = GuestDao.create(guest);
                    guest.setId(guestId);
                } else {
                    // Existing guest — update fields
                    guest.setFirstName(firstName);
                    guest.setLastName(lastName);
                    guest.setEmail(email);
                    guest.setPhone(phoneField.getText().trim());
                    GuestDao.update(guest);
                }

                // Create reservation
                Reservation res = new Reservation();
                res.setGuestId(guest.getId());
                res.setRoomId(room.getId());
                res.setBookedByUserId((int) SessionManager.getCurrentUser().getId());
                res.setCheckInDate(checkIn);
                res.setCheckOutDate(checkOut);
                res.setAdultsCount(adultsSpinner.getValue());
                res.setChildrenCount(childrenSpinner.getValue());

                int reservationId = ReservationDao.create(res);

                // Populate display fields for payment screen
                res.setRoomNumber(room.getRoomNumber());
                res.setRoomTypeName(room.getTypeName());
                res.setPricePerNight(room.getPricePerNight());
                res.setGuestFullName(guest.getFullName());

                final Reservation finalRes = res;
                javafx.application.Platform.runLater(() ->
                    Navigator.navigateTo(welcomeLabel, Navigator.PAYMENT,
                        ctrl -> ((PaymentController) ctrl).setReservation(finalRes)));

            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> showError("Error creating reservation: " + ex.getMessage()));
            }
        }, "booking-thread").start();
    }

    private void showError(String msg) { errorLabel.setText(msg); }
}