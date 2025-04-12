package sfu.cmpt371.group7.game.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sfu.cmpt371.group7.game.server.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * The {@code Menu} class provides the GUI for the main menu of the
 * game, letting the user to either host a new game or join already started game.
 *
 * <p> This JavaFX application serves as the game's entry point.</p>
*/
public class Menu extends Application {
    /**
     * Starts the JavaFX application and displays main menu window.
     *
     * @param primaryStage The main stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CAPTURE THE FLAG");

        VBox root = setupRoot();

        //Label titleLabel = setupTitleLabel();

        // New Game Button
        Button newGameButton = new Button("NEW GAME");
        styleButton(newGameButton, "#1FB6FF", "#009EE0");
        newGameButton.setEffect(new GaussianBlur(10));


        // Join Game Button
        Button joinGameButton = new Button("JOIN GAME");
        styleButton(joinGameButton, "#FF1744", "#D50032");
        joinGameButton.setEffect(new GaussianBlur(10));

        newGameButton.setOnAction(e -> showNewGameDialog(primaryStage));
        joinGameButton.setOnAction(e -> showJoinGameDialog(primaryStage));

        root.getChildren().addAll(newGameButton, joinGameButton);

        setupScene(primaryStage, root);
    }

    /**
     * Configures and displays the main application scene.
     *
     * @param primaryStage The main application window
     * @param root The root layout of the scene.
     */
    public static void setupScene(Stage primaryStage, VBox root) {
        Scene scene = new Scene(root, 500, 490);
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
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(Menu.class.getResourceAsStream("/sfu/cmpt371/group7/game/gameIcon.png"))));
        primaryStage.show();
    }


    /**
     * Creates and returns the Vbox with background image and styles.
     *
     * @return the VBox layout.
     */
    public static VBox setupRoot() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        //root.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e); -fx-background-radius: 8;");
        root.setBackground(new Background(new BackgroundImage(
                new Image(Objects.requireNonNull(Menu.class.getResourceAsStream("/sfu/cmpt371/group7/game/BackgroundImage.png"))),
                null,
                null,
                BackgroundPosition.CENTER,
                null
        )));
        root.setAlignment(Pos.CENTER);

        return root;
    }

    /**
     * Displays a dialog for creating a new game. It opens the launches
     * the server and opens game console.
     *
     * @param stage The parent stage to close when a new game starts.
     */
    private void showNewGameDialog(Stage stage) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Create New Game");
        dialogStage.getIcons().add(new Image(Objects.requireNonNull(Menu.class.getResourceAsStream("/sfu/cmpt371/group7/game/gameIcon.png"))));

        VBox dialogVBox = new VBox(15);
        dialogVBox.setPadding(new Insets(30));
        dialogVBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");
        dialogVBox.setAlignment(Pos.CENTER);

        String address = "Unknown";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {}

        Label ipLabel = new Label("Your IP: " + address);
        ipLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Button startButton = new Button("Start Game");
        styleButton(startButton, "#27ae60", "#1e8449");

        String ip = address;
        startButton.setOnAction(e -> {

            Thread serverThread = new Thread(() -> new Server().start());
            serverThread.setDaemon(true); // ensures it closes when the app exits
            serverThread.start();

            try {
                new Console(ip);
                dialogStage.close();
                stage.close();
            } catch (Exception ex) {
                showAlert("Failed to launch game screen.");
            }
        });

        dialogVBox.getChildren().addAll(ipLabel, startButton);

        Scene dialogScene = new Scene(dialogVBox, 350, 250);
        dialogStage.setScene(dialogScene);
        dialogStage.setResizable(false);
        dialogStage.centerOnScreen();
        dialogStage.show();
    }

    /**
     *
     * Displays a dialog for joining a preloaded; existing game using an IP addr.
     *
     * @param stage The parent stage to close when one joins a game.
     */
    private void showJoinGameDialog(Stage stage) {
        Stage joinStage = new Stage();
        joinStage.setTitle("Join Game");
        joinStage.getIcons().add(new Image(Objects.requireNonNull(Menu.class.getResourceAsStream("/sfu/cmpt371/group7/game/gameIcon.png"))));
        VBox joinBox = new VBox(15);
        joinBox.setPadding(new Insets(30));
        joinBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");
        joinBox.setAlignment(Pos.CENTER);

        Label prompt = new Label("Enter host IP address:");
        prompt.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
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
                new Console(ip);
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

    /**
     * Styles button with specified background and hover colors.
     *
     * @param button The button to style
     * @param color The default color
     * @param hoverColor The background color when hovered.
     */
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

    /**
     * Display an error alert with particular message
     *
     * @param msg The message to display while alerting
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Main to launch application
     *
     * @param args Command-line argument.
     */
    public static void main(String[] args) {
        launch(args);
    }
}