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

        // ADMIN IDENTIFICATION
        if ("__ADMIN__".equals(message)) {
            GameState.admins.add(conn);
            System.out.println("Admin connected: " + conn.getRemoteSocketAddress());
            AdminManager.sendAdminData(conn);

            // Send chat history to admin
            JSONObject history = new JSONObject();
            history.put("type", "chatHistory");
            history.put("history", GameState.chatHistory);
            conn.send(history.toString());
            return;
        }

        // ADMIN COMMANDS
        if (GameState.admins.contains(conn)) {
            try {
                JSONObject adminMsg = new JSONObject(message);
                if ("adminPrivateChat".equals(adminMsg.optString("command"))) {
                    String targetUser = adminMsg.getJSONObject("data").getString("to");
                    String text = adminMsg.getJSONObject("data").getString("message");

                    WebSocket targetConn = null;

                    // Find the player's WebSocket connection
                    for (WebSocket wsConn : GameState.players) {
                        String name = GameState.usernames.get(wsConn);
                        if (name != null && name.equalsIgnoreCase(targetUser)) {
                            targetConn = wsConn;
                            break;
                        }
                    }

                    if (targetConn != null) {
                        JSONObject chat = new JSONObject();
                        chat.put("type", "chat");
                        chat.put("from", "admin");
                        chat.put("username", "Admin (private)");
                        chat.put("message", text);
                        chat.put("timestamp", System.currentTimeMillis());

                        // Send only to target user
                        targetConn.send(chat.toString());
                    }

                    return;
                }

                if ("adminChat".equals(adminMsg.optString("command"))) {
                    String text = adminMsg.getJSONObject("data").getString("message");

                    JSONObject chat = new JSONObject();
                    chat.put("type", "chat");
                    chat.put("from", "admin");
                    chat.put("username", "Admin");
                    chat.put("message", text);
                    chat.put("timestamp", System.currentTimeMillis());

                    GameState.chatHistory.add(chat);
                    broadcastChat(conn, chat);

                    return;
                }
            } catch (Exception ignored) {
            }

            AdminManager.handleAdminCommand(conn, message);
            return;
        }

        // NEW PLAYER USERNAME REGISTRATION
        if (GameState.usernames.get(conn) == null) {
            String username = message.trim();
            if (username.isEmpty())
                username = "Anonymous";
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

        // ✅ PLAYER CHAT HANDLING
        try {
            JSONObject obj = new JSONObject(message);
            if ("chat".equals(obj.optString("type"))) {
                String username = GameState.usernames.get(conn);
                String text = obj.getString("message");

                JSONObject chat = new JSONObject();
                chat.put("type", "chat");
                chat.put("from", "player");
                chat.put("username", username);
                chat.put("message", text);
                chat.put("timestamp", System.currentTimeMillis());

                GameState.chatHistory.add(chat);
                broadcastChat(conn, chat);

                return;
            }
        } catch (Exception ignore) {
        }

        // ✅ ANSWER HANDLING (unchanged)
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
                        }

                        int currentScore = GameState.scores.getOrDefault(username, 0);
                        GameState.scores.put(username, currentScore + GameState.pointsPerAnswer);

                        JSONObject correctMsg = new JSONObject();
                        correctMsg.put("type", "answerResult");
                        correctMsg.put("correct", true);
                        correctMsg.put("score", GameState.scores.get(username));
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
        if (ex != null)
            System.err.println("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Quiz Server started on port " + GameState.PORT);
        new Thread(new QuestionEngine()).start();
    }

    private void sendToAllExcludingSender(String message, WebSocket sender) {
        for (WebSocket player : GameState.players) {
            if (player != sender && player.isOpen())
                player.send(message);
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

    private void broadcastChat(WebSocket sender, JSONObject chatMessage) {
        String from = chatMessage.optString("from");
        String msg = chatMessage.toString();

        if ("admin".equals(from)) {
            // Admin messages go to all players + other admins (not the sender)
            for (WebSocket player : GameState.players) {
                if (player != null && player.isOpen())
                    player.send(msg);
            }
            for (WebSocket admin : GameState.admins) {
                if (admin != null && admin.isOpen() && admin != sender)
                    admin.send(msg);
            }
        } else if ("player".equals(from)) {
            // Player messages go to all admins + the player who sent it
            for (WebSocket admin : GameState.admins) {
                if (admin != null && admin.isOpen())
                    admin.send(msg);
            }
        }
    }

}