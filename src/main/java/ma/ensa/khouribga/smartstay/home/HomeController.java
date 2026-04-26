package ma.ensa.khouribga.smartstay.home;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.guest.RoomDetailController;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private VBox roomsPanel;
    @FXML private VBox reservationsPanel;
    @FXML private Button btnRooms;
    @FXML private Button btnMyRes;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> adultSpinner;
    @FXML private FlowPane roomGrid;
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colRoom;
    @FXML private TableColumn<Reservation, String> colType;
    @FXML private TableColumn<Reservation, LocalDate> colCheckIn;
    @FXML private TableColumn<Reservation, LocalDate> colCheckOut;
    @FXML private TableColumn<Reservation, Long> colNights;
    @FXML private TableColumn<Reservation, Double> colTotal;
    @FXML private TableColumn<Reservation, String> colResStatus;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        try { SessionManager.requireLoggedIn(); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));

        adultSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));

        colCode.setCellValueFactory(new PropertyValueFactory<>("reservationCode"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colType.setCellValueFactory(new PropertyValueFactory<>("roomTypeName"));
        colCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        colCheckOut.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        colResStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNights.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getNights()).asObject());
        colTotal.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getBaseTotal()).asObject());

        colCheckIn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        colCheckOut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATE_FMT));
            }
        });
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f MAD", v));
            }
        });

        loadAllRooms();
    }

    @FXML public void showRooms() {
        roomsPanel.setVisible(true); roomsPanel.setManaged(true);
        reservationsPanel.setVisible(false); reservationsPanel.setManaged(false);
        btnRooms.getStyleClass().remove("nav-button-active");
        btnRooms.getStyleClass().add("nav-button-active");
        btnMyRes.getStyleClass().remove("nav-button-active");
        loadAllRooms();
    }

    @FXML public void showReservations() {
        roomsPanel.setVisible(false); roomsPanel.setManaged(false);
        reservationsPanel.setVisible(true); reservationsPanel.setManaged(true);
        btnMyRes.getStyleClass().remove("nav-button-active");
        btnMyRes.getStyleClass().add("nav-button-active");
        btnRooms.getStyleClass().remove("nav-button-active");
        loadUserReservations();
    }

    @FXML public void onSearch()  { loadAllRooms(); }
    @FXML public void onShowAll() { checkInPicker.setValue(null); checkOutPicker.setValue(null); loadAllRooms(); }

    private void loadAllRooms() {
        new Thread(() -> {
            try {
                List<Room> rooms = RoomDao.findAvailable();
                Platform.runLater(() -> populateRoomGrid(rooms));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "home-room-loader").start();
    }

    private void populateRoomGrid(List<Room> rooms) {
        roomGrid.getChildren().clear();
        if (rooms.isEmpty()) {
            Label empty = new Label("No available rooms found.");
            empty.getStyleClass().add("label-muted");
            roomGrid.getChildren().add(empty);
            return;
        }
        for (Room room : rooms) roomGrid.getChildren().add(buildRoomCard(room));
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(8);
        card.getStyleClass().add("room-card");

        Label title = new Label("Room " + room.getRoomNumber());
        title.getStyleClass().add("room-card-title");

        Label type = new Label(room.getTypeName().toUpperCase());
        type.getStyleClass().add("room-card-type");

        Region divider = new Region();
        divider.getStyleClass().add("room-card-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        HBox priceRow = new HBox(4);
        Label price = new Label(String.format("%.0f", room.getPricePerNight()));
        price.getStyleClass().add("room-card-price");
        Label unit = new Label("MAD / night");
        unit.getStyleClass().add("room-card-price-unit");
        priceRow.getChildren().addAll(price, unit);

        Label details = new Label("Floor " + room.getFloor() + "  •  Max " + room.getMaxOccupancy() + " guests");
        details.getStyleClass().add("room-card-detail");

        Label amenities = new Label(room.getAmenities() == null ? "" : room.getAmenities());
        amenities.getStyleClass().add("room-card-detail");
        amenities.setWrapText(true);

        card.getChildren().addAll(title, type, divider, priceRow, details, amenities);
        card.setOnMouseClicked(e -> Navigator.navigateTo(welcomeLabel, Navigator.ROOM_DETAIL,
                ctrl -> ((RoomDetailController) ctrl).setRoom(room)));
        return card;
    }

    @FXML public void refreshReservations() { loadUserReservations(); }

    private void loadUserReservations() {
        int userId = (int) SessionManager.getCurrentUser().getId();
        new Thread(() -> {
            try {
                ObservableList<Reservation> items =
                    FXCollections.observableArrayList(ReservationDao.findByGuest(userId));
                Platform.runLater(() -> reservationTable.setItems(items));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "home-res-loader").start();
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        Navigator.goToLogin(welcomeLabel);
    }
}