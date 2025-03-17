package sfu.cmpt371.group7.game;

import io.github.cdimascio.dotenv.Dotenv;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public Maze(Player player) {
        localPlayer = player;
        grid = new char[rows][cols];
        players = new ArrayList<>();
        initGrid();
    }

    public void initGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = ' ';
            }
        }
        addBarriers();
        addFlags();
    }

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

    public void addFlags(){
        grid[0][0] = 'F';
        grid[19][19] = 'F';
        grid[0][19] = 'F';
        grid[19][0] = 'F';
    }

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


        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: gray; -fx-border-width: 1;");
        statusLabel = new Label("Players: 0");
        name = new Label("Name: " + localPlayer.getName());
        statusLabel.setStyle("-fx-font-size: 12px;");
        sidePanel.getChildren().addAll(statusLabel, name);

        root.setRight(sidePanel);
        root.setCenter(gridPane);

        Scene scene = new Scene(root, 800, 800); // Adjusted size for extra space
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        scene.setOnKeyPressed(event ->{
            if(localPlayer != null) {
                int newX = localPlayer.getX();
                int newY = localPlayer.getY();
                boolean hasMoved = false;

                System.out.println("Key pressed: " + event.getCode());


                if (event.getCode() == KeyCode.W) {
                    if(checkValidMove(newX - 1, newY)) {
                        hasMoved = true;
                        newX--;
                        out.println("movePlayer " + localPlayer.getTeam() + " " + newX + " " + newY);
                    }
                    else{
                        System.err.println("Invalid move");
                    }
                } else if (event.getCode() == KeyCode.S) {
                    if(checkValidMove(newX + 1, newY)) {
                        hasMoved = true;
                        newX++;
                        out.println("movePlayer " + localPlayer.getTeam() + " " + newX + " " + newY);
                    }
                    else{
                        System.err.println("Invalid move");
                    }
                } else if (event.getCode() == KeyCode.A) {
                    if(checkValidMove(newX, newY - 1)) {
                        hasMoved = true;
                        newY--;
                        out.println("movePlayer " + localPlayer.getTeam() + " " + newX + " " + newY);
                    }
                    else{
                        System.err.println("Invalid move");
                    }
                } else if (event.getCode() == KeyCode.D) {
                    if(checkValidMove(newX, newY + 1)) {
                        hasMoved = true;
                        newY++;
                        out.println("movePlayer " + localPlayer.getTeam() + " " + newX + " " + newY);
                    }
                    else{
                        System.err.println("Invalid move");
                    }
                }

                if (hasMoved) {
                    localPlayer.setX(newX);
                    localPlayer.setY(newY);
                    out.println("movePlayer " + localPlayer.getTeam() + " " + newX + " " + newY);
                }
            }
        });

    }

    private boolean checkValidMove(int newX, int newY) {
        return newX >= 0 && newX < rows && newY >= 0 && newY < cols && grid[newX][newY] != 'X' && grid[newX][newY] != 'F' && !players.stream().anyMatch(p -> p.getX() == newX && p.getY() == newY);
    }

    // need to fix this part to make sure the movement is correct

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

    private void connectToServer() throws IOException {
        socket = new Socket(ADDRESS, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void addPlayer() {
        String team = localPlayer.getTeam();
        int x = team.equals("red") ? new Random().nextInt(rows) : new Random().nextInt(rows);
        int y = team.equals("red") ? new Random().nextInt(cols / 2) : (cols / 2) + new Random().nextInt(cols / 2);

        Platform.runLater(() -> addPlayerToUI(localPlayer.getName(), team, x, y));


    }

    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                     if(parts[0].equals("movePlayer")) {
                         String team = parts[1];
                         int newX = Integer.parseInt(parts[2]);
                         int newY = Integer.parseInt(parts[3]);
                         Platform.runLater(() -> { // ensures that the movePlayer call is executed safely on the JavaFX thread
                             Player player = players.stream()
                                     .filter(p -> p.getTeam().equals(team) &&
                                             (localPlayer == null || !p.equals(localPlayer)))
                                     .findFirst()
                                     .orElse(null);
                             if (player != null) {
                                 movePlayer(player, newX, newY);
                             } else if (localPlayer != null && localPlayer.getTeam().equals(team)) {
                                 movePlayer(localPlayer, newX, newY);
                             }
                         });
                     }
                     else if(parts[0].equals("newPlayer")){
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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

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

    private void getNumberOfPlayers() {
        out.println("tellMeTheCurrentPlayers");
    }

    public static void main(String[] args) {
        System.out.println("Starting Maze...");
        launch();
    }
}