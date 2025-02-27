module sfu.cmpt371.group7.game {
    requires javafx.controls;
    requires javafx.fxml;


    opens sfu.cmpt371.group7.game to javafx.fxml;
    exports sfu.cmpt371.group7.game;
}