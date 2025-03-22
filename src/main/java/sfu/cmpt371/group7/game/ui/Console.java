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
import sfu.cmpt371.group7.game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Console extends Application {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    private static final String ADDRESS = dotenv.get("ADDRESS");
    private static final int PORT = Integer.parseInt(dotenv.get("PORT_NUMBER"));


    static private Label countLabel;
    static private int totalCount = 0;
    private TextField nameField;
    // global variables required for the server, to send messages and to get the updates back from the server
    // in used to read the messages from the server
    // out used to send messages to the server
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // each console will have a player and the player will be passed onto the maze class to basically put the player in the maze.
     Player player = new Player();

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) {
        connectToServer(); // connect to the server before starting the game.
        getUpdatesFromServer(); // get the updates from the server
        countLabel = new Label("Total count: " + totalCount);
        countLabel.setStyle("-fx-font-size: 16px;");

        Label nameLabel = new Label("Enter your name: ");
        nameLabel.setStyle("-fx-font-size: 16px;");
        nameField = new TextField();
        nameField.setStyle("-fx-font-size: 16px;");

        Button redButton = new Button("Join red team");
        redButton.setStyle("-fx-font-size: 14px; -fx-background-color: #ff5555; -fx-text-fill: white;");
        redButton.setOnAction(e -> {
            String playerName = nameField.getText().trim();
            if(!playerName.isEmpty()){
                sendToServer("teamSelection red " +  playerName);
                player.setName(playerName);
                player.setTeam("red");
            }
            else {
                System.out.println("name empty");
            }
        });

        Button blueButton = new Button("Join blue team");
        blueButton.setStyle("-fx-font-size: 14px; -fx-background-color: #5555ff; -fx-text-fill: white;");
        blueButton.setOnAction(e -> {
            String playerName = nameField.getText().trim();
            if(!playerName.isEmpty()){
                sendToServer("teamSelection blue " + playerName);
                player.setName(playerName);
                player.setTeam("blue");
            }
            else{
            System.out.println("name empty");
            }
        });

        HBox buttonBox = new HBox(10, redButton, blueButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10, nameLabel, nameField, countLabel, buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("Select team");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

    }

    private void sendToServer(String message) {
        out.println(message);
    }

    private void connectToServer(){
        try {
            socket = new Socket(ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (IOException e) {
            System.err.println("error in console -> connect to server");
            e.printStackTrace();
        }
    }

    private void updateCount() {
        totalCount++;
        countLabel.setText("Total count: " + totalCount);
    }

    private void getUpdatesFromServer(){
        // the only update going to be recieved, is the total new count of players
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("updateCount")) {
                        String[] tokens =  message.split(" ");
                        int newCount = Integer.parseInt(tokens[1]);
                        totalCount = newCount;
                        Platform.runLater(() -> countLabel.setText("Total count: " + totalCount));
                    }
                    else if(message.startsWith("startGame")){
                        // team, x , y, name
                        // x and y will be overridden in the maze class.

                        // start the game
                        // add a function to start the game and basically start the maze class
                        Platform.runLater(() -> {
                            System.out.println("Starting the game...");
                            try {
                                Stage mazeStage = new Stage();
                                new Maze(player).start(mazeStage);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                System.err.println("error in console -> get updates from server");
                e.printStackTrace();
            }
        }).start();
    }

    private void validate(){
    // we need validate from the client to the server that everything is okay. can be done in the starting after the maze is started.
        // basically the server will send a message to the client to validate that everything is okay.
        // done after everything is set up.
        // if client is not okay then the server will send all the stuff again to the client and the game would be restarted.
    }
}
