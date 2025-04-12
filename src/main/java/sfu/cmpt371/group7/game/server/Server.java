package sfu.cmpt371.group7.game.server;

import sfu.cmpt371.group7.game.model.Flag;
import sfu.cmpt371.group7.game.model.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

/**
 * The {@code Server} class handles multiplayer game server logic.
 * It manages client connections, game initializations, player communication and the win conditions.
 * It supports up to four players and deals with game events, for example,
 * player movement, selecting teams, handling the flags and respawning the players.
 * Communication is done over sockets using the TCP protocol.
 */
public class Server {
    private static final int NUM_PLAYERS = 4;
    private static final int PORT = 65000;
    private final List<ClientHandler> clients = new ArrayList<>();
    private int clientCount = 0;
    private boolean gameStarted = false;
    private final List<Player> PLAYERS = new ArrayList<>();
    private final List<Flag> flags = new ArrayList<>();
    private int redFlagCount = 0;
    private int blueFlagCount = 0;
    private int redTeamCount = 0;
    private int blueTeamCount = 0;

    private final int RED_1_X = 2;
    private final int RED_1_Y = 0;

    private final int RED_2_X = 17;
    private final int RED_2_Y = 0;

    private final int BLUE_1_X = 2;
    private final int BLUE_1_Y = 19;

    private final int BLUE_2_X = 17;
    private final int BLUE_2_Y = 19;

    public Server() {
        System.out.println("Server starting on port " + PORT);
    }

