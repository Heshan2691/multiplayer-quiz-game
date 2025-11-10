package com.quizgame;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;

import java.net.InetSocketAddress;

public class QuizServer extends WebSocketServer {

    public QuizServer() {
        super(new InetSocketAddress(GameState.PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        GameState.players.add(conn);
        System.out.println("New player connected: " + conn.getRemoteSocketAddress());
        conn.send("Enter your username:");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (GameState.admins.contains(conn)) {
            GameState.admins.remove(conn);
            System.out.println("Admin disconnected.");
            return;
        }

        GameState.players.remove(conn);
        String username = GameState.usernames.remove(conn);
        if (username != null) {
            GameState.scores.remove(username);
            GameState.totalAnswerTimes.remove(username);
            sendToAllExcludingSender(username + " has left the game.", null);
            System.out.println("Player '" + username + "' disconnected.");
            AdminManager.broadcastToAdmins();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if ("__ADMIN__".equals(message)) {
            GameState.admins.add(conn);
            System.out.println("Admin connected: " + conn.getRemoteSocketAddress());
            AdminManager.sendAdminData(conn);
            return;
        }

        if (GameState.admins.contains(conn)) {
            AdminManager.handleAdminCommand(conn, message);
            return;
        }

        if (GameState.usernames.get(conn) == null) {
            String username = message.trim();
            if (username.isEmpty()) username = "Anonymous";
            GameState.usernames.put(conn, username);
            GameState.scores.put(username, 0);
            GameState.totalAnswerTimes.put(username, 0L);

            JSONObject welcomeMsg = new JSONObject();
            welcomeMsg.put("type", "welcome");
            welcomeMsg.put("username", username);
            conn.send(welcomeMsg.toString());

            JSONObject joinMsg = new JSONObject();
            joinMsg.put("type", "playerJoined");
            joinMsg.put("username", username);
            sendToAllExcludingSender(joinMsg.toString(), conn);

            System.out.println("Player '" + username + "' joined the game.");
            AdminManager.broadcastToAdmins();
            return;
        }

        try {
            JSONObject msgObj = new JSONObject(message);
            if ("answer".equals(msgObj.getString("type"))) {
                String username = GameState.usernames.get(conn);
                String answer = msgObj.getString("answer");
                int questionIdx = msgObj.getInt("questionIndex");
                long answerReceivedTime = System.currentTimeMillis();

                if (questionIdx < GameState.questions.length()) {
                    JSONObject questionObj = GameState.questions.getJSONObject(questionIdx);
                    String correctAnswer = questionObj.getString("correctAnswer");

                    if (answer.equals(correctAnswer)) {
                        long timeTaken = answerReceivedTime - GameState.questionStartTime;
                        if (timeTaken > 0 && timeTaken <= (GameState.questionDuration * 2)) {
                            long currentTotalTime = GameState.totalAnswerTimes.getOrDefault(username, 0L);
                            GameState.totalAnswerTimes.put(username, currentTotalTime + timeTaken);
                            System.out.println("Player '" + username + "' answered correctly in " + timeTaken + "ms");
                        } else if (timeTaken <= 0) {
                            System.err.println("Warning: Invalid time for " + username + " - timeTaken=" + timeTaken);
                        }

                        int currentScore = GameState.scores.getOrDefault(username, 0);
                        GameState.scores.put(username, currentScore + GameState.pointsPerAnswer);

                        JSONObject correctMsg = new JSONObject();
                        correctMsg.put("type", "answerResult");
                        correctMsg.put("correct", true);
                        correctMsg.put("score", GameState.scores.get(username));
                        correctMsg.put("timeTaken", timeTaken);
                        conn.send(correctMsg.toString());
                    } else {
                        JSONObject wrongMsg = new JSONObject();
                        wrongMsg.put("type", "answerResult");
                        wrongMsg.put("correct", false);
                        wrongMsg.put("correctAnswer", correctAnswer);
                        conn.send(wrongMsg.toString());
                    }

                    AdminManager.broadcastToAdmins();
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex != null) System.err.println("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Quiz Server started on port " + GameState.PORT);
        new Thread(new QuestionEngine()).start();
    }

    private void sendToAllExcludingSender(String message, WebSocket sender) {
        for (WebSocket player : GameState.players) {
            if (player != sender && player.isOpen()) player.send(message);
        }
    }

    public static void main(String[] args) {
        QuizServer quizServer = new QuizServer();
        quizServer.start();

        AuthServer authServer = new AuthServer();
        authServer.start();

        System.out.println("Quiz Server is running on ws://localhost:" + GameState.PORT);
        System.out.println("Auth Server is running on ws://localhost:1235");
    }

}