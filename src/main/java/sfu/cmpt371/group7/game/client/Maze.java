package sfu.cmpt371.group7.game.client;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
 * @see Flag
 * @see Player
 * @see Results
 */

public class Maze {
    /** The Port number at which the server runs. */
    private static final int PORT = 65000;

    /** Number of rows in the maze*/
    private final int ROWS = 20;

    /** Number of columns in the maze*/
    private final int COLS = 20;

    /** The IP Address of the sever hosting the game. */
    private final String ip;

    /** The game grid representing the maze */
    private final int[][] grid;

    /** List of all players in the game */
    private final List<Player> players;

    /** List of all flags in the game */
    private final ArrayList<Flag> flags;


    /** Input stream for network communication */
    private BufferedReader in;

    /** Output stream for network communication */
    private PrintWriter out;


    /** The main game grid UI component */
    private GridPane gridPane;

    /** The root layout container */
    private BorderPane root;

    /** The local player instance */
    private final Player localPlayer;

    /** Label displaying player count status */
    private Label statusLabel;

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
    public Maze(String ip, Player player) {
        this.ip = ip;
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
        Scene scene = new Scene(root, 800, 650);
        setupKeyboardControls(scene);

        // Configure and show the stage
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> {
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
        stage.show();
    }

    /**
     * Creates all the UI components
     */
    private void createUI() {

        // Draw the maze in a grid pane
        gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        gridPane.setPadding(new Insets(5));
        gridPane.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        int numFlags = 1;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (grid[i][j] == 1) {
                    // Wall cell: Set color to black
                    Rectangle rect = new Rectangle(25, 25);
                    rect.setFill(Color.rgb(50, 50, 50));
                    rect.setArcHeight(5);
                    rect.setArcWidth(5);
                    rect.setStroke(Color.DARKGRAY);
                    rect.setStrokeWidth(0.5);
                    gridPane.add(rect, j, i);
                } else if (grid[i][j] == 2) {
                    // Flag cell: Load the flag image
                    flags.add(new Flag(i, j, "flag" + numFlags));
                    Rectangle baseRect = new Rectangle(25, 25);
                    baseRect.setFill(Color.WHITE);
                    baseRect.setStroke(Color.DARKGRAY);
                    baseRect.setStrokeWidth(0.5);
                    StackPane flagCell = new StackPane(baseRect, render("flag"));
                    gridPane.add(flagCell, j, i);
                    numFlags++;
                } else if (grid[i][j] == 3) {
                    Rectangle baseRect = new Rectangle(25, 25);
                    baseRect.setFill(Color.WHITE);
                    baseRect.setStroke(Color.DARKGRAY);
                    baseRect.setStrokeWidth(0.5);
                    StackPane flagCell = new StackPane(baseRect, render("redHome"));
                    gridPane.add(flagCell, j, i);
                } else if (grid[i][j] == 4) {
                    Rectangle baseRect = new Rectangle(25, 25);
                    baseRect.setFill(Color.WHITE);
                    baseRect.setStroke(Color.DARKGRAY);
                    baseRect.setStrokeWidth(0.5);
                    StackPane flagCell = new StackPane(baseRect, render("blueHome"));
                    gridPane.add(flagCell, j, i);
                } else {
                    // Empty cell
                    Rectangle rect = new Rectangle(25, 25);
                    rect.setFill(Color.WHITE);
                    rect.setEffect(new InnerShadow(2, Color.LIGHTGRAY));
                    rect.setStroke(Color.DARKGRAY);
                    rect.setStrokeWidth(0.5);
                    gridPane.add(rect, j, i);
                }
            }
        }

        // Add the local player to the grid
        if (localPlayer != null) {
            addPlayerToUI(localPlayer.getName(), localPlayer.getTeam(), localPlayer.getX(), localPlayer.getY());
        }

        // Fill out the information in all the labels
        flagCountLabel = new Label("Red: " + redFlagCount + " Blue: " + blueFlagCount);
        flagCountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333; -fx-font-weight: bold;");

        flagCaptureLabel = new Label("No flags captured yet");
        flagCaptureLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        capturePromptLabel = new Label();
        capturePromptLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        capturePromptLabel.setVisible(false);

        // Create a side panel displaying number of players, name of the local player and the exit button
        VBox sidePanel = new VBox(15);
        sidePanel.setPadding(new Insets(15, 25, 15, 25));
        sidePanel.setAlignment(Pos.TOP_CENTER);

