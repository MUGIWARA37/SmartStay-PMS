package ma.ensa.khouribga.smartstay.home;

import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HomeController implements Initializable {

    @FXML private MediaView bgMediaView;

    // Sidebar
    @FXML private Button btnBrowse;
    @FXML private Button btnMyBookings;
    @FXML private VBox   guestActions;   // shown when NOT logged in
    @FXML private VBox   userActions;    // shown when logged in
    @FXML private Label  welcomeLabel;
    @FXML private Label  avatarInitials;
    @FXML private Button btnLogout;

    // Header
    @FXML private Label headerPanelLabel;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator spinner;

    // Browse panel
    @FXML private VBox         panelBrowse;
    @FXML private ComboBox<String> cbRoomType;
    @FXML private Slider       priceSlider;
    @FXML private Label        priceLabel;
    @FXML private DatePicker   dpCheckIn;
    @FXML private DatePicker   dpCheckOut;
    @FXML private Button       btnSearch;
    @FXML private FlowPane     roomGrid;

    // Bookings panel
    @FXML private VBox      panelBookings;
    @FXML private FlowPane  bookingsGrid;

    private List<Room> allRooms;
    private boolean loggedIn = false;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        VideoBackground.register(bgMediaView);

        // ── Determine login state ─────────────────────────────────────────────
        loggedIn = SessionManager.getCurrentUser() != null;

        if (loggedIn) {
            // Show user avatar + sign-out; hide sign-in button
            String name = SessionManager.getCurrentUser().getUsername();
            welcomeLabel.setText(name);
            String initials = name.length() >= 2
                    ? name.substring(0, 2).toUpperCase()
                    : name.toUpperCase();
            avatarInitials.setText(initials);
            guestActions.setVisible(false);
            guestActions.setManaged(false);
            userActions.setVisible(true);
            userActions.setManaged(true);
        } else {
            // Public mode — show sign-in button, hide user area
            guestActions.setVisible(true);
            guestActions.setManaged(true);
            userActions.setVisible(false);
            userActions.setManaged(false);
        }

        // ── Price slider ──────────────────────────────────────────────────────
        priceSlider.setMin(0);
        priceSlider.setMax(5000);
        priceSlider.setValue(5000);
        priceSlider.setShowTickMarks(true);
        priceSlider.setMajorTickUnit(1000);
        priceLabel.setText("Up to: 5000 MAD / night");
        priceSlider.valueProperty().addListener((obs, old, val) ->
                priceLabel.setText("Up to: " + val.intValue() + " MAD / night"));

        cbRoomType.getItems().add("All Types");
        cbRoomType.setValue("All Types");

        loadRooms();
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    @FXML
    public void showBrowseTab() {
        panelBrowse.setVisible(true);   panelBrowse.setManaged(true);
        panelBookings.setVisible(false); panelBookings.setManaged(false);
        btnBrowse.getStyleClass().setAll("nav-button-active");
        btnMyBookings.getStyleClass().setAll("nav-button");
        if (headerPanelLabel != null) headerPanelLabel.setText("Browse Rooms");
    }

    @FXML
    public void showBookingsTab() {
        // If not logged in, redirect to login instead of showing empty bookings
        if (!loggedIn) {
            Navigator.goToLogin(btnMyBookings);
            return;
        }
        panelBrowse.setVisible(false);  panelBrowse.setManaged(false);
        panelBookings.setVisible(true); panelBookings.setManaged(true);
        btnMyBookings.getStyleClass().setAll("nav-button-active");
        btnBrowse.getStyleClass().setAll("nav-button");
        if (headerPanelLabel != null) headerPanelLabel.setText("My Bookings");
        loadMyBookings();
    }

    // ── Browse Rooms ──────────────────────────────────────────────────────────

    private void loadRooms() {
        spinner.setVisible(true);
        lblStatus.setText("Loading available rooms…");
        roomGrid.getChildren().clear();

        Task<List<Room>> task = new Task<>() {
            @Override protected List<Room> call() throws Exception {
                return RoomDao.findAvailable();
            }
        };
        task.setOnSucceeded(e -> {
            allRooms = task.getValue();
            spinner.setVisible(false);
            populateTypeFilter(allRooms);
            renderCards(allRooms);
            lblStatus.setText(allRooms.size() + " room(s) available");
        });
        task.setOnFailed(e -> {
            spinner.setVisible(false);
            lblStatus.setText("Failed to load rooms.");
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void populateTypeFilter(List<Room> rooms) {
        rooms.stream().map(Room::getTypeName).distinct().sorted().forEach(type -> {
            if (!cbRoomType.getItems().contains(type))
                cbRoomType.getItems().add(type);
        });
    }

    @FXML
    public void applyFilters() {
        if (allRooms == null) return;
        String selectedType = cbRoomType.getValue();
        double maxPrice = priceSlider.getValue();
        List<Room> filtered = allRooms.stream()
                .filter(r -> "All Types".equals(selectedType) || selectedType.equals(r.getTypeName()))
                .filter(r -> r.getPricePerNight() <= maxPrice)
                .collect(Collectors.toList());
        renderCards(filtered);
        lblStatus.setText(filtered.size() + " room(s) match your filters");
    }

    private void renderCards(List<Room> rooms) {
        roomGrid.getChildren().clear();
        if (rooms.isEmpty()) {
            Label empty = new Label("No rooms match your search.");
            empty.getStyleClass().add("label-muted");
            roomGrid.getChildren().add(empty);
            return;
        }
        for (Room room : rooms) roomGrid.getChildren().add(buildRoomCard(room));
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(0);
        card.getStyleClass().add("room-card");
        card.setPrefWidth(300); card.setMinWidth(260); card.setMaxWidth(360);
        card.setStyle("-fx-cursor: hand;");

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 16 16 12 16;");

        StackPane icon = new StackPane(); icon.setPrefSize(52, 52); icon.setMinSize(52, 52);
        icon.setStyle("-fx-background-color:rgba(197,160,89,0.15);-fx-background-radius:10;" +
            "-fx-border-color:rgba(197,160,89,0.35);-fx-border-radius:10;-fx-border-width:1;");
        Label iconLbl = new Label("🏨"); iconLbl.setStyle("-fx-font-size:24px;");
        icon.getChildren().add(iconLbl);

        VBox nameCol = new VBox(4); HBox.setHgrow(nameCol, Priority.ALWAYS);
        Label title = new Label("Room " + room.getRoomNumber());
        title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");
        Label typeBadge = new Label(room.getTypeName() != null ? room.getTypeName().toUpperCase() : "STANDARD");
        typeBadge.setStyle("-fx-background-color:rgba(197,160,89,0.15);-fx-text-fill:#c5a059;" +
            "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 8;-fx-background-radius:4;");
        nameCol.getChildren().addAll(title, typeBadge);

        // Status pill
        Label statusPill = new Label("AVAILABLE");
        statusPill.setStyle("-fx-background-color:rgba(30,132,73,0.20);-fx-text-fill:#1e8449;" +
            "-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:3 8;-fx-background-radius:10;");

        header.getChildren().addAll(icon, nameCol, statusPill);

        // ── Divider ───────────────────────────────────────────────────────────
        Region divider = new Region(); divider.setMaxWidth(Double.MAX_VALUE); divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color:rgba(197,160,89,0.18);");

        // ── Body ──────────────────────────────────────────────────────────────
        VBox body = new VBox(9); body.setStyle("-fx-padding:14 16 6 16;");
        body.getChildren().addAll(
            detailRow("🏢", "Floor " + room.getFloor(), "#a0a0a0"),
            detailRow("👥", "Max " + room.getMaxOccupancy() + " guests", "#a0a0a0"),
            detailRow("💴", String.format("%.2f MAD / night", room.getPricePerNight()), "#c5a059"),
            detailRow("✨", room.getAmenities() != null && !room.getAmenities().isEmpty()
                ? room.getAmenities() : "Standard amenities", "#888")
        );
        if (room.getTypeDescription() != null && !room.getTypeDescription().isEmpty())
            body.getChildren().add(detailRow("📋", room.getTypeDescription(), "#666"));

        // ── Book button ───────────────────────────────────────────────────────
        VBox footer = new VBox(); footer.setStyle("-fx-padding:12 16 16 16;");
        Button btnBook = new Button(loggedIn ? "⚔  View & Book" : "🔑  Sign In to Book");
        btnBook.getStyleClass().add("btn-primary");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> {
            if (!loggedIn) Navigator.goToLogin(btnBook);
            else           openRoomDetail(room);
        });
        footer.getChildren().add(btnBook);

        card.getChildren().addAll(header, divider, body, footer);
        return card;
    }

    // ── Row helpers (mirror AdminController) ─────────────────────────────────

    private HBox detailRow(String icon, String text, String color) {
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:13px; -fx-min-width:20;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + color + "; -fx-wrap-text:true;");
        HBox row  = new HBox(8, ico, lbl); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void openRoomDetail(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/guest/room_detail.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/samurai.css").toExternalForm());

            ma.ensa.khouribga.smartstay.guest.RoomDetailController ctrl = loader.getController();
            ctrl.initData(room, dpCheckIn.getValue(), dpCheckOut.getValue());

            Stage dialog = new Stage();
            dialog.setTitle("Room " + room.getRoomNumber() + " — Details");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            loadRooms();
        } catch (IOException ex) {
            showError("Could not open room details: " + ex.getMessage());
        }
    }

    // ── My Bookings ───────────────────────────────────────────────────────────

    @FXML
    public void loadMyBookings() {
        if (bookingsGrid == null || !loggedIn) return;
        bookingsGrid.getChildren().clear();
        Label loading = new Label("Loading your reservations…");
        loading.getStyleClass().add("label-muted");
        bookingsGrid.getChildren().add(loading);

        int userId = (int) SessionManager.getCurrentUser().getId();
        new Thread(() -> {
            try {
                List<Reservation> list = ReservationDao.findByGuest(userId);
                Platform.runLater(() -> renderBookingCards(list));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    bookingsGrid.getChildren().clear();
                    Label err = new Label("Could not load reservations.");
                    err.getStyleClass().add("label-muted");
                    bookingsGrid.getChildren().add(err);
                });
            }
        }).start();
    }

    private void renderBookingCards(List<Reservation> list) {
        bookingsGrid.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("No reservations yet. Browse rooms to make your first booking!");
            empty.getStyleClass().add("label-muted"); empty.setWrapText(true);
            bookingsGrid.getChildren().add(empty);
            return;
        }
        for (Reservation res : list) {
            VBox card = new VBox(0);
            card.getStyleClass().add("data-card");
            card.setPrefWidth(320); card.setMinWidth(280); card.setMaxWidth(380);

            // ── Header ─────────────────────────────────────────────────────────
            HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-padding: 16 16 12 16;");

            String code = res.getReservationCode();
            String initials = code != null && code.length() >= 2 ? code.substring(0, 2).toUpperCase() : "RV";
            StackPane avatar = new StackPane(); avatar.setPrefSize(48, 48); avatar.setMinSize(48, 48);
            avatar.getStyleClass().add("avatar-circle");
            Label avLbl = new Label(initials); avLbl.getStyleClass().add("avatar-initials"); avLbl.setStyle("-fx-font-size:15px;");
            avatar.getChildren().add(avLbl);

            VBox nameCol = new VBox(4); HBox.setHgrow(nameCol, Priority.ALWAYS);
            Label lblCode = new Label(res.getReservationCode());
            lblCode.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");
            Label lblRoom = new Label("Room " + res.getRoomNumber()
                + (res.getRoomTypeName() != null ? "  ·  " + res.getRoomTypeName() : ""));
            lblRoom.setStyle("-fx-font-size:11px;-fx-text-fill:#c5a059;");
            nameCol.getChildren().addAll(lblCode, lblRoom);
            header.getChildren().addAll(avatar, nameCol, createBadge(res.getStatus().toString()));

            // ── Divider ────────────────────────────────────────────────────────
            Region divider = new Region(); divider.setMaxWidth(Double.MAX_VALUE); divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color:rgba(197,160,89,0.18);");

            // ── Body ───────────────────────────────────────────────────────────
            VBox body = new VBox(9); body.setStyle("-fx-padding:14 16 16 16;");
            String checkIn  = res.getCheckInDate()  != null ? res.getCheckInDate().format(DATE_FMT)  : "?";
            String checkOut = res.getCheckOutDate() != null ? res.getCheckOutDate().format(DATE_FMT) : "?";
            body.getChildren().addAll(
                detailRow("📅", checkIn + "  →  " + checkOut, "#a0a0a0")
            );
            if (res.getCheckInDate() != null && res.getCheckOutDate() != null) {
                long nights = res.getCheckOutDate().toEpochDay() - res.getCheckInDate().toEpochDay();
                double total = nights * res.getPricePerNight();
                body.getChildren().addAll(
                    detailRow("🌙", nights + " night" + (nights == 1 ? "" : "s"), "#888"),
                    detailRow("💴", String.format("%.2f MAD", total), "#c5a059")
                );
            }
            card.getChildren().addAll(header, divider, body);
            bookingsGrid.getChildren().add(card);
        }
    }

    private Label createBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("badge");
        switch (status) {
            case "CONFIRMED"   -> badge.getStyleClass().add("badge-cleaning");
            case "CHECKED_IN"  -> badge.getStyleClass().add("badge-available");
            case "CHECKED_OUT" -> badge.getStyleClass().add("badge-available");
            case "CANCELLED"   -> badge.getStyleClass().add("badge-occupied");
            default            -> badge.getStyleClass().add("badge-maintenance");
        }
        return badge;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** "Sign In" button — goes to login page. */
    @FXML
    public void handleLogin() {
        Navigator.goToLogin(btnBrowse);
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        Navigator.goToLanding(btnLogout);
    }

    @FXML
    public void goToProfile() {
        Navigator.navigateTo(btnLogout, Navigator.CLIENT_PROFILE);
    }

    @FXML
    public void handleThemeToggle() {
        ThemeManager.toggle();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}