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
                    if (message.startsWith("newPlayer")) {
                        synchronized (players) {
                            Player player = new Player(message.split(" ")[1], Integer.parseInt(message.split(" ")[2]), Integer.parseInt(message.split(" ")[3]));
                            players.add(player);
                        }
                        broadcast(message); // send the new player message to all clients
                        System.out.println("num of players = " + players.size());
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

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }
}