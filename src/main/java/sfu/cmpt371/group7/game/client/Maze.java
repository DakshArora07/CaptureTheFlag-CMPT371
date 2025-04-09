package sfu.cmpt371.group7.game.client;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.Objects;

/**
 * The maze class represents the main gameplay area in the Capture the Flag game.
 * It handles player movements, flag capturing, collision handling and communication
 * with the server. A {@link Player} object navigates through the maze to capture the
 * {@link Flag} objects.
 *
 * @author Group 7
 * @version 1.0
 */

public class Maze {

    /** Game configuration */
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    /** The IP Address of the sever hosting the game. */
    private static final String ADDRESS = dotenv.get("ADDRESS");

    /** The Port number at which the server runs. */
    private static final int PORT = Integer.parseInt(dotenv.get("PORT_NUMBER"));

    /** Number of rows in the maze*/
    private final int ROWS = 20;

    /** Number of columns in the maze*/
    private final int COLS = 20;

    /** The game grid representing the maze */
    private final int[][] grid;

    /** List of all players in the game */
    private final List<Player> players;

    /** List of all flags in the game */
    private final ArrayList<Flag> flags;


    /** Network communication socket */
    private Socket socket;

    /** Input stream for network communication */
    private BufferedReader in;

    /** Output stream for network communication */
    private PrintWriter out;


    /** The main game grid UI component */
    private GridPane gridPane;

    /** The root layout container */
    private BorderPane root;

    /** The local player instance */
    private Player localPlayer;

    /** Label displaying player count status */
    private Label statusLabel;

    /** Label displaying the local player name */
    private Label name;

    /** Label displaying the remaining time in the game */
    private Label timerLabel;

    /** Label displaying the total flags captured by each team */
    private Label flagCountLabel;

    /** Label displaying the info about the latest flag capture */
    private Label flagCaptureLabel;

    /** Label displaying flag capturing messages */
    private Label capturePromptLabel;

    /** Store the timestamp when player starts attempting to capture a flag*/
    private long captureStartTime;

    /** Number of flags captured by red team */
    private int redFlagCount;

    /** Number of flags captured by blue team */
    private int blueFlagCount;


    /**
     * Constructs a new Maze game instance for the specified player.
     *
     * @param player The local player who will be playing the game
     */
    public Maze(Player player) {
        localPlayer = player;
        grid = new int[ROWS][COLS];
        players = new ArrayList<>();
        flags = new ArrayList<>();
        players.add(localPlayer);
        blueFlagCount = 0;
        redFlagCount = 0;
        captureStartTime = -1;
        loadMap();
        System.out.println("player name is " + player.getName());
    }

    /**
     * This map reads the text file for that level's map and fills it out.
     * Each number corresponds to a different entity. <br>
     * 0 - Empty Space <br>
     * 1 - Wall <br>
     * 2 - Flag
     */
    private void loadMap(){
        try(InputStreamReader tileReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/map.txt")))) {
            BufferedReader tileMap = new BufferedReader(tileReader);
            for(int row = 0; row < ROWS; ++row){
                String currentLine = tileMap.readLine();
                char[] tileValues = currentLine.replaceAll(" ", "").toCharArray();
                for(int col = 0; col < COLS; ++col){
                    if(tileValues[col] >= '1' && tileValues[col] <= '9') {
                        grid[row][col] = tileValues[col] - '0';
                    }
                }
            }
        } catch(IOException e){
            System.out.println("Error reading tile map");
        }
    }

