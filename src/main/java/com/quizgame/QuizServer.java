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
        // Handle answers or commands later
        sendToAllExcludingSender(usernames.get(conn) + ": " + message, conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Quiz Server started on port " + PORT);
    }

    // Renamed to avoid ambiguity with WebSocketServer.broadcast
    private void sendToAllExcludingSender(String message, WebSocket sender) {
        for (WebSocket player : players) {
            if (player != sender && player.isOpen()) {
                player.send(message);
            }
        }
    }

    public static void main(String[] args) {
        QuizServer server = new QuizServer();
        server.run();
    }
}