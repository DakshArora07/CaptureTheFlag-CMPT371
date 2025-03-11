package sfu.cmpt371.group7.game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Maze extends Application {

    private final int rows = 10;
    private final int cols = 10;
    private final char[][] grid;

    public Maze(){
        grid = new char[rows][cols];
        initGrid();

    }

    public void initGrid(){
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < cols; j++){
                grid[i][j] = ' ';
            }
        }
        addBarriers();
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

    @Override
    public void start(Stage stage){
        GridPane gridPane = new GridPane();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Rectangle rect = new Rectangle(30, 30);
                if (grid[i][j] == 'X') {
                    rect.setFill(Color.BLACK);
                } else {
                    rect.setFill(Color.WHITE);
                }
                rect.setStroke(Color.GRAY);
                gridPane.add(rect, j, i);
            }
        }

        Scene scene = new Scene(gridPane, 300, 300);
        stage.setTitle("Maze");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