    /**
     * Initializes the game UI and starts the game.
     *
     * @param stage The primary stage for the game UI
     * @throws IOException if there is an error connecting to the server
     */
    public void initiate(Stage stage) throws IOException {

        // Establishes a TCP Connection with the game server
        connectToServer();
        out.println("resendPlayers");

        // Request players' info from the server
        getNumberOfPlayers();
        assert(localPlayer != null);

        // Create UI components
        createUI();
        sendFlagCoordinates();

        // Handles messages coming from the server
        listenForServerMessages();

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
     * Creates all the UI components
     */
    private void createUI() {

        // Draw the maze in a grid pane
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(5));
        gridPane.setHgap(1);
        gridPane.setVgap(1);
        int numFlags = 1;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Rectangle rect = new Rectangle(30, 30);
                if (grid[i][j] == 1) {
                    rect.setFill(Color.BLACK);
                } else if(grid[i][j] == 2) {
                    rect.setFill(Color.YELLOW);
                    flags.add(new Flag(i, j, "flag" + numFlags));
                    numFlags++;
                } else {
                    rect.setFill(Color.WHITE);
                }
                rect.setStroke(Color.GRAY);
                gridPane.add(rect, j, i);
            }
        }

        // Add the local player to the grid
        if (localPlayer != null) {
            addPlayerToUI(localPlayer.getName(), localPlayer.getTeam(), localPlayer.getX(), localPlayer.getY());
        }

        // Fill out the information in all the labels
        flagCountLabel = new Label("Red: " + redFlagCount + " Blue: " + blueFlagCount);
        flagCaptureLabel = new Label("No flags captured yet");
        flagCaptureLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        capturePromptLabel = new Label();
        capturePromptLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        capturePromptLabel.setText("Hold C to capture flag!");
        capturePromptLabel.setVisible(false);

        // Create exit button
        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
        exitButton.setOnAction(e -> {
            out.println("exitGame " + localPlayer.getName());
            System.exit(0);
        });

        // Create a side panel displaying number of players, name of the local player and the exit button
        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: linear-gradient(to bottom, #eeeeee, #cccccc); "
                + "-fx-border-color: gray; -fx-border-width: 1;");
        statusLabel = new Label("Players: 0");
        name = new Label("Name: " + localPlayer.getName());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        name.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        sidePanel.getChildren().addAll(statusLabel, name, exitButton, flagCountLabel, flagCaptureLabel);

        // Create timer
        timerLabel = new Label("Time left:  3:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #dd3333; -fx-font-weight: bold;");
        startTimer();
        timerLabel.setVisible(false);

        // Create top panel in a border pane displaying the remaining time
        BorderPane topPane = new BorderPane();
        topPane.setLeft(new Label(" "));

        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(timerLabel, capturePromptLabel);
        topPane.setCenter(centerBox);

        topPane.setRight(new Label(" "));
        topPane.setPadding(new Insets(10, 0, 10, 0));

        // Assemble main layout in the root container
        root = new BorderPane();
        root.setTop(topPane);
        root.setRight(sidePanel);
        root.setCenter(gridPane);

    }

    /**
     * Set up keyboard controls for player movement <br>
     * W - move up <br>
     * S - move down <br>
     * A - move right <br>
     * D - move left <br>
     * C - capture flag
     */
    private void setupKeyboardControls(Scene scene) {

        scene.setOnKeyPressed(event -> {
            if (localPlayer != null) {
                int newX = localPlayer.getX();
                int newY = localPlayer.getY();
                boolean hasMoved = false;

                // Move a block up if W pressed
                if (event.getCode() == KeyCode.W) {
                    if (checkValidMove(newX - 1, newY)) {
                        newX--;
                        hasMoved = true;
                    }
                }
                // Move a block down if S pressed
                else if (event.getCode() == KeyCode.S) {
                    if (checkValidMove(newX + 1, newY)) {
                        newX++;
                        hasMoved = true;
                    }
                }
                // Move a block left if A pressed
                else if (event.getCode() == KeyCode.A) {
                    if (checkValidMove(newX, newY - 1)) {
                        newY--;
                        hasMoved = true;
                    }
                }
                // Move a block right if D pressed
                else if (event.getCode() == KeyCode.D) {
                    if (checkValidMove(newX, newY + 1)) {
                        newY++;
                        hasMoved = true;
                    }
                }

                // Start capturing when C is pressed and player is on a flag
                else if (event.getCode() == KeyCode.C && checkForUncapturedFlagAtPosition(newX, newY)) {

                    if (captureStartTime == -1) {
                        captureStartTime = System.currentTimeMillis();
                    }
                }

                // Update the players x and y co-ordinates according to the moves and inform the server
                if (hasMoved) {
                    localPlayer.setX(newX);
                    localPlayer.setY(newY);
                    checkForUncapturedFlagAtPosition(newX, newY);
                    out.println("movePlayer " + localPlayer.getName() + " " + newX + " " + newY);
                }
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.C) {

                long captureDuration = System.currentTimeMillis() - captureStartTime;
                System.out.println("C pressed for " + captureDuration/1000.0 + " seconds");
                captureStartTime = -1;
            }
        });
    }

    private boolean checkForUncapturedFlagAtPosition(int x, int y) {
        for (Flag flag : flags) {

            if (flag.getX() == x && flag.getY() == y && !flag.isCaptured()) {
                capturePromptLabel.setVisible(true);
                return true;
            }
        }
        capturePromptLabel.setVisible(false);
        return false;
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
        if (newX < 0 || newX >= ROWS || newY < 0 || newY >= COLS) {
            return false;
        }

        // Check if cell is a barrier
        if (grid[newX][newY] == 1) {
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
     * Send the flag coordinates to the server
     */
    private void sendFlagCoordinates(){
        String message = "flagCoordinates ";
        for (Flag f : flags) {
            message += (f.getX() + " " + f.getY() + " ");
        }
        out.println(message);
    }

    /**
     * Listen for server messages and appropriately handle them
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
     * Handle move player message from server
     */
    private void handleMovePlayerMessage(String[] parts) {

        if (parts.length >= 4) {
            String playerName = parts[1];
            int newX = Integer.parseInt(parts[2]);
            int newY = Integer.parseInt(parts[3]);

            System.out.println("Processing move for player: " + playerName);
            Player playerToMove = findPlayerByName(playerName);

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
     * Find a flag by name
     */
    private Flag findFlagByName(String name) {
        for (Flag f : flags) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
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
                Flag capturedFlag = findFlagByName(flagName);
                if (capturedFlag != null) {
                    capturedFlag.setCaptured(true);
                }
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

            if (flagName.equals(flags.get(0).getName())) {
                flags.get(0).setCaptured(true);
            } else if (flagName.equals(flags.get(1).getName())) {
                flags.get(1).setCaptured(true);
            } else if (flagName.equals(flags.get(2).getName())) {
                flags.get(2).setCaptured(true);
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