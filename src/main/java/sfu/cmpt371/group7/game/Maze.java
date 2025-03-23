package sfu.cmpt371.group7.game;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
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
import sfu.cmpt371.group7.game.ui.Results;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
* this class is responsible for creating the maze and adding the players to the maze. ( MAZE = GRID)
* the player can move in the grid using the W,A,S,D keys.
* the player can exit the game using the exit button.
* the player can see the total number of players in the game.
* the player can see the time left to play the game.
* the player can see the name of the player.
* the player can see the flags in the maze.
 */
public class Maze extends Application {

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

    /*
    * constructor to initialize the player and the grid.
    * the grid is a 2D array of characters.
    * the player is the object of the player class.
    * the players is the list of all the players in the game. [ MIGHT NOT NEED IT. KEPT FOR NOW. TODO]
     */
    public Maze(Player player) {
        localPlayer = player;
        grid = new char[rows][cols];
        players = new ArrayList<>();
        initGrid();
        System.out.println("player name is " + player.getName());
    }

    /*
    * this function is used to initialize the grid.
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

    /*
    * this function is used to add the barriers in the maze.
    * will change the barriers to make it slightly more complex.
     */
    public void addBarriers() {
        grid[1][1] = 'X';
        grid[1][2] = 'X';
        grid[2][2] = 'X';
        grid[3][4] = 'X';
        grid[4][4] = 'X';
        grid[5][6] = 'X';
        grid[6][6] = 'X';
        grid[7][8] = 'X';
        grid[8][8] = 'X';
    }

    /*
    * this function is used to add the flags in the maze.
     */
    public void addFlags(){
        grid[0][0] = 'F';
        grid[19][19] = 'F';
        grid[0][19] = 'F';
        grid[19][0] = 'F';
    }

