package sfu.cmpt371.group7.game.ui;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sfu.cmpt371.group7.game.Maze;
import sfu.cmpt371.group7.game.logistics.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * This class is responsible for getting the name and team of the player.
 * Once MIN_PLAYERS_REQUIRED are connected to the server, the game will start.
 */
public class Console extends Application {

    // Load configuration from .env file
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    private static final String ADDRESS = dotenv.get("ADDRESS");
    private static final int PORT = Integer.parseInt(dotenv.get("PORT_NUMBER"));

    // UI components
    private static Label countLabel;
    private static int totalCount = 0;
    private TextField nameField;
    private Button redButton;
    private Button blueButton;

    // Network
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Game state
    private Player player;
    private Stage primaryStage;
    private boolean gameStarting = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            // Connect to the server
            connectToServer();

            // Create the UI
            createUI(stage);

            // Start listening for server messages
            listenForServerMessages();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
            showConnectionError(stage);
        }
    }

    /**
     * Show connection error dialog
     */
    private void showConnectionError(Stage stage) {
        Label errorLabel = new Label("Failed to connect to server at " + ADDRESS + ":" + PORT);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(ev -> System.exit(0));

        VBox errorBox = new VBox(10, errorLabel, exitButton);
        errorBox.setPadding(new Insets(20));

        Scene errorScene = new Scene(errorBox, 300, 100);
        stage.setTitle("Connection Error");
        stage.setScene(errorScene);
        stage.show();
    }

    /**
     * Create the user interface
     */
    private void createUI(Stage stage) {
        // Create count label
        countLabel = new Label("Total count: " + totalCount);
        countLabel.setStyle("-fx-font-size: 16px;");

        // Create name input
        Label nameLabel = new Label("Enter your name: ");
        nameLabel.setStyle("-fx-font-size: 16px;");
        nameField = new TextField();
        nameField.setStyle("-fx-font-size: 16px;");

        // Create team buttons
        redButton = createRedTeamButton();
        blueButton = createBlueTeamButton();

        // Create layout
        HBox buttonBox = new HBox(10, redButton, blueButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10, nameLabel, nameField, countLabel, buttonBox);
        root.setPadding(new Insets(10));

        // Create scene
        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("Select Team");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Create the red team button
     */
    private Button createRedTeamButton() {
        Button redButton = new Button("Join red team");
        redButton.setStyle("-fx-font-size: 14px; -fx-background-color: #ff5555; -fx-text-fill: white;");
        redButton.setOnAction(e -> {
            String playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                // Disable buttons to prevent multiple submissions
                redButton.setDisable(true);
                blueButton.setDisable(true);

                sendToServer("teamSelection red " + playerName);

                // Create player
                player = new Player("red", 0, 0, playerName);
                player.setName(playerName);
                player.setTeam("red");
            } else {
                System.out.println("Name cannot be empty");
            }
        });
        return redButton;
    }

    /**
     * Create the blue team button
     */
    private Button createBlueTeamButton() {
        Button blueButton = new Button("Join blue team");
        blueButton.setStyle("-fx-font-size: 14px; -fx-background-color: #5555ff; -fx-text-fill: white;");
        blueButton.setOnAction(e -> {
            String playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                // Disable buttons to prevent multiple submissions
                redButton.setDisable(true);
                blueButton.setDisable(true);

                sendToServer("teamSelection blue " + playerName);

                // Create player
                player = new Player("blue", 0, 0, playerName);
                player.setName(playerName);
                player.setTeam("blue");
            } else {
                System.out.println("Name cannot be empty");
            }
        });
        return blueButton;
    }

    /**
     * Send a message to the server
     */
    private void sendToServer(String message) {
        if (out != null) {
            System.out.println("Sending to server: " + message);
            out.println(message);
        }
    }

    /**
     * Connect to the server
     */
    private void connectToServer() throws IOException {
        try {
            socket = new Socket(ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server at " + ADDRESS + ":" + PORT);
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Listen for messages from the server
     */
    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from server: " + message);

                    // Process messages based on type
                    if (message.startsWith("updateCount")) {
                        handleUpdateCount(message);
                    } else if (message.startsWith("startGame")) {
                        handleStartGame();
                    } else if (message.startsWith("sendingPlayer")) {
                        handlePlayerData(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
                e.printStackTrace();

                // Show error on UI thread
                Platform.runLater(() -> {
                    if (primaryStage != null && primaryStage.isShowing()) {
                        showConnectionError(primaryStage);
                    }
                });
            }
        }).start();
    }

    /**
     * Handle update count message
     */
    private void handleUpdateCount(String message) {
        try {
            String[] tokens = message.split(" ");
            if (tokens.length >= 2) {
                int newCount = Integer.parseInt(tokens[1]);
                totalCount = newCount;

                Platform.runLater(() -> countLabel.setText("Total count: " + totalCount));
            }
        } catch (Exception e) {
            System.err.println("Error parsing update count message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle start game message
     */
    private void handleStartGame() {
        if (!gameStarting) {
            gameStarting = true;

            Platform.runLater(() -> {
                try {
                    System.out.println("Starting the game...");
                    Stage mazeStage = new Stage();
                    new Maze(player).start(mazeStage);

                    // Close the console window
                    if (primaryStage != null) {
                        primaryStage.close();
                    }
                } catch (Exception e) {
                    System.err.println("Error starting game: " + e.getMessage());
                    e.printStackTrace();
                    gameStarting = false;
                }
            });
        }
    }

    /**
     * Handle player data message
     */
    private void handlePlayerData(String message) {
        try {
            // Format: sendingPlayer <n> <team> <x> <y>
            String[] tokens = message.split(" ");

            if (tokens.length >= 5 && player != null && tokens[1].equals(player.getName())) {
                player.setName(tokens[1]);
                player.setTeam(tokens[2]);
                player.setX(Integer.parseInt(tokens[3]));
                player.setY(Integer.parseInt(tokens[4]));

                System.out.println("Updated player position: " + player.getX() + ", " + player.getY());
            }
        } catch (Exception e) {
            System.err.println("Error parsing player data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clean up resources when application closes
     */
    @Override
    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}