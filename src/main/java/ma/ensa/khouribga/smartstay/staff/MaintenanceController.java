package ma.ensa.khouribga.smartstay.staff;
import ma.ensa.khouribga.smartstay.ThemeManager;
import ma.ensa.khouribga.smartstay.VideoBackground;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.media.MediaView;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import ma.ensa.khouribga.smartstay.Navigator;
import ma.ensa.khouribga.smartstay.dao.CleaningDao;
import ma.ensa.khouribga.smartstay.dao.MaintenanceDao;
import ma.ensa.khouribga.smartstay.dao.RoomDao;
import ma.ensa.khouribga.smartstay.model.MaintenanceRequest;
import ma.ensa.khouribga.smartstay.model.Room;
import ma.ensa.khouribga.smartstay.model.User;
import ma.ensa.khouribga.smartstay.session.SessionManager;
import ma.ensa.khouribga.smartstay.util.SidebarToggleUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MaintenanceController {

    @FXML private MediaView bgMediaView;
    @FXML private VBox sidebar;
    @FXML private Button btnSidebarToggle;
    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<MaintenanceRequest> taskTable;
    @FXML private TableColumn<MaintenanceRequest, String>                    colRoom;
    @FXML private TableColumn<MaintenanceRequest, String>                    colTitle;
    @FXML private TableColumn<MaintenanceRequest, MaintenanceRequest.Priority> colPriority;
    @FXML private TableColumn<MaintenanceRequest, MaintenanceRequest.Status>   colStatus;
    @FXML private TableColumn<MaintenanceRequest, String>                    colDesc;
    @FXML private TableColumn<MaintenanceRequest, LocalDateTime>             colCreated;

    // Report form
    @FXML private ComboBox<Room> roomCombo;
    @FXML private TextField titleField;
    @FXML private ComboBox<MaintenanceRequest.Priority> priorityCombo;
    @FXML private TextArea descField;
    @FXML private Label formError;

    private int staffProfileId = -1;
    private boolean sidebarCollapsed = false;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    @FXML
    public void initialize() {
        VideoBackground.register(bgMediaView);
        SidebarToggleUtil.initialize(sidebar, btnSidebarToggle);
        try { SessionManager.requireRole(User.Role.STAFF); }
        catch (Exception e) { Navigator.goToLogin(welcomeLabel); return; }

        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText(user.getUsername());

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CANCELLED"));
        statusFilter.setValue("ALL");

        priorityCombo.setItems(FXCollections.observableArrayList(MaintenanceRequest.Priority.values()));
        priorityCombo.setValue(MaintenanceRequest.Priority.MEDIUM);

        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colCreated.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "—" : v.format(DT_FMT));
            }
        });

        roomCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : "Room " + r.getRoomNumber() + " – " + r.getTypeName());
            }
        });
        roomCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Room r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "Select room" : "Room " + r.getRoomNumber());
            }
        });

        new Thread(() -> {
            try {
                staffProfileId = CleaningDao.findStaffProfileId((int) user.getId());
                List<Room> rooms = RoomDao.findAll();
                Platform.runLater(() -> {
                    roomCombo.setItems(FXCollections.observableArrayList(rooms));
                    loadTasks();
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "maint-init").start();
    }

    /**
     * FIX: Use findByStaffOrUnassigned() so reception dispatches (NULL assigned_to)
     * appear in the maintenance staff task list.
     */
    @FXML public void loadTasks() {
        new Thread(() -> {
            try {
                List<MaintenanceRequest> all = (staffProfileId > 0)
                        ? MaintenanceDao.findByStaffOrUnassigned(staffProfileId)
                        : MaintenanceDao.findAll();
                Platform.runLater(() -> taskTable.setItems(FXCollections.observableArrayList(all)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "maint-load").start();
    }

    /**
     * FIX: Status filter now also uses the OR-unassigned logic.
     */
    @FXML public void applyFilter() {
        String sel = statusFilter.getValue();
        if (sel == null || sel.equals("ALL")) { loadTasks(); return; }
        new Thread(() -> {
            try {
                MaintenanceRequest.Status status = MaintenanceRequest.Status.valueOf(sel);
                List<MaintenanceRequest> filtered = (staffProfileId > 0)
                        ? MaintenanceDao.findByStaffOrUnassignedAndStatus(staffProfileId, status)
                        : MaintenanceDao.findByStatus(status);
                Platform.runLater(() -> taskTable.setItems(FXCollections.observableArrayList(filtered)));
            } catch (Exception ex) { ex.printStackTrace(); }
        }, "maint-filter").start();
    }

    @FXML public void markInProgress() { updateStatus(MaintenanceRequest.Status.IN_PROGRESS); }
    @FXML public void markResolved()   { updateStatus(MaintenanceRequest.Status.RESOLVED); }

    private void updateStatus(MaintenanceRequest.Status newStatus) {
        MaintenanceRequest sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Select a task first."); return; }
        new Thread(() -> {
            try {
                MaintenanceDao.updateStatus(sel.getId(), newStatus);
                if (newStatus == MaintenanceRequest.Status.RESOLVED) {
                    // Re-mark room as AVAILABLE after maintenance is resolved
                    RoomDao.updateStatus(sel.getRoomId(), Room.Status.AVAILABLE);
                }
                Platform.runLater(this::loadTasks);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert("Update failed: " + ex.getMessage()));
            }
        }, "maint-update").start();
    }

    @FXML public void reportIssue() {
        formError.setText("");
        Room room = roomCombo.getValue();
        String title = titleField.getText().trim();
        if (room == null)    { formError.setText("Please select a room."); return; }
        if (title.isEmpty()) { formError.setText("Please enter an issue title."); return; }

        MaintenanceRequest req = new MaintenanceRequest();
        req.setRoomId(room.getId());
        req.setReportedByUserId((int) SessionManager.getCurrentUser().getId());
        req.setPriority(priorityCombo.getValue() != null
                ? priorityCombo.getValue() : MaintenanceRequest.Priority.MEDIUM);
        req.setTitle(title);
        req.setDescription(descField.getText().trim());
        if (staffProfileId > 0) req.setAssignedToStaffId(staffProfileId);

        new Thread(() -> {
            try {
                MaintenanceDao.create(req);
                Platform.runLater(() -> {
                    formError.setText("");
                    titleField.clear();
                    descField.clear();
                    loadTasks();
                    showAlert("Maintenance request created successfully.");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> formError.setText("Error: " + ex.getMessage()));
            }
        }, "maint-report").start();
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
                        .setPreviousRoute(Navigator.MAINTENANCE));
    }

    @FXML public void handleThemeToggle() {
        ThemeManager.toggle();
    }
    @FXML public void toggleSidebar() {
        sidebarCollapsed = SidebarToggleUtil.toggle(sidebar, btnSidebarToggle, sidebarCollapsed);
    }
}
