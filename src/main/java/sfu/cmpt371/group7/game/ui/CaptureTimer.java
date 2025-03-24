package sfu.cmpt371.group7.game.ui;

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
    private Label label;
    private Timeline timeline;
    private Circle circle;

    public CaptureTimer() {
        // Create a circular background for the label
        circle = new Circle(40); // radius 60 for the circle
        circle.setFill(Color.LIGHTGRAY); // Circle fill color
        circle.setStroke(Color.BLACK); // Circle border color

        // Create the label and set its initial text
        label = new Label(seconds + "");
        label.setFont(Font.font("Arial", 30)); // Set the font size
        label.setStyle("-fx-font-weight: bold;"); // Make the text bold

        // Add label and circle to the StackPane
        this.getChildren().addAll(circle, label);
    }

    public void start() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            seconds--;
            if (seconds > 0) {
                label.setText(seconds + "");
            } else {
                this.getChildren().clear(); // Remove the label when timer reaches 0
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
