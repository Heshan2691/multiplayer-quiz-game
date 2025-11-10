package com.quizgame;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URI;
import java.util.Random;

public class MockClient extends WebSocketClient {
    private final String username;
    private final Random rnd = new Random();

    public MockClient(URI serverUri, String username) {
        super(serverUri);
        this.username = username;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println(username + " connected");
        // Send username as first message (protocol used by server)
        send(username);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[" + username + "] Received: " + message);
        try {
            JSONObject obj = new JSONObject(message);
            String type = obj.optString("type");
            if ("question".equals(type)) {
                JSONArray options = obj.getJSONArray("options");
                int idx = rnd.nextInt(Math.max(1, options.length()));
                String answer = options.getString(idx);

                JSONObject resp = new JSONObject();
                resp.put("type", "answer");
                resp.put("answer", answer);
                resp.put("questionIndex", obj.getInt("questionIndex"));

                // simulate thinking time
                Thread.sleep(400 + rnd.nextInt(1200));
                send(resp.toString());
                System.out.println("[" + username + "] Sent answer: " + answer);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(username + " disconnected: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[" + username + "] Error: " + ex.getMessage());
    }

    public static void main(String[] args) throws Exception {
        String name = args.length > 0 ? args[0] : "MockPlayer";
        MockClient c = new MockClient(new URI("ws://localhost:" + GameState.PORT), name);
        c.connectBlocking();
        // Keep the client alive for 60 seconds then exit
        Thread.sleep(60_000);
        c.close();
    }
}
