package sfu.cmpt371.group7.game.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Console extends Application {
    static private Label countLabel;
    static private int totalCount = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        countLabel = new Label("Total count: " + totalCount);
        countLabel.setStyle("-fx-font-size: 16px;");

        Button redButton = new Button("Join red team");
        redButton.setStyle("-fx-font-size: 14px; -fx-background-color: #ff5555; -fx-text-fill: white;");

        Button blueButton = new Button("Join blue team");
        blueButton.setStyle("-fx-font-size: 14px; -fx-background-color: #5555ff; -fx-text-fill: white;");

        HBox buttonBox = new HBox(10, redButton, blueButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10,countLabel,  buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("Select team");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

    }

    private void updateCount() {
        totalCount++;
        countLabel.setText("Total count: " + totalCount);
    }
}
