package ma.ensa.khouribga.smartstay;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class Navigator {

    // ── FXML route constants ──────────────────────────────────────────────────
    public static final String LANDING      = "/fxml/landing.fxml";   // ← NEW: welcome screen
    public static final String LOGIN        = "/fxml/auth/login.fxml";
    public static final String HOME         = "/fxml/home/home.fxml";
    public static final String ROOM_DETAIL  = "/fxml/guest/room_detail.fxml";
    public static final String PAYMENT      = "/fxml/guest/payment.fxml";
    public static final String CLEANING     = "/fxml/staff/cleaning.fxml";
    public static final String MAINTENANCE  = "/fxml/staff/maintenance.fxml";
    public static final String RECEPTION    = "/fxml/staff/reception.fxml";
    public static final String ADMIN        = "/fxml/admin/admin.fxml";
    public static final String STAFF_PROFILE  = "/fxml/profile/staff_profile.fxml";
    public static final String CLIENT_PROFILE = "/fxml/profile/client_profile.fxml";
    public static final String ADMIN_PROFILE  = "/fxml/profile/admin_profile.fxml";

    // ── Core navigation ───────────────────────────────────────────────────────

    public static void navigateTo(Node sourceNode, String fxmlPath) {
        navigateTo(sourceNode, fxmlPath, null);
    }

    public static void navigateTo(Node sourceNode, String fxmlPath,
                                  Consumer<Object> controllerConsumer) {
        try {
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            loadIntoStage(stage, fxmlPath, controllerConsumer);
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    public static void navigateOnStage(Stage stage, String fxmlPath) {
        try {
            loadIntoStage(stage, fxmlPath, null);
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    // ── Convenience shortcuts ─────────────────────────────────────────────────

    /** Go back to the landing/home page (e.g. on logout). */
    public static void goToLanding(Node sourceNode) {
        navigateTo(sourceNode, LANDING);
    }

    /** Redirect to the login screen. */
    public static void goToLogin(Node sourceNode) {
        navigateTo(sourceNode, LOGIN);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private static void loadIntoStage(Stage stage, String fxmlPath,
                                      Consumer<Object> controllerConsumer) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
        Parent root = loader.load();

        if (controllerConsumer != null) {
            controllerConsumer.accept(loader.getController());
        }

        Scene currentScene = stage.getScene();
        double width  = (currentScene != null) ? currentScene.getWidth()  : 1100;
        double height = (currentScene != null) ? currentScene.getHeight() : 700;

        Scene newScene = new Scene(root, width, height);
        ThemeManager.applyToScene(newScene);
        stage.setScene(newScene);
        stage.show();
    }
}
