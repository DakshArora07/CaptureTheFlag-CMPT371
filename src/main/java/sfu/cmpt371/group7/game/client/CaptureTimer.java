package sfu.cmpt371.group7.game.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class CaptureTimer extends StackPane {
    private int seconds = 10;
    private final Label label;

    public CaptureTimer() {

        Circle circle = new Circle(40);
        circle.setFill(Color.LIGHTGRAY);
        circle.setStroke(Color.BLACK);


        label = new Label(seconds + "");
        label.setFont(Font.font("Arial", 30));
        label.setStyle("-fx-font-weight: bold;");


        this.getChildren().addAll(circle, label);
    }

    public void start() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            seconds--;
            if (seconds > 0) {
                label.setText(seconds + "");
            } else {
                this.getChildren().clear();
            }
        }));

        timeline.setCycleCount(10);
        timeline.play();
    }

    public void reset() {
        this.seconds = 10;
        label.setText(seconds + "");
    }

    public void hide() {
        this.getChildren().clear();
    }
}
