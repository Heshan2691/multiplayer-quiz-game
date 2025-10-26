package com.quizgame;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class QuizServer extends WebSocketServer {
    private static final int PORT = 1234;
    private static final Set<WebSocket> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<WebSocket, String> usernames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();

    // Dynamic question list loaded from JSON
    private static JSONArray questions;
    private static int questionIndex = 0;
    private static boolean answersProcessed = false; // Flag for timer reset coordination

    static {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get("questions.json")));
            questions = new JSONArray(jsonContent);
        } catch (IOException e) {
            System.err.println("Error loading questions.json: " + e.getMessage());
            questions = new JSONArray(); // Fallback empty array
        }
    }

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
            // Coordinate with Answer Processing: Set flag when all answers processed
            // This is a placeholder—replace with actual signal logic
            if (allAnswersReceived()) { // Hypothetical method
                answersProcessed = true;
            }
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
        private static final int ROUND_DURATION = 15000; // 15 seconds

        @Override
        public void run() {
            while (true) {
                if (!questions.isEmpty()) {
                    JSONObject questionObj = questions.getJSONObject(questionIndex++ % questions.length());
                    String question = questionObj.getString("question") + " " +
                            String.join(" ", questionObj.getJSONArray("options").toList().toArray(new String[0]));
                    System.out.println("Broadcasting: " + question);
                    // Broadcast to all players
                    for (WebSocket player : players) {
                        if (player.isOpen()) {
                            player.send(question);
                        }
                    }

                    // Reset flag for this round
                    answersProcessed = false;

                    // Wait for round duration or until answers processed
                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < ROUND_DURATION) {
                        if (answersProcessed) {
                            break; // Reset timer if answers processed
                        }
                        try {
                            Thread.sleep(100); // Check every 100ms
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } else {
                    System.out.println("No questions available.");
                }
                try {
                    Thread.sleep(1000); // Brief pause before next round
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Placeholder method for coordination with Answer Processing
    private boolean allAnswersReceived() {
        // This should be implemented by the Answer Processing role
        // For now, simulate after a delay (e.g., 5 seconds)
        try {
            Thread.sleep(5000); // Simulate answer collection
            return players.size() > 0; // Dummy condition
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void main(String[] args) {
        QuizServer server = new QuizServer();
        server.run();
    }
}