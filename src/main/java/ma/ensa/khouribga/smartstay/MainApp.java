package ma.ensa.khouribga.smartstay;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    private static final String APP_TITLE = "SmartStay PMS";
    private static final String LOGIN_FXML = "/fxml/auth/login.fxml";
    private static final double APP_WIDTH = 1000;
    private static final double APP_HEIGHT = 650;

    @Override
    public void start(Stage stage) throws Exception {
        URL loginResource = getClass().getResource(LOGIN_FXML);
        if (loginResource == null) {
            throw new IllegalStateException("FXML not found: " + LOGIN_FXML);
        }

        FXMLLoader loader = new FXMLLoader(loginResource);
        Scene scene = new Scene(loader.load(), APP_WIDTH, APP_HEIGHT);

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}