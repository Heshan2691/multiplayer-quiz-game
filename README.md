# 🎯 Multiplayer Quiz Game (Kahoot-style)

A real-time multiplayer quiz game similar to Kahoot and Mentimeter, built with Java WebSocket and modern web technologies.

## Features

✨ **Real-time Multiplayer**: Multiple players can join and compete simultaneously
🎨 **Beautiful UI**: Colorful, responsive design inspired by Kahoot
⏱️ **Timed Questions**: Configurable countdown for each question (default 20s)
🏆 **Live Leaderboard**: Real-time score tracking and rankings for players
🎯 **Multiple Choice**: Four colorful answer options
📊 **Results Display**: Shows correct answers and updated leaderboard after each question
🔔 **Notifications**: Real-time updates when players join/leave
👨‍💼 **Admin Panel**: Full game control with question management, timing, and settings
📊 **Admin Leaderboard**: Live ranked leaderboard in admin panel with medals and times
🎮 **Game Control**: Start/stop/pause game from admin panel
🏁 **Final Leaderboard**: Automatic winner announcement with medals when game ends
⚡ **Time-Based Ranking**: Players ranked by score first, then by speed (faster wins ties)
👨‍🏫 **Admin-Controlled Pacing**: Admin manually triggers each question for better control
⚙️ **Customizable**: Adjust timer, points, and pause duration in real-time

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Modern web browser

## Project Structure

```
multiplayer-quiz-game/
├── src/main/java/com/quizgame/
│   └── QuizServer.java          # WebSocket server with admin support
├── web/
│   ├── index.html               # Player game interface
│   └── admin.html               # Admin control panel (NEW!)
├── questions.json               # Quiz questions database
├── pom.xml                      # Maven dependencies
├── README.md                    # Main documentation
└── ADMIN-GUIDE.md              # Complete admin panel guide (NEW!)
```

## How to Run

### 1. Start the Server

```bash
# Compile and run the server
mvn clean compile
mvn exec:java -Dexec.mainClass="com.quizgame.QuizServer" #or
mvn exec:java "-Dexec.mainClass=com.quizgame.QuizServer"
```

The server will start on `ws://localhost:1234`

### 2. Open the Client

Open `web/index.html` in your web browser. You can open multiple browser tabs/windows to simulate multiple players.

### 3. Open Admin Panel (Optional but Recommended)

Open `web/admin.html` in another browser tab to control the game:

- Add/delete questions in real-time
- Set question timer duration
- Start/stop the game
- Monitor connected players
- Reset scores
- Skip questions

**Important**: Game starts in STOPPED mode. Click "▶️ Start Game" in admin panel to begin broadcasting questions.

### 4. Play the Game

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
- Answer submission: `{"type":"answer", "answer":"Option", "questionIndex":0, "answerTime":...}` _(includes timestamp)_

### Server → Client

- Welcome: `{"type":"welcome", "username":"..."}`
- Question: `{"type":"question", "question":"...", "options":[...], "startTime":...}` _(includes timestamp)_
- Answer result: `{"type":"answerResult", "correct":true/false, "score":100, "timeTaken":...}` _(includes time)_
- Results: `{"type":"results", "correctAnswer":"...", "leaderboard":[...]}` _(leaderboard includes totalTime)_
- Game ended: `{"type":"gameEnded", "message":"...", "leaderboard":[...]}` _(leaderboard includes totalTime)_
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

## Recent Updates

### 📊 Player's Own Rank Display (Nov 2025)

Each player now sees their own rank prominently after every question:

- Large, prominent rank display with medals
- Personal statistics (score, time, total players)
- Ordinal suffixes (1st, 2nd, 3rd, 4th...)
- Highlighted row in leaderboard
- Instant awareness of personal standing
- Beautiful purple gradient design

### 🏆 Leaderboard While Waiting (Nov 2025)

Players now see the current leaderboard continuously while waiting for admin:

- Results screen stays visible (no auto-hide)
- Leaderboard displayed throughout waiting period
- Clear "Waiting for admin..." message shown
- Better engagement and user experience
- Players always see their current standings

### 👥 Connected Players Display (Nov 2025)

Admin panel now prominently displays connected players:

- Large, visible player count
- List of all connected player names (comma-separated)
- Real-time updates on join/leave
- Located at top of Live Leaderboard panel
- Professional blue-themed design

### 📊 Admin Live Leaderboard (Nov 2025)

Admin panel now displays a live ranked leaderboard:

- Real-time rankings with medals (🥇🥈🥉) for top 3
- Shows player scores and answer times
- Visual hierarchy with colored borders
- Sorted by score + time
- Updates automatically as players answer

### 👨‍🏫 Admin-Controlled Question Progression (Nov 2025)

Admin now controls when each question starts:

- After each question, game waits for admin
- Click "Next Question" button to continue
- Perfect for classroom pacing and discussions
- No more automatic progression
- First question auto-starts, last question auto-ends

### ⚡ Time-Based Ranking System (Nov 2025)

Players are now ranked using **both score AND time**:

- Primary ranking by score (higher is better)
- Tiebreaker by speed (faster wins ties)
- Time displayed in leaderboards: "⏱️12.3s"
- Only correct answers count toward total time
- Fair and accurate using server timestamps

### 🎉 Final Leaderboard Feature (Nov 2025)

When the admin stops the game, all connected players now see a beautiful final leaderboard with:

- 🥇🥈🥉 Medal system for top 3 players
- 🏆 Trophy for the winner
- Complete score rankings with times
- Professional presentation

## Future Enhancements

- [ ] Different point values based on answer speed
- [ ] Question categories/difficulty levels
- [ ] Private rooms with game codes
- [ ] Statistics and player profiles
- [ ] Mobile app version
- [ ] Sound effects and animations

## Completed Features

- [x] ~~Admin dashboard for game control~~ ✅ **IMPLEMENTED**
- [x] ~~Final leaderboard on game end~~ ✅ **IMPLEMENTED**
- [x] ~~Time-based ranking system~~ ✅ **IMPLEMENTED**
- [x] ~~Auto-complete game after all questions~~ ✅ **IMPLEMENTED**
- [x] ~~Admin-controlled question progression~~ ✅ **IMPLEMENTED**
- [x] ~~Admin live leaderboard view~~ ✅ **IMPLEMENTED**
- [x] ~~Connected players count and names display~~ ✅ **IMPLEMENTED**
- [x] ~~Show leaderboard while waiting for admin~~ ✅ **IMPLEMENTED**
- [x] ~~Player's own rank display after each question~~ ✅ **IMPLEMENTED**

## License

MIT License - Feel free to use and modify!

## Author

Built with ❤️ for multiplayer quiz fun!