    /**
     * Starts the server and listens for client connections on a specified port.
     * For each connection, a new {@code ClientHandler} thread is initiated.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected clients
     * @param message The message to be broadcast to all the clients.
     */
    private void broadcast(String message) {
        System.out.println("Broadcasting: " + message);

        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Checks if the game should start based on player count
     * if the minimum number of players is reached then the
     * broadcast message is sent to all the players
     */
    private void checkGameStart() {
        if (!gameStarted && clientCount >= NUM_PLAYERS) {
            System.out.println("Starting game with " + clientCount + " players");
            gameStarted = true;
            broadcast("startGame");

            // Send all players' info to everyone
            for (Player player : PLAYERS) {
                broadcast("newPlayer " + player.getTeam() + " " + player.getX() + " " + player.getY() + " " + player.getName());
            }
        }
    }

    /**
     * Finds the player object by its name and returns it.
     *
     * @param name The name of the player to find.
     * @return The {@code Player} with the matching name, or {@code null} if that name is not found.
     */
    private Player findPlayerByName(String name) {
        for (Player player : PLAYERS) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Finds a flag by its name.
     *
     * @param name The name of the flag to find.
     * @return The {@code Flag} with same name, or {@code null} if it is not found.
     */
    private Flag findFlagByName(String name) {
        for (Flag f : flags) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Checks if a team has won by capturing enough number of flags.
     * Sends terminating message if above condition is fulfilled.
     */
    private void checkWinCondition() {
        if (redFlagCount >= 4) {
            broadcast("gameOver red");
        } else if (blueFlagCount >= 4) {
            broadcast("gameOver blue");
        }
    }

    /**
     * Decides if a specific grid position is unoccupied by any player.
     *
     * @param x The x-co-ord of particular position
     * @param y the y-co-ord of particular position
     * @return {@code true} is particular position is empty, {@code false} otherwise.
    */
    private boolean isNoPlayerAtPosition(int x, int y) {
        for (Player player : PLAYERS) {
            if (player.getX() == x && player.getY() == y) {
                return false;
            }
        }
        return true;
    }


    /**
     * Handles communication with a single client in a separate thread;
     * implemented as a Runnable to allow for multi-threading.
     * Each client has corresponding {@code ClientHandler} instance.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;

        /**
         * Constructs {@code ClientHandler} for particular client socket.
         * @param socket The socket that is connected to client.
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error setting up client handler: " + e.getMessage());
            }
        }

        /**
         * Sends a message to this specific client.
         *
         * @param message The message that needs to be sent.
         */
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        /**
         * Listens for messages from the client and sends them according to
         * the type of message. e.g., team selection.
         */
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    String[] parts = message.split(" ");
                    String messageType = parts.length > 0 ? parts[0] : "";

                    switch (messageType) {
                        case "teamSelection":
                            handleTeamSelection(parts);
                            sendClientToAllPlayers(message);
                            break;
                        case "movePlayer":
                            handleMovePlayer(parts);
                            break;
                        case "tellMeTheCurrentPlayers":
                            handleCurrentPlayers();
                            break;
                        case "exitGame":
                            handleExitGame(parts);
                            break;
                        case "flagCoordinates":
                            handleFlagCoordinates(parts);
                            break;
                        case "resendPlayers":
                            handleResendPlayers();
                            break;
                        case "gameOver":
                            handleGameOver(parts);
                            break;
                        case "captureDuration":
                            handleCaptureDuration(parts);
                            break;
                        default:
                            System.out.println("i dont know what you mean. when you wanna say less but you wanna say no" + messageType);
                            break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                handleDisconnect();
            }
        }

        /**
         * Respawn a player to their team's designated spawn point.
         * Notifies all clients of the updated position.
         *
         * @param player The player to respawn.
         */
        private void respawnPlayer(Player player) {
            int spawnX = 10, spawnY = 10;
            if (player.getTeam().equals("red")) {
                if(isNoPlayerAtPosition(2, 0)) {
                    System.out.println("spawning at 0,2");
                    spawnX = RED_1_X;
                    spawnY = RED_1_Y;
                } else if(isNoPlayerAtPosition(3, 0)){
                    System.out.println("spawning at 0,3");
                    spawnX = RED_2_X;
                    spawnY = RED_2_Y;
                }
            } else {
                if(isNoPlayerAtPosition(2, 19)) {
                    System.out.println("spawning at 2,19");
                    spawnX = BLUE_1_X;
                    spawnY = BLUE_1_Y;
                } else if(isNoPlayerAtPosition(3, 19)){
                    System.out.println("spawning at 3,19");
                    spawnX = BLUE_2_X;
                    spawnY = BLUE_2_Y;
                }
            }

            // Update player position
            player.setX(spawnX);
            player.setY(spawnY);

            // Notify all clients about respawn
            broadcast("respawnPlayer " + player.getName() + " " + spawnX + " " + spawnY);
            broadcast("movePlayer " + player.getName() + " " + spawnX + " " + spawnY);

            System.out.println("Respawning player " + player.getName() + " to " + spawnX + "," + spawnY);
        }

        /**
         * Handles team selection message sent by the client.
         * Adds the player to either the red or blue team and broadcasts their position.
         * Makes sure game starts after player joins.
         */
        private void handleTeamSelection(String[] parts) {
            if (parts.length >= 3) {
                String team = parts[1];
                String playerName = parts[2];
                this.playerName = playerName;

                int x, y;
                if (team.equals("red")) {
                    if(redTeamCount== 0){
                        x = RED_1_X;
                    }
                    else{
                        x = RED_2_X;
                    }
                    y = RED_1_Y;
                    redTeamCount++;
                } else {
                    if(blueTeamCount == 0){
                        x = BLUE_1_X;
                    }
                    else{
                        x = BLUE_2_X;
                    }
                    y = BLUE_1_Y;
                    blueTeamCount++;
                }

                Player player = new Player(team, x, y, playerName);

                boolean playerExists = false;
                for (Player p : PLAYERS) {
                    if (p.getName().equals(playerName)) {
                        playerExists = true;
                        break;
                    }
                }

                if (!playerExists) {
                    PLAYERS.add(player);
                    clientCount++;
                }

                broadcast("sendingPlayer " + player.getName() + " " + player.getTeam() + " " + player.getX() + " " + player.getY());
                broadcast("updateCount " + clientCount);

                checkGameStart();
            }
        }

        /**
         * Sends a message to all connected clients except the sender
         * to notify them that a player has joined a team.
         *
         * @param message The message that came which has team and player name.
         */
        private void sendClientToAllPlayers(String message) {
            for (ClientHandler client : clients) {
                if (client != null && !client.equals(this)) {
                    String team = message.split(" ")[1];
                    String playerName = message.split(" ")[2];

                    String info = "showPlayerJoined " + team + " " + playerName;
                    client.sendMessage(info);
                }
            }
        }

        /**
         * Handles move player message
         * first we update the location of the player, and then we check if the player has captured a flag
         * if then we broadcast the lock flag message to all the players.
         *
         * @param parts The message containing player name and new coordinates.
         * @throws InterruptedException If thread sleep or it gets block.
         */
        private void handleMovePlayer(String[] parts) throws InterruptedException {

            if (parts.length >= 4) {
                String playerName = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                updatePlayerPosition(playerName, x, y);

                //checkIfPlayerCapturedFlag(playerName, x, y);

                String moveMessage = "movePlayer " + playerName + " " + x + " " + y;
                broadcast(moveMessage);
            }
        }

        /**
         * Updates player position with new coordinates in the server's state
         *
         * @param name The name of the player.
         * @param x The new x-co-ord.
         * @param y The new y-co-ord.
         */
        private void updatePlayerPosition(String name, int x, int y) {
            for (Player player : PLAYERS) {
                if (player.getName().equals(name)) {
                    player.setX(x);
                    player.setY(y);
                    break;
                }
            }
        }

        /**
         * Handles request from the client for current player count.
         * Only used to get the size of the players and to set the number of players label in the UI
         */
        private void handleCurrentPlayers() {
            sendMessage("sizeOfPlayersIs " + PLAYERS.size());
        }

        /**
         * Handles a request from the client for the current number of plaeyers.
         * Sends back the number of players currently connected.
         */
        private void handleExitGame(String[] parts) {
            if (parts.length >= 2) {
                String name = parts[1];

                // Remove player from list
                PLAYERS.removeIf(p -> p.getName().equals(name));

                clientCount--;
                if (clientCount < 0) {
                    System.err.println("Client count is negative. Something went wrong.");
                    return;
                }

                broadcast("playerLeft " + name);
                broadcast("sizeOfPlayersIs " + PLAYERS.size());

                endServer();
            }
        }

        /**
         * Handles flag coordinates message.
         * Stores the coordinates of all flags sent from the client.
         */
        private void handleFlagCoordinates(String[] parts) {
            //flagCoordinates <flag1.x> <flag1.y> <flag2.x> <flag2.y> <flag3.x> <flag3.y>
            int NUM_FLAGS = 7;
            if (parts.length >= NUM_FLAGS * 2 + 1) {
                try {
                    for (int i = 0; i < NUM_FLAGS; i++) {
                        flags.add(new Flag(Integer.parseInt(parts[2 * i + 1]), Integer.parseInt(parts[2 * i + 2]), "flag" + (i + 1)));
                    }
                    System.out.println("Flag coordinates set");
                } catch (Exception e) {
                    System.err.println("Error parsing flag coordinates: " + e.getMessage());
                }
            }
        }

        /**
         * Handles resend all current players request and flag data to a client.
         * Used to get the resend the players in case of an error to get the location of the players
         */
        private void handleResendPlayers() {
            System.out.println("Resending all players to client");

            sendMessage("sizeOfPlayersIs " + PLAYERS.size());

            for (Player player : PLAYERS) {
                sendMessage("sendingPlayer " + player.getName() + " " + player.getTeam() + " " + player.getX() + " " + player.getY());
            }

            for (Flag flag : flags) {
                if (flag != null && flag.isCaptured()) {
                    sendMessage("lockFlag " + flag.getName());
                }
            }
        }

        /**
         * Handles game over message.
         * Determines winner based on flag counts and broadcasts the result.
         */
        private void handleGameOver(String[] parts) {
            String winner = parts.length > 1 ? parts[1] : "";
            if (winner.isEmpty()) {
                // Determine winner based on flag count
                if (redFlagCount > blueFlagCount) {
                    winner = "red";
                } else if (blueFlagCount > redFlagCount) {
                    winner = "blue";
                } else {
                    winner = "tie";
                }
            }

            broadcast("gameOver " + winner);
        }

        /**
         * Handles client disconnection
         * Cleans the client from the server and from the game
         */
        private void handleDisconnect() {
            try {
                if (playerName != null) {
                    PLAYERS.removeIf(p -> p.getName().equals(playerName));
                    clientCount--;
                    broadcast("playerLeft " + playerName);
                    broadcast("sizeOfPlayersIs " + clientCount);
                }

                synchronized (clients) {
                    clients.remove(this);
                }

                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }

        /**
         * Handle capture duration message from clients
         * Checks if duration is within valid range (4.5-5.2 seconds)
         * If valid, flag is captured and any other players on the flag are respawned
         * If invalid, the attempting player is respawned.
         */
        private void handleCaptureDuration(String[] parts) {
            // captureDuration <player name> <flag name> <time (sec)>
            if (parts.length >= 4) {
                String playerName = parts[1];
                String flagName = parts[2];
                double duration = Double.parseDouble(parts[3]);

                Flag flagToCapture = findFlagByName(flagName);
                Player attemptingPlayer = findPlayerByName(playerName);

                if (flagToCapture != null && attemptingPlayer != null) {
                    // Check if capture duration is within valid range
                    double MIN_CAPTURE_DURATION = 3;
                    double MAX_CAPTURE_DURATION = 4;
                    if (duration >= MIN_CAPTURE_DURATION && duration <= MAX_CAPTURE_DURATION && !flagToCapture.isCaptured()) {
                        // Successful capture
                        flagToCapture.setCaptured(true);
                        broadcast("flagCaptured " + playerName + " " + flagName);

                        // Update team score
                        if (attemptingPlayer.getTeam().equals("red")) {
                            redFlagCount++;
                        } else {
                            blueFlagCount++;
                        }

                        // Check for other players on the same flag position and respawn them
                        for (Player player : PLAYERS) {
                            if (!player.getName().equals(playerName) &&
                                    player.getX() == flagToCapture.getX() &&
                                    player.getY() == flagToCapture.getY()) {

                                respawnPlayer(player);
                            }
                        }

                        // Check if this capture results in a win
                        checkWinCondition();
                    } else {
                        // Failed capture - respawn the player
                        respawnPlayer(attemptingPlayer);
                    }
                }
            }
        }
    }

    /**
     * Ends the server if all clients are disconnected.
     * This is a graceful shutdown trigger when client count drops to zero.
    */
    private void endServer() {
        System.out.println("ending server");
        if (clientCount == 0) {
            exit(0);
        }
    }
}