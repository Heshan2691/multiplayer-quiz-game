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
    private static final ConcurrentHashMap<String, Long> totalAnswerTimes = new ConcurrentHashMap<>(); // Total time in
                                                                                                       // milliseconds

    // Dynamic question list loaded from JSON
    private static JSONArray questions;
    private static int questionIndex = 0;
    private static boolean gameRunning = false;
    private static QuestionEngine questionEngine;
    private static long questionStartTime = 0; // Track when current question started
    private static boolean waitingForNextQuestion = false; // Admin needs to trigger next question

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
            totalAnswerTimes.remove(username);
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
            if (username.isEmpty())
                username = "Anonymous";
            usernames.put(conn, username);
            scores.put(username, 0);
            totalAnswerTimes.put(username, 0L); // Initialize answer time tracking

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

                // Use SERVER time when answer is received (more reliable)
                long answerReceivedTime = System.currentTimeMillis();

                // Check if answer is correct
                if (questionIdx < questions.length()) {
                    JSONObject questionObj = questions.getJSONObject(questionIdx);
                    String correctAnswer = questionObj.getString("correctAnswer");

                    if (answer.equals(correctAnswer)) {
                        // Calculate time taken using server timestamps (in milliseconds)
                        long timeTaken = answerReceivedTime - questionStartTime;

                        // Only add time if valid (positive and within reasonable bounds)
                        // Allow up to 2x question duration to handle delays
                        if (timeTaken > 0 && timeTaken <= (questionDuration * 2)) {
                            // Add to total answer time
                            long currentTotalTime = totalAnswerTimes.getOrDefault(username, 0L);
                            totalAnswerTimes.put(username, currentTotalTime + timeTaken);
                            System.out.println("Player '" + username + "' answered correctly in " + timeTaken + "ms");
                        } else if (timeTaken <= 0) {
                            System.err.println("Warning: Invalid time for " + username + " - timeTaken=" + timeTaken);
                        }

                        // Award points (configurable)
                        int currentScore = scores.getOrDefault(username, 0);
                        scores.put(username, currentScore + pointsPerAnswer);

                        JSONObject correctMsg = new JSONObject();
                        correctMsg.put("type", "answerResult");
                        correctMsg.put("correct", true);
                        correctMsg.put("score", scores.get(username));
                        correctMsg.put("timeTaken", timeTaken);
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
        data.put("waitingForNext", waitingForNextQuestion);

        JSONArray playerList = new JSONArray();
        for (WebSocket player : players) {
            String username = usernames.get(player);
            if (username != null) {
                JSONObject playerData = new JSONObject();
                playerData.put("username", username);
                playerData.put("score", scores.getOrDefault(username, 0));
                playerData.put("totalTime", totalAnswerTimes.getOrDefault(username, 0L));
                playerList.put(playerData);
            }
        }
        data.put("players", playerList);

        admin.send(data.toString());
    }

    private void broadcastToAdmins() {
        broadcastToAdminsStatic();
    }

    private static void broadcastToAdminsStatic() {
        for (WebSocket admin : admins) {
            if (admin.isOpen()) {
                JSONObject data = new JSONObject();
                data.put("type", "adminData");
                data.put("questions", questions);
                data.put("currentQuestion", questionIndex);
                data.put("waitingForNext", waitingForNextQuestion);

                JSONArray playerList = new JSONArray();
                for (WebSocket player : players) {
                    String username = usernames.get(player);
                    if (username != null) {
                        JSONObject playerData = new JSONObject();
                        playerData.put("username", username);
                        playerData.put("score", scores.getOrDefault(username, 0));
                        playerData.put("totalTime", totalAnswerTimes.getOrDefault(username, 0L));
                        playerList.put(playerData);
                    }
                }
                data.put("players", playerList);

                admin.send(data.toString());
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
                case "nextQuestion":
                    nextQuestion();
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
            System.out.println("Settings updated: Timer=" + (questionDuration / 1000) + "s, Points=" + pointsPerAnswer);
        } catch (Exception e) {
            System.err.println("Error updating settings: " + e.getMessage());
        }
    }

    private void startGame() {
        gameRunning = true;
        questionIndex = 0; // Reset to start from first question
        waitingForNextQuestion = false; // Reset flag

        // Reset answer times for all players
        for (String username : totalAnswerTimes.keySet()) {
            totalAnswerTimes.put(username, 0L);
        }

        System.out.println("Game started by admin");
    }

    private void stopGame() {
        gameRunning = false;
        System.out.println("Game stopped by admin");

        // Broadcast final leaderboard to all players
        broadcastFinalLeaderboard();
    }

    private void broadcastFinalLeaderboard() {
        broadcastFinalLeaderboardStatic();
    }

    private static void broadcastFinalLeaderboardStatic() {
        // Create leaderboard sorted by score (descending), then by time (ascending)
        JSONArray leaderboard = new JSONArray();
        scores.entrySet().stream()
                .sorted((a, b) -> {
                    int scoreCompare = b.getValue().compareTo(a.getValue()); // Higher score first
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    // If scores are equal, compare by time (lower time is better)
                    // Treat 0 time as worst (player never answered correctly)
                    Long timeA = totalAnswerTimes.getOrDefault(a.getKey(), 0L);
                    Long timeB = totalAnswerTimes.getOrDefault(b.getKey(), 0L);

                    // If time is 0, treat as worst (Long.MAX_VALUE)
                    timeA = (timeA == 0L) ? Long.MAX_VALUE : timeA;
                    timeB = (timeB == 0L) ? Long.MAX_VALUE : timeB;

                    return timeA.compareTo(timeB);
                })
                .forEach(entry -> {
                    JSONObject playerScore = new JSONObject();
                    playerScore.put("username", entry.getKey());
                    playerScore.put("score", entry.getValue());
                    playerScore.put("totalTime", totalAnswerTimes.getOrDefault(entry.getKey(), 0L));
                    leaderboard.put(playerScore);
                });

        JSONObject finalResultsMsg = new JSONObject();
        finalResultsMsg.put("type", "gameEnded");
        finalResultsMsg.put("leaderboard", leaderboard);
        finalResultsMsg.put("message", "Game has ended! Here are the final results:");

        String finalMessage = finalResultsMsg.toString();
        for (WebSocket player : players) {
            if (player.isOpen()) {
                player.send(finalMessage);
            }
        }

        System.out.println("Broadcasting final leaderboard to all players");
    }

    private void skipQuestion() {
        waitingForNextQuestion = false; // Allow next question to proceed
        System.out.println("Question skipped by admin");
    }

    private void nextQuestion() {
        if (waitingForNextQuestion) {
            waitingForNextQuestion = false; // Allow next question to proceed
            System.out.println("Admin triggered next question");
        } else {
            System.out.println("Not waiting for next question - command ignored");
        }
    }

    private void resetScores() {
        scores.clear();
        totalAnswerTimes.clear();
        for (WebSocket player : players) {
            String username = usernames.get(player);
            if (username != null) {
                scores.put(username, 0);
                totalAnswerTimes.put(username, 0L);
            }
        }
        broadcastToAdmins();
        System.out.println("All scores and times reset by admin");
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
                    // Check if all questions have been completed
                    if (questionIndex >= questions.length()) {
                        System.out.println("All questions completed! Ending game...");
                        gameRunning = false;
                        broadcastFinalLeaderboardStatic();
                        questionIndex = 0; // Reset for next game
                        continue;
                    }

                    int currentIndex = questionIndex;
                    JSONObject questionObj = questions.getJSONObject(currentIndex);

                    // Record question start time
                    questionStartTime = System.currentTimeMillis();

                    // Create question message
                    JSONObject questionMsg = new JSONObject();
                    questionMsg.put("type", "question");
                    questionMsg.put("question", questionObj.getString("question"));
                    questionMsg.put("options", questionObj.getJSONArray("options"));
                    questionMsg.put("questionIndex", currentIndex);
                    questionMsg.put("duration", questionDuration / 1000); // Send duration in seconds
                    questionMsg.put("questionNumber", questionIndex + 1);
                    questionMsg.put("totalQuestions", questions.length());
                    questionMsg.put("startTime", questionStartTime); // Send start time to clients

                    String questionMessage = questionMsg.toString();
                    System.out.println("Broadcasting question " + (questionIndex + 1) + "/" + questions.length() + ": "
                            + questionObj.getString("question"));

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

                    // Wait for admin to trigger next question
                    waitingForNextQuestion = true;
                    System.out.println("Waiting for admin to start next question...");
                    broadcastToAdminsStatic(); // Update admin panel to show waiting status

                    // Notify players to wait
                    JSONObject waitMsg = new JSONObject();
                    waitMsg.put("type", "waitingForAdmin");
                    waitMsg.put("message", "Waiting for admin to start next question...");
                    String waitMessage = waitMsg.toString();
                    for (WebSocket player : players) {
                        if (player.isOpen()) {
                            player.send(waitMessage);
                        }
                    }

                    // Wait until admin triggers next question
                    while (waitingForNextQuestion && gameRunning) {
                        try {
                            Thread.sleep(500); // Check every 500ms
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
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

            // Create leaderboard sorted by score (descending), then by time (ascending)
            JSONArray leaderboard = new JSONArray();
            scores.entrySet().stream()
                    .sorted((a, b) -> {
                        int scoreCompare = b.getValue().compareTo(a.getValue()); // Higher score first
                        if (scoreCompare != 0) {
                            return scoreCompare;
                        }
                        // If scores are equal, compare by time (lower time is better)
                        // Treat 0 time as worst (player never answered correctly)
                        Long timeA = totalAnswerTimes.getOrDefault(a.getKey(), 0L);
                        Long timeB = totalAnswerTimes.getOrDefault(b.getKey(), 0L);

                        // If time is 0, treat as worst (Long.MAX_VALUE)
                        timeA = (timeA == 0L) ? Long.MAX_VALUE : timeA;
                        timeB = (timeB == 0L) ? Long.MAX_VALUE : timeB;

                        return timeA.compareTo(timeB);
                    })
                    .forEach(entry -> {
                        JSONObject playerScore = new JSONObject();
                        playerScore.put("username", entry.getKey());
                        playerScore.put("score", entry.getValue());
                        playerScore.put("totalTime", totalAnswerTimes.getOrDefault(entry.getKey(), 0L));
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
        QuizServer quizServer = new QuizServer();
        quizServer.start();

        AuthServer authServer = new AuthServer();
        authServer.start();

        System.out.println("Quiz Server is running on ws://localhost:1234");
        System.out.println("Auth Server is running on ws://localhost:1235");
    }

}