package ma.ensa.khouribga.smartstay.guest;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.InvoiceDao;
import ma.ensa.khouribga.smartstay.dao.ServiceDao;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Service;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentController {

    // Summary labels
    @FXML private Label summaryRoom;
    @FXML private Label summaryType;
    @FXML private Label summaryCheckIn;
    @FXML private Label summaryCheckOut;
    @FXML private Label summaryNights;
    @FXML private Label summaryGuest;
    @FXML private Label summaryRoomTotal;
    @FXML private Label summaryServicesTotal;
    @FXML private Label summaryTax;
    @FXML private Label summaryTotal;

    // Payment method
    @FXML private Button btnCash;
    @FXML private Button btnCard;
    @FXML private Button btnTransfer;
    @FXML private VBox   cardForm;
    @FXML private TextField transactionRef;

    // Services
    @FXML private VBox servicesBox;

    @FXML private Label errorLabel;

    private Reservation reservation;
    private String selectedMethod = "CASH";
    private final List<Service> services = new ArrayList<>();
    private final List<Spinner<Integer>> serviceSpinners = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Called by RoomDetailController ───────────────────────────────────────
    public void setReservation(Reservation res) {
        this.reservation = res;
        populateSummary();
        loadServices();
    }

    @FXML
    public void initialize() {
        try { SessionManager.requireLoggedIn(); }
        catch (Exception e) { Navigator.goToLogin(summaryRoom); return; }
        cardForm.setVisible(false);
        cardForm.setManaged(false);
    }

    private void populateSummary() {
        if (reservation == null) return;
        summaryRoom.setText("Room " + reservation.getRoomNumber());
        summaryType.setText(reservation.getRoomTypeName());
        summaryCheckIn.setText(reservation.getCheckInDate() != null
                ? reservation.getCheckInDate().format(DATE_FMT) : "—");
        summaryCheckOut.setText(reservation.getCheckOutDate() != null
                ? reservation.getCheckOutDate().format(DATE_FMT) : "—");
        summaryNights.setText(reservation.getNights() + " nights");
        summaryGuest.setText(reservation.getGuestFullName() != null
                ? reservation.getGuestFullName() : "—");
        updateTotals();
    }

    private void loadServices() {
        new Thread(() -> {
            try {
                List<Service> loaded = ServiceDao.findAllActive();
                services.clear();
                services.addAll(loaded);
                Platform.runLater(this::populateServicesUI);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "payment-services-loader").start();
    }

    private void populateServicesUI() {
        servicesBox.getChildren().clear();
        serviceSpinners.clear();
        for (Service s : services) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            VBox.setMargin(row, new Insets(2, 0, 2, 0));

            CheckBox cb = new CheckBox();
            cb.setSelected(false);
            VBox info = new VBox(2);
            Label name = new Label(s.getName());
            name.getStyleClass().add("form-label");
            Label desc = new Label(String.format("%.2f MAD / unit", s.getUnitPrice()));
            desc.getStyleClass().add("label-muted");
            info.getChildren().addAll(name, desc);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            Spinner<Integer> qty = new Spinner<>(0, 20, 0);
            qty.setEditable(true);
            qty.setPrefWidth(70);
            qty.getStyleClass().add("spinner");
            qty.setDisable(true);

            cb.selectedProperty().addListener((o, ov, sel) -> {
                qty.setDisable(!sel);
                if (!sel) { qty.getValueFactory().setValue(0); s.setQuantity(0); }
                else if (qty.getValue() == 0) { qty.getValueFactory().setValue(1); s.setQuantity(1); }
                updateTotals();
            });
            qty.valueProperty().addListener((o, ov, nv) -> { s.setQuantity(nv); updateTotals(); });

            serviceSpinners.add(qty);
            row.getChildren().addAll(cb, info, qty);
            servicesBox.getChildren().add(row);
        }
    }

    private void updateTotals() {
        if (reservation == null) return;
        double roomSubtotal     = reservation.getBaseTotal();
        double servicesSubtotal = services.stream().mapToDouble(Service::getLineTotal).sum();
        double subtotal         = roomSubtotal + servicesSubtotal;
        double tax              = subtotal * 0.10;
        double total            = subtotal + tax;

        summaryRoomTotal.setText(String.format("%.2f MAD", roomSubtotal));
        summaryServicesTotal.setText(String.format("%.2f MAD", servicesSubtotal));
        summaryTax.setText(String.format("%.2f MAD", tax));
        summaryTotal.setText(String.format("%.2f MAD", total));
    }

    // ── Payment method selection ──────────────────────────────────────────────

    @FXML public void selectCash()     { setMethod("CASH");     }
    @FXML public void selectCard()     { setMethod("CARD");     }
    @FXML public void selectTransfer() { setMethod("TRANSFER"); }

    private void setMethod(String method) {
        selectedMethod = method;
        btnCash.getStyleClass().remove("payment-method-selected");
        btnCard.getStyleClass().remove("payment-method-selected");
        btnTransfer.getStyleClass().remove("payment-method-selected");
        switch (method) {
            case "CASH"     -> { btnCash.getStyleClass().add("payment-method-selected");     }
            case "CARD"     -> { btnCard.getStyleClass().add("payment-method-selected");     }
            case "TRANSFER" -> { btnTransfer.getStyleClass().add("payment-method-selected"); }
        }
        boolean showRef = !method.equals("CASH");
        cardForm.setVisible(showRef);
        cardForm.setManaged(showRef);
    }

    // ── Pay ──────────────────────────────────────────────────────────────────

    @FXML
    public void onPay() {
        if (reservation == null) return;
        errorLabel.setText("");

        List<Service> selectedServices = services.stream()
                .filter(s -> s.getQuantity() > 0)
                .toList();

        double roomSubtotal = reservation.getBaseTotal();
        String roomDesc     = String.format("Room %s – %d nights", reservation.getRoomNumber(), reservation.getNights());
        String ref          = transactionRef.getText().trim();

        new Thread(() -> {
            try {
                int invoiceId = InvoiceDao.createWithPayment(
                        reservation.getId(),
                        roomDesc,
                        roomSubtotal,
                        selectedServices,
                        selectedMethod,
                        ref.isEmpty() ? null : ref);

                Platform.runLater(() -> showConfirmation(invoiceId));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Payment failed: " + ex.getMessage()));
            }
        }, "payment-thread").start();
    }

    private void showConfirmation(int invoiceId) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Confirmed");
        alert.setHeaderText("Booking Confirmed!");
        alert.setContentText(String.format(
                "Your reservation is confirmed.\nInvoice ID: %d\nPayment method: %s\n\nThank you for choosing SmartStay!",
                invoiceId, selectedMethod));
        alert.showAndWait();
        Navigator.navigateTo(summaryRoom, Navigator.HOME);
    }

    @FXML
    public void onBack() {
        Navigator.navigateTo(summaryRoom, Navigator.HOME);
    }
}