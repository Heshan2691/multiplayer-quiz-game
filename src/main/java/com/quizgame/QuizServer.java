package com.quizgame;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Collections;
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

            // Send welcome message with player info
            JSONObject welcomeMsg = new JSONObject();
            welcomeMsg.put("type", "welcome");
            welcomeMsg.put("username", username);
            conn.send(welcomeMsg.toString());

            // Broadcast to others
            JSONObject joinMsg = new JSONObject();
            joinMsg.put("type", "playerJoined");
            joinMsg.put("username", username);
            sendToAllExcludingSender(joinMsg.toString(), conn);

            System.out.println("Player '" + username + "' joined the game.");
            return;
        }

        // Handle answer submission
        try {
            JSONObject msgObj = new JSONObject(message);
            if ("answer".equals(msgObj.getString("type"))) {
                String username = usernames.get(conn);
                String answer = msgObj.getString("answer");
                int questionIdx = msgObj.getInt("questionIndex");

                // Check if answer is correct
                if (questionIdx < questions.length()) {
                    JSONObject questionObj = questions.getJSONObject(questionIdx);
                    String correctAnswer = questionObj.getString("correctAnswer");

                    if (answer.equals(correctAnswer)) {
                        // Award points (more points for faster answers)
                        int currentScore = scores.getOrDefault(username, 0);
                        scores.put(username, currentScore + 100);

                        JSONObject correctMsg = new JSONObject();
                        correctMsg.put("type", "answerResult");
                        correctMsg.put("correct", true);
                        correctMsg.put("score", scores.get(username));
                        conn.send(correctMsg.toString());
                    } else {
                        JSONObject wrongMsg = new JSONObject();
                        wrongMsg.put("type", "answerResult");
                        wrongMsg.put("correct", false);
                        wrongMsg.put("correctAnswer", correctAnswer);
                        conn.send(wrongMsg.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex != null) {
            System.err.println("WebSocket error: " + ex.getMessage());
        }
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
        private static final int ROUND_DURATION = 20000; // 20 seconds
        private static final int PAUSE_BETWEEN_QUESTIONS = 5000; // 5 seconds

        @Override
        public void run() {
            try {
                Thread.sleep(3000); // Initial delay for players to join
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            while (true) {
                if (!questions.isEmpty() && !players.isEmpty()) {
                    int currentIndex = questionIndex % questions.length();
                    JSONObject questionObj = questions.getJSONObject(currentIndex);

                    // Create question message
                    JSONObject questionMsg = new JSONObject();
                    questionMsg.put("type", "question");
                    questionMsg.put("question", questionObj.getString("question"));
                    questionMsg.put("options", questionObj.getJSONArray("options"));
                    questionMsg.put("questionIndex", currentIndex);
                    questionMsg.put("duration", ROUND_DURATION / 1000); // Send duration in seconds
                    questionMsg.put("questionNumber", questionIndex + 1);
                    questionMsg.put("totalQuestions", questions.length());

                    String questionMessage = questionMsg.toString();
                    System.out.println("Broadcasting question " + (questionIndex + 1) + ": " + questionObj.getString("question"));

                    // Broadcast to all players
                    for (WebSocket player : players) {
                        if (player.isOpen()) {
                            player.send(questionMessage);
                        }
                    }

                    // Wait for round duration
                    try {
                        Thread.sleep(ROUND_DURATION);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // Show correct answer and leaderboard
                    broadcastResults(currentIndex);

                    questionIndex++;

                    // Pause before next question
                    try {
                        Thread.sleep(PAUSE_BETWEEN_QUESTIONS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    if (questions.isEmpty()) {
                        System.out.println("No questions available.");
                    }
                    try {
                        Thread.sleep(2000); // Wait for players
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        private void broadcastResults(int questionIdx) {
            JSONObject questionObj = questions.getJSONObject(questionIdx);
            String correctAnswer = questionObj.getString("correctAnswer");

            // Create leaderboard
            JSONArray leaderboard = new JSONArray();
            scores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    JSONObject playerScore = new JSONObject();
                    playerScore.put("username", entry.getKey());
                    playerScore.put("score", entry.getValue());
                    leaderboard.put(playerScore);
                });

            JSONObject resultsMsg = new JSONObject();
            resultsMsg.put("type", "results");
            resultsMsg.put("correctAnswer", correctAnswer);
            resultsMsg.put("leaderboard", leaderboard);

            String resultsMessage = resultsMsg.toString();
            for (WebSocket player : players) {
                if (player.isOpen()) {
                    player.send(resultsMessage);
                }
            }

            System.out.println("Broadcasting results and leaderboard");
        }
    }

    public static void main(String[] args) {
        QuizServer server = new QuizServer();
        server.start();
        System.out.println("Quiz Server is running on ws://localhost:" + PORT);
    }
}