package ma.ensa.khouribga.smartstay.staff;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.ReservationDao;
import ma.ensa.khouribga.smartstay.db.Database;
import ma.ensa.khouribga.smartstay.model.Reservation;
import ma.ensa.khouribga.smartstay.profile.StaffProfileController;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceptionController {

    // ── Grid Bindings (Replaced Tables) ───────────────────────────────────────
    @FXML private MediaView bgMediaView;
    @FXML private FlowPane resGrid;
    @FXML private TextField txtSearch;

    // ── Operations Bindings ───────────────────────────────────────────────────
    @FXML private TextField txtCleanRoom;

    // ── Maintenance Dispatch Bindings ─────────────────────────────────────────
    @FXML private TextField txtMaintRoom;
    @FXML private ComboBox<String> cmbMaintCategory;
    @FXML private TextArea txtMaintDesc;

    // ── Selection State ───────────────────────────────────────────────────────
    private Reservation selectedRes;
    private VBox selectedResCard;
    private List<Reservation> allReservations = List.of();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

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

    // ── Logistics & Grid Generation ───────────────────────────────────────────

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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void filterReservations() {
        String query = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            populateResGrid(allReservations);
            return;
        }
        List<Reservation> filtered = allReservations.stream()
            .filter(r ->
                r.getReservationCode().toLowerCase().contains(query) ||
                r.getGuestFullName().toLowerCase().contains(query) ||
                r.getRoomNumber().toLowerCase().contains(query))
            .toList();
        populateResGrid(filtered);
    }

    private void populateResGrid(List<Reservation> list) {
        if (resGrid == null) return;
        resGrid.getChildren().clear();
        selectedRes = null;
        selectedResCard = null;

        for (Reservation res : list) {
            VBox card = new VBox(8);
            card.getStyleClass().add("data-card");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblCode = new Label(res.getReservationCode());
            lblCode.getStyleClass().add("card-header-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = createBadge(res.getStatus().toString());
            header.getChildren().addAll(lblCode, spacer, badge);

            Label lblGuest = new Label("Guest: " + res.getGuestFullName());
            lblGuest.getStyleClass().add("card-detail-text");
            Label lblRoom = new Label("Room: " + res.getRoomNumber());
            lblRoom.getStyleClass().add("card-detail-text");
            
            // Adding Dates for the front desk
            Label lblDates = new Label(res.getCheckInDate().format(DATE_FMT) + " to " + res.getCheckOutDate().format(DATE_FMT));
            lblDates.getStyleClass().add("card-detail-text");
            lblDates.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");

            card.getChildren().addAll(header, new Separator(), lblGuest, lblRoom, lblDates);

            card.setOnMouseClicked(e -> {
                if (selectedResCard != null) selectedResCard.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                selectedResCard = card;
                selectedRes = res;
            });

            resGrid.getChildren().add(card);
        }
    }

    private Label createBadge(String statusText) {
        Label badge = new Label(statusText);
        badge.getStyleClass().add("badge");
        if (statusText.equals("AVAILABLE") || statusText.equals("CHECKED_OUT")) {
            badge.getStyleClass().add("badge-available");
        } else if (statusText.equals("CONFIRMED")) {
            badge.getStyleClass().add("badge-paid");
        } else if (statusText.equals("CHECKED_IN")) {
            badge.getStyleClass().add("badge-cleaning"); // Blue badge in our theme
        } else {
            badge.getStyleClass().add("badge-pending");
        }
        return badge;
    }

    @FXML
    public void doCheckIn() {
        if (selectedRes == null) { showAlert(Alert.AlertType.WARNING, "Select a reservation card to check-in."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_IN);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Check-in failed."));
            }
        }).start();
    }

    @FXML
    public void doCheckOut() {
        if (selectedRes == null) { showAlert(Alert.AlertType.WARNING, "Select a reservation card to check-out."); return; }
        new Thread(() -> {
            try {
                ReservationDao.updateStatus(selectedRes.getId(), selectedRes.getRoomId(), Reservation.Status.CHECKED_OUT);
                Platform.runLater(this::loadReservations);
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Check-out failed."));
            }
        }).start();
    }

    @FXML
    public void doWalkIn(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Walk-in registration initiated...");
    }

    @FXML
    public void requestCleaning() {
        String room = txtCleanRoom.getText().trim();
        if(room.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Enter a room number to dispatch cleaning."); return; }
        showAlert(Alert.AlertType.INFORMATION, "Cleaning Crew dispatched to room " + room);
        txtCleanRoom.clear();
    }

    // ── Maintenance Dispatch ──────────────────────────────────────────────────

    @FXML
    public void dispatchMaintenance() {
        String roomStr = txtMaintRoom.getText().trim();
        String category = cmbMaintCategory.getValue();
        String desc = txtMaintDesc.getText().trim();

        if (roomStr.isEmpty() || category == null || desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please fill out Room, Hazard Type, and Situation Report.");
            return;
        }

        new Thread(() -> {
            try (Connection conn = Database.getConnection()) {
                String roomSql = "SELECT id FROM rooms WHERE room_number = ?";
                long roomId = -1;
                try (PreparedStatement psRoom = conn.prepareStatement(roomSql)) {
                    psRoom.setString(1, roomStr);
                    try (ResultSet rs = psRoom.executeQuery()) {
                        if (rs.next()) roomId = rs.getLong("id");
                    }
                }

                if (roomId == -1) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Room " + roomStr + " does not exist in the compound."));
                    return;
                }

                String title = "[" + category + "] Reception Dispatch";
                String insertSql = "INSERT INTO maintenance_requests (room_id, title, priority, status, description) VALUES (?, ?, 'HIGH', 'PENDING', ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setLong(1, roomId);
                    psInsert.setString(2, title);
                    psInsert.setString(3, desc);
                    psInsert.executeUpdate();
                }

                String updateRoomSql = "UPDATE rooms SET status = 'MAINTENANCE' WHERE id = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateRoomSql)) {
                    psUpdate.setLong(1, roomId);
                    psUpdate.executeUpdate();
                }

                Platform.runLater(() -> {
                    txtMaintRoom.clear();
                    txtMaintDesc.clear();
                    cmbMaintCategory.setValue(null);
                    showAlert(Alert.AlertType.INFORMATION, "Crew dispatched to Room " + roomStr + ". Room locked to MAINTENANCE.");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database transmission failed."));
            }
        }).start();
    }

    // ── Navigation & System ───────────────────────────────────────────────────

    @FXML
    public void goToProfile(MouseEvent event) {
        Navigator.navigateTo((Node) event.getSource(), Navigator.STAFF_PROFILE, ctrl -> {
            ((StaffProfileController) ctrl).setPreviousRoute(Navigator.RECEPTION);
        });
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.logout();
        Navigator.goToLogin((Node) event.getSource());
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/samurai.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML
    public void handleThemeToggle() {
        ma.ensa.khouribga.smartstay.ThemeManager.toggle();
    }

}