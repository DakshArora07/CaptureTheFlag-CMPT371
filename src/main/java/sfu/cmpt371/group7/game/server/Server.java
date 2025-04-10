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
 * this class is responsible for starting the server and handling the clients.
 */
public class Server {
    private static final int NUM_PLAYERS = 1;
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
     * starts the server and listens for client connections
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
     * broadcasts a message to all connected clients
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
     * checks if the game should start based on player count
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
     * find a player by their name
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
     * find a flag by its name
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
     * check if a team has won
     */
    private void checkWinCondition() {
        if (redFlagCount >= 4) {
            broadcast("gameOver red");
        } else if (blueFlagCount >= 4) {
            broadcast("gameOver blue");
        }
    }

    private boolean isNoPlayerAtPosition(int x, int y) {
        for (Player player : PLAYERS) {
            if (player.getX() == x && player.getY() == y) {
                return false;
            }
        }
        return true;
    }


    /**
     * this class handles communication with a single client
     * implemented as a Runnable to allow for multi-threading
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;

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
         * sends a message to this client
         */
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        /**
         * handles incoming messages from the client
         * new and improved switch case statement to handle different message types from the server
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
         * Respawn a player to their team's spawn point
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
         * handle team selection message
         * place the red team players in left side and blue team players on the right side
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

        /*
         * send client has joined team color to all players
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
         * handle move player message
         * first we update the location of the player, and then we check if the player has captured a flag
         * if then we broadcast the lock flag message to all the players
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
         * update player position in the server's state
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
         * handle request for current player count
         * only used to get the size of the players and to set the number of players label in the UI
         */
        private void handleCurrentPlayers() {
            sendMessage("sizeOfPlayersIs " + PLAYERS.size());
        }

        /**
         * handle exit game message
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
         * handle flag coordinates message
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
         * handle resend players request
         * used to get the resend the players in case of an error to get the location of the players
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
         * handle game over message
         * also opens the results window to show the team that won
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
         * handle client disconnection
         * remove the client from the server and from the game
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
         * If invalid, the attempting player is respawned
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

    private void endServer() {
        System.out.println("ending server");
        if (clientCount == 0) {
            exit(0);
        }
    }
}