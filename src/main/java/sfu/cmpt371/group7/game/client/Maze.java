package sfu.cmpt371.group7.game.client;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import sfu.cmpt371.group7.game.model.Flag;
import sfu.cmpt371.group7.game.model.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for creating the maze and handling player movement.
 */
public class Maze {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    private static final String ADDRESS = dotenv.get("ADDRESS");
    private static final int PORT = Integer.parseInt(dotenv.get("PORT_NUMBER"));

    private final int rows = 20;
    private final int cols = 20;
    private final char[][] grid;
    private final List<Player> players; // stores all players in the game
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GridPane gridPane;
    private BorderPane root;
    private Player localPlayer;
    private Label statusLabel;
    private Label name;
    private Label timerLabel;
    private int redFlagCount = 0;
    private int blueFlagCount = 0;
    private Label flagCountLabel;
    private Label flagCaptureLabel;
    private Flag flag1; // the right one
    private Flag flag2; // left
    private Flag flag3; // bottom

    /**
     * Constructor
     */
    public Maze(Player player) {
        localPlayer = player;
        grid = new char[rows][cols];
        players = new ArrayList<>();
        players.add(localPlayer); // Add local player to the players list
        initGrid();
        System.out.println("player name is " + player.getName());
    }

    /**
     * Initialize the grid with empty spaces
     */
    public void initGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = ' ';
            }
        }
        addBarriers();
        addFlags();
    }

    /**
     * Add barriers to create maze structure
     */
    public void addBarriers() {
        // Left-middle vertical barrier
        for (int i = 4; i <= 12; i++) {
            grid[i][5] = 'X';
        }

        // Right-middle vertical barrier
        for (int i = 7; i <= 14; i++) {
            grid[i][15] = 'X';
        }

        // Bottom-middle horizontal barrier
        for (int j = 7; j <= 15; j++) {
            grid[17][j] = 'X';
        }

        // Add some small gaps to make barriers more interesting
        grid[7][5] = ' ';  // Gap in left barrier
        grid[12][15] = ' '; // Gap in right barrier
        grid[17][10] = ' '; // Gap in bottom barrier
    }

    /**
     * Add flag positions
     */
    public void addFlags() {
        // Flag behind left barrier
        grid[8][7] = 'F';

        // Flag behind right barrier
        grid[10][13] = 'F';

        // Flag below bottom barrier
        grid[18][10] = 'F';

        flag1 = new Flag(8, 7, "flag1");
        flag2 = new Flag(10, 13, "flag2");
        flag3 = new Flag(18, 10, "flag3");
    }

    public void initiate(Stage stage) throws IOException {
        connectToServer();
        out.println("resendPlayers");
        getNumberOfPlayers();
        assert(localPlayer != null);
        sendFlagCoordinates();
        listenForServerMessages();

        // Create UI components
        createUI();

        // Create scene with keyboard controls
        Scene scene = new Scene(root, 800, 800);
        setupKeyboardControls(scene);

        // Configure and show the stage
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Create the user interface
     */
    private void createUI() {
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(5));
        gridPane.setHgap(1);
        gridPane.setVgap(1);

        root = new BorderPane();

        flagCountLabel = new Label("Red: " + redFlagCount + " Blue: " + blueFlagCount);
        flagCaptureLabel = new Label("No flags captured yet");
        flagCaptureLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        // Draw grid
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Rectangle rect = new Rectangle(30, 30);
                if (grid[i][j] == 'X') {
                    rect.setFill(Color.BLACK);
                } else if(grid[i][j] == 'F') {
                    rect.setFill(Color.YELLOW);
                } else {
                    rect.setFill(Color.WHITE);
                }
                rect.setStroke(Color.GRAY);
                gridPane.add(rect, j, i);
            }
        }

        // Create timer
        timerLabel = new Label("Time left:  3:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #dd3333; -fx-font-weight: bold;");
        startTimer();

        // Create exit button
        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
        exitButton.setOnAction(e -> {
            out.println("exitGame " + localPlayer.getName());
            System.exit(0);
        });

        // Create side panel
        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: linear-gradient(to bottom, #eeeeee, #cccccc); "
                + "-fx-border-color: gray; -fx-border-width: 1;");
        statusLabel = new Label("Players: 0");
        name = new Label("Name: " + localPlayer.getName());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        name.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        sidePanel.getChildren().addAll(statusLabel, name, exitButton, flagCountLabel, flagCaptureLabel);

        // Create top panel with timer
        BorderPane topPane = new BorderPane();
        topPane.setLeft(new Label(" "));
        topPane.setCenter(timerLabel);
        topPane.setRight(new Label(" "));
        topPane.setPadding(new Insets(10, 0, 10, 0));

        // Assemble main layout
        root.setTop(topPane);
        root.setRight(sidePanel);
        root.setCenter(gridPane);

        // Add the local player to the grid
        if (localPlayer != null) {
            addPlayerToUI(localPlayer.getName(), localPlayer.getTeam(), localPlayer.getX(), localPlayer.getY());
        }
    }

    /**
     * Set up keyboard controls for player movement
     */
    private void setupKeyboardControls(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (localPlayer != null) {
                int newX = localPlayer.getX();
                int newY = localPlayer.getY();
                boolean hasMoved = false;

                if (event.getCode() == KeyCode.W) {
                    if (checkValidMove(newX - 1, newY)) {
                        newX--;
                        hasMoved = true;
                    }
                } else if (event.getCode() == KeyCode.S) {
                    if (checkValidMove(newX + 1, newY)) {
                        newX++;
                        hasMoved = true;
                    }
                } else if (event.getCode() == KeyCode.A) {
                    if (checkValidMove(newX, newY - 1)) {
                        newY--;
                        hasMoved = true;
                    }
                } else if (event.getCode() == KeyCode.D) {
                    if (checkValidMove(newX, newY + 1)) {
                        newY++;
                        hasMoved = true;
                    }
                }

                if (hasMoved) {
                    localPlayer.setX(newX);
                    localPlayer.setY(newY);
                    out.println("movePlayer " + localPlayer.getName() + " " + newX + " " + newY);

                    // Also move the player locally to make the UI more responsive
                    //smovePlayer(localPlayer, newX, newY);

                    // Check if player reached a flag
                    //checkForFlagCapture(localPlayer, newX, newY);
                }
            }
        });
    }


    /**
     * End the game and show results
     */
    private void endGame(String winner) {
        Platform.runLater(() -> {
            Stage resultStage = new Stage();
            Results results = new Results(resultStage, winner);
            results.showResults();
        });
    }

    /**
     * Start the game timer
     */
    private void startTimer() {
        final int totalTime = 180; // 3 minutes in seconds
        final Timeline[] timelineRef = new Timeline[1];

        timerLabel.setText(String.format("Time left: %d:%02d", totalTime / 60, totalTime % 60));

        // Create the timeline with access to the reference
        timelineRef[0] = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String currentText = timerLabel.getText().replace("Time left: ", "");
                    String[] parts = currentText.split(":");

                    try {
                        int minutes = Integer.parseInt(parts[0].trim());
                        int seconds = Integer.parseInt(parts[1].trim());
                        int totalSeconds = minutes * 60 + seconds;
                        totalSeconds = Math.max(totalSeconds - 1, 0);

                        int newMin = totalSeconds / 60;
                        int newSec = totalSeconds % 60;
                        timerLabel.setText(String.format("Time left: %d:%02d", newMin, newSec));

                        if (totalSeconds == 0) {
                            timelineRef[0].stop();
                            out.println("gameOver");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing time: " + e.getMessage());
                    }
                })
        );

        timelineRef[0].setCycleCount(Timeline.INDEFINITE);
        timelineRef[0].play();
    }

    /**
     * Check if a move is valid
     */
    private boolean checkValidMove(int newX, int newY) {
        // Check grid boundaries
        if (newX < 0 || newX >= rows || newY < 0 || newY >= cols) {
            return false;
        }

        // Check if cell is a barrier
        if (grid[newX][newY] == 'X') {
            return false;
        }

        // Check if cell is occupied by another player
        for (Player p : players) {
            if (p != localPlayer && p.getX() == newX && p.getY() == newY) {
                return false;
            }
        }

        return true;
    }

    /**
     * Move a player on the grid
     */
    private void movePlayer(Player player, int newX, int newY) {
        Platform.runLater(() -> {
            // Remove all instances of this player from the grid first
            gridPane.getChildren().removeIf(node ->
                    node instanceof StackPane &&
                            ((StackPane)node).getChildren().stream()
                                    .anyMatch(child ->
                                            child instanceof Text &&
                                                    ((Text)child).getText().equals(player.getName())));

            // Update player position
            player.setX(newX);
            player.setY(newY);

            // Create player representation
            Rectangle rect = new Rectangle(30, 30);
            rect.setFill(player.getTeam().equals("red") ? Color.RED : Color.BLUE);
            rect.setStroke(Color.GRAY);

            Text textNode = new Text(player.getName());
            textNode.setFill(Color.WHITE);

            // Stack them together
            StackPane pane = new StackPane(rect, textNode);

            // Add to the grid
            gridPane.add(pane, newY, newX);

            System.out.println("Player " + player.getName() + " moved to " + newX + "," + newY);
        });
    }

    /**
     * Connect to the server
     */
    private void connectToServer() throws IOException {
        socket = new Socket(ADDRESS, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Send flag coordinates to the server
     */
    private void sendFlagCoordinates(){
        out.println("flagCoordinates " + flag1.getX() + " " + flag1.getY() + " " +
                flag2.getX() + " " + flag2.getY() + " " +
                flag3.getX() + " " + flag3.getY());
    }

    /**
     * Listen for server messages
     */
    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                    String messageType = parts[0];

                    System.out.println("Received: " + message);

                    if (messageType.equals("movePlayer")) {
                        handleMovePlayerMessage(parts);
                    }
                    else if (messageType.equals("newPlayer")) {
                        handleNewPlayerMessage(parts);
                    }
                    else if (messageType.equals("sizeOfPlayersIs")) {
                        handlePlayerCountMessage(parts);
                    }
                    else if (messageType.equals("gameOver")) {
                        handleGameOverMessage(parts);
                    }
                    else if (messageType.equals("flagCaptured")) {
                        handleFlagCapturedMessage(parts);
                    }
                    else if (messageType.equals("lockFlag")) {
                        handleLockFlagMessage(parts);
                    }
                    else if (messageType.equals("sendingPlayer")) {
                        handlePlayerUpdateMessage(parts);
                    }
                    else if (messageType.equals("playerLeft")) {
                        handlePlayerLeftMessage(parts);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Handle new player message from server
     */
    private void handleNewPlayerMessage(String[] parts) {
        // Format: newPlayer <team> <x> <y> <name>
        if (parts.length >= 5) {
            String team = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            String playerName = parts[4];

            // Don't add if it's the local player or already exists
            if (!playerName.equals(localPlayer.getName()) &&
                    !playerExists(playerName)) {

                Platform.runLater(() -> addPlayerToUI(playerName, team, x, y));
            }
        }
    }

    /**
     * Check if a player already exists in our list
     */
    private boolean playerExists(String name) {
        for (Player p : players) {
            if (p.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find a player by name
     */
    private Player findPlayerByName(String name) {
        for (Player p : players) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Handle move player message from server
     */
    private void handleMovePlayerMessage(String[] parts) {
        // name x y
        // Format: movePlayer <n> <x> <y>


        if (parts.length >= 4) {
            String playerName = parts[1];
            int newX = Integer.parseInt(parts[2]);
            int newY = Integer.parseInt(parts[3]);

            System.out.println("Processing move for player: " + playerName);

            // Skip if it's our own movement (we already handled it locally)
            //if (playerName.equals(localPlayer.getName())) {
              //  return;
            //}

            // Find the player to move
            Player playerToMove = findPlayerByName(playerName);



            // If player doesn't exist yet, create them
            if (playerToMove == null) {
                System.out.println("Creating new player: " + playerName);
                // Use opposite team as a fallback
                String team = localPlayer.getTeam().equals("red") ? "blue" : "red";
                playerToMove = new Player(team, newX, newY, playerName);
                players.add(playerToMove);

                Platform.runLater(() -> {
                    addPlayerToUI(playerName, team, newX, newY);
                });
            }

            else if(parts[1].equals(localPlayer.getName())){
                System.out.println("--------------------------");
                System.out.println("Moving existing player: " + playerName);
                Player finalPlayerToMove = playerToMove;
                Platform.runLater(() -> {
                    movePlayer(finalPlayerToMove, newX, newY);
                });
            }else {
                // Move existing player
                System.out.println("Moving existing player: " + playerName);
                Player finalPlayerToMove = playerToMove;
                Platform.runLater(() -> {
                    movePlayer(finalPlayerToMove, newX, newY);
                });
            }
        }
    }

    /**
     * Handle player count message from server
     */
    private void handlePlayerCountMessage(String[] parts) {
        if (parts.length >= 2) {
            int count = Integer.parseInt(parts[1]);
            Platform.runLater(() -> statusLabel.setText("Players: " + count));
        }
    }

    /**
     * Handle game over message from server
     */
    private void handleGameOverMessage(String[] parts) {
        String winner = parts.length > 1 ? parts[1] : "unknown";
        endGame(winner);
    }

    /**
     * Handle flag captured message from server
     */
    private void handleFlagCapturedMessage(String[] parts) {
        if (parts.length >= 3) {
            String playerName = parts[1];
            String flagName = parts[2];

            Platform.runLater(() -> {
                flagCaptureLabel.setText(playerName + " captured " + flagName);

                // Update flag counts
                Player capturingPlayer = findPlayerByName(playerName);
                if (capturingPlayer != null) {
                    if (capturingPlayer.getTeam().equals("red")) {
                        redFlagCount++;
                    } else {
                        blueFlagCount++;
                    }
                    flagCountLabel.setText("Red: " + redFlagCount + " Blue: " + blueFlagCount);
                }
            });
        }
    }

    /**
     * Handle lock flag message from server
     */
    private void handleLockFlagMessage(String[] parts) {
        if (parts.length >= 2) {
            String flagName = parts[1];

            if (flagName.equals(flag1.getName())) {
                flag1.setCaptured(true);
            } else if (flagName.equals(flag2.getName())) {
                flag2.setCaptured(true);
            } else if (flagName.equals(flag3.getName())) {
                flag3.setCaptured(true);
            }
        }
    }

    /**
     * Handle player update message from server
     */
    private void handlePlayerUpdateMessage(String[] parts) {
        // Format: sendingPlayer <name> <team> <x> <y>
        if (parts.length >= 5) {
            String playerName = parts[1];
            String team = parts[2];
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);

            // Skip if it's our own player (we already show ourselves)
            if (playerName.equals(localPlayer.getName())) {
                return;
            }

            // Find or create player
            Player existingPlayer = findPlayerByName(playerName);

            if (existingPlayer != null) {
                // Update existing player
                System.out.println("Updating existing player: " + playerName);
                Platform.runLater(() -> movePlayer(existingPlayer, x, y));
            } else {
                // Add new player
                System.out.println("Adding new player: " + playerName);
                Platform.runLater(() -> addPlayerToUI(playerName, team, x, y));
            }
        }
    }

    /**
     * Handle player left message from server
     */
    private void handlePlayerLeftMessage(String[] parts) {
        if (parts.length >= 2) {
            String playerName = parts[1];

            Platform.runLater(() -> {
                // Remove player from UI
                gridPane.getChildren().removeIf(node ->
                        node instanceof StackPane &&
                                ((StackPane)node).getChildren().stream()
                                        .anyMatch(child ->
                                                child instanceof Text &&
                                                        ((Text)child).getText().equals(playerName)));

                // Remove from players list
                players.removeIf(p -> p.getName().equals(playerName));
            });
        }
    }

    /**
     * Add a player to the UI
     */
    private void addPlayerToUI(String playerName, String team, int x, int y) {
        // Create new player object if it doesn't exist
        Player player = findPlayerByName(playerName);
        if (player == null) {
            player = new Player(team, x, y, playerName);
            players.add(player);
        } else {
            // Update existing player's position
            player.setX(x);
            player.setY(y);
        }

        // Create visual representation
        Rectangle rect = new Rectangle(30, 30);
        rect.setFill(team.equals("red") ? Color.RED : Color.BLUE);
        rect.setStroke(Color.GRAY);

        Text textNode = new Text(playerName);
        textNode.setFill(Color.WHITE);

        // Stack them together
        StackPane pane = new StackPane(rect, textNode);

        // Add to grid
        gridPane.add(pane, y, x);
    }

    /**
     * Request current player count from server
     */
    private void getNumberOfPlayers() {
        out.println("tellMeTheCurrentPlayers");
    }
}