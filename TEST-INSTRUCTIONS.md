# 🎯 How to Test Your Multiplayer Quiz Game

## ✅ Server Status: RUNNING on port 1234

## Quick Test Instructions:

### Step 1: Open the Game Client
1. Navigate to: `/Users/nimeshmadhusanka/Desktop/multiplayer-quiz-game/web/index.html`
2. Open this file in your web browser (Chrome, Safari, Firefox, etc.)
3. You can open it by:
   - Double-clicking the file
   - Or right-click → Open With → Your browser
   - Or drag and drop into browser window

### Step 2: Test with Multiple Players
1. **Open the same file in MULTIPLE browser tabs/windows** to simulate different players
2. Each tab represents a different player

### Step 3: Play the Game
1. In each browser tab:
   - Enter a different username (e.g., "Alice", "Bob", "Charlie")
   - Click "Join Game"
2. Wait for questions (server broadcasts questions every 20 seconds)
3. Click on your answer choice
4. See if you got it right!
5. Check the leaderboard after each question

## What You Should See:

### ✅ When You Connect:
- Welcome message with your username
- Notification when other players join
- "Waiting for next question..." screen

### ✅ During Questions:
- Question displayed with 4 colorful answer buttons
- Timer counting down from 20 seconds
- Buttons in different colors (red, blue, orange, green)

### ✅ After Each Answer:
- Notification if you got it correct (✅ +100 points) or wrong (❌)
- Your score updates in the top right

### ✅ Results Screen:
- Shows the correct answer
- Displays leaderboard with all players ranked by score
- Top 3 players get special colored backgrounds (🥇🥈🥉)

## Testing Scenarios:

### Test 1: Single Player
- Open one browser tab
- Join with any username
- Answer questions when they appear
- Your score should increase by 100 for each correct answer

### Test 2: Multiplayer (RECOMMENDED)
- Open 2-3 browser tabs
- Join with different usernames in each
- Answer questions at different speeds
- Watch the leaderboard update in real-time

### Test 3: Late Joiner
- Start with one player
- Wait for a question to be asked
- Open a new tab and join mid-game
- New player should receive the next question

## Current Questions:
1. What's 2+2? → Answer: 4
2. Capital of France? → Answer: Paris
3. How many planets? → Answer: 8

## Game Settings:
- Question duration: **20 seconds**
- Pause between questions: **5 seconds**
- Points per correct answer: **100**
- Questions loop continuously

## Troubleshooting:

### Can't connect?
```bash
# Check if server is running:
lsof -i :1234

# If not running, restart:
cd /Users/nimeshmadhusanka/Desktop/multiplayer-quiz-game
mvn exec:java -Dexec.mainClass="com.quizgame.QuizServer"
```

### Stop the server:
```bash
# Find the process:
lsof -ti:1234 | xargs kill -9
```

### Restart the server:
```bash
cd /Users/nimeshmadhusanka/Desktop/multiplayer-quiz-game
./start-server.sh
```

## Expected Server Console Output:
```
Quiz Server started on port 1234
Quiz Server is running on ws://localhost:1234
New player connected: /127.0.0.1:xxxxx
Player 'Alice' joined the game.
Broadcasting question 1: What's 2+2?
Broadcasting results and leaderboard
```

## 🎉 Features to Try:
- ✅ Join with different usernames
- ✅ Answer correctly and watch your score go up
- ✅ Answer incorrectly and see the correct answer revealed
- ✅ Let the timer run out without answering
- ✅ Close one browser tab and watch the player leave notification
- ✅ Compete with friends by opening on different computers (use your computer's IP instead of localhost)

## Next Steps:
- Add more questions to `questions.json`
- Customize colors in `web/index.html`
- Adjust timing in `QuizServer.java`
- Share with friends and play together!

---
**Server is currently RUNNING! Go test it now! 🚀**

