package com.quizgame;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;

public class GameState {
    public static final int PORT = 1234;

    public static final Set<WebSocket> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Set<WebSocket> admins = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static final ConcurrentMap<WebSocket, String> usernames = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, Integer> scores = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, Long> totalAnswerTimes = new ConcurrentHashMap<>();

    public static JSONArray questions = new JSONArray();
    public static int questionIndex = 0;
    public static boolean gameRunning = false;
    public static long questionStartTime = 0L;
    public static boolean waitingForNextQuestion = false;

    // Configurable settings
    public static int questionDuration = 20000; // 20 seconds
    public static int pauseDuration = 5000; // 5 seconds
    public static int pointsPerAnswer = 100;

    static {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get("questions.json")));
            questions = new JSONArray(jsonContent);
        } catch (IOException e) {
            System.err.println("Error loading questions.json: " + e.getMessage());
            questions = new JSONArray();
        }
    }

    public static void saveQuestions() {
        try {
            Files.write(Paths.get("questions.json"), questions.toString(2).getBytes());
            System.out.println("Questions saved to file");
        } catch (IOException e) {
            System.err.println("Error saving questions: " + e.getMessage());
        }
    }

    public static List<JSONObject> chatHistory = new ArrayList<>();

}
