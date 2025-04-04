module sfu.cmpt371.group7.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires dotenv.java;
    requires java.logging;


    opens sfu.cmpt371.group7.game to javafx.fxml;
    exports sfu.cmpt371.group7.game;
    opens sfu.cmpt371.group7.game.ui to javafx.fxml;
    exports sfu.cmpt371.group7.game.ui;
    opens sfu.cmpt371.group7.game.logistics to javafx.fxml;
    exports sfu.cmpt371.group7.game.logistics;
}