package com.quizgame;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class QuizServer extends WebSocketServer {
    private static final int PORT = 1234;
    private static final Set<WebSocket> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<WebSocket, String> usernames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();

    // Question bank (static for simplicity)
    private static final String[] QUESTIONS = {
            "Q: What’s 2+2? A)1 B)2 C)3 D)4",
            "Q: Capital of France? A)London B)Paris C)Berlin D)Rome",
            "Q: How many planets? A)7 B)8 C)9 D)10"
    };
    private static int questionIndex = 0;

    public QuizServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        players.add(conn);
        System.out.println("New player connected: " + conn.getRemoteSocketAddress());
        conn.send("Enter your username:");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        players.remove(conn);
        String username = usernames.remove(conn);
        if (username != null) {
            scores.remove(username);
            sendToAllExcludingSender(username + " has left the game.", null);
            System.out.println("Player '" + username + "' disconnected.");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (usernames.get(conn) == null) {
            String username = message.trim();
            if (username.isEmpty()) username = "Anonymous";
            usernames.put(conn, username);
            scores.put(username, 0);
            sendToAllExcludingSender(username + " has joined the game!", null);
            conn.send("Welcome, " + username + "! Get ready for questions.");
            return;
        }
        // Placeholder for answer handling (to be expanded by Answer Processing role)
        if (message.startsWith("answer")) {
            System.out.println(usernames.get(conn) + " submitted: " + message);
            return;
        }
        sendToAllExcludingSender(usernames.get(conn) + ": " + message, conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Quiz Server started on port " + PORT);
        // Start question broadcast thread
        new Thread(new QuestionEngine()).start();
    }

    // Custom broadcast method (renamed to avoid ambiguity)
    private void sendToAllExcludingSender(String message, WebSocket sender) {
        for (WebSocket player : players) {
            if (player != sender && player.isOpen()) {
                player.send(message);
            }
        }
    }

    // Inner class for question broadcasting and timing
    private static class QuestionEngine implements Runnable {
        @Override
        public void run() {
            while (true) {
                String question = QUESTIONS[questionIndex++ % QUESTIONS.length];
                System.out.println("Broadcasting: " + question);
                // Broadcast to all players (including sender for simplicity)
                for (WebSocket player : players) {
                    if (player.isOpen()) {
                        player.send(question);
                    }
                }
                // Optional countdown (uncomment to enable)

                for (int i = 15; i > 0; i--) {
                    for (WebSocket player : players) {
                        if (player.isOpen()) {
                            player.send("Time left: " + i + "s");
                        }
                    }
                    try {
                        Thread.sleep(1000); // 1 second per update
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Wait 15 seconds before next question
                try {
                    Thread.sleep(15000); // 15 seconds per round
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        QuizServer server = new QuizServer();
        server.run();
    }
}