package sfu.cmpt371.group7.game;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    private final int rows = 20;
    private final int cols = 20;
    private final char[][] grid;
    private final List<Player> players; // stores all players in the game
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GridPane gridPane;
    private BorderPane root;

    public Maze() {
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
        connectToServer();
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

        HBox teamSelection = new HBox();
        Button redButton = new Button("Join Red Team");
        Button blueButton = new Button("Join Blue Team");

        redButton.setOnAction(e -> {
            System.out.println("total number of players " + players.size());
            int x = new Random().nextInt(20);
            int y = new Random().nextInt(20);
            out.println("newPlayer red " + x + " " + y); // used to get the loscation of the player
            addPlayerToUI("red", x, y);
            root.setBottom(null);
        });

        blueButton.setOnAction(e -> {
            System.out.println("total number of players " + players.size());
            int x = new Random().nextInt(20);
            int y = new Random().nextInt(20);
            out.println("newPlayer blue " + x + " " + y );
            addPlayerToUI("blue", x, y);
            root.setBottom(null);
        });

        teamSelection.getChildren().addAll(redButton, blueButton);

        VBox sidePanel = new VBox();
        sidePanel.getChildren().addAll(teamSelection); // Add more UI elements like score here

        root.setRight(sidePanel);
        root.setCenter(gridPane);

        Scene scene = new Scene(root, 800, 800); // Adjusted size for extra space
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    private void connectToServer() throws IOException {
        socket = new Socket("localhost", 1234);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                    if (parts[0].equals("newPlayer")) {
                        String team = parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        Platform.runLater(() -> addPlayerToUI(team, x, y));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addPlayerToUI(String team, int x, int y) {
        Player player = new Player(team, x, y);
        players.add(player);

        Rectangle rect = new Rectangle(30, 30);
        if (team.equals("red")) {
            rect.setFill(Color.RED);
        } else {
            rect.setFill(Color.BLUE);
        }
        rect.setStroke(Color.GRAY);
        gridPane.add(rect, y, x);
    }

    public static void main(String[] args) {
        launch();
    }
}