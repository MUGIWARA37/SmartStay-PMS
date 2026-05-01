package ma.ensa.khouribga.smartstay;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    private static final String APP_TITLE = "SmartStay PMS - Khouribga Edition";
    private static final String LOGIN_FXML = "/fxml/auth/login.fxml";
    
    private static final double APP_WIDTH = 1100;
    private static final double APP_HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        installGlobalExceptionHandlers();

        try {
            URL loginResource = getClass().getResource(LOGIN_FXML);
            if (loginResource == null) {
                System.err.println("CRITICAL: FXML file not found at " + LOGIN_FXML);
                return;
            }

            FXMLLoader loader = new FXMLLoader(loginResource);
            Parent root = loader.load();

            Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);

            stage.setTitle(APP_TITLE);
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            System.err.println("Failed to launch SmartStay: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Installs themed alert dialogs for any unhandled exceptions,
     * both on the JavaFX thread and background threads.
     */
    private void installGlobalExceptionHandlers() {
        // ── JavaFX thread exceptions ──────────────────────────────────────────
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) ->
                showUncaughtError(throwable));

        // ── Background thread exceptions ──────────────────────────────────────
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                Platform.runLater(() -> showUncaughtError(throwable)));
    }

    private static void showUncaughtError(Throwable t) {
        t.printStackTrace();
        try {
            String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "An unexpected error occurred:\n\n" + msg + "\n\nCheck the console for details.",
                    ButtonType.OK);
            alert.setTitle("SmartStay — System Error");
            alert.setHeaderText("⚠  Unhandled Exception");
            try {
                alert.getDialogPane().getStylesheets()
                        .add(MainApp.class.getResource("/styles/samurai.css").toExternalForm());
                alert.getDialogPane().getStyleClass().add("dialog-pane");
            } catch (Exception ignored) { /* stylesheet not critical */ }
            alert.showAndWait();
        } catch (Exception ignored) { /* do not recurse */ }
    }

    public static void main(String[] args) {
        launch(args);
    }
}