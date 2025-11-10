package com.quizgame;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuestionEngine implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (true) {
            if (GameState.gameRunning && !GameState.questions.isEmpty() && !GameState.players.isEmpty()) {
                if (GameState.questionIndex >= GameState.questions.length()) {
                    System.out.println("All questions completed! Ending game...");
                    GameState.gameRunning = false;
                    AdminManager.broadcastFinalLeaderboard();
                    GameState.questionIndex = 0;
                    continue;
                }

                int currentIndex = GameState.questionIndex;
                JSONObject questionObj = GameState.questions.getJSONObject(currentIndex);

                GameState.questionStartTime = System.currentTimeMillis();

                JSONObject questionMsg = new JSONObject();
                questionMsg.put("type", "question");
                questionMsg.put("question", questionObj.getString("question"));
                questionMsg.put("options", questionObj.getJSONArray("options"));
                questionMsg.put("questionIndex", currentIndex);
                questionMsg.put("duration", GameState.questionDuration / 1000);
                questionMsg.put("questionNumber", GameState.questionIndex + 1);
                questionMsg.put("totalQuestions", GameState.questions.length());
                questionMsg.put("startTime", GameState.questionStartTime);

                String questionMessage = questionMsg.toString();
                System.out.println("Broadcasting question " + (GameState.questionIndex + 1) + "/" + GameState.questions.length() + ": "
                        + questionObj.getString("question"));

                for (WebSocket player : GameState.players) {
                    if (player.isOpen()) player.send(questionMessage);
                }

                try {
                    Thread.sleep(GameState.questionDuration);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                broadcastResults(currentIndex);

                GameState.questionIndex++;

                GameState.waitingForNextQuestion = true;
                System.out.println("Waiting for admin to start next question...");
                AdminManager.broadcastToAdmins();

                JSONObject waitMsg = new JSONObject();
                waitMsg.put("type", "waitingForAdmin");
                waitMsg.put("message", "Waiting for admin to start next question...");
                String waitMessage = waitMsg.toString();
                for (WebSocket player : GameState.players) {
                    if (player.isOpen()) player.send(waitMessage);
                }

                while (GameState.waitingForNextQuestion && GameState.gameRunning) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } else {
                if (GameState.questions.isEmpty()) {
                    System.out.println("No questions available.");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void broadcastResults(int questionIdx) {
        JSONObject questionObj = GameState.questions.getJSONObject(questionIdx);
        String correctAnswer = questionObj.getString("correctAnswer");

        JSONArray leaderboard = new JSONArray();
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
                    JSONObject playerScore = new JSONObject();
                    playerScore.put("username", entry.getKey());
                    playerScore.put("score", entry.getValue());
                    playerScore.put("totalTime", GameState.totalAnswerTimes.getOrDefault(entry.getKey(), 0L));
                    leaderboard.put(playerScore);
                });

        JSONObject resultsMsg = new JSONObject();
        resultsMsg.put("type", "results");
        resultsMsg.put("correctAnswer", correctAnswer);
        resultsMsg.put("leaderboard", leaderboard);

        String resultsMessage = resultsMsg.toString();
        for (WebSocket player : GameState.players) {
            if (player.isOpen()) {
                player.send(resultsMessage);
            }
        }

        System.out.println("Broadcasting results and leaderboard");
    }
}
