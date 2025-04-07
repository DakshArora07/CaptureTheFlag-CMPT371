package sfu.cmpt371.group7.game.server;

import io.github.cdimascio.dotenv.Dotenv;
import sfu.cmpt371.group7.game.logistics.Flag;
import sfu.cmpt371.group7.game.logistics.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * this class is responsible for starting the server and handling the clients.
 */
public class Server {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    private static final int PORT_NUMBER = Integer.parseInt(dotenv.get("PORT_NUMBER"));
    private static final int MIN_PLAYERS_REQUIRED = Integer.parseInt(dotenv.get("MIN_PLAYERS"));

    private static final int PORT = PORT_NUMBER;
    private static final int MIN_PLAYERS = MIN_PLAYERS_REQUIRED;

    private List<ClientHandler> clients = new ArrayList<>();
    private int clientCount = 0;
    private boolean gameStarted = false;
    private final Random random = new Random();
    private final List<Player> PLAYERS = new ArrayList<>();
    private Flag flag1; // the right one
    private Flag flag2; // left
    private Flag flag3; // bottom
    private int redFlagCount = 0;
    private int blueFlagCount = 0;

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
            e.printStackTrace();
        }
    }

    /**
     * broadcasts a message to all connected clients
     */
    private void broadcast(String message) {
        System.out.println("Broadcasting: " + message);

        synchronized(clients) {
            for (ClientHandler client : clients) {
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * checks if the game should start based on player count
     * if the mminimum number of players is reached then the
     * broadcast message is sent to all the players
     */
    private void checkGameStart() {
        if (!gameStarted && clientCount >= MIN_PLAYERS) {
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
     * checks if a move has resulted in capturing a flag
     * checks if the players are in the same coordinates as the flag
     * if it is true then the broadcast message is sent to all the players to lock the flag
     * the red flag count and the blue flag count is also increased
     */
    //private boolean checkIfPlayerCapturedFlag(String name, int x, int y) throws InterruptedException {

//        Player player = findPlayerByName(name);
//        assert player != null;
//
//        Flag flag = null;
//        if (flag1 != null && flag1.getX() == x && flag1.getY() == y && !flag1.isCaptured()){
//            flag = flag1;
//        } else if (flag2 != null && flag2.getX() == x && flag2.getY() == y && !flag2.isCaptured()) {
//            flag = flag2;
//        } else if (flag3 != null && flag3.getX() == x && flag3.getY() == y && !flag3.isCaptured()) {
//            flag = flag3;
//        }
//
//        if (flag != null) {
//            player.setCapturingStatus(true);
//            int timer = 10;
//            broadcast("Timer started");
//            while (timer > 0) {
//                Thread.sleep(Duration.ofSeconds(1));
//                if (flag.getX() != x || flag.getY() != y) {
//                    broadcast("Player" + player.getName() + "moved");
//                    broadcast("exit timer");
//                    break;
//                }
//                timer --;
//                broadcast("Time remaining: " + timer);
//
//            }
//            if (timer == 0 && x == flag.getX() && y == flag.getY()) {
//                player.setCapturingStatus(false);
//                player.setCapturedFlag(true);
//                flag.setCaptured(true);
//                broadcast("flagCaptured " + name + " " + flag.getName());
//                if (player.getTeam().equals("red")) {
//                    redFlagCount++;
//                } else {
//                    blueFlagCount++;
//                }
//                checkWinCondition();
//            }
//        }




        /*if (flag1 != null && flag1.getX() == x && flag1.getY() == y && !flag1.isCaptured()) {
            //flag1.setCaptured(true);

            broadcast("flagCaptured " + name + " " + flag1.getName());
            broadcast("lockFlag " + flag1.getName());
            checkWinCondition();
            return true;
        } else if (flag2 != null && flag2.getX() == x && flag2.getY() == y && !flag2.isCaptured()) {
            flag2.setCaptured(true);


            broadcast("flagCaptured " + name + " " + flag2.getName());
            broadcast("lockFlag " + flag2.getName());
            checkWinCondition();
            return true;
        } else if (flag3 != null && flag3.getX() == x && flag3.getY() == y && !flag3.isCaptured()) {
            flag3.setCaptured(true);

            broadcast("flagCaptured " + name + " " + flag3.getName());
            broadcast("lockFlag " + flag3.getName());
            checkWinCondition();
            return true;
        }

        return false;
    }
         */


        private boolean checkIfPlayerCapturedFlag(String name, int x, int y) {
            if (flag1 != null && flag1.getX() == x && flag1.getY() == y && !flag1.isCaptured()) {
                flag1.setCaptured(true);

                Player player = findPlayerByName(name);
                if (player != null) {
                    if (player.getTeam().equals("red")) {
                        redFlagCount++;
                    } else {
                        blueFlagCount++;
                    }
                }

                broadcast("flagCaptured " + name + " " + flag1.getName());
                broadcast("lockFlag " + flag1.getName());
                checkWinCondition();
                return true;
            } else if (flag2 != null && flag2.getX() == x && flag2.getY() == y && !flag2.isCaptured()) {
                flag2.setCaptured(true);

                Player player = findPlayerByName(name);
                if (player != null) {
                    if (player.getTeam().equals("red")) {
                        redFlagCount++;
                    } else {
                        blueFlagCount++;
                    }
                }

                broadcast("flagCaptured " + name + " " + flag2.getName());
                broadcast("lockFlag " + flag2.getName());
                checkWinCondition();
                return true;
            } else if (flag3 != null && flag3.getX() == x && flag3.getY() == y && !flag3.isCaptured()) {
                flag3.setCaptured(true);

                Player player = findPlayerByName(name);
                if (player != null) {
                    if (player.getTeam().equals("red")) {
                        redFlagCount++;
                    } else {
                        blueFlagCount++;
                    }
                }

                broadcast("flagCaptured " + name + " " + flag3.getName());
                broadcast("lockFlag " + flag3.getName());
                checkWinCondition();
                return true;
            }
            return false;
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
     * check if a team has won
     */
    private void checkWinCondition() {
        if (redFlagCount >= 2) {
            broadcast("gameOver red");
        } else if (blueFlagCount >= 2) {
            broadcast("gameOver blue");
        }
    }

    /**
     * this class handles communication with a single client
     * implemented as a Runnable to allow for multi-threading
     */
    private class ClientHandler implements Runnable {
        private Socket socket;
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
                e.printStackTrace();
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
         * handle team selection message
         * place the red team players in left side and blue team players in the right side
         */
        private void handleTeamSelection(String[] parts) {
            if (parts.length >= 3) {
                String team = parts[1];
                String playerName = parts[2];
                this.playerName = playerName;

                int x, y;
                if (team.equals("red")) {
                    x = random.nextInt(5) + 1;
                    y = 0;
                } else {
                    x = random.nextInt(5) + 1;
                    y = 19;
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
            for(ClientHandler client : clients) {
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
         * first we update the location of the player and then we check if the player has captured a flag
         * if then we broadcast the lock flag message to all the players
         */
        private void handleMovePlayer(String[] parts) throws InterruptedException {

            if (parts.length >= 4) {
                String playerName = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                updatePlayerPosition(playerName, x, y);

                checkIfPlayerCapturedFlag(playerName, x, y);

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
                if(clientCount < 0){
                    System.err.println("Client count is negative. Something went wrong.");
                    return;
                }

                broadcast("playerLeft " + name);
                broadcast("sizeOfPlayersIs " + PLAYERS.size());
            }
        }

        /**
         * handle flag coordinates message
         */
        private void handleFlagCoordinates(String[] parts) {
            //flagCoordinates <flag1.x> <flag1.y> <flag2.x> <flag2.y> <flag3.x> <flag3.y>
            if (parts.length >= 7) {
                try {
                    flag1 = new Flag(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), "flag1");
                    flag2 = new Flag(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), "flag2");
                    flag3 = new Flag(Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), "flag3");
                    System.out.println("Flag coordinates set");
                } catch (Exception e) {
                    System.err.println("Error parsing flag coordinates: " + e.getMessage());
                    e.printStackTrace();
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

            if (flag1 != null && flag1.isCaptured()) {
                sendMessage("lockFlag " + flag1.getName());
            }
            if (flag2 != null && flag2.isCaptured()) {
                sendMessage("lockFlag " + flag2.getName());
            }
            if (flag3 != null && flag3.isCaptured()) {
                sendMessage("lockFlag " + flag3.getName());
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

                synchronized(clients) {
                    clients.remove(this);
                }

                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}