package ma.ensa.khouribga.smartstay.home;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ma.ensa.khouribga.smartstay.model.Room;

public class RoomCardController {

    @FXML private ImageView roomImageView;
    @FXML private Label imagePlaceholder;
    @FXML private Label lblRoomNumber;
    @FXML private Label lblRoomType;
    @FXML private Label lblStatus;

    @FXML private javafx.scene.control.Button btnDetails;

    private Runnable onAction;

    public void setOnAction(Runnable onAction) {
        this.onAction = onAction;
    }

    @FXML
    private void handleDetails() {
        if (onAction != null) onAction.run();
    }

    public void setRoomData(Room room) {
        lblRoomNumber.setText(room.getRoomNumber());
        lblRoomType.setText(room.getTypeName()); 

        String statusText = "UNKNOWN";
        if (room.getStatus() != null) {
            statusText = room.getStatus().name();
        }
        lblStatus.setText(statusText);

        lblStatus.getStyleClass().removeAll("status-available", "status-occupied", "status-cleaning", "status-maintenance");
        
        switch (statusText.toLowerCase()) {
            case "available":
                lblStatus.getStyleClass().add("status-available");
                break;
            case "occupied":
                lblStatus.getStyleClass().add("status-occupied");
                break;
            case "cleaning":
                lblStatus.getStyleClass().add("status-cleaning");
                break;
            case "maintenance":
                lblStatus.getStyleClass().add("status-maintenance");
                break;
        }

        if (room.getImagePath() != null && !room.getImagePath().isEmpty()) {
            try {
                var resource = getClass().getResource(room.getImagePath());
                if (resource != null) {
                    Image image = new Image(resource.toExternalForm(), 320, 180, false, true, true);
                    roomImageView.setImage(image);
                    imagePlaceholder.setVisible(false);
                } else {
                    roomImageView.setImage(null);
                    imagePlaceholder.setVisible(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                roomImageView.setImage(null);
                imagePlaceholder.setVisible(true);
            }
        } else {
            roomImageView.setImage(null);
            imagePlaceholder.setVisible(true);
        }
    }
}