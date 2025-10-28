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
    private static final Set<WebSocket> admins = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<WebSocket, String> usernames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();

    // Dynamic question list loaded from JSON
    private static JSONArray questions;
    private static int questionIndex = 0;
    private static boolean gameRunning = false;
    private static QuestionEngine questionEngine;

    // Configurable settings
    private static int questionDuration = 20000; // 20 seconds
    private static int pauseDuration = 5000; // 5 seconds
    private static int pointsPerAnswer = 100;

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
        // Check if admin disconnected
        if (admins.contains(conn)) {
            admins.remove(conn);
            System.out.println("Admin disconnected.");
            return;
        }

        players.remove(conn);
        String username = usernames.remove(conn);
        if (username != null) {
            scores.remove(username);
            sendToAllExcludingSender(username + " has left the game.", null);
            System.out.println("Player '" + username + "' disconnected.");
            broadcastToAdmins();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Check if this is an admin connection
        if ("__ADMIN__".equals(message)) {
            admins.add(conn);
            System.out.println("Admin connected: " + conn.getRemoteSocketAddress());
            sendAdminData(conn);
            return;
        }

        // Check if this is an admin command
        if (admins.contains(conn)) {
            handleAdminCommand(conn, message);
            return;
        }

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

            // Update admin panels
            broadcastToAdmins();
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
                        // Award points (configurable)
                        int currentScore = scores.getOrDefault(username, 0);
                        scores.put(username, currentScore + pointsPerAnswer);

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

                    // Update admin panels
                    broadcastToAdmins();
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

    // Admin methods
    private void sendAdminData(WebSocket admin) {
        JSONObject data = new JSONObject();
        data.put("type", "adminData");
        data.put("questions", questions);
        data.put("currentQuestion", questionIndex);

        JSONArray playerList = new JSONArray();
        for (WebSocket player : players) {
            String username = usernames.get(player);
            if (username != null) {
                JSONObject playerData = new JSONObject();
                playerData.put("username", username);
                playerData.put("score", scores.getOrDefault(username, 0));
                playerList.put(playerData);
            }
        }
        data.put("players", playerList);

        admin.send(data.toString());
    }

    private void broadcastToAdmins() {
        for (WebSocket admin : admins) {
            if (admin.isOpen()) {
                sendAdminData(admin);
            }
        }
    }

    private void handleAdminCommand(WebSocket admin, String message) {
        try {
            JSONObject cmd = new JSONObject(message);
            String command = cmd.getString("command");
            JSONObject data = cmd.optJSONObject("data");

            switch (command) {
                case "addQuestion":
                    addQuestion(data);
                    break;
                case "deleteQuestion":
                    deleteQuestion(data.getInt("index"));
                    break;
                case "updateSettings":
                    updateSettings(data);
                    break;
                case "startGame":
                    startGame();
                    break;
                case "stopGame":
                    stopGame();
                    break;
                case "skipQuestion":
                    skipQuestion();
                    break;
                case "resetScores":
                    resetScores();
                    break;
                case "getStatus":
                    sendAdminData(admin);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling admin command: " + e.getMessage());
        }
    }

    private void addQuestion(JSONObject questionData) {
        try {
            questions.put(questionData);
            saveQuestions();
            broadcastToAdmins();
            System.out.println("Question added: " + questionData.getString("question"));
        } catch (Exception e) {
            System.err.println("Error adding question: " + e.getMessage());
        }
    }

    private void deleteQuestion(int index) {
        try {
            if (index >= 0 && index < questions.length()) {
                questions.remove(index);
                saveQuestions();
                broadcastToAdmins();
                System.out.println("Question deleted at index: " + index);
            }
        } catch (Exception e) {
            System.err.println("Error deleting question: " + e.getMessage());
        }
    }

    private void updateSettings(JSONObject settings) {
        try {
            if (settings.has("questionDuration")) {
                questionDuration = settings.getInt("questionDuration");
            }
            if (settings.has("pauseDuration")) {
                pauseDuration = settings.getInt("pauseDuration");
            }
            if (settings.has("pointsPerAnswer")) {
                pointsPerAnswer = settings.getInt("pointsPerAnswer");
            }
            System.out.println("Settings updated: Timer=" + (questionDuration/1000) + "s, Points=" + pointsPerAnswer);
        } catch (Exception e) {
            System.err.println("Error updating settings: " + e.getMessage());
        }
    }

    private void startGame() {
        gameRunning = true;
        System.out.println("Game started by admin");
    }

    private void stopGame() {
        gameRunning = false;
        System.out.println("Game stopped by admin");
    }

    private void skipQuestion() {
        questionIndex++;
        System.out.println("Question skipped by admin");
    }

    private void resetScores() {
        scores.clear();
        for (WebSocket player : players) {
            String username = usernames.get(player);
            if (username != null) {
                scores.put(username, 0);
            }
        }
        broadcastToAdmins();
        System.out.println("All scores reset by admin");
    }

    private void saveQuestions() {
        try {
            Files.write(Paths.get("questions.json"), questions.toString(2).getBytes());
            System.out.println("Questions saved to file");
        } catch (IOException e) {
            System.err.println("Error saving questions: " + e.getMessage());
        }
    }

    // Inner class for question broadcasting and timing
    private static class QuestionEngine implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(3000); // Initial delay for players to join
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            while (true) {
                // Only broadcast if game is running
                if (gameRunning && !questions.isEmpty() && !players.isEmpty()) {
                    int currentIndex = questionIndex % questions.length();
                    JSONObject questionObj = questions.getJSONObject(currentIndex);

                    // Create question message
                    JSONObject questionMsg = new JSONObject();
                    questionMsg.put("type", "question");
                    questionMsg.put("question", questionObj.getString("question"));
                    questionMsg.put("options", questionObj.getJSONArray("options"));
                    questionMsg.put("questionIndex", currentIndex);
                    questionMsg.put("duration", questionDuration / 1000); // Send duration in seconds
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
                        Thread.sleep(questionDuration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // Show correct answer and leaderboard
                    broadcastResults(currentIndex);

                    questionIndex++;

                    // Pause before next question
                    try {
                        Thread.sleep(pauseDuration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    if (questions.isEmpty()) {
                        System.out.println("No questions available.");
                    }
                    try {
                        Thread.sleep(2000); // Wait for players or game to start
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