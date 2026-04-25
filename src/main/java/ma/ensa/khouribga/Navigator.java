package ma.ensa.khouribga.smartstay;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Centralized navigation utility.
 * Eliminates the duplicated goToLogin() / scene-switching boilerplate
 * that was scattered across every controller.
 *
 * Usage:
 *   Navigator.navigateTo(anyNode, "/fxml/auth/login.fxml");
 *   Navigator.navigateTo(anyNode, "/fxml/home/home.fxml", controller -> {
 *       ((HomeController) controller).setSelectedRoom(room);
 *   });
 */
public final class Navigator {

    private Navigator() { /* utility class */ }

    // ── Default window size (matches MainApp) ─────────────────
    private static final double DEFAULT_WIDTH  = 1000;
    private static final double DEFAULT_HEIGHT = 650;

    // ── Named routes — keep in sync with your FXML paths ─────
    public static final String LOGIN        = "/fxml/auth/login.fxml";
    public static final String HOME         = "/fxml/home/home.fxml";
    public static final String ROOM_DETAIL  = "/fxml/guest/room_detail.fxml";
    public static final String PAYMENT      = "/fxml/guest/payment.fxml";
    public static final String CLEANING     = "/fxml/staff/cleaning.fxml";
    public static final String MAINTENANCE  = "/fxml/staff/maintenance.fxml";
    public static final String RECEPTION    = "/fxml/staff/reception.fxml";
    public static final String ADMIN        = "/fxml/admin/admin.fxml";

    // ── Core navigate methods ─────────────────────────────────

    /**
     * Navigate from any JavaFX node to the given FXML path.
     * The current Stage is resolved from the node's Scene.
     *
     * @param sourceNode any node currently on the stage
     * @param fxmlPath   classpath-relative FXML path (use the constants above)
     */
    public static void navigateTo(javafx.scene.Node sourceNode, String fxmlPath) {
        navigateTo(sourceNode, fxmlPath, null);
    }

    /**
     * Navigate and receive the newly loaded controller so you can
     * pass data to the next screen before it's displayed.
     *
     * @param sourceNode        any node currently on the stage
     * @param fxmlPath          classpath-relative FXML path
     * @param controllerSetup   called with the new controller right after load;
     *                          use this to pass parameters (may be null)
     */
    public static void navigateTo(javafx.scene.Node sourceNode,
                                  String fxmlPath,
                                  Consumer<Object> controllerSetup) {
        try {
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            navigateOnStage(stage, fxmlPath, controllerSetup);
        } catch (Exception e) {
            showError("Navigation failed: " + fxmlPath, e);
        }
    }

    /**
     * Navigate directly from a Stage reference (useful in MainApp).
     */
    public static void navigateOnStage(Stage stage, String fxmlPath) {
        navigateOnStage(stage, fxmlPath, null);
    }

    /**
     * Navigate directly from a Stage reference with controller setup.
     */
    public static void navigateOnStage(Stage stage,
                                       String fxmlPath,
                                       Consumer<Object> controllerSetup) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Navigator.class.getResource(fxmlPath)
            );
            Parent root = loader.load();

            if (controllerSetup != null) {
                controllerSetup.accept(loader.getController());
            }

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.show();

        } catch (IOException e) {
            showError("Cannot load FXML: " + fxmlPath, e);
        }
    }

    // ── Convenience shortcuts ─────────────────────────────────

    /** Go to the login screen from any node. */
    public static void goToLogin(javafx.scene.Node sourceNode) {
        navigateTo(sourceNode, LOGIN);
    }

    /** Go to the login screen from a Stage. */
    public static void goToLogin(Stage stage) {
        navigateOnStage(stage, LOGIN);
    }

    // ── Error helper ─────────────────────────────────────────

    private static void showError(String message, Exception e) {
        e.printStackTrace();
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Navigation Error");
        alert.setHeaderText("Could not navigate");
        alert.setContentText(message + "\n\n" + e.getMessage());
        alert.showAndWait();
    }
}