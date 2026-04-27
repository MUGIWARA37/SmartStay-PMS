package ma.ensa.khouribga.smartstay.guest;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.InvoiceDao;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.model.*;
import ma.ensa.khouribga.smartstay.model.Invoice.InvoiceLine;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Modal dialog: show room details → pick dates → proceed to payment.
 *
 * Flow:
 *  1. initData() called by HomeController before showAndWait()
 *  2. User reviews amenities, adjusts check-in / check-out
 *  3. "Proceed to Payment" validates dates → opens PaymentController dialog
 */
public class RoomDetailController {

    // ── Room info ─────────────────────────────────────────────────────────────
    @FXML private Label lblRoomNumber;
    @FXML private Label lblRoomType;
    @FXML private Label lblFloor;
    @FXML private Label lblMaxOccupancy;
    @FXML private Label lblPrice;
    @FXML private Label lblAmenities;
    @FXML private TextArea taDescription;

    // ── Date selection ────────────────────────────────────────────────────────
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Label lblNights;
    @FXML private Label lblSubtotal;
    @FXML private Label lblError;

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML private Button btnBook;
    @FXML private Button btnCancel;

    // ── State ─────────────────────────────────────────────────────────────────
    private Room room;

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Called by HomeController immediately after FXML load.
     *
     * @param room      the room to display
     * @param checkIn   pre-filled check-in date (may be null)
     * @param checkOut  pre-filled check-out date (may be null)
     */
    public void initData(Room room, LocalDate checkIn, LocalDate checkOut) {
        this.room = room;

        lblRoomNumber.setText("Room " + room.getRoomNumber());
        lblRoomType.setText(room.getRoomTypeName());
        lblFloor.setText("Floor " + room.getFloor());
        lblMaxOccupancy.setText("Up to " + room.getMaxOccupancy() + " guests");
        lblPrice.setText(room.getPricePerNight().toPlainString() + " MAD / night");

        String amenities = room.getAmenities() != null ? room.getAmenities() : "Standard amenities included";
        lblAmenities.setText(amenities);

        if (room.getNotes() != null && !room.getNotes().isBlank()) {
            taDescription.setText(room.getNotes());
        } else {
            taDescription.setText("A comfortable " + room.getRoomTypeName().toLowerCase()
                    + " on floor " + room.getFloor() + ".");
        }

        // Pre-fill dates if passed from the filter bar
        if (checkIn != null) dpCheckIn.setValue(checkIn);
        else dpCheckIn.setValue(LocalDate.now());

        if (checkOut != null) dpCheckOut.setValue(checkOut);
        else dpCheckOut.setValue(LocalDate.now().plusDays(1));

        refreshNightsLabel();

        // Recalculate on date change
        dpCheckIn.valueProperty().addListener((obs, o, n) -> refreshNightsLabel());
        dpCheckOut.valueProperty().addListener((obs, o, n) -> refreshNightsLabel());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshNightsLabel() {
        LocalDate ci = dpCheckIn.getValue();
        LocalDate co = dpCheckOut.getValue();
        lblError.setText("");
        if (ci == null || co == null || !co.isAfter(ci)) {
            lblNights.setText("—");
            lblSubtotal.setText("—");
            return;
        }
        long nights = ChronoUnit.DAYS.between(ci, co);
        BigDecimal subtotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        lblNights.setText(nights + " night(s)");
        lblSubtotal.setText(subtotal.toPlainString() + " MAD");
    }

    private boolean validateDates() {
        LocalDate ci = dpCheckIn.getValue();
        LocalDate co = dpCheckOut.getValue();
        if (ci == null || co == null) {
            lblError.setText("Please select both dates.");
            return false;
        }
        if (!ci.isBefore(co)) {
            lblError.setText("Check-out must be after check-in.");
            return false;
        }
        if (ci.isBefore(LocalDate.now())) {
            lblError.setText("Check-in cannot be in the past.");
            return false;
        }
        return true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void handleBook() {
        if (!validateDates()) return;

        btnBook.setDisable(true);
        btnBook.setText("Processing…");

        LocalDate ci = dpCheckIn.getValue();
        LocalDate co = dpCheckOut.getValue();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/guest/payment.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/samurai.css").toExternalForm());

            PaymentController ctrl = loader.getController();
            ctrl.initData(room, ci, co);

            Stage stage = new Stage();
            stage.setTitle("Confirm & Pay — Room " + room.getRoomNumber());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.showAndWait();

            // Close this dialog too — booking is done
            if (ctrl.isBookingConfirmed()) {
                ((Stage) btnBook.getScene().getWindow()).close();
            }

        } catch (IOException ex) {
            lblError.setText("Could not open payment screen: " + ex.getMessage());
        } finally {
            btnBook.setDisable(false);
            btnBook.setText("Proceed to Payment");
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}