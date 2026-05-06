package ma.ensa.khouribga.smartstay.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class CardBuilder {

    private CardBuilder() {}

    public static HBox detailRow(String icon, String text, String color) {
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:13px;-fx-min-width:20;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";-fx-wrap-text:true;");
        HBox row = new HBox(8, ico, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static Label createBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("badge");
        if (status.equals("AVAILABLE") || status.equals("CHECKED_OUT") || status.equals("DONE") || status.equals("RESOLVED")) {
            badge.getStyleClass().add("badge-available");
        } else if (status.equals("CONFIRMED") || status.equals("PAID")) {
            badge.getStyleClass().add("badge-paid");
        } else if (status.equals("CHECKED_IN") || status.equals("IN_PROGRESS") || status.equals("CLEANING")) {
            badge.getStyleClass().add("badge-cleaning");
        } else if (status.equals("CANCELLED") || status.equals("MAINTENANCE") || status.equals("OCCUPIED")) {
            badge.getStyleClass().add("badge-occupied");
        } else {
            badge.getStyleClass().add("badge-pending");
        }
        return badge;
    }
}