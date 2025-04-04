package sfu.cmpt371.group7.game.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * This class displays the results of the game.
 */
public class Results {
    private Stage stage;
    private String winningTeam;

    public Results(Stage stage, String winningTeam) {
        this.stage = stage;
        this.winningTeam = winningTeam;
    }

    /**
     * Show the results window
     */
    public void showResults() {
        // Create title label
        Label titleLabel = new Label("Game Over");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        // Create winner label
        Label winnerLabel = new Label(getWinnerText());
        winnerLabel.setFont(Font.font("Arial", 18));
        winnerLabel.setTextFill(getWinnerColor());

        // Create exit button
        Button exitButton = new Button("Exit Game");
        exitButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        exitButton.setOnAction(e -> System.exit(0));

        // Create layout
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(titleLabel, winnerLabel, exitButton);

        // Create scene
        Scene scene = new Scene(layout, 300, 200);

        // Configure stage
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Game Results");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Get the text to display for the winner
     */
    private String getWinnerText() {
        if (winningTeam == null || winningTeam.isEmpty() || winningTeam.equalsIgnoreCase("tie")) {
            return "It's a tie!";
        } else {
            return winningTeam.toUpperCase() + " team wins!";
        }
    }

    /**
     * Get the color to use for the winner text
     */
    private Color getWinnerColor() {
        if (winningTeam == null || winningTeam.isEmpty() || winningTeam.equalsIgnoreCase("tie")) {
            return Color.BLACK;
        } else if (winningTeam.equalsIgnoreCase("red")) {
            return Color.RED;
        } else if (winningTeam.equalsIgnoreCase("blue")) {
            return Color.BLUE;
        } else {
            return Color.BLACK;
        }
    }
}