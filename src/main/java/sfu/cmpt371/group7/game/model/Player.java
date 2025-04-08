package sfu.cmpt371.group7.game.model;

/**
 * Represents a player in the Capture the Flag game.
 * <p>
 * Each player belongs to a team, has a name, coordinates in the maze,
 * and tracks whether they are holding a captured flag or currently
 * attempting to capture one.
 *
 * @author Kabir Singh Sidhu
 * @version 1.0
 */
public class Player {

    /** The team to which the player belongs. */
    private String team;

    /** The x-coordinate of the player's position in the maze. */
    private int x;

    /** The y-coordinate of the player's position in the maze. */
    private int y;

    /** The name of the player. */
    private String name;

    /** Whether the player has captured a flag. */
    private boolean capturedFlag;

    /** Whether the player is currently in the process of capturing a flag. */
    private boolean capturingStatus;

    /**
     * Constructs a new Player object with specified team, position, and name.
     *
     * @param team the team the player belongs to
     * @param x    the initial x-coordinate of the player
     * @param y    the initial y-coordinate of the player
     * @param name the name of the player
     */
    public Player(String team, int x, int y, String name) {
        this.team = team;
        this.x = x;
        this.y = y;
        this.name = name;
        this.capturedFlag = false;
        this.capturingStatus = false;
    }

    /**
     * Sets the player's team.
     * @param team the new team name
     */
    public void setTeam(String team) {
        this.team = team;
    }

    /**
     * Sets the player's x-coordinate.
     * @param x the new x-coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the player's y-coordinate.
     * @param y the new y-coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Sets the player's name.
     * @param name the new name of the player
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the flag captured status for the player.
     * @param capturedFlag true if the player has captured a flag, false otherwise
     */
    public void setCapturedFlag(boolean capturedFlag) {
        this.capturedFlag = capturedFlag;
    }

    /**
     * Sets whether the player is currently trying to capture a flag.
     * @param flag true if the player is capturing a flag, false otherwise
     */
    public void setCapturingStatus(boolean flag) {
        this.capturingStatus = flag;
    }

    /**
     * Gets the player's team.
     * @return the team name
     */
    public String getTeam() {
        return team;
    }

    /**
     * Gets the player's x-coordinate.
     * @return the x-coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the player's y-coordinate.
     * @return the y-coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the player's name.
     * @return the name of the player
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the player is holding a captured flag.
     * @return true if the player has captured a flag, false otherwise
     */
    public boolean getCapturedFlag() {
        return capturedFlag;
    }

    /**
     * Returns whether the player is currently trying to capture a flag.
     * @return true if the player is capturing a flag, false otherwise
     */
    public boolean getCapturingStatus() {
        return capturingStatus;
    }

}
