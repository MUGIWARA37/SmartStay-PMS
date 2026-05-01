package ma.ensa.khouribga.smartstay.guest;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.InvoiceDao;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.dao.ServiceDao;
import ma.ensa.khouribga.smartstay.model.*;
import ma.ensa.khouribga.smartstay.model.Invoice.InvoiceLine;
import ma.ensa.khouribga.smartstay.model.Invoice.InvoiceLine.LineType;
import ma.ensa.khouribga.smartstay.service.InvoiceExportService;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentController {

    // Guest info
    @FXML private TextField tfFirstName;
    @FXML private TextField tfLastName;
    @FXML private TextField tfPassport;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;

    // Booking summary
    @FXML private Label lblRoom;
    @FXML private Label lblDates;
    @FXML private Label lblNights;
    @FXML private Label lblRoomTotal;

    // Services
    @FXML private TableView<ServiceLineItem> tblServices;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcName;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcPrice;
    @FXML private TableColumn<ServiceLineItem, Integer> colSvcQty;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcTotal;

    // Grand total
    @FXML private Label lblGrandTotal;

    // Feedback
    @FXML private Label lblError;
    @FXML private Label lblSuccess;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;
    @FXML private Button btnExportPdf;

    // State
    private Room room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long nights;
    private boolean bookingConfirmed = false;
    private List<ServiceLineItem> serviceItems = new ArrayList<>();
    private long lastInvoiceId = -1;
    private int lastReservationId = -1;

    public void initData(Room room, LocalDate checkIn, LocalDate checkOut) {
        this.room     = room;
        this.checkIn  = checkIn;
        this.checkOut = checkOut;
        this.nights   = ChronoUnit.DAYS.between(checkIn, checkOut);

        lblRoom.setText("Room " + room.getRoomNumber() + " (" + room.getTypeName() + ")");
        lblDates.setText(checkIn + "  →  " + checkOut);
        lblNights.setText(nights + " night(s) × " + String.format("%.2f", room.getPricePerNight()) + " MAD");
        lblRoomTotal.setText(String.format("%.2f MAD", room.getPricePerNight() * nights));

        setupServicesTable();
        loadServices();
    }

    private void setupServicesTable() {
        colSvcName.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        colSvcPrice.setCellValueFactory(new PropertyValueFactory<>("unitPriceDisplay"));
        colSvcTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotalDisplay"));

        colSvcQty.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(0, 20, 0);
            {
                spinner.setEditable(true);
                spinner.setPrefWidth(75);
                spinner.valueProperty().addListener((obs, old, qty) -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        getTableView().getItems().get(getIndex()).setQuantity(qty);
                        refreshGrandTotal();
                    }
                });
            }
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getIndex() < 0) { setGraphic(null); return; }
                spinner.getValueFactory().setValue(
                        getTableView().getItems().get(getIndex()).getQuantity());
                setGraphic(spinner);
            }
        });

        tblServices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadServices() {
        Task<List<Service>> task = new Task<>() {
            @Override protected List<Service> call() throws Exception { return ServiceDao.findActive(); }
        };
        task.setOnSucceeded(e -> {
            for (Service svc : task.getValue()) serviceItems.add(new ServiceLineItem(svc));
            tblServices.setItems(FXCollections.observableList(serviceItems));
            refreshGrandTotal();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void refreshGrandTotal() {
        double roomTotal = room.getPricePerNight() * nights;
        double svcTotal  = serviceItems.stream().mapToDouble(ServiceLineItem::getLineTotal).sum();
        lblGrandTotal.setText(String.format("%.2f MAD", roomTotal + svcTotal));
    }

    @FXML
    private void handleConfirm() {
        lblError.setText("");
        if (!validateGuestForm()) return;

        btnConfirm.setDisable(true);
        btnConfirm.setText("Processing…");

        Task<String[]> task = new Task<>() {
            @Override protected String[] call() throws Exception { return performBooking(); }
        };

        task.setOnSucceeded(e -> {
            bookingConfirmed = true;
            String[] result = task.getValue();
            lastReservationId = Integer.parseInt(result[0]);
            lastInvoiceId     = Integer.parseInt(result[1]);
            lblSuccess.setText("✓ Booking confirmed! Reservation Code: " + result[2]);
            lblSuccess.setVisible(true);
            lblSuccess.setManaged(true);
            btnConfirm.setVisible(false);
            btnConfirm.setManaged(false);
            if (btnExportPdf != null) {
                btnExportPdf.setVisible(true);
                btnExportPdf.setManaged(true);
            }
            btnCancel.setText("Close");
        });

        task.setOnFailed(e -> {
            lblError.setText("Booking failed: " + task.getException().getMessage());
            btnConfirm.setDisable(false);
            btnConfirm.setText("Confirm Booking");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String[] performBooking() throws Exception {
        // 1. Guest
        String passport = tfPassport.getText().trim();
        Optional<Guest> existingGuest = GuestDao.findByPassport(passport);

        int guestId;
        if (existingGuest.isPresent()) {
            guestId = existingGuest.get().getId();
        } else {
            Guest g = new Guest();
            g.setFirstName(tfFirstName.getText().trim());
            g.setLastName(tfLastName.getText().trim());
            g.setIdPassportNumber(passport);
            g.setEmail(tfEmail.getText().trim());
            guestId = GuestDao.create(g);
        }

        // 2. Reservation
        User booker = SessionManager.getCurrentUser();
        Reservation res = new Reservation();
        res.setGuestId(guestId);
        res.setRoomId(room.getId());
        res.setBookedByUserId((int) booker.getId());
        res.setCheckInDate(checkIn);
        res.setCheckOutDate(checkOut);
        res.setStatus(Reservation.Status.CONFIRMED);

        int reservationId = ReservationDao.create(res);
        String reservationCode = ReservationDao.findById(reservationId)
                .map(Reservation::getReservationCode)
                .orElse("SS-UNKNOWN");

        // 3. Invoice lines
        List<InvoiceLine> lines = new ArrayList<>();

        InvoiceLine roomLine = new InvoiceLine();
        roomLine.setLineType(LineType.ROOM);
        roomLine.setReferenceId(room.getId());
        roomLine.setDescription("Room " + room.getRoomNumber() + " × " + nights + " night(s)");
        roomLine.setQuantity((int) nights);
        roomLine.setUnitPrice(room.getPricePerNight());
        lines.add(roomLine);

        for (ServiceLineItem item : serviceItems) {
            if (item.getQuantity() > 0) {
                InvoiceLine svcLine = new InvoiceLine();
                svcLine.setLineType(LineType.SERVICE);
                svcLine.setReferenceId(item.getService().getId());
                svcLine.setDescription(item.getServiceName());
                svcLine.setQuantity(item.getQuantity());
                svcLine.setUnitPrice(item.getService().getUnitPrice());
                lines.add(svcLine);
            }
        }

        Invoice invoice = new Invoice();
        invoice.setReservationId(reservationId);
        invoice.setLines(lines);

        long invoiceId = InvoiceDao.create(invoice);
        InvoiceDao.updateStatus(invoiceId, Invoice.Status.ISSUED);

        return new String[]{ String.valueOf(reservationId), String.valueOf(invoiceId), reservationCode };
    }

    @FXML
    private void handleExportPdf() {
        if (lastInvoiceId < 0 || lastReservationId < 0) return;
        new Thread(() -> {
            try {
                Invoice invoice = InvoiceDao.findById(lastInvoiceId).orElseThrow();
                Reservation res = ReservationDao.findById(lastReservationId).orElseThrow();
                File pdf = InvoiceExportService.export(invoice, res);
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "Invoice saved to:\n" + pdf.getAbsolutePath(), ButtonType.OK);
                    alert.setHeaderText("📜  Invoice Exported");
                    alert.getDialogPane().getStylesheets().add(
                            getClass().getResource("/styles/samurai.css").toExternalForm());
                    alert.getDialogPane().getStyleClass().add("dialog-pane");
                    alert.showAndWait();
                    try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf); }
                    catch (Exception ignored) {}
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "PDF export failed: " + ex.getMessage(), ButtonType.OK);
                    err.setHeaderText(null);
                    err.showAndWait();
                });
            }
        }).start();
    }

    @FXML private void handleCancel() { ((Stage) btnCancel.getScene().getWindow()).close(); }

    public boolean isBookingConfirmed() { return bookingConfirmed; }

    private boolean validateGuestForm() {
        if (tfFirstName.getText().isBlank() || tfLastName.getText().isBlank()) {
            lblError.setText("First name and last name are required."); return false;
        }
        if (tfPassport.getText().isBlank()) {
            lblError.setText("Passport / ID number is required."); return false;
        }
        return true;
    }

    // ── Inner helper ──────────────────────────────────────────────────────────

    public static class ServiceLineItem {
        private final Service service;
        private int quantity = 0;

        public ServiceLineItem(Service service) { this.service = service; }

        public Service getService()          { return service; }
        public String  getServiceName()      { return service.getName(); }
        public String  getUnitPriceDisplay() { return String.format("%.2f MAD", service.getUnitPrice()); }
        public int     getQuantity()         { return quantity; }
        public void    setQuantity(int q)    { this.quantity = q; }
        public double  getLineTotal()        { return service.getUnitPrice() * quantity; }
        public String  getLineTotalDisplay() {
            return quantity == 0 ? "—" : String.format("%.2f MAD", getLineTotal());
        }
    }
}