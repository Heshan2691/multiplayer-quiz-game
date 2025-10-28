# 🎯 Multiplayer Quiz Game (Kahoot-style)

A real-time multiplayer quiz game similar to Kahoot and Mentimeter, built with Java WebSocket and modern web technologies.

## Features

✨ **Real-time Multiplayer**: Multiple players can join and compete simultaneously
🎨 **Beautiful UI**: Colorful, responsive design inspired by Kahoot
⏱️ **Timed Questions**: 20-second countdown for each question
🏆 **Live Leaderboard**: Real-time score tracking and rankings
🎯 **Multiple Choice**: Four colorful answer options
📊 **Results Display**: Shows correct answers and updated leaderboard after each question
🔔 **Notifications**: Real-time updates when players join/leave

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Modern web browser

## Project Structure

```
multiplayer-quiz-game/
├── src/main/java/com/quizgame/
│   └── QuizServer.java          # WebSocket server
├── web/
│   └── index.html               # Game client interface
├── questions.json               # Quiz questions database
├── pom.xml                      # Maven dependencies
└── README.md
```

## How to Run

### 1. Start the Server

```bash
# Compile and run the server
mvn clean compile
mvn exec:java -Dexec.mainClass="com.quizgame.QuizServer"
```

The server will start on `ws://localhost:1234`

### 2. Open the Client

Open `web/index.html` in your web browser. You can open multiple browser tabs/windows to simulate multiple players.

### 3. Play the Game

1. Enter your username
2. Wait for questions to be broadcast (3-second initial delay)
3. Answer questions within 20 seconds
4. View results and leaderboard after each question
5. Compete with other players!

## Game Flow

1. **Join Phase**: Players enter their username and connect
2. **Question Phase**: Server broadcasts a question with 4 options
3. **Answer Phase**: Players have 20 seconds to submit answers
4. **Results Phase**: Server shows correct answer and leaderboard (5 seconds)
5. **Repeat**: Next question after 5-second pause

## Customizing Questions

Edit `questions.json` to add your own questions:

```json
[
  {
    "question": "Your question here?",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctAnswer": "Option B"
  }
]
```

## Scoring System

- Correct answer: **+100 points**
- Wrong answer: **0 points**
- No answer: **0 points**

## Technical Details

- **Backend**: Java WebSocket Server (Java-WebSocket library)
- **Frontend**: Pure HTML/CSS/JavaScript
- **Communication**: JSON-based WebSocket messages
- **Concurrency**: Thread-safe player management with ConcurrentHashMap

## Message Protocol

### Client → Server
- Username string (first message)
- Answer submission: `{"type":"answer", "answer":"Option", "questionIndex":0}`

### Server → Client
- Welcome: `{"type":"welcome", "username":"..."}`
- Question: `{"type":"question", "question":"...", "options":[...], ...}`
- Answer result: `{"type":"answerResult", "correct":true/false, "score":100}`
- Results: `{"type":"results", "correctAnswer":"...", "leaderboard":[...]}`
- Player joined: `{"type":"playerJoined", "username":"..."}`

## Configuration

Edit `QuizServer.java` to modify:
- Port number: `PORT = 1234`
- Round duration: `ROUND_DURATION = 20000` (milliseconds)
- Pause between questions: `PAUSE_BETWEEN_QUESTIONS = 5000` (milliseconds)
- Initial delay: `Thread.sleep(3000)` in QuestionEngine

## Troubleshooting

**Server won't start?**
- Make sure port 1234 is not in use
- Check Java version: `java -version` (should be 11+)
- Rebuild with: `mvn clean install`

**Client can't connect?**
- Verify server is running
- Check browser console for errors
- Make sure WebSocket URL is correct: `ws://localhost:1234`

**Questions not loading?**
- Ensure `questions.json` is in the project root
- Validate JSON syntax
- Check server console for error messages

## Future Enhancements

- [ ] Different point values based on answer speed
- [ ] Question categories/difficulty levels
- [ ] Sound effects and animations
- [ ] Private rooms with game codes
- [ ] Admin dashboard for game control
- [ ] Statistics and player profiles
- [ ] Mobile app version

## License

MIT License - Feel free to use and modify!

## Author

Built with ❤️ for multiplayer quiz fun!

