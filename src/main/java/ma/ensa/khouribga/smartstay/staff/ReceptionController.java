package ma.ensa.khouribga.smartstay.staff;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.CleaningDao;
import ma.ensa.khouribga.smartstay.dao.GuestDao;
import ma.ensa.khouribga.smartstay.dao.MaintenanceDao;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.dao.ServiceDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.CleaningRequest;
import ma.ensa.khouribga.smartstay.model.Guest;
import ma.ensa.khouribga.smartstay.model.MaintenanceRequest;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.model.Service;
import ma.ensa.khouribga.smartstay.profile.StaffProfileController;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReceptionController {

    // ── FXML Bindings ─────────────────────────────────────────────────────────
    @FXML private MediaView bgMediaView;
    @FXML private FlowPane  resGrid;
    @FXML private Label     lblSelectedRes;
    @FXML private TextField txtSearch;
    @FXML private TextField txtCleanRoom;
    @FXML private TextField txtMaintRoom;
    @FXML private ComboBox<String> cmbMaintCategory;
    @FXML private TextArea  txtMaintDesc;

    // ── State ─────────────────────────────────────────────────────────────────
    private Reservation selectedRes;
    private VBox        selectedResCard;
    private List<Reservation> allReservations = List.of();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        if (cmbMaintCategory != null) {
            cmbMaintCategory.setItems(FXCollections.observableArrayList(
                "PLUMBING / WATER",
                "ELECTRICAL / LIGHTING",
                "HVAC / CLIMATE",
                "FURNITURE / FIXTURES",
                "ELECTRONICS / TV",
                "CLEANING HAZARD"
            ));
        }
        loadReservations();
    }

    // ── Reservations Grid ─────────────────────────────────────────────────────
    @FXML
    public void loadReservations() {
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findActive();
                Platform.runLater(() -> {
                    allReservations = list;
                    if (txtSearch != null) txtSearch.clear();
                    populateResGrid(list);
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    @FXML
    public void filterReservations() {
        String q = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase();
        if (q.isEmpty()) { populateResGrid(allReservations); return; }
        populateResGrid(allReservations.stream()
            .filter(r -> r.getReservationCode().toLowerCase().contains(q)
                      || r.getGuestFullName().toLowerCase().contains(q)
                      || r.getRoomNumber().toLowerCase().contains(q))
            .toList());
    }

    private void populateResGrid(List<Reservation> list) {
        if (resGrid == null) return;
        resGrid.getChildren().clear();
        selectedRes = null; selectedResCard = null;
        for (Reservation res : list) {
            VBox card = new VBox(0);
            card.getStyleClass().add("data-card");
            card.setPrefWidth(320); card.setMinWidth(280); card.setMaxWidth(380);

            // Header
            HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-padding: 16 16 12 16;");
            String gName = res.getGuestFullName();
            String initials = gName != null && gName.length() >= 2 ? gName.substring(0,2).toUpperCase() : "??";
            StackPane avatar = new StackPane(); avatar.setPrefSize(48,48); avatar.setMinSize(48,48);
            avatar.getStyleClass().add("avatar-circle");
            Label avLbl = new Label(initials); avLbl.getStyleClass().add("avatar-initials"); avLbl.setStyle("-fx-font-size:16px;");
            avatar.getChildren().add(avLbl);
            VBox nameCol = new VBox(4); HBox.setHgrow(nameCol, Priority.ALWAYS);
            Label lblCode = new Label(res.getReservationCode()); lblCode.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");
            Label lblGuest = new Label(gName); lblGuest.setStyle("-fx-font-size:11px;-fx-text-fill:#a0a0a0;");
            nameCol.getChildren().addAll(lblCode, lblGuest);
            header.getChildren().addAll(avatar, nameCol, createBadge(res.getStatus().toString()));

            Region divider = new Region(); divider.setMaxWidth(Double.MAX_VALUE); divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color:rgba(197,160,89,0.18);");

            long nights = res.getCheckInDate() != null && res.getCheckOutDate() != null
                ? res.getCheckOutDate().toEpochDay() - res.getCheckInDate().toEpochDay() : 0;
            VBox body = new VBox(9); body.setStyle("-fx-padding:14 16 16 16;");
            body.getChildren().addAll(
                detailRow("🏯", "Room " + res.getRoomNumber(), "#c5a059"),
                detailRow("📅", res.getCheckInDate().format(DATE_FMT) + "  →  " + res.getCheckOutDate().format(DATE_FMT), "#a0a0a0"),
                detailRow("🌙", nights + " night" + (nights == 1 ? "" : "s"), "#888"),
                detailRow("💴", String.format("%.2f MAD", res.getBaseTotal()), "#1e8449")
            );

            card.getChildren().addAll(header, divider, body);
            card.setOnMouseClicked(e -> {
                if (selectedResCard != null) selectedResCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedResCard = card; selectedRes = res;
                if (lblSelectedRes != null) lblSelectedRes.setText(res.getReservationCode() + "  ·  " + gName);
            });
            resGrid.getChildren().add(card);
        }
    }

    private HBox detailRow(String icon, String text, String color) {
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:13px;-fx-min-width:20;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";");
        HBox row  = new HBox(8, ico, lbl); row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private Label createBadge(String s) {
        Label b = new Label(s); b.getStyleClass().add("badge");
        if (s.equals("AVAILABLE") || s.equals("CHECKED_OUT"))  b.getStyleClass().add("badge-available");
        else if (s.equals("CONFIRMED"))                         b.getStyleClass().add("badge-paid");
        else if (s.equals("CHECKED_IN"))                        b.getStyleClass().add("badge-cleaning");
        else                                                     b.getStyleClass().add("badge-pending");
        return b;
    }

    // ── Check-In / Out ────────────────────────────────────────────────────────
    @FXML public void doCheckIn() {
        if (selectedRes == null) { showAlert(Alert.AlertType.WARNING, "Select a reservation card first."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_IN);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Check-in failed.")); }
        }).start();
    }

    @FXML public void doCheckOut() {
        if (selectedRes == null) { showAlert(Alert.AlertType.WARNING, "Select a reservation card first."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_OUT);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) { Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Check-out failed.")); }
        }).start();
    }

    // ── Walk-In Registration ──────────────────────────────────────────────────
    @FXML
    public void doWalkIn(ActionEvent event) {
        try {
            // Load rooms and services on background thread, then open dialog
            new Thread(() -> {
                try {
                    List<Room>    availableRooms = RoomDao.findAvailable();
                    List<Service> services       = ServiceDao.findActive();
                    Platform.runLater(() -> openWalkInDialog(availableRooms, services));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Failed to load rooms/services: " + ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openWalkInDialog(List<Room> availableRooms, List<Service> services) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Walk-In Guest Registration");
        dialog.setMinWidth(620);
        dialog.setMinHeight(700);

        // ── Form fields ───────────────────────────────────────────────────────
        TextField tfFirstName  = styled(new TextField(), "First Name");
        TextField tfLastName   = styled(new TextField(), "Last Name");
        TextField tfIdDoc      = styled(new TextField(), "CIN / Passport No.");
        TextField tfEmail      = styled(new TextField(), "Email (optional)");
        TextField tfPhone      = styled(new TextField(), "Phone (optional)");
        TextField tfNationality= styled(new TextField(), "Nationality");

        ComboBox<Room> cbRoom  = new ComboBox<>();
        cbRoom.setItems(FXCollections.observableArrayList(availableRooms));
        cbRoom.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : "Room " + r.getRoomNumber() + " — " + r.getTypeName() + " (" + String.format("%.0f", r.getPricePerNight()) + " MAD/night)");
            }
        });
        cbRoom.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "Select available room..." : "Room " + r.getRoomNumber() + " — " + r.getTypeName());
            }
        });
        cbRoom.setMaxWidth(Double.MAX_VALUE);
        cbRoom.getStyleClass().add("combo-box");

        DatePicker dpCheckIn  = new DatePicker(LocalDate.now());
        DatePicker dpCheckOut = new DatePicker(LocalDate.now().plusDays(1));
        dpCheckIn.setMaxWidth(Double.MAX_VALUE);
        dpCheckOut.setMaxWidth(Double.MAX_VALUE);

        // Services checkboxes with quantity spinners
        VBox svcBox = new VBox(6);
        List<CheckBox> svcChecks = new ArrayList<>();
        List<Spinner<Integer>> svcSpinners = new ArrayList<>();
        for (Service svc : services) {
            CheckBox cb = new CheckBox(svc.getName() + "  —  " + String.format("%.0f", svc.getUnitPrice()) + " MAD");
            cb.setStyle("-fx-text-fill: #e0e0e0;");
            Spinner<Integer> sp = new Spinner<>(1, 20, 1);
            sp.setPrefWidth(70);
            sp.setDisable(true);
            cb.selectedProperty().addListener((obs, o, n) -> sp.setDisable(!n));
            HBox row = new HBox(10, cb, new Region(), sp);
            HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            svcBox.getChildren().add(row);
            svcChecks.add(cb);
            svcSpinners.add(sp);
        }

        Label lblError = new Label("");
        lblError.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 12px;");

        // ── Layout ────────────────────────────────────────────────────────────
        VBox form = new VBox(12);
        form.setStyle("-fx-padding: 24; -fx-background-color: rgba(14,14,17,0.97);");

        Label title = new Label("WALK-IN GUEST REGISTRATION");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c5a059; -fx-padding: 0 0 10 0;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(col(0.5), col(0.5));

        addRow(grid, 0, lbl("FIRST NAME"), lbl("LAST NAME"));
        addRow(grid, 1, tfFirstName, tfLastName);
        addRow(grid, 2, lbl("CIN / PASSPORT"), lbl("NATIONALITY"));
        addRow(grid, 3, tfIdDoc, tfNationality);
        addRow(grid, 4, lbl("EMAIL"), lbl("PHONE"));
        addRow(grid, 5, tfEmail, tfPhone);
        addRow(grid, 6, lbl("CHECK-IN DATE"), lbl("CHECK-OUT DATE"));
        addRow(grid, 7, dpCheckIn, dpCheckOut);

        Label svcTitle = new Label("SERVICES");
        svcTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #c5a059; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        ScrollPane svcScroll = new ScrollPane(svcBox);
        svcScroll.setFitToWidth(true);
        svcScroll.setPrefHeight(180);
        svcScroll.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(197,160,89,0.3); -fx-background: rgba(0,0,0,0.2);");

        Button btnConfirm = new Button("CONFIRM WALK-IN");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setStyle("-fx-font-size: 14px; -fx-padding: 12;");

        Button btnCancel = new Button("CANCEL");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        form.getChildren().addAll(
            title, new Label("ROOM ASSIGNMENT") {{ setStyle("-fx-font-size:13px; -fx-text-fill:#c5a059; -fx-font-weight:bold;"); }},
            cbRoom, grid, svcTitle, svcScroll, lblError,
            btnConfirm, btnCancel
        );

        // ── Submit logic ──────────────────────────────────────────────────────
        btnConfirm.setOnAction(e -> {
            lblError.setText("");
            String firstName   = tfFirstName.getText().trim();
            String lastName    = tfLastName.getText().trim();
            String idDoc       = tfIdDoc.getText().trim();
            String nationality = tfNationality.getText().trim();
            Room   room        = cbRoom.getValue();
            LocalDate ci       = dpCheckIn.getValue();
            LocalDate co       = dpCheckOut.getValue();

            if (firstName.isEmpty() || lastName.isEmpty()) { lblError.setText("First and last name are required."); return; }
            if (idDoc.isEmpty())                           { lblError.setText("CIN or Passport number is required."); return; }
            if (room == null)                              { lblError.setText("Please select an available room."); return; }
            if (ci == null || co == null || !co.isAfter(ci)) { lblError.setText("Check-out must be after check-in."); return; }

            btnConfirm.setDisable(true);
            btnConfirm.setText("Processing...");

            // Collect selected services
            List<Service> selectedSvcs = new ArrayList<>();
            for (int i = 0; i < services.size(); i++) {
                if (svcChecks.get(i).isSelected()) {
                    Service svc = services.get(i);
                    svc.setQuantity(svcSpinners.get(i).getValue());
                    selectedSvcs.add(svc);
                }
            }

            final String fn = firstName, ln = lastName, id = idDoc,
                         nat = nationality.isEmpty() ? "Unknown" : nationality,
                         email = tfEmail.getText().trim(), phone = tfPhone.getText().trim();

            new Thread(() -> {
                try {
                    // 1. Upsert guest (reuse if same passport)
                    Guest guest = GuestDao.findByPassport(id).orElse(null);
                    int guestId;
                    if (guest != null) {
                        guestId = guest.getId();
                    } else {
                        Guest g = new Guest();
                        g.setFirstName(fn); g.setLastName(ln);
                        g.setIdPassportNumber(id); g.setNationality(nat);
                        g.setEmail(email.isEmpty() ? null : email);
                        g.setPhone(phone.isEmpty() ? null : phone);
                        guestId = GuestDao.create(g);
                    }

                    // 2. Create reservation (CHECKED_IN immediately — walk-in)
                    long staffUserId = SessionManager.getCurrentUser().getId();
                    Reservation res = new Reservation();
                    res.setGuestId(guestId);
                    res.setRoomId(room.getId());
                    res.setBookedByUserId((int) staffUserId);
                    res.setCheckInDate(ci);
                    res.setCheckOutDate(co);
                    res.setAdultsCount(1);
                    res.setChildrenCount(0);
                    int resId = ReservationDao.create(res);

                    // 3. Immediately check in
                    ReservationDao.updateStatus(resId, room.getId(), Reservation.Status.CHECKED_IN);

                    // 4. Attach services
                    if (!selectedSvcs.isEmpty()) {
                        try (Connection conn = Database.getConnection()) {
                            String svcSql = """
                                INSERT INTO reservation_services
                                  (reservation_id, service_id, quantity, unit_price, requested_at)
                                VALUES (?, ?, ?, ?, NOW())
                                """;
                            try (PreparedStatement ps = conn.prepareStatement(svcSql)) {
                                for (Service svc : selectedSvcs) {
                                    ps.setInt(1, resId);
                                    ps.setInt(2, svc.getId());
                                    ps.setInt(3, svc.getQuantity());
                                    ps.setDouble(4, svc.getUnitPrice());
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                        }
                    }

                    final String code = res.getReservationCode();
                    Platform.runLater(() -> {
                        dialog.close();
                        loadReservations();
                        showAlert(Alert.AlertType.INFORMATION,
                            "Walk-in registered!\nGuest: " + fn + " " + ln +
                            "\nRoom: " + room.getRoomNumber() +
                            "\nCode: " + code +
                            (selectedSvcs.isEmpty() ? "" : "\nServices: " + selectedSvcs.size() + " added"));
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        lblError.setText("Error: " + ex.getMessage());
                        btnConfirm.setDisable(false);
                        btnConfirm.setText("CONFIRM WALK-IN");
                    });
                }
            }).start();
        });

        ScrollPane root = new ScrollPane(form);
        root.setFitToWidth(true);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 640, 700);
        try { scene.getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm()); } catch (Exception ignored) {}
        ThemeManager.applyToScene(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Dispatch Cleaning ─────────────────────────────────────────────────────
    @FXML
    public void requestCleaning() {
        String room = txtCleanRoom.getText().trim();
        if (room.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Enter a room number to dispatch cleaning."); return; }

        new Thread(() -> {
            try (Connection conn = Database.getConnection()) {
                // Resolve room id
                long roomId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM rooms WHERE room_number = ?")) {
                    ps.setString(1, room);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) roomId = rs.getLong("id"); }
                }
                if (roomId == -1) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Room " + room + " not found."));
                    return;
                }

                long userId = SessionManager.getCurrentUser().getId();

                // Insert cleaning request
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO cleaning_requests
                      (room_id, requested_by_user_id, priority, status, request_note, created_at)
                    VALUES (?, ?, 'HIGH', 'NEW', 'Reception dispatch — immediate cleaning required', NOW())
                    """)) {
                    ps.setLong(1, roomId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                // Mark room CLEANING
                try (PreparedStatement ps = conn.prepareStatement("UPDATE rooms SET status = 'CLEANING' WHERE id = ?")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }

                Platform.runLater(() -> {
                    txtCleanRoom.clear();
                    showAlert(Alert.AlertType.INFORMATION, "Cleaning crew dispatched to room " + room + ". Room marked CLEANING.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Failed to dispatch cleaning: " + e.getMessage()));
            }
        }).start();
    }

    // ── Dispatch Maintenance ──────────────────────────────────────────────────
    @FXML
    public void dispatchMaintenance() {
        String roomStr  = txtMaintRoom.getText().trim();
        String category = cmbMaintCategory.getValue();
        String desc     = txtMaintDesc.getText().trim();

        if (roomStr.isEmpty() || category == null || desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please fill out Room, Hazard Type, and Situation Report.");
            return;
        }

        new Thread(() -> {
            try (Connection conn = Database.getConnection()) {
                // Resolve room
                long roomId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM rooms WHERE room_number = ?")) {
                    ps.setString(1, roomStr);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) roomId = rs.getLong("id"); }
                }
                if (roomId == -1) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Room " + roomStr + " does not exist."));
                    return;
                }

                long userId = SessionManager.getCurrentUser().getId();
                String title = "[" + category + "] Reception Dispatch";

                // Insert with all required NOT-NULL columns using valid status 'NEW'
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO maintenance_requests
                      (room_id, reported_by_user_id, priority, status, title, description, created_at)
                    VALUES (?, ?, 'HIGH', 'NEW', ?, ?, NOW())
                    """)) {
                    ps.setLong(1, roomId);
                    ps.setLong(2, userId);
                    ps.setString(3, title);
                    ps.setString(4, desc);
                    ps.executeUpdate();
                }

                // Mark room MAINTENANCE
                try (PreparedStatement ps = conn.prepareStatement("UPDATE rooms SET status = 'MAINTENANCE' WHERE id = ?")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }

                Platform.runLater(() -> {
                    txtMaintRoom.clear();
                    txtMaintDesc.clear();
                    cmbMaintCategory.setValue(null);
                    showAlert(Alert.AlertType.INFORMATION, "Maintenance crew dispatched to Room " + roomStr + ". Room locked to MAINTENANCE.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML public void goToProfile(MouseEvent event) {
        Navigator.navigateTo((Node) event.getSource(), Navigator.STAFF_PROFILE,
            ctrl -> ((StaffProfileController) ctrl).setPreviousRoute(Navigator.RECEPTION));
    }

    @FXML public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Navigator.goToLogin((Node) event.getSource());
    }

    @FXML public void handleThemeToggle() { ThemeManager.toggle(); }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm()); } catch (Exception ignored) {}
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    private TextField styled(TextField tf, String prompt) {
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #c5a059; -fx-font-weight: bold;");
        return l;
    }

    private ColumnConstraints col(double pct) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(pct * 100);
        c.setHgrow(Priority.ALWAYS);
        return c;
    }

    private void addRow(GridPane g, int row, Node left, Node right) {
        GridPane.setColumnIndex(left, 0);  GridPane.setRowIndex(left, row);
        GridPane.setColumnIndex(right, 1); GridPane.setRowIndex(right, row);
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
        if (left  instanceof Control c) c.setMaxWidth(Double.MAX_VALUE);
        if (right instanceof Control c) c.setMaxWidth(Double.MAX_VALUE);
        g.getChildren().addAll(left, right);
    }
}