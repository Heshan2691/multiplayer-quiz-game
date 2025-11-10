package com.quizgame;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminManager {

    public static void sendAdminData(WebSocket admin) {
        JSONObject data = new JSONObject();
        data.put("type", "adminData");
        data.put("questions", GameState.questions);
        data.put("currentQuestion", GameState.questionIndex);
        data.put("waitingForNext", GameState.waitingForNextQuestion);

        JSONArray playerList = new JSONArray();
        for (WebSocket player : GameState.players) {
            String username = GameState.usernames.get(player);
            if (username != null) {
                JSONObject playerData = new JSONObject();
                playerData.put("username", username);
                playerData.put("score", GameState.scores.getOrDefault(username, 0));
                playerData.put("totalTime", GameState.totalAnswerTimes.getOrDefault(username, 0L));
                playerList.put(playerData);
            }
        }
        data.put("players", playerList);

        admin.send(data.toString());
    }

    public static void broadcastToAdmins() {
        for (WebSocket admin : GameState.admins) {
            if (admin.isOpen()) {
                sendAdminData(admin);
            }
        }
    }

    public static void handleAdminCommand(WebSocket admin, String message) {
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

    public static void addQuestion(JSONObject questionData) {
        try {
            GameState.questions.put(questionData);
            GameState.saveQuestions();
            broadcastToAdmins();
            System.out.println("Question added: " + questionData.getString("question"));
        } catch (Exception e) {
            System.err.println("Error adding question: " + e.getMessage());
        }
    }

    public static void deleteQuestion(int index) {
        try {
            if (index >= 0 && index < GameState.questions.length()) {
                GameState.questions.remove(index);
                GameState.saveQuestions();
                broadcastToAdmins();
                System.out.println("Question deleted at index: " + index);
            }
        } catch (Exception e) {
            System.err.println("Error deleting question: " + e.getMessage());
        }
    }

    public static void updateSettings(JSONObject settings) {
        try {
            if (settings.has("questionDuration")) {
                GameState.questionDuration = settings.getInt("questionDuration");
            }
            if (settings.has("pauseDuration")) {
                GameState.pauseDuration = settings.getInt("pauseDuration");
            }
            if (settings.has("pointsPerAnswer")) {
                GameState.pointsPerAnswer = settings.getInt("pointsPerAnswer");
            }
            System.out.println("Settings updated: Timer=" + (GameState.questionDuration / 1000) + "s, Points=" + GameState.pointsPerAnswer);
        } catch (Exception e) {
            System.err.println("Error updating settings: " + e.getMessage());
        }
    }

    public static void startGame() {
        GameState.gameRunning = true;
        GameState.questionIndex = 0;
        GameState.waitingForNextQuestion = false;
        for (String username : GameState.totalAnswerTimes.keySet()) {
            GameState.totalAnswerTimes.put(username, 0L);
        }
        System.out.println("Game started by admin");
    }

    public static void stopGame() {
        GameState.gameRunning = false;
        System.out.println("Game stopped by admin");
        broadcastFinalLeaderboard();
    }

    public static void skipQuestion() {
        GameState.waitingForNextQuestion = false;
        System.out.println("Question skipped by admin");
    }

    public static void nextQuestion() {
        if (GameState.waitingForNextQuestion) {
            GameState.waitingForNextQuestion = false;
            System.out.println("Admin triggered next question");
        } else {
            System.out.println("Not waiting for next question - command ignored");
        }
    }

    public static void resetScores() {
        GameState.scores.clear();
        GameState.totalAnswerTimes.clear();
        for (WebSocket player : GameState.players) {
            String username = GameState.usernames.get(player);
            if (username != null) {
                GameState.scores.put(username, 0);
                GameState.totalAnswerTimes.put(username, 0L);
            }
        }
        broadcastToAdmins();
        System.out.println("All scores and times reset by admin");
    }

    public static void broadcastFinalLeaderboard() {
        org.json.JSONArray leaderboard = new org.json.JSONArray();
        GameState.scores.entrySet().stream()
                .sorted((a, b) -> {
                    int scoreCompare = b.getValue().compareTo(a.getValue());
                    if (scoreCompare != 0) return scoreCompare;
                    Long timeA = GameState.totalAnswerTimes.getOrDefault(a.getKey(), 0L);
                    Long timeB = GameState.totalAnswerTimes.getOrDefault(b.getKey(), 0L);
                    timeA = (timeA == 0L) ? Long.MAX_VALUE : timeA;
                    timeB = (timeB == 0L) ? Long.MAX_VALUE : timeB;
                    return timeA.compareTo(timeB);
                })
                .forEach(entry -> {
                    org.json.JSONObject playerScore = new org.json.JSONObject();
                    playerScore.put("username", entry.getKey());
                    playerScore.put("score", entry.getValue());
                    playerScore.put("totalTime", GameState.totalAnswerTimes.getOrDefault(entry.getKey(), 0L));
                    leaderboard.put(playerScore);
                });

        org.json.JSONObject finalResultsMsg = new org.json.JSONObject();
        finalResultsMsg.put("type", "gameEnded");
        finalResultsMsg.put("leaderboard", leaderboard);
        finalResultsMsg.put("message", "Game has ended! Here are the final results:");

        String finalMessage = finalResultsMsg.toString();
        for (WebSocket player : GameState.players) {
            if (player.isOpen()) player.send(finalMessage);
        }
        System.out.println("Broadcasting final leaderboard to all players");
    }
}
