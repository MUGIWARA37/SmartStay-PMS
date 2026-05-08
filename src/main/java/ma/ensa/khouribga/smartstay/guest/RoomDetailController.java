package ma.ensa.khouribga.smartstay.guest;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.model.Room;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RoomDetailController {

    @FXML private Label lblRoomNumber;
    @FXML private Label lblRoomType;
    @FXML private Label lblFloor;
    @FXML private Label lblMaxOccupancy;
    @FXML private Label lblPrice;
    @FXML private Label lblAmenities;
    @FXML private TextArea taDescription;

    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Label lblNights;
    @FXML private Label lblSubtotal;
    @FXML private Label lblError;

    @FXML private Button btnBook;
    @FXML private Button btnCancel;

    private Room room;

    public void initData(Room room, LocalDate checkIn, LocalDate checkOut) {
        this.room = room;

        lblRoomNumber.setText("Room " + room.getRoomNumber());
        lblRoomType.setText(room.getTypeName());
        lblFloor.setText("Floor " + room.getFloor());
        lblMaxOccupancy.setText("Up to " + room.getMaxOccupancy() + " guests");
        lblPrice.setText(String.format("%.2f MAD / night", room.getPricePerNight()));

        String amenities = room.getAmenities() != null ? room.getAmenities() : "Standard amenities included";
        lblAmenities.setText(amenities);

        if (room.getNotes() != null && !room.getNotes().isBlank()) {
            taDescription.setText(room.getNotes());
        } else {
            taDescription.setText("A comfortable " + room.getTypeName().toLowerCase()
                    + " on floor " + room.getFloor() + ".");
        }

        dpCheckIn.setValue(checkIn != null ? checkIn : LocalDate.now());
        dpCheckOut.setValue(checkOut != null ? checkOut : LocalDate.now().plusDays(1));

        refreshNightsLabel();
        dpCheckIn.valueProperty().addListener((obs, o, n) -> refreshNightsLabel());
        dpCheckOut.valueProperty().addListener((obs, o, n) -> refreshNightsLabel());
    }

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
        double subtotal = room.getPricePerNight() * nights;
        lblNights.setText(nights + " night(s)");
        lblSubtotal.setText(String.format("%.2f MAD", subtotal));
    }

    private boolean validateDates() {
        LocalDate ci = dpCheckIn.getValue();
        LocalDate co = dpCheckOut.getValue();
        if (ci == null || co == null) { lblError.setText("Please select both dates."); return false; }
        if (!ci.isBefore(co)) { lblError.setText("Check-out must be after check-in."); return false; }
        if (ci.isBefore(LocalDate.now())) { lblError.setText("Check-in cannot be in the past."); return false; }
        return true;
    }

    @FXML
    private void handleBook() {
        if (!validateDates()) return;
        btnBook.setDisable(true);
        btnBook.setText("Processing…");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/guest/payment.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            PaymentController ctrl = loader.getController();
            ctrl.initData(room, dpCheckIn.getValue(), dpCheckOut.getValue());

            Stage stage = new Stage();
            stage.setTitle("Confirm & Pay — Room " + room.getRoomNumber());
            stage.initOwner(btnBook.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.showAndWait();

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
