package sfu.cmpt371.group7.game.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import sfu.cmpt371.group7.game.server.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Menu extends Application {

    private String numPlayers;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CAPTURE THE FLAG");

        VBox root = setupRoot();

        Label titleLabel = setupTitleLabel();

        // New Game Button
        Button newGameButton = new Button("NEW GAME");
        styleButton(newGameButton, "#27ae60", "#1e8449");

        // Join Game Button
        Button joinGameButton = new Button("JOIN GAME");
        styleButton(joinGameButton, "#2980b9", "#2471a3");

        newGameButton.setOnAction(e -> showNewGameDialog(primaryStage));
        joinGameButton.setOnAction(e -> showJoinGameDialog(primaryStage));

        root.getChildren().addAll(titleLabel, newGameButton, joinGameButton);

        setupScene(primaryStage, root);
    }

    public static void setupScene(Stage primaryStage, VBox root) {
        Scene scene = new Scene(root, 480, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    System.exit(0);
                }
                if (response == ButtonType.NO) {
                    e.consume();
                }
            });
        });
        primaryStage.setTitle("Capture the Flag");
        primaryStage.show();
    }

    public static VBox setupRoot() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e); -fx-background-radius: 8;");
        root.setAlignment(Pos.CENTER);

        return root;
    }

    public static Label setupTitleLabel() {
        Label titleLabel = new Label("CAPTURE THE FLAG");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));

        return titleLabel;
    }

    private void showNewGameDialog(Stage stage) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Create New Game");

        VBox dialogVBox = new VBox(15);
        dialogVBox.setPadding(new Insets(30));
        dialogVBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");
        dialogVBox.setAlignment(Pos.CENTER);

        Label instruction = new Label("Enter number of players:");
        TextField playerCountField = new TextField();
        playerCountField.setMaxWidth(200);
        playerCountField.setPromptText("e.g. 4");

        String address = "Unknown";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {}

        Label ipLabel = new Label("Your IP: " + address);
        Button startButton = new Button("Start Game");
        styleButton(startButton, "#27ae60", "#1e8449");

        String ip = address;
        startButton.setOnAction(e -> {
            numPlayers = playerCountField.getText().trim();
            if (!numPlayers.matches("\\d+")) {
                showAlert("Please enter a valid number.");
                return;
            }

            Thread serverThread = new Thread(() -> new Server(Integer.parseInt(numPlayers)).start());
            serverThread.setDaemon(true); // ensures it closes when the app exits
            serverThread.start();

            try {
                new Console(ip, Integer.parseInt(numPlayers));
                dialogStage.close();
                stage.close();
            } catch (Exception ex) {
                showAlert("Failed to launch game screen.");
            }
        });

        dialogVBox.getChildren().addAll(instruction, playerCountField, ipLabel, startButton);

        Scene dialogScene = new Scene(dialogVBox, 350, 250);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.centerOnScreen();
        dialogStage.show();
    }

    private void showJoinGameDialog(Stage stage) {
        Stage joinStage = new Stage();
        joinStage.setTitle("Join Game");

        VBox joinBox = new VBox(15);
        joinBox.setPadding(new Insets(30));
        joinBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");
        joinBox.setAlignment(Pos.CENTER);

        Label prompt = new Label("Enter host IP address:");
        TextField ipField = new TextField();
        ipField.setMaxWidth(200);
        ipField.setPromptText("e.g. 192.168.1.100");

        Button joinButton = new Button("Join");
        styleButton(joinButton, "#2980b9", "#2471a3");

        joinButton.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                showAlert("IP address cannot be empty.");
                return;
            }

            try {
                new Console(ip, 1);
                joinStage.close();
                stage.close();
            } catch (Exception ex) {
                showAlert("Failed to connect to host.");
            }
        });

        joinBox.getChildren().addAll(prompt, ipField, joinButton);

        Scene joinScene = new Scene(joinBox, 350, 220);
        joinStage.setScene(joinScene);
        joinStage.setResizable(false);
        joinStage.centerOnScreen();
        joinStage.show();
    }

    private void styleButton(Button button, String color, String hoverColor) {
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: " + color + "; " +
                "-fx-text-fill: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 5);");

        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: " + hoverColor + "; " +
                "-fx-text-fill: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 8);"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: " + color + "; " +
                "-fx-text-fill: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 5);"));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}