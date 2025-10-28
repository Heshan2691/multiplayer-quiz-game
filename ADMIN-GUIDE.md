# 🎮 Admin Panel Guide - Quiz Game

## 🚀 Quick Start

### 1. Access Admin Panel
Open in your browser:
```
/Users/nimeshmadhusanka/Desktop/multiplayer-quiz-game/web/admin.html
```

The admin panel will automatically connect to your quiz server on `ws://localhost:1234`

---

## 📋 Admin Panel Features

### 🎯 Dashboard Overview

#### Status Bar (Top)
- **Players Online**: Real-time count of connected players
- **Total Questions**: Number of questions in the database
- **Question Timer**: Current timer duration
- **Current Question**: Which question is being shown

---

## 🎮 Game Controls

### Start Game Button ▶️
- **Purpose**: Begin broadcasting questions to players
- **Status**: Game must be started for questions to appear
- **Note**: By default, game is STOPPED when server starts

### Stop Game Button ⏹️
- **Purpose**: Pause the question broadcast
- **Effect**: Players will see "Waiting for next question"
- **Use Case**: Take a break, make adjustments

### Skip Question Button ⏭️
- **Purpose**: Immediately move to next question
- **Effect**: Current question ends, results shown, next question starts
- **Use Case**: Skip problematic questions

### Reset Scores Button 🔄
- **Purpose**: Reset all player scores to 0
- **Effect**: Leaderboard clears, fresh start
- **Confirmation**: Asks for confirmation before resetting

---

## ⚙️ Game Settings

### Question Duration
- **Range**: 5 to 120 seconds
- **Default**: 20 seconds
- **Effect**: How long players have to answer each question
- **Updates**: Takes effect on next question

### Pause Between Questions
- **Range**: 0 to 30 seconds
- **Default**: 5 seconds
- **Effect**: Delay after showing results before next question
- **Purpose**: Give players time to see leaderboard

### Points per Correct Answer
- **Range**: 10 to 1000 points
- **Default**: 100 points
- **Effect**: How many points awarded for each correct answer
- **Strategy**: Higher points = more competitive

### Save Settings Button 💾
Click to apply changes to the server

---

## ➕ Add New Question

### Step-by-Step:

1. **Question Field**
   - Enter your quiz question
   - Example: "What is the capital of Japan?"

2. **Options (4 required)**
   - Option 1: First choice
   - Option 2: Second choice
   - Option 3: Third choice
   - Option 4: Fourth choice
   - Example: Tokyo, Beijing, Seoul, Bangkok

3. **Correct Answer Dropdown**
   - Select which option (1-4) is correct
   - Example: Option 1 (Tokyo)

4. **Add Question Button** ✅
   - Click to save
   - Question appears immediately in Questions List
   - Auto-saved to `questions.json`

### Tips:
- ✅ All fields must be filled
- ✅ Make questions clear and concise
- ✅ Ensure one option is clearly correct
- ✅ Avoid trick questions that frustrate players

---

## 📝 Questions List

### Features:
- **View All Questions**: Scrollable list of all quiz questions
- **See Options**: All 4 options displayed
- **Correct Answer**: Marked with green ✓
- **Delete Button**: 🗑️ Remove unwanted questions

### Deleting Questions:
1. Click 🗑️ Delete button on any question
2. Confirm deletion
3. Question removed immediately
4. Changes saved to file

### Question Format:
```
Question 1: What's 2+2?
1. 1
2. 2
3. 3
4. 4 ✓ Correct
[Delete Button]
```

---

## 👥 Connected Players

### Real-Time Player List:
- **Name**: Player username
- **Score**: Current points
- **Auto-Updates**: Every 2 seconds

### Player Info Shows:
```
Alice ............... 300 pts
Bob ................. 200 pts
Charlie ............. 100 pts
```

### Updates When:
- ✅ Player joins
- ✅ Player answers correctly
- ✅ Player disconnects
- ✅ Scores reset

---

## 🎯 Complete Workflow

### Setting Up a Game Session:

1. **Open Admin Panel**
   ```
   Open: web/admin.html
   ```

2. **Configure Settings**
   - Set question timer (e.g., 30 seconds)
   - Set pause duration (e.g., 10 seconds)
   - Set points (e.g., 200 per answer)
   - Click "Save Settings"

3. **Add Questions** (if needed)
   - Fill in question form
   - Add multiple questions
   - Review in Questions List

4. **Wait for Players**
   - Watch "Players Online" counter
   - See players join in real-time
   - Note their usernames

5. **Start the Game**
   - Click "▶️ Start Game"
   - Status changes to "Game Running"
   - Questions broadcast automatically

6. **Monitor Progress**
   - Watch player scores update
   - See who's winning
   - Track question progress

7. **Manage Game**
   - Skip problematic questions
   - Stop game for breaks
   - Reset scores for new round

---

## 🎮 Game States

