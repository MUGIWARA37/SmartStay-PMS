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
    public static final String LOGIN       = "/fxml/auth/login.fxml";
    public static final String HOME        = "/fxml/home/home.fxml";
    public static final String ROOM_DETAIL = "/fxml/guest/room_detail.fxml";
    public static final String PAYMENT     = "/fxml/guest/payment.fxml";
    public static final String CLEANING    = "/fxml/staff/cleaning.fxml";
    public static final String MAINTENANCE = "/fxml/staff/maintenance.fxml";
    public static final String RECEPTION   = "/fxml/staff/reception.fxml";
    public static final String ADMIN       = "/fxml/admin/admin.fxml";

    // ── Core navigation (replaces current scene content) ─────────────────────

    /**
     * Navigate to a new screen by replacing the current scene.
     *
     * @param sourceNode any @FXML node in the current controller (used to get the Stage)
     * @param fxmlPath   one of the route constants above
     */
    public static void navigateTo(Node sourceNode, String fxmlPath) {
        navigateTo(sourceNode, fxmlPath, null);
    }

    /**
     * Navigate and pass data to the destination controller before it is shown.
     *
     * @param sourceNode    any @FXML node in the current controller
     * @param fxmlPath      one of the route constants above
     * @param controllerConsumer lambda that receives the destination controller, e.g.:
     *                           {@code ctrl -> ((RoomDetailController) ctrl).setRoom(room)}
     */
    public static void navigateTo(Node sourceNode, String fxmlPath,
                                  Consumer<Object> controllerConsumer) {
        try {
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            loadIntoStage(stage, fxmlPath, controllerConsumer);
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    /**
     * Navigate directly from a {@link Stage} reference (used in MainApp).
     */
    public static void navigateOnStage(Stage stage, String fxmlPath) {
        try {
            loadIntoStage(stage, fxmlPath, null);
        } catch (IOException e) {
            throw new RuntimeException("Navigation failed: " + fxmlPath, e);
        }
    }

    // ── Convenience shortcuts ─────────────────────────────────────────────────

    /** Redirect to the login screen (e.g. on session expiry or unauthorized access). */
    public static void goToLogin(Node sourceNode) {
        navigateTo(sourceNode, LOGIN);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private static void loadIntoStage(Stage stage, String fxmlPath,
                                      Consumer<Object> controllerConsumer) throws IOException {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
        Parent root = loader.load();

        if (controllerConsumer != null) {
            Object controller = loader.getController();
            controllerConsumer.accept(controller);
        }

        // Preserve current stage dimensions
        Scene currentScene = stage.getScene();
        double width  = (currentScene != null) ? currentScene.getWidth()  : 1100;
        double height = (currentScene != null) ? currentScene.getHeight() : 700;

        Scene newScene = new Scene(root, width, height);
        stage.setScene(newScene);
        stage.show();
    }
}