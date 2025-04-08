module sfu.cmpt371.group7.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires dotenv.java;
    requires java.logging;


    opens sfu.cmpt371.group7.game.client to javafx.fxml;
    exports sfu.cmpt371.group7.game.client;
    opens sfu.cmpt371.group7.game.model to javafx.fxml;
    exports sfu.cmpt371.group7.game.model;
    exports sfu.cmpt371.group7.game;
    opens sfu.cmpt371.group7.game to javafx.fxml;
}