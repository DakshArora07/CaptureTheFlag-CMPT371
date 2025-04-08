package sfu.cmpt371.group7.game.model;

/**
 * Represents a flag in the maze.
 * <p>
 * A flag has coordinates (x, y), a name, and a captured status.
 * This object can be used to track the state of the flag during gameplay,
 * such as whether it's been captured or not.
 *
 * @author Kabir Singh Sidhu
 * @version 1.0
 */

public class Flag {

    /** The x-coordinate of the flag. */
    private int x;

    /** The y-coordinate of the flag. */
    private int y;

    /** The captured status of the flag. */
    private boolean captured;

    /** The name of the flag. */
    private String name;

    /**
     * Constructs a new Flag object with the given coordinates and name.
     * @param x    the x-coordinate of the flag
     * @param y    the y-coordinate of the flag
     * @param name the name of the flag
     */
    public Flag(int x, int y, String name){
        this.x = x;
        this.y = y;
        this.name = name;
        this.captured = false;
    }


    /**
     * Gets the x-coordinate of the flag.
     * @return the x-coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the x-coordinate of the flag.
     * @param x the new x-coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Gets the y-coordinate of the flag.
     * @return the y-coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the y-coordinate of the flag.
     * @param y the new y-coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Checks whether the flag has been captured.
     * @return true if the flag is captured, false otherwise
     */
    public boolean isCaptured() {
        return captured;
    }

    /**
     * Sets the captured status of the flag.
     * @param captured true if the flag is captured, false otherwise
     */
    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    /**
     * Gets the name of the flag.
     * @return the name of the flag
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the flag.
     * @param name the new name of the flag
     */
    public void setName(String name) {
        this.name = name;
    }
}
