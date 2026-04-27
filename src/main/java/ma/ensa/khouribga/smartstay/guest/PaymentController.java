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
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Payment + booking confirmation dialog.
 *
 * Steps shown on one screen:
 *  [1] Guest info (first name, last name, passport, email, phone)
 *  [2] Booking summary  (room, dates, nights, rate)
 *  [3] Optional services  (ListView of active services with qty spinners)
 *  [4] Grand total preview
 *  [5] "Confirm Booking" → creates Guest (or finds existing) + Reservation + Invoice
 */
public class PaymentController {

    // ── Guest info ────────────────────────────────────────────────────────────
    @FXML private TextField tfFirstName;
    @FXML private TextField tfLastName;
    @FXML private TextField tfPassport;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;

    // ── Booking summary ───────────────────────────────────────────────────────
    @FXML private Label lblRoom;
    @FXML private Label lblDates;
    @FXML private Label lblNights;
    @FXML private Label lblRoomTotal;

    // ── Services ──────────────────────────────────────────────────────────────
    @FXML private TableView<ServiceLineItem> tblServices;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcName;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcPrice;
    @FXML private TableColumn<ServiceLineItem, Integer> colSvcQty;
    @FXML private TableColumn<ServiceLineItem, String>  colSvcTotal;

    // ── Grand total ───────────────────────────────────────────────────────────
    @FXML private Label lblGrandTotal;

    // ── Feedback ─────────────────────────────────────────────────────────────
    @FXML private Label lblError;
    @FXML private Label lblSuccess;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;

    // ── State ─────────────────────────────────────────────────────────────────
    private Room     room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long nights;
    private boolean bookingConfirmed = false;
    private List<ServiceLineItem> serviceItems = new ArrayList<>();

    // ── Init ──────────────────────────────────────────────────────────────────

    public void initData(Room room, LocalDate checkIn, LocalDate checkOut) {
        this.room     = room;
        this.checkIn  = checkIn;
        this.checkOut = checkOut;
        this.nights   = ChronoUnit.DAYS.between(checkIn, checkOut);

        // Summary labels
        lblRoom.setText("Room " + room.getRoomNumber() + " (" + room.getRoomTypeName() + ")");
        lblDates.setText(checkIn + "  →  " + checkOut);
        lblNights.setText(nights + " night(s) × " + room.getPricePerNight() + " MAD");
        BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        lblRoomTotal.setText(roomTotal.toPlainString() + " MAD");

        // Wire table
        setupServicesTable();

        // Load services on background thread
        loadServices();
    }

    // ── Services table setup ──────────────────────────────────────────────────

    private void setupServicesTable() {
        colSvcName.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        colSvcPrice.setCellValueFactory(new PropertyValueFactory<>("unitPriceDisplay"));
        colSvcTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotalDisplay"));

        // Spinner column for quantity
        colSvcQty.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(0, 20, 0);

            {
                spinner.setEditable(true);
                spinner.setPrefWidth(75);
                spinner.valueProperty().addListener((obs, old, qty) -> {
                    ServiceLineItem item = getTableView().getItems().get(getIndex());
                    item.setQuantity(qty);
                    refreshGrandTotal();
                });
            }

            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setGraphic(null); return; }
                spinner.getValueFactory().setValue(
                        getTableView().getItems().get(getIndex()).getQuantity());
                setGraphic(spinner);
            }
        });

        tblServices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadServices() {
        Task<List<Service>> task = new Task<>() {
            @Override protected List<Service> call() {
                return ServiceDao.findActive();
            }
        };
        task.setOnSucceeded(e -> {
            for (Service svc : task.getValue()) {
                serviceItems.add(new ServiceLineItem(svc));
            }
            tblServices.setItems(FXCollections.observableList(serviceItems));
            refreshGrandTotal();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void refreshGrandTotal() {
        BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        BigDecimal svcTotal = serviceItems.stream()
                .map(ServiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblGrandTotal.setText((roomTotal.add(svcTotal)).toPlainString() + " MAD");
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void handleConfirm() {
        lblError.setText("");
        if (!validateGuestForm()) return;

        btnConfirm.setDisable(true);
        btnConfirm.setText("Processing…");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return performBooking();
            }
        };

        task.setOnSucceeded(e -> {
            bookingConfirmed = true;
            String code = task.getValue();
            lblSuccess.setText("✓ Booking confirmed! Code: " + code);
            btnConfirm.setVisible(false);
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

    /**
     * Core booking logic — runs on background thread.
     * 1. Resolve or create Guest
     * 2. Create Reservation
     * 3. Build Invoice with room line + service lines
     * 4. Issue invoice
     *
     * @return reservation code on success
     */
    private String performBooking() throws Exception {
        // 1. Guest
        String passport = tfPassport.getText().trim();
        Optional<Guest> existingGuest = GuestDao.findByPassport(passport);

        long guestId;
        if (existingGuest.isPresent()) {
            guestId = existingGuest.get().getId();
        } else {
            Guest g = new Guest();
            g.setFirstName(tfFirstName.getText().trim());
            g.setLastName(tfLastName.getText().trim());
            g.setIdPassportNumber(passport);
            g.setEmail(tfEmail.getText().trim());
            g.setPhone(tfPhone.getText().trim());
            guestId = GuestDao.create(g);
        }

        // 2. Reservation
        User booker = SessionManager.getInstance().getCurrentUser();
        Reservation res = new Reservation();
        res.setGuestId(guestId);
        res.setRoomId(room.getId());
        res.setBookedByUserId(booker.getId());
        res.setCheckInDate(checkIn);
        res.setCheckOutDate(checkOut);
        res.setStatus(Reservation.Status.CONFIRMED);

        long reservationId = ReservationDao.create(res);
        // Fetch to get the generated code
        Reservation created = ReservationDao.findByCode(
                ReservationDao.findById(reservationId).get().getReservationCode()
        ).get();

        // 3. Invoice — build lines
        List<InvoiceLine> lines = new ArrayList<>();

        // Room accommodation line
        InvoiceLine roomLine = new InvoiceLine();
        roomLine.setLineType(LineType.ACCOMMODATION);
        roomLine.setReferenceId(room.getId());
        roomLine.setDescription("Room " + room.getRoomNumber() + " × " + nights + " night(s)");
        roomLine.setQuantity((int) nights);
        roomLine.setUnitPrice(room.getPricePerNight());
        lines.add(roomLine);

        // Service lines (qty > 0 only)
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

        return created.getReservationCode();
    }

    @FXML
    private void handleCancel() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    public boolean isBookingConfirmed() {
        return bookingConfirmed;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateGuestForm() {
        if (tfFirstName.getText().isBlank() || tfLastName.getText().isBlank()) {
            lblError.setText("First name and last name are required.");
            return false;
        }
        if (tfPassport.getText().isBlank()) {
            lblError.setText("Passport / ID number is required.");
            return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner helper: wraps a Service with an observable quantity for the table
    // ─────────────────────────────────────────────────────────────────────────

    public static class ServiceLineItem {
        private final Service service;
        private int quantity = 0;

        public ServiceLineItem(Service service) { this.service = service; }

        public Service getService()         { return service; }
        public String  getServiceName()     { return service.getName(); }
        public String  getUnitPriceDisplay(){ return service.getUnitPrice().toPlainString() + " MAD"; }
        public int     getQuantity()        { return quantity; }
        public void    setQuantity(int q)   { this.quantity = q; }
        public BigDecimal getLineTotal()    {
            return service.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        }
        public String getLineTotalDisplay() {
            return quantity == 0 ? "—" : getLineTotal().toPlainString() + " MAD";
        }
    }
}