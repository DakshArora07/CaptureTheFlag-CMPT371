package sfu.cmpt371.group7.game.model;

/*
    * this class is responsible for creating the flag object.
    * x and y are the coordinates of the flag.
    * captured is the bool value to check if the flag is captured or not.
    * name is going to be the unique name of the flag.
    * this flag will be placed in the maze.
    * useful b\c will be able to identify which flag is captured by using the unique name.
 */
public class Flag {

    private int x;
    private int y;
    private boolean captured;
    private String name;

    public Flag(int x, int y, String name){
        this.x = x;
        this.y = y;
        this.name = name;
        this.captured = false;
    }


    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
