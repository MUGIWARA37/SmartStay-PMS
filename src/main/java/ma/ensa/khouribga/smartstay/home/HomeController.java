package ma.ensa.khouribga.smartstay.home;
import ma.ensa.khouribga.smartstay.ThemeManager;

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

    @FXML private Label welcomeLabel;
    @FXML private Label lblSubtitle;
    @FXML private Button btnLogout;
    @FXML private Button btnBrowse;
    @FXML private Button btnMyBookings;
    @FXML private ComboBox<String> cbRoomType;
    @FXML private Slider priceSlider;
    @FXML private Label priceLabel;
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Button btnSearch;
    @FXML private FlowPane roomGrid;
    @FXML private FlowPane bookingsGrid;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator spinner;
    @FXML private VBox panelBrowse;
    @FXML private VBox panelBookings;

    private List<Room> allRooms;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String name = SessionManager.getCurrentUser().getUsername();
        welcomeLabel.setText("Welcome back, " + name);

        priceSlider.setMin(0);
        priceSlider.setMax(5000);
        priceSlider.setValue(5000);
        priceSlider.setShowTickMarks(true);
        priceSlider.setMajorTickUnit(1000);
        priceLabel.setText("Up to: " + (int) priceSlider.getValue() + " MAD / night");
        priceSlider.valueProperty().addListener((obs, old, val) ->
                priceLabel.setText("Up to: " + val.intValue() + " MAD / night"));

        cbRoomType.getItems().add("All Types");
        cbRoomType.setValue("All Types");

        btnSearch.setOnAction(e -> applyFilters());

        loadRooms();
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    @FXML
    private void showBrowseTab() {
        panelBrowse.setVisible(true);
        panelBrowse.setManaged(true);
        panelBookings.setVisible(false);
        panelBookings.setManaged(false);
        btnBrowse.getStyleClass().setAll("nav-button-active");
        btnMyBookings.getStyleClass().setAll("nav-button");
        if (lblSubtitle != null) lblSubtitle.setText("Find and book your perfect room");
    }

    @FXML
    private void showBookingsTab() {
        panelBrowse.setVisible(false);
        panelBrowse.setManaged(false);
        panelBookings.setVisible(true);
        panelBookings.setManaged(true);
        btnMyBookings.getStyleClass().setAll("nav-button-active");
        btnBrowse.getStyleClass().setAll("nav-button");
        if (lblSubtitle != null) lblSubtitle.setText("Your reservation history");
        loadMyBookings();
    }

    // ── Browse Rooms ──────────────────────────────────────────────────────────

    private void loadRooms() {
        spinner.setVisible(true);
        lblStatus.setText("Loading available rooms…");
        roomGrid.getChildren().clear();

        Task<List<Room>> task = new Task<>() {
            @Override protected List<Room> call() throws Exception { return RoomDao.findAvailable(); }
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
            lblStatus.setText("Failed to load rooms. Please try again.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void populateTypeFilter(List<Room> rooms) {
        rooms.stream()
                .map(Room::getTypeName)
                .distinct()
                .sorted()
                .forEach(type -> {
                    if (!cbRoomType.getItems().contains(type))
                        cbRoomType.getItems().add(type);
                });
    }

    @FXML
    private void applyFilters() {
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

        for (Room room : rooms) {
            roomGrid.getChildren().add(buildRoomCard(room));
        }
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(8);
        card.getStyleClass().add("room-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(16));

        Label title = new Label("Room " + room.getRoomNumber());
        title.getStyleClass().add("room-card-title");

        Label type = new Label(room.getTypeName());
        type.getStyleClass().add("room-card-detail");

        Label floor = new Label("Floor " + room.getFloor());
        floor.getStyleClass().add("room-card-detail");

        Label occupancy = new Label("Max " + room.getMaxOccupancy() + " guests");
        occupancy.getStyleClass().add("room-card-detail");

        Label price = new Label(String.format("%.2f MAD / night", room.getPricePerNight()));
        price.getStyleClass().add("room-card-price");

        Button btnBook = new Button("View & Book");
        btnBook.getStyleClass().add("btn-primary");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> openRoomDetail(room));

        card.getChildren().addAll(title, type, floor, occupancy, price, btnBook);
        return card;
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
        if (bookingsGrid == null) return;
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
            Label empty = new Label("You have no reservations yet. Browse rooms to make your first booking!");
            empty.getStyleClass().add("label-muted");
            empty.setWrapText(true);
            bookingsGrid.getChildren().add(empty);
            return;
        }

        for (Reservation res : list) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");
            card.setPrefWidth(300);

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblCode = new Label(res.getReservationCode());
            lblCode.getStyleClass().add("card-header-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(res.getStatus().toString());
            header.getChildren().addAll(lblCode, spacer, badge);

            Label lblRoom = new Label("🏯  Room " + res.getRoomNumber() + "  ·  " + res.getRoomTypeName());
            lblRoom.getStyleClass().add("card-detail-text");

            String dates = (res.getCheckInDate() != null ? res.getCheckInDate().format(DATE_FMT) : "?")
                    + "  →  "
                    + (res.getCheckOutDate() != null ? res.getCheckOutDate().format(DATE_FMT) : "?");
            Label lblDates = new Label("📅  " + dates);
            lblDates.getStyleClass().add("card-detail-text");

            if (res.getCheckInDate() != null && res.getCheckOutDate() != null) {
                long nights = res.getCheckOutDate().toEpochDay() - res.getCheckInDate().toEpochDay();
                double total = nights * res.getPricePerNight();
                Label lblTotal = new Label(String.format("💴  %.2f MAD  (%d night%s)", total, nights, nights == 1 ? "" : "s"));
                lblTotal.getStyleClass().add("card-detail-text");
                lblTotal.setStyle("-fx-text-fill: #c5a059; -fx-font-weight: bold;");
                card.getChildren().addAll(header, new Separator(), lblRoom, lblDates, lblTotal);
            } else {
                card.getChildren().addAll(header, new Separator(), lblRoom, lblDates);
            }

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

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Navigator.goToLogin(btnLogout);
    }

    @FXML
    private void goToProfile() {
        Navigator.navigateTo(btnLogout, Navigator.CLIENT_PROFILE);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    public void handleThemeToggle() {
        ma.ensa.khouribga.smartstay.ThemeManager.toggle();
    }

}