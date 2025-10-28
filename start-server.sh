#!/bin/bash
# Start the Quiz Server

echo "🎯 Starting Multiplayer Quiz Game Server..."
echo "=========================================="
echo ""

cd "$(dirname "$0")"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if questions.json exists
if [ ! -f "questions.json" ]; then
    echo "⚠️  Warning: questions.json not found!"
fi

echo "📦 Compiling project..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilation successful!"
    echo "🚀 Starting server on ws://localhost:1234"
    echo ""
    echo "📝 Instructions:"
    echo "   1. Open web/index.html in your browser"
    echo "   2. Enter your username to join"
    echo "   3. Have fun! 🎉"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo "=========================================="
    echo ""

    mvn exec:java -Dexec.mainClass="com.quizgame.QuizServer"
else
    echo "❌ Compilation failed. Please check the errors above."
    exit 1
fi