    /*
    * this function is used to start the game.
    * see below for connectToServer, getNumberOfPlayers, listenForServerMessages.
    * all the barriers are black
    * all the flags are yellow
    * all the empty spaces are white
    * the player is red or blue depending on the team
     */
    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("Starting JavaFX application...");
        connectToServer();
        getNumberOfPlayers();
        listenForServerMessages();
        gridPane = new GridPane();
        root = new BorderPane();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Rectangle rect = new Rectangle(30, 30);
                if (grid[i][j] == 'X') {
                    rect.setFill(Color.BLACK);
                }
                else if(grid[i][j] == 'F') {
                    rect.setFill(Color.YELLOW);
                }else {
                    rect.setFill(Color.WHITE);
                }
                rect.setStroke(Color.GRAY);
                gridPane.add(rect, j, i);
            }
        }

        // add a new timer for the player
        timerLabel = new Label("Time left:  3:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #dd3333; -fx-font-weight: bold;");
        startTimer();

        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
        exitButton.setOnAction(e -> {
            // exit the game and reduce the count and also remove the player from the all the other persons game.
            // need to use a token to remove the player.
            // server can send message to all the clients to remove the player.
            // a corresponding fxn will be executed in the maze class
            out.println("exitGame " + localPlayer.getName());
            System.exit(0);
        });


        // Side panel
        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: linear-gradient(to bottom, #eeeeee, #cccccc); "
                + "-fx-border-color: gray; -fx-border-width: 1;");
        statusLabel = new Label("Players: 0");
        name = new Label("Name: " + localPlayer.getName());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        name.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold;");
        sidePanel.getChildren().addAll(statusLabel, name, exitButton);

        BorderPane topPane = new BorderPane();
        topPane.setLeft(new Label(" "));
        topPane.setCenter(timerLabel);
        topPane.setRight(new Label(" "));
        topPane.setPadding(new Insets(10, 0, 10, 0));

        root.setTop(topPane);
        root.setRight(sidePanel);
        root.setCenter(gridPane);

        Scene scene = new Scene(root, 800, 800);
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        // Fade transition on startup
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), root);
        root.setOpacity(0);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        /*
        * event handler for the key pressed.
        * based on what key is pressed, first the move is validated. valid move is a move that does not go out of the grid,
        * does not go into any barrier or any other person. at the moment also does not go into the flags.
        * the new position of the player is then broadcasted to all the other players so they can update the location.
        * this ensures that all players can see the movement of the player in real time
         */
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
                }
            }
        });

    }

    /*
    * this function is used to start the timer.
    * the timer is set to 3 minutes.
    * NOT WORKING AT THE MOMENT. NEED TO FIX IT.
     */

    /*
     * This function initializes and starts a 3-minute countdown timer.
     * It sets the initial timer display and updates it every second.
     */
    private void startTimer() {
        final int totalTime = 180; // 3 minutes in seconds

        timerLabel.setText(String.format("Time left: %d:%02d", totalTime / 60, totalTime % 60));

        final Timeline[] timelineRef = new Timeline[1];

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

    /*
    * this function is used to check if the move is valid.
    * returns true or false
     */

    private boolean checkValidMove(int newX, int newY) {
        return newX >= 0 && newX < rows && newY >= 0 && newY < cols && grid[newX][newY] != 'X' && grid[newX][newY] != 'F' && !players.stream().anyMatch(p -> p.getX() == newX && p.getY() == newY);
    }


    /*
    * this function is used to move the player.
    * first the player is removed from the old position.
    * then the player is added to the new position.
    * the player is red or blue depending on the team.
    * the player is a rectangle with the name of the player.
    * the player is added to the gridpane.
    * the args come from listenForServerMessages.
     */
    private void movePlayer(Player player, int newX, int newY) {
        // Check if move is valid (not into barrier)
        if (newX >= 0 && newX < rows && newY >= 0 && newY < cols && grid[newX][newY] != 'X') {
            Platform.runLater(() -> {
                // Remove player from old position first
                // We need to find the StackPane that contains the player
                gridPane.getChildren().removeIf(node ->
                        GridPane.getColumnIndex(node) == player.getY() &&
                                GridPane.getRowIndex(node) == player.getX() &&
                                node instanceof StackPane);

                // Update player position
                player.setX(newX);
                player.setY(newY);

                // Add player at new position
                Rectangle rect = new Rectangle(30, 30);
                rect.setFill(player.getTeam().equals("red") ? Color.RED : Color.BLUE);
                rect.setStroke(Color.GRAY);

                Text textNode = new Text(player.getName());
                textNode.setFill(Color.WHITE);

                // Stack them together
                StackPane pane = new StackPane(rect, textNode);
                gridPane.add(pane, newY, newX);
            });
        }
    }

    /*
    * this function is used to connect to the server.
     */
    private void connectToServer() throws IOException {
        socket = new Socket(ADDRESS, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    /*
    * NOT BEING USED AT THE MOMENT.
     */
    private void addPlayer() {
        String team = localPlayer.getTeam();
        int x = team.equals("red") ? new Random().nextInt(rows) : new Random().nextInt(rows);
        int y = team.equals("red") ? new Random().nextInt(cols / 2) : (cols / 2) + new Random().nextInt(cols / 2);

        Platform.runLater(() -> addPlayerToUI(localPlayer.getName(), team, x, y));


    }

    /*
    * this function is used to get listen for various messages from the server.
    * the messages are movePlayer, newPlayer, sizeOfPlayersIs.
    * movePlayer -> the player has moved. the new position of the player is sent to all the other players.
    * newPlayer -> a new player has joined the game. the new player is added to the grid.
    * sizeOfPlayersIs -> the total number of players in the game is received from the server.
    * used to update the total number of players in the game.
     */
    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                     if(parts[0].equals("movePlayer")) {
                         String name = parts[1];
                         int newX = Integer.parseInt(parts[2]);
                         int newY = Integer.parseInt(parts[3]);
                         Platform.runLater(() -> { // ensures that the movePlayer call is executed safely on the JavaFX thread
                             Player player = players.stream()
                                     .filter(p -> p.getName().equals(name) &&
                                             (localPlayer == null || !p.equals(localPlayer)))
                                     .findFirst()
                                     .orElse(null);
                             if (player != null) {
                                 movePlayer(player, newX, newY);
                             } else if (localPlayer != null && localPlayer.getName().equals(name)) {
                                 movePlayer(localPlayer, newX, newY);
                             }
                         });
                     }
                     else if(parts[0].equals("newPlayer")){
                         System.out.println("HEREEEEEEEEE");
                            String team = parts[1];
                            int x = Integer.parseInt(parts[2]);
                            int y = Integer.parseInt(parts[3]);
                            String name = parts[4];
                            Platform.runLater(() -> addPlayerToUI(name, team, x, y));
                        }

                     else if(parts[0].equals("sizeOfPlayersIs")){
                         int playerCount = Integer.parseInt(parts[1]);
                            Platform.runLater(() -> statusLabel.setText("Players: " + playerCount));
                     }

                     else if(parts[0].equals("gameOver")){
                         // we would have the info of the winning team here.
                         // open the who won stage to show who won the game
                            Platform.runLater(() ->{
                                Results results = new Results(new Stage(), "<the winning team>");
                                results.showResults();
                            });
                     }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void validate(){
        if(localPlayer == null)
        out.println("validate " + localPlayer.getName());
    }

    /*
    * this function is used to add the player to the UI.
    * the player is a rectangle with the name of the player.
    * the player is red or blue depending on the team.
     */
    private void addPlayerToUI(String playerName, String team, int x, int y) {
        Player player = new Player(team , x, y, playerName);
        players.add(player);
        if (localPlayer == null) {
            localPlayer = player;
        }

        // Create a rectangle and text to display name
        Rectangle rect = new Rectangle(30, 30);
        rect.setFill(team.equals("red") ? Color.RED : Color.BLUE);
        rect.setStroke(Color.GRAY);

        Text textNode = new Text(playerName);
        textNode.setFill(Color.BLACK);
        // Stack them together
        StackPane pane = new StackPane(rect, textNode);
        gridPane.add(pane, y, x);
    }

    /*
    * token to get the number of players in the game from the server
     */
    private void getNumberOfPlayers() {
        out.println("tellMeTheCurrentPlayers");
    }

    public static void main(String[] args) {
        System.out.println("Starting Maze...");
        launch();
    }
}