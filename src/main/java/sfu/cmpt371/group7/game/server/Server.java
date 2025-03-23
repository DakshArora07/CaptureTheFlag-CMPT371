package sfu.cmpt371.group7.game.server;

import io.github.cdimascio.dotenv.Dotenv;
import sfu.cmpt371.group7.game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * this class is responsible for starting the server and handling the clients..
 */
public class Server {

    /*
    * used to get the port number and the minimum players required to start the game from the .even file in the root directory.
     */
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .filename("var.env")
            .load();

    private static final int PORT_NUMBER = Integer.parseInt(dotenv.get("PORT_NUMBER"));
    private static final int MIN_PLAYERS_REQUIRED = Integer.parseInt(dotenv.get("MIN_PLAYERS"));

    /*
    * PORT = port number
    * MIN_PLAYERS = minimum players required to start the game
    * clients = list of clients connected to the server
    * clientCount = total count of clients connected to the server
    * gameStarted = boolean to check if the game has started or not
    * random = to generate random numbers
    * PLAYERS = list of players connected to the server
    *
     */
    private static final int PORT = PORT_NUMBER;
    private static final int MIN_PLAYERS = MIN_PLAYERS_REQUIRED; // Minimum players needed to start a game
    private List<ClientHandler> clients = new ArrayList<>();
    private int clientCount = 0;
    private boolean gameStarted = false;
    private final Random random = new Random();
    private final List<Player> PLAYERS = new ArrayList<>();

    public Server() {
        System.out.println("Server starting on port " + PORT);
    }

    /*
    * this method is responsible for starting the server and handling the clients.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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

    /*
    * this method is responsible for broadcasting the message to all the clients.
    * message = message to be broadcasted
     */
    private void broadcast(String message) {
        System.out.println("Broadcasting: " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /*
    * this method is responsible for checking if the game has started or not.
    * if the game has not started and the client count is greater than or equal to the minimum players required to start the game,
    * if the condition is satisfied, the game will start.
    * the game will start by sending a message to all the clients to add all the players to the maze.
    * the message will start a game. the message is received by the console class b/c that is where the player essentially is.
    * the message is received by the console class and the maze class is initialized and the player object is passed to the maze class.
    * check concole for more info.
     */
    private void checkGameStart() {
        if (!gameStarted && clientCount >= MIN_PLAYERS) {
            // convey to all the players to add all the players to the maze
            System.out.println("Starting game with " + clientCount + " players");
            gameStarted = true;
            broadcast("startGame");
            //System.out.println("uerakaaaaaaaaaaaaaa");

            for(int i=0; i<PLAYERS.size(); i++){
                System.out.println("number of players: " + PLAYERS.size());
                broadcast("newPlayer " + PLAYERS.get(i).getTeam() + " " + PLAYERS.get(i).getX() + " " + PLAYERS.get(i).getY() + " " + PLAYERS.get(i).getName());
            }
        }
    }

    /*
    * this class is responsible for handling the clients.
    * the client handler is responsible for sending and receiving messages from the clients.
     */
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error setting up client handler: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        /*
        * this method is responsible for receiving messages from the clients.
        * the messages received are teamSelection and movePlayer.
        * teamSelection is received when a player selects a team.
        * movePlayer is received when a player moves.
        * teamSelection -> the message is split and the team and the player name is extracted.
        * the spawn position is determined based on the team.
        * the player object is created and added to the PLAYERS list.
        * the client count is incremented.
        * the new count is broadcasted to all the clients.
        * checkGameStart is called to check if the game should start or not.
        * movePlayer -> the message is broadcasted to all the clients.
        * tellMeTheCurrentPlayers -> the message is broadcasted to all the clients.
        * exitGame -> the message is broadcasted to all the clients.
        * the player is removed from the PLAYERS list.
        * the client count is decremented.
        * the new count is broadcasted to all the clients.
         */
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    String[] parts = message.split(" ");

                    if (message.startsWith("teamSelection")) {
                        // Format: teamSelection <team> <playerName>
                        String team = parts[1];
                        String playerName = parts[2];

                        // Determine spawn position based on team
                        int x, y;
                        if (team.equals("red")) {
                            // Red team spawns on the left side
                            x = random.nextInt(5) + 1;
                            y = random.nextInt(10) + 1;
                        } else {
                            // Blue team spawns on the right side
                            x = random.nextInt(5) + 14;
                            y = random.nextInt(10) + 1;
                        }

                        // increment client count
                        clientCount++;

                        // create a new player object
                        Player player = new Player(team, x, y, playerName);
                        PLAYERS.add(player);

                        // notify all clients of new count
                        broadcast("updateCount " + clientCount);

                        // check if we should start the game
                        checkGameStart();
                    }
                    else if (message.startsWith("movePlayer")) {
                        // format: movePlayer <name of player> <x> <y>
                        // just broadcast this to all clients
                        broadcast(message);
                    }
                    else if(message.startsWith("tellMeTheCurrentPlayers")){
                        // tell the maze that there are PLAYERS.size() players in the session
                        broadcast("sizeOfPlayersIs " + String.valueOf(PLAYERS.size()));
                    }
                    else if(message.startsWith("exitGame")){
                        // exit the game, the second arg is the player name.
                        // remove the player from the PLAYERS list
                        // decrement the client count
                        // broadcast the new count

                        String playerName = parts[1];
                        for(int i=0; i<PLAYERS.size(); i++){
                            if(PLAYERS.get(i).getName().equals(playerName)){
                                PLAYERS.remove(i);
                                break;
                            }
                        }
                        clientCount--;
                        broadcast("sizeOfPlayersIs " + String.valueOf(PLAYERS.size()));

                    }
                    else if(message.startsWith("gameOver")){
                        // the game is over b\c the timer ran out.
                        // send message to all the players in the game and compute the winner based on who captured the most flags
                        broadcast("gameOver " + "<send the winning team info here>");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                    clientCount--;
                    broadcast("updateCount " + clientCount);
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}