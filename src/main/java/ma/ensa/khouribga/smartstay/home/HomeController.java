package ma.ensa.khouribga.smartstay.home;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for home.fxml — the client-facing room browser.
 *
 * Layout:
 *  Sidebar (nav) | Header bar | Content:
 *      filter bar → FlowPane of .room-card tiles
 *
 * Threading: all DB calls on background Task, UI updates via Platform.runLater.
 */
public class HomeController implements Initializable {

    // ── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Button btnLogout;

    // ── Filters ──────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbRoomType;
    @FXML private Slider priceSlider;
    @FXML private Label priceLabel;
    @FXML private DatePicker dpCheckIn;
    @FXML private DatePicker dpCheckOut;
    @FXML private Button btnSearch;

    // ── Room grid ────────────────────────────────────────────────────────────
    @FXML private FlowPane roomGrid;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator spinner;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Room> allRooms;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Greet the logged-in user
        String name = SessionManager.getInstance().getCurrentUser().getUsername();
        welcomeLabel.setText("Welcome back, " + name);

        // Price slider wiring
        priceSlider.setMin(0);
        priceSlider.setMax(5000);
        priceSlider.setValue(5000);
        priceSlider.setShowTickMarks(true);
        priceSlider.setMajorTickUnit(1000);
        priceLabel.setText("Up to: " + (int) priceSlider.getValue() + " MAD / night");
        priceSlider.valueProperty().addListener((obs, old, val) ->
                priceLabel.setText("Up to: " + val.intValue() + " MAD / night"));

        // Room type filter — populated after rooms load
        cbRoomType.getItems().add("All Types");
        cbRoomType.setValue("All Types");

        // Wire search button
        btnSearch.setOnAction(e -> applyFilters());

        // Initial load
        loadRooms();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadRooms() {
        spinner.setVisible(true);
        lblStatus.setText("Loading available rooms…");
        roomGrid.getChildren().clear();

        Task<List<Room>> task = new Task<>() {
            @Override
            protected List<Room> call() {
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
            lblStatus.setText("Failed to load rooms. Please try again.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void populateTypeFilter(List<Room> rooms) {
        rooms.stream()
                .map(Room::getRoomTypeName)
                .distinct()
                .sorted()
                .forEach(type -> {
                    if (!cbRoomType.getItems().contains(type))
                        cbRoomType.getItems().add(type);
                });
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @FXML
    private void applyFilters() {
        if (allRooms == null) return;

        String selectedType = cbRoomType.getValue();
        double maxPrice = priceSlider.getValue();

        List<Room> filtered = allRooms.stream()
                .filter(r -> "All Types".equals(selectedType) || selectedType.equals(r.getRoomTypeName()))
                .filter(r -> r.getPricePerNight() != null
                        && r.getPricePerNight().doubleValue() <= maxPrice)
                .collect(Collectors.toList());

        renderCards(filtered);
        lblStatus.setText(filtered.size() + " room(s) match your filters");
    }

    // ── Card rendering ────────────────────────────────────────────────────────

    private void renderCards(List<Room> rooms) {
        roomGrid.getChildren().clear();

        if (rooms.isEmpty()) {
            Label empty = new Label("No rooms match your search.");
            empty.getStyleClass().add("label-muted");
            roomGrid.getChildren().add(empty);
            return;
        }

        for (Room room : rooms) {
            VBox card = buildRoomCard(room);
            roomGrid.getChildren().add(card);
        }
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(8);
        card.getStyleClass().add("room-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(16));

        Label title = new Label("Room " + room.getRoomNumber());
        title.getStyleClass().add("room-card-title");

        Label type = new Label(room.getRoomTypeName());
        type.getStyleClass().add("room-card-detail");

        Label floor = new Label("Floor " + room.getFloor());
        floor.getStyleClass().add("room-card-detail");

        Label occupancy = new Label("Max " + room.getMaxOccupancy() + " guests");
        occupancy.getStyleClass().add("room-card-detail");

        Label price = new Label(room.getPricePerNight().toPlainString() + " MAD / night");
        price.getStyleClass().add("room-card-price");

        Button btnBook = new Button("View & Book");
        btnBook.getStyleClass().add("btn-primary");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> openRoomDetail(room));

        card.getChildren().addAll(title, type, floor, occupancy, price, btnBook);
        return card;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openRoomDetail(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/guest/room_detail.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/samurai.css").toExternalForm());

            RoomDetailController ctrl = loader.getController();
            ctrl.initData(room,
                    dpCheckIn.getValue(),
                    dpCheckOut.getValue());

            Stage dialog = new Stage();
            dialog.setTitle("Room " + room.getRoomNumber() + " — Details");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            // Refresh grid after booking (room may now be unavailable)
            loadRooms();

        } catch (IOException ex) {
            showError("Could not open room details: " + ex.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        Stage stage = (Stage) btnLogout.getScene().getWindow();
        Navigator.navigateTo("/fxml/auth/login.fxml", stage);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}