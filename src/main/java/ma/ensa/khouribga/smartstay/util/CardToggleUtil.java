package ma.ensa.khouribga.smartstay.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class CardToggleUtil {

    private CardToggleUtil() {}

    public static void toggle(VBox body, Label arrow) {
        boolean opening = !body.isVisible();
        if (opening) {
            body.setVisible(true);
            body.setManaged(true);
            body.setOpacity(0);
            body.setPrefHeight(0);
            Platform.runLater(() -> {
                double target = body.prefHeight(-1);
                if (target <= 0) target = 200;
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(body.prefHeightProperty(), 0),
                        new KeyValue(body.opacityProperty(), 0)),
                    new KeyFrame(Duration.millis(260),
                        new KeyValue(body.prefHeightProperty(), target),
                        new KeyValue(body.opacityProperty(), 1.0))
                );
                tl.setOnFinished(ev -> body.setPrefHeight(Region.USE_COMPUTED_SIZE));
                tl.play();
            });
            arrow.setText("▾");
        } else {
            double current = body.getHeight();
            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(body.prefHeightProperty(), current),
                    new KeyValue(body.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(220),
                    new KeyValue(body.prefHeightProperty(), 0),
                    new KeyValue(body.opacityProperty(), 0))
            );
            tl.setOnFinished(ev -> {
                body.setVisible(false);
                body.setManaged(false);
                body.setPrefHeight(Region.USE_COMPUTED_SIZE);
            });
            tl.play();
            arrow.setText("▸");
        }
    }
}