package ma.ensa.khouribga.smartstay.staff;
import ma.ensa.khouribga.smartstay.ThemeManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.CleaningDao;
import ma.ensa.khouribga.smartstay.model.CleaningRequest;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CleaningController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<CleaningRequest> taskTable;
    @FXML private TableColumn<CleaningRequest, String> colRoom;
    @FXML private TableColumn<CleaningRequest, CleaningRequest.Priority> colPriority;
    @FXML private TableColumn<CleaningRequest, CleaningRequest.Status> colStatus;
    @FXML private TableColumn<CleaningRequest, String> colNote;
    @FXML private TableColumn<CleaningRequest, LocalDateTime> colCreated;
    @FXML private TableColumn<CleaningRequest, LocalDateTime> colCompleted;

    private int staffProfileId = -1;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    @FXML
    public void initialize() {
        try { SessionManager.requireRole(User.Role.STAFF); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText(user.getUsername());

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "NEW", "ASSIGNED", "IN_PROGRESS", "DONE", "CANCELLED"));
        statusFilter.setValue("ALL");

        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("requestNote"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCompleted.setCellValueFactory(new PropertyValueFactory<>("completedAt"));

        formatDateTimeCol(colCreated);
        formatDateTimeCol(colCompleted);

        new Thread(() -> {
            try {
                staffProfileId = CleaningDao.findStaffProfileId((int) user.getId());
                loadTasks();
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "cleaning-init").start();
    }

    private <T> void formatDateTimeCol(TableColumn<CleaningRequest, LocalDateTime> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : v.format(DT_FMT));
            }
        });
    }

    @FXML public void loadTasks() {
        new Thread(() -> {
            try {
                List<CleaningRequest> all = staffProfileId > 0
                        ? CleaningDao.findByStaff(staffProfileId)
                        : CleaningDao.findAll();
                Platform.runLater(() -> taskTable.setItems(FXCollections.observableArrayList(all)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "cleaning-load").start();
    }

    @FXML public void applyFilter() {
        String sel = statusFilter.getValue();
        if (sel == null || sel.equals("ALL")) { loadTasks(); return; }
        new Thread(() -> {
            try {
                List<CleaningRequest> filtered =
                        CleaningDao.findByStatus(CleaningRequest.Status.valueOf(sel));
                Platform.runLater(() -> taskTable.setItems(FXCollections.observableArrayList(filtered)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "cleaning-filter").start();
    }

    @FXML public void markInProgress() { updateSelectedStatus(CleaningRequest.Status.IN_PROGRESS); }
    @FXML public void markDone()       { updateSelectedStatus(CleaningRequest.Status.DONE); }

    private void updateSelectedStatus(CleaningRequest.Status newStatus) {
        CleaningRequest sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a task first."); return; }
        new Thread(() -> {
            try {
                CleaningDao.updateStatus(sel.getId(), newStatus);
                Platform.runLater(this::loadTasks);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert("Update failed: " + ex.getMessage()));
            }
        }, "cleaning-update").start();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        Navigator.goToLogin(welcomeLabel);
    }
    @FXML public void goToProfile() {
        Navigator.navigateTo(welcomeLabel, Navigator.STAFF_PROFILE,
            ctrl -> ((ma.ensa.khouribga.smartstay.profile.StaffProfileController) ctrl)
                        .setPreviousRoute(Navigator.CLEANING));
    }

    @FXML
    public void handleThemeToggle() {
        ma.ensa.khouribga.smartstay.ThemeManager.toggle();
    }

}