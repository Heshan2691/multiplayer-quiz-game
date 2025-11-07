package com.quizgame;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServer extends WebSocketServer {

    private static final int PORT = 1235;

    // Predefined credentials
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String USER_USERNAME = "user";
    private static final String USER_PASSWORD = "user123";

    private static final Set<WebSocket> connections =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AuthServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Client connected to AuthServer: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject req = new JSONObject(message);
            String username = req.optString("username");
            String password = req.optString("password");

            JSONObject res = new JSONObject();
            res.put("type", "authResponse");

            if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
                res.put("status", "ok");
                res.put("role", "admin");
            } else if (username.equals(USER_USERNAME) && password.equals(USER_PASSWORD)) {
                res.put("status", "ok");
                res.put("role", "user");
            } else {
                res.put("status", "error");
                res.put("message", "Invalid username or password");
            }

            conn.send(res.toString());
        } catch (Exception e) {
            System.err.println("Error in AuthServer: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Client disconnected from AuthServer");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("AuthServer error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Auth WebSocket Server started on ws://localhost:" + PORT);
    }
}
