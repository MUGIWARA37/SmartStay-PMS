package ma.ensa.khouribga.smartstay.util;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class SidebarToggleUtil {

    private static final String KEY_EXPANDED_WIDTH = "sidebar.expandedWidth";
    private static final Duration TOGGLE_DURATION = Duration.millis(260);

    private SidebarToggleUtil() {}

    public static void initialize(VBox sidebar, Button toggleButton) {
        if (sidebar == null) return;
        double expandedWidth = resolveExpandedWidth(sidebar);
        sidebar.setMinWidth(expandedWidth);
        sidebar.setPrefWidth(expandedWidth);
        sidebar.setMaxWidth(expandedWidth);
        sidebar.setTranslateX(0);
        sidebar.setOpacity(1);
        updateButton(toggleButton, false);
    }

    public static boolean toggle(VBox sidebar, Button toggleButton, boolean collapsed) {
        if (sidebar == null) return collapsed;

        double expandedWidth = resolveExpandedWidth(sidebar);
        double currentWidth = sidebar.getPrefWidth() > 0
            ? sidebar.getPrefWidth()
            : (collapsed ? 0 : expandedWidth);
        double endWidth = collapsed ? expandedWidth : 0;
        double endTranslate = collapsed ? 0 : -expandedWidth;
        double endOpacity = collapsed ? 1 : 0.94;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sidebar.minWidthProperty(), currentWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.prefWidthProperty(), currentWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.maxWidthProperty(), currentWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.translateXProperty(), sidebar.getTranslateX(), Interpolator.EASE_BOTH),
                new KeyValue(sidebar.opacityProperty(), sidebar.getOpacity(), Interpolator.EASE_BOTH)
            ),
            new KeyFrame(TOGGLE_DURATION,
                new KeyValue(sidebar.minWidthProperty(), endWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.prefWidthProperty(), endWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.maxWidthProperty(), endWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.translateXProperty(), endTranslate, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.opacityProperty(), endOpacity, Interpolator.EASE_BOTH)
            )
        );
        timeline.play();

        boolean nextCollapsed = !collapsed;
        updateButton(toggleButton, nextCollapsed);
        return nextCollapsed;
    }

    private static double resolveExpandedWidth(VBox sidebar) {
        Object existing = sidebar.getProperties().get(KEY_EXPANDED_WIDTH);
        if (existing instanceof Number n && n.doubleValue() > 0) {
            return n.doubleValue();
        }
        double width = sidebar.getPrefWidth();
        if (width <= 0 || Double.isNaN(width)) width = sidebar.prefWidth(-1);
        if (width <= 0 || Double.isNaN(width)) width = 300;
        sidebar.getProperties().put(KEY_EXPANDED_WIDTH, width);
        return width;
    }

    private static void updateButton(Button toggleButton, boolean collapsed) {
        if (toggleButton == null) return;
        toggleButton.setText(collapsed ? "⇥" : "⇤");
    }
}
