package sfu.cmpt371.group7.game;

import sfu.cmpt371.group7.game.client.Console;
import sfu.cmpt371.group7.game.client.Menu;

/**
 * Entry point for the Capture the Flag game.
 * <p>
 * This class delegates control to the {@link Console} class to launch the client-side
 * application or interface for the game.
 */

public class Game {
    public static void main(String[] args) {
        Menu.main(args);
    }
}
