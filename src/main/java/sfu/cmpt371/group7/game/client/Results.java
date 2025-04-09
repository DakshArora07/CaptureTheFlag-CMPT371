package sfu.cmpt371.group7.game.client;

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
 * This class shows the final results of the game in a new window.
 */
public class Results {
    private final Stage stage;
    private final String winningTeam;

    /**
     * Creates a results window.
     *
     * @param stage       the main stage where the results will be shown
     * @param winningTeam the name of the team that won ("red", "blue", or "tie")
     */
    public Results(Stage stage, String winningTeam) {
        this.stage = stage;
        this.winningTeam = winningTeam;
    }

    /**
     * Displays the results window with the winning team and an exit button.
     */
    public void showResults() {

        // Setting up the result info in different labels.
        Label titleLabel = new Label("Game Over");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Label winnerLabel = new Label(getWinnerText());
        winnerLabel.setFont(Font.font("Arial", 18));
        winnerLabel.setTextFill(getWinnerColor());

        // Button to exit the results window.
        Button exitButton = new Button("Exit Game");
        exitButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        exitButton.setOnAction(_-> System.exit(0));

        // Organizing all the elements in a VBox.
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(titleLabel, winnerLabel, exitButton);

        // Setting up the scene and displaying the stage
        Scene scene = new Scene(layout, 300, 200);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Game Results");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Returns the message to display based on the game status.
     * @return a string showing the result (Winning team or a tie situation)
     */
    private String getWinnerText() {
        if (winningTeam == null || winningTeam.isEmpty() || winningTeam.equalsIgnoreCase("tie")) {
            return "It's a tie!";
        } else {
            return winningTeam.toUpperCase() + " team wins!";
        }
    }

    /**
     * Returns the color to use for the winner message.
     * @return a Color based on the winning team (Red, Blue or White for a tie)
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
