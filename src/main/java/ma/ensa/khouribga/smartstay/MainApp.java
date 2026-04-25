package ma.ensa.khouribga.smartstay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    private static final String APP_TITLE = "SmartStay PMS - Khouribga Edition";
    private static final String LOGIN_FXML = "/fxml/auth/login.fxml";
    
    // Adjusted default dimensions for better aspect ratio with video
    private static final double APP_WIDTH = 1100;
    private static final double APP_HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        try {
            // 1. Locate FXML
            URL loginResource = getClass().getResource(LOGIN_FXML);
            if (loginResource == null) {
                System.err.println("CRITICAL: FXML file not found at " + LOGIN_FXML);
                return;
            }

            // 2. Load the UI
            FXMLLoader loader = new FXMLLoader(loginResource);
            Parent root = loader.load();

            // 3. Create Scene
            Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);

            // 4. Configure Stage
            stage.setTitle(APP_TITLE);
            
            // Set minimum constraints to prevent UI breaking on tiny windows
            stage.setMinWidth(1000);
            stage.setMinHeight(650);

            stage.setScene(scene);
            
            // Optional: Start in maximized mode to show off the video background
            stage.setMaximized(true); 

            // 5. Reveal the Dojo
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            System.err.println("Failed to launch SmartStay: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Launches via the Main.java wrapper we created earlier
        launch(args);
    }
}