### 🔴 Game Stopped (Default)
- Status: Red "Game Stopped"
- Questions: NOT broadcasting
- Players: See "Waiting for next question"
- Admin Action: Click "Start Game"

### 🟢 Game Running
- Status: Green "Game Running"
- Questions: Broadcasting automatically
- Players: Answering questions
- Admin Action: Monitor and manage

### 🟡 Game Paused
- Status: Yellow "Game Paused" (after Stop)
- Questions: Stopped at current position
- Players: Waiting
- Admin Action: Resume with "Start Game"

---

## 💡 Pro Tips

### For Best Experience:

1. **Pre-Load Questions**
   - Add all questions BEFORE starting game
   - Test questions with friends first
   - Have at least 10-15 questions

2. **Timing Strategy**
   - Easy questions: 15-20 seconds
   - Medium questions: 25-30 seconds
   - Hard questions: 35-45 seconds

3. **Scoring Strategy**
   - Casual game: 100 points
   - Competitive: 200-300 points
   - Tournament: 500+ points

4. **Player Management**
   - Wait for all players to join
   - Announce in chat before starting
   - Use pause between for breaks

5. **Mid-Game Adjustments**
   - Can add questions while game runs
   - Can change settings anytime
   - Skip questions if needed

---

## 🔧 Troubleshooting

### "Players Online" Shows 0
- ✅ Check if players opened `index.html`
- ✅ Verify server is running (port 1234)
- ✅ Check browser console for errors

### Questions Not Broadcasting
- ✅ Click "▶️ Start Game"
- ✅ Verify "Total Questions" > 0
- ✅ Check at least 1 player connected

### Settings Not Saving
- ✅ Click "💾 Save Settings" button
- ✅ Check server console for errors
- ✅ Verify values are in valid range

### Can't Add Question
- ✅ Fill ALL fields (question + 4 options)
- ✅ Select correct answer from dropdown
- ✅ Check for special characters

### Admin Panel Not Connecting
- ✅ Verify server running: `lsof -i :1234`
- ✅ Check WebSocket URL (ws://localhost:1234)
- ✅ Refresh page
- ✅ Check browser console

---

## 🎯 Example Game Session

### Scenario: Hosting a 10-Question Quiz

**Preparation (5 minutes):**
```
1. Open admin panel
2. Add 10 questions about general knowledge
3. Set timer to 25 seconds
4. Set points to 150
5. Save settings
```

**Pre-Game (2 minutes):**
```
1. Share index.html link with players
2. Watch players join (target: 5-10 players)
3. Announce game is starting
4. Click "▶️ Start Game"
```

**During Game (8 minutes):**
```
- Questions broadcast every 30 seconds (25s + 5s pause)
- Monitor leaderboard
- Skip any controversial questions
- Keep energy high
```

**Post-Game:**
```
1. Click "⏹️ Stop Game"
2. Announce winner from leaderboard
3. Click "🔄 Reset Scores" for next round
4. Or add more questions and continue
```

---

## 🎨 Visual Indicators

### Status Colors:
- 🟢 **Green**: Active/Running/Success
- 🔴 **Red**: Stopped/Error/Delete
- 🟡 **Yellow**: Warning/Paused
- 🔵 **Blue**: Information/Default

### Buttons Colors:
- **Blue**: Primary actions (Save, Add)
- **Green**: Start/Success actions
- **Red**: Stop/Delete/Danger actions
- **Orange**: Warning actions (Skip, Reset)

---

## 📊 Admin Panel Sections

### Top Section
```
┌─────────────────────────────────────┐
│  🎮 Quiz Game Admin Panel           │
│  Status: [Game Running]             │
├─────────────────────────────────────┤
│ Players: 5 │ Questions: 10 │ etc... │
└─────────────────────────────────────┘
```

### Left Column
```
┌─────────────────┐
│ Game Controls   │
├─────────────────┤
│ Settings        │
└─────────────────┘
```

### Right Column
```
┌─────────────────┐
│ Add Question    │
├─────────────────┤
│ Players List    │
└─────────────────┘
```

### Bottom Full Width
```
┌─────────────────────────────────────┐
│ Questions List (All Questions)      │
│ [Scrollable]                        │
└─────────────────────────────────────┘
```

---

## 🚀 Advanced Features

### Real-Time Updates
- Dashboard refreshes every 2 seconds
- No manual refresh needed
- Live player count
- Live score updates

### Auto-Save
- Questions saved to `questions.json`
- No data loss on server restart
- Persistent across sessions

### Multiple Admins
- Multiple admin panels can connect
- All see same data
- All can control game
- Changes sync across panels

---

## 🎉 You're Ready!

Your admin panel is now fully functional with:
- ✅ Game start/stop control
- ✅ Dynamic question management
- ✅ Customizable timing
- ✅ Flexible scoring
- ✅ Real-time player monitoring
- ✅ Live leaderboard tracking

**Enjoy hosting your quiz game!** 🎮

