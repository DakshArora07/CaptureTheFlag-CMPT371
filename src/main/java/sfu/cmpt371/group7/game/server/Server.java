package sfu.cmpt371.group7.game.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import sfu.cmpt371.group7.game.Player;

public class Server {
    private static final int PORT = 1234;
    private static List<Player> players = new ArrayList<>();
    private static List<PrintWriter> clientWriters = new ArrayList<>();
    private static int clientCount = 0;
    private final static int MIN_PLAYERS = 3;

    public static void main(String[] args) {
        System.out.println("Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    String parts[] = message.split(" ");
                    if (message.startsWith("newPlayer")) {
                        // token to add a new player
                        synchronized (players) {
                            Player player = new Player(message.split(" ")[1], Integer.parseInt(message.split(" ")[2]), Integer.parseInt(message.split(" ")[3]));
                            players.add(player);
                        }
                        broadcast(message); // send the new player message to all clients
                        System.out.println("num of players = " + players.size());
                    }
                    else if(message.startsWith("movePlayer")){
                        // token to move a player

                        String team = parts[1];
                        int newX = Integer.parseInt(parts[2]);
                        int newY = Integer.parseInt(parts[3]);
                        synchronized (players) {
                            Player player = players.stream()
                                    .filter(p -> p.getTeam().equals(team))
                                    .findFirst()
                                    .orElse(null);
                            if (player != null) {
                                player.setX(newX);
                                player.setY(newY);
                            }
                        }
                        broadcast(message); // Broadcast movement to all clients

                    }

                    else if(message.startsWith("teamSelection")){
                        clientCount++;
                        broadcast("updateCount " + clientCount);
                        System.out.println("in team selection");
                        startGame();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                    }
                }
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void startGame(){
            // if there are atleast MIN_PLAYERS start the game
            // send a message to all clients to start the game

            if(clientCount >= MIN_PLAYERS){
                broadcast("startGame");
            }
            else{
                System.out.println("Not enough players to start the game");
            }

        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }
}