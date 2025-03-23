package sfu.cmpt371.group7.game.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/*
 * this class is responsible for displaying the results of the game to the user.
 * the winning team will be displayed on the screen. along with the players name.
 */
public class Results {
    private final Stage stage;
    private final String winningTeam;

    public Results(Stage stage, String winningTeam) {
        this.stage = stage;
        this.winningTeam = winningTeam;
    }

    public void showResults() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;");

        Text titleText = new Text("GAME OVER");
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleText.setFill(Color.WHITE);

        Text winnerText = new Text(winningTeam.toUpperCase() + " TEAM WINS!");
        winnerText.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        winnerText.setFill(winningTeam.equalsIgnoreCase("red") ? Color.RED : Color.BLUE);

        Button exitButton = new Button("Exit Game");
        exitButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-size: 16px;");
        exitButton.setPrefWidth(150);
        exitButton.setPrefHeight(40);
        exitButton.setOnAction(e -> System.exit(0));

        VBox centerBox = new VBox(30);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(titleText, winnerText, exitButton);

        root.setCenter(centerBox);

        Scene scene = new Scene(root, 800, 800);
        stage.setScene(scene);
        stage.setTitle("Game Results");

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        root.setOpacity(0);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
}