        sidePanel.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f8f8, #e0e0e0); "
                + "-fx-border-color: #bdbdbd; -fx-border-width: 1; -fx-border-radius: 5;");

        statusLabel = new Label("Players: 0");

        assert localPlayer != null;
        Label nameLabel = new Label("Name: " + localPlayer.getName());
        Label teamLabel = new Label("Team: " + localPlayer.getTeam().toUpperCase());

        String teamColor = localPlayer.getTeam().equals("red") ? "#d32f2f" : "#1976d2";
        teamLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + teamColor + "; -fx-font-weight: bold;");

        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");

        // Score section
        Label scoreTitle = new Label("SCORE");
        scoreTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-padding: 15 0 5 0;");

        sidePanel.getChildren().addAll(
                statusLabel,
                nameLabel,
                teamLabel,
                scoreTitle,
                flagCountLabel,
                flagCaptureLabel
        );

        // Create top panel in a border pane displaying the remaining time
        BorderPane topPane = new BorderPane();
        topPane.setLeft(new Label(" "));

        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(capturePromptLabel);
        topPane.setCenter(centerBox);

        topPane.setRight(new Label(" "));
        topPane.setPadding(new Insets(10, 0, 10, 0));

        // Assemble main layout in the root container
        root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f5f5;");
        root.setTop(topPane);
        root.setRight(sidePanel);
        root.setCenter(gridPane);
    }

    private ImageView render (String type) {

        Image image = null;
        switch (type) {
            case "redHome" -> image = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/redHome.png")));
            case "blueHome" -> image = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/blueHome.png")));
            case "flag" -> image = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/flag.png")));
            case "blueFlag" -> image = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/blueFlag.png")));
            case "redFlag" -> image = new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/sfu/cmpt371/group7/game/redFlag.png")));
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        imageView.setPreserveRatio(true);

        return imageView;
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
        Rectangle rect = new Rectangle(25, 25);
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
                else if (event.getCode() == KeyCode.C && getUncapturedFlagAtPosition(newX, newY) != null) {

                    if (captureStartTime == -1) {
                        captureStartTime = System.currentTimeMillis();
                        capturePromptLabel.setText("Capturing ...");
                    }
                }

                // Update the players x and y co-ordinates according to the moves and inform the server
                if (hasMoved) {
                    localPlayer.setX(newX);
                    localPlayer.setY(newY);
                    if (getUncapturedFlagAtPosition(newX, newY) != null) {
                        capturePromptLabel.setVisible(true);
                        capturePromptLabel.setText("Hold C to capture the flag!");
                    } else {
                        captureStartTime = -1;
                    }
                    out.println("movePlayer " + localPlayer.getName() + " " + newX + " " + newY);
                }
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.C && getUncapturedFlagAtPosition(localPlayer.getX(), localPlayer.getY()) != null) {
                Flag flagAtPosition = getUncapturedFlagAtPosition(localPlayer.getX(), localPlayer.getY());
                if (captureStartTime != -1) {
                    long captureDuration = System.currentTimeMillis() - captureStartTime;
                    double durationInSeconds = captureDuration/1000.0;
                    System.out.println("C pressed for " + durationInSeconds + " seconds");
                    assert flagAtPosition != null;
                    out.println("captureDuration " + localPlayer.getName() + " " + flagAtPosition.getName() + " " + durationInSeconds);
                    capturePromptLabel.setVisible(false);
                    captureStartTime = -1;
                }
            }
        });
    }

    /**
     * Checks if a move is valid or invalid. <br>
     * <br>
     * Invalid moves: <br>
     * - Player moves on a cell out of the 20 x 20 grid <br>
     * - Player moves on a cell occupied by a wall <br>
     * <br>
     * Allows multiple people to stand on a flag
     * @param newX The new x coordinate after moving
     * @param newY The new y coordinate after moving
     * @return false if a move is one of the invalid moves, true otherwise
     *
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

        for (Flag flag : flags) {
            if (flag.getX() == newX && flag.getY() == newY && flag.isCaptured()) {
                return false;
            }
        }

        // Check if the move is to a flag position - allow this even if other players are there
        if (isFlagPosition(newX, newY)) {
            return true;
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
     * Helper method to check if a position contains a flag
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return true if a flag is at this position, false otherwise
     */
    private boolean isFlagPosition(int x, int y) {
        for (Flag flag : flags) {
            if (flag.getX() == x && flag.getY() == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the visual representation of a flag when it's captured
     * by changing its color to grey.
     *
     * @param flagName The name of the captured flag
     */
    private void updateFlagColor(String flagName) {
        Flag capturedFlag = findFlagByName(flagName);
        if (capturedFlag == null) return;

        int flagX = capturedFlag.getX();
        int flagY = capturedFlag.getY();

        Platform.runLater(() -> {
            for (Node node : gridPane.getChildren()) {
                if (GridPane.getRowIndex(node) == flagX && GridPane.getColumnIndex(node) == flagY &&
                        node instanceof Rectangle) {
                    ((Rectangle) node).setFill(Color.GREY);
                    break;
                }
            }
        });
    }

    /**
     * Moves a player on the grid
     *
     * @param player The player to move
     * @param newX The new x co-ordinate after the move
     * @param newY The new y co-ordinate after the move
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

            // Create a new player representation
            Rectangle rect = new Rectangle(22, 22);

            Color teamColor = player.getTeam().equals("red") ? Color.rgb(211, 47, 47) : Color.rgb(25, 118, 210);
            rect.setFill(teamColor);
            rect.setStroke(Color.WHITE);
            rect.setStrokeWidth(1.5);
            rect.setArcHeight(10);
            rect.setArcWidth(10);

            // Add glow effect for the local player
            if (player.getName().equals(localPlayer.getName())) {
                DropShadow glow = new DropShadow();
                glow.setColor(teamColor);
                glow.setRadius(10);
                rect.setEffect(glow);
            }

            Text textNode = new Text(player.getName());
            textNode.setFill(Color.WHITE);
            textNode.setFont(Font.font("System", FontWeight.BOLD, 10));
            textNode.setEffect(new DropShadow(2, Color.BLACK));

            // Stack them together
            StackPane pane = new StackPane(rect, textNode);

            // Add a subtle animation effect
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), pane);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // Add new representation to the grid
            gridPane.add(pane, newY, newX);

            System.out.println("Player " + player.getName() + " moved to " + newX + "," + newY);
        });
    }

    /**
     * Connect to the server
     */
    private void connectToServer() throws IOException {
        Socket socket = new Socket(ip, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Send the flag coordinates to the server
     */
    private void sendFlagCoordinates(){
        StringBuilder message = new StringBuilder("flagCoordinates ");
        for (Flag f : flags) {
            message.append(f.getX()).append(" ").append(f.getY()).append(" ");
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

                    switch (messageType) {
                        case "movePlayer" -> handleMovePlayerMessage(parts);
                        case "newPlayer" -> handleNewPlayerMessage(parts);
                        case "sizeOfPlayersIs" -> handlePlayerCountMessage(parts);
                        case "gameOver" -> handleGameOverMessage(parts);
                        case "flagCaptured" -> handleFlagCapturedMessage(parts);
                        case "lockFlag" -> handleLockFlagMessage(parts);
                        case "sendingPlayer" -> handlePlayerUpdateMessage(parts);
                        case "playerLeft" -> handlePlayerLeftMessage(parts);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }).start();
    }

    /**
     * Handles movePlayer message from server
     * @param parts The complete message received from the server
     */
    private void handleMovePlayerMessage(String[] parts) {

        // movePlayer <player name> <newX> <newY>
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

                Platform.runLater(() -> addPlayerToUI(playerName, team, newX, newY));
            }

            else if(parts[1].equals(localPlayer.getName())){
                System.out.println("--------------------------");
                System.out.println("Moving existing player: " + playerName);
                Player finalPlayerToMove = playerToMove;
                Platform.runLater(() -> movePlayer(finalPlayerToMove, newX, newY));
            }else {
                // Move existing player
                System.out.println("Moving existing player: " + playerName);
                Player finalPlayerToMove = playerToMove;
                Platform.runLater(() -> movePlayer(finalPlayerToMove, newX, newY));
            }
        }
    }
    /**
     * Handle newPlayer message from server
     * @param parts The complete message received from the server
     */
    private void handleNewPlayerMessage(String[] parts) {
        // newPlayer <team> <x> <y> <name>
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
     * Handle sizeOfPlayerIs message from server
     * @param parts The complete message received from the server
     */
    private void handlePlayerCountMessage(String[] parts) {

        // sizeOfPlayerIs <number of players connected>
        if (parts.length >= 2) {
            int count = Integer.parseInt(parts[1]);
            Platform.runLater(() -> statusLabel.setText("Players: " + count));
        }
    }


    /**
     * Handle gameOver message from server
     * @param parts The complete message received from the server
     */
    private void handleGameOverMessage(String[] parts) {

        // gameOver <winner name>
        String winner = parts.length > 1 ? parts[1] : "unknown";
        endGame(winner);
    }

    /**
     * Handle flagCaptured message from server
     * @param parts The complete message received from the server
     * Also changes the color of the flag to grey to mark it as captured
     */
    private void handleFlagCapturedMessage(String[] parts) {

        // flagCaptured <capturing player> <captured flag>
        if (parts.length >= 3) {
            String playerName = parts[1];
            String flagName = parts[2];

            // Display the flag captured information
            Platform.runLater(() -> {
                flagCaptureLabel.setText(playerName + " captured " + flagName);
                Flag capturedFlag = findFlagByName(flagName);
                if (capturedFlag != null) {
                    capturedFlag.setCaptured(true);
                    updateFlagColor(flagName);
                }

                // Update flag counts
                Player capturingPlayer = findPlayerByName(playerName);
                if (capturingPlayer != null) {
                    Rectangle baseRect = new Rectangle(25, 25);
                    baseRect.setFill(Color.LIGHTGRAY);
                    baseRect.setStroke(Color.DARKGRAY);
                    baseRect.setStrokeWidth(0.5);
                    if (capturingPlayer.getTeam().equals("red")) {
                        redFlagCount++;
                        StackPane flagCell = new StackPane(baseRect, render("redFlag"));
                        gridPane.add(flagCell, capturedFlag.getY(), capturedFlag.getX());
                    } else {
                        blueFlagCount++;
                        StackPane flagCell = new StackPane(baseRect, render("blueFlag"));
                        gridPane.add(flagCell, capturedFlag.getY(), capturedFlag.getX());
                    }
                    flagCountLabel.setText("Red: " + redFlagCount + " Blue: " + blueFlagCount);
                    capturePromptLabel.setVisible(true);
                    capturePromptLabel.setText(parts[2].toUpperCase() + " CAPTURED !");
                }
            });
        }
    }

    /**
     * Handle lockFlag message from server
     * @param parts The complete message received from the server
     */
    private void handleLockFlagMessage(String[] parts) {
        // lockFlag <flag name>
        if (parts.length >= 2) {
            String flagName = parts[1];
            Flag flag = findFlagByName(flagName);
            if (flag != null) {
                flag.setCaptured(true);
                updateFlagColor(flagName);
            }
        }
    }

    /**
     * Handle sendingPlayer message from server
     * @param parts The complete message received from the server
     */
    private void handlePlayerUpdateMessage(String[] parts) {
        // sendingPlayer <name> <team> <x> <y>
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
     * @param parts The complete message received from the server
     */
    private void handlePlayerLeftMessage(String[] parts) {

        // playerLeft <player name>
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
     * Request current player count from server
     */
    private void getNumberOfPlayers() {
        out.println("tellMeTheCurrentPlayers");
    }

    /**
     * Helper method to check if a player exists in the game.
     * @param name The name of the player to lookup
     * @return true if the player already exist, false otherwise
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
     * Helper method to find an existing player in the game
     * @param name the name of the player to find
     * @return the player if it exists, null otherwise
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
     * Helper method to find an existing flag in the game
     * @param name the name of the flag to find
     * @return the flag if it exists, null otherwise
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
     * Helper method to verify whether a flag exists at position (x ,y) and if it is
     * captured or not.
     * @param x The x co-ordinate
     * @param y The y co-ordinate
     *
     * @return the Flag object if an uncaptured flag is present at the given position, null otherwise.
     */
    private Flag getUncapturedFlagAtPosition(int x, int y) {
        for (Flag flag : flags) {
            if (flag.getX() == x && flag.getY() == y && !flag.isCaptured()) {
                capturePromptLabel.setVisible(true);
                return flag;
            }
        }
        capturePromptLabel.setVisible(false);
        return null;
    }

    /**
     * End the game and show results
     * @param winner A string with the name of the winning team
     */
    private void endGame(String winner) {
        Platform.runLater(() -> {
            Stage resultStage = new Stage();
            Results results = new Results(resultStage, winner);
            results.showResults();
        });
    }
}