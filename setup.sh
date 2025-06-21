#!/bin/bash

# Clara XLSX Diff - Development Setup Script
# This script helps set up the development environment safely

set -e  # Exit on any error

echo "🚀 Setting up Clara XLSX Diff development environment..."

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Please install Node.js 18+"
    exit 1
fi

if ! command -v clojure &> /dev/null; then
    echo "❌ Clojure CLI not found. Please install Clojure CLI tools"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Install Node.js dependencies
echo "📦 Installing Node.js dependencies..."
npm install

echo "🔧 Pre-downloading Clojure dependencies..."
clojure -P

# Check for existing shadow-cljs processes
echo "🔍 Checking for existing shadow-cljs processes..."
EXISTING_SHADOW=$(ps aux | grep -v grep | grep shadow-cljs || true)

if [ -n "$EXISTING_SHADOW" ]; then
    echo "⚠️  Found existing shadow-cljs processes:"
    echo "$EXISTING_SHADOW"
    echo ""
    echo "Please stop existing processes before starting:"
    echo "  npx shadow-cljs stop"
    echo ""
    echo "Then re-run this script."
    exit 1
fi

# Start shadow-cljs server
echo "🌟 Starting shadow-cljs server with nREPL support..."
echo "This will start:"
echo "  - Shadow-CLJS server on port 9630"
echo "  - nREPL server on port 7002"
echo ""

# Start in background and save PID
npx shadow-cljs server &
SERVER_PID=$!

echo "🔄 Waiting for server to start..."
sleep 5

# Check if server started successfully
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "❌ Failed to start shadow-cljs server"
    exit 1
fi

echo "✅ Shadow-CLJS server started successfully (PID: $SERVER_PID)"

echo ""
echo "🎉 Setup complete! Next steps:"
echo ""
echo "1. Start development builds:"
echo "   npx shadow-cljs watch :dev    # Browser development"
echo "   npx shadow-cljs watch :test   # Test runner"
echo ""
echo "2. Connect REPL in VS Code (Calva):"
echo "   - Command: 'Calva: Connect to Running REPL'"
echo "   - Host: localhost, Port: 7002"
echo "   - Project Type: shadow-cljs"
echo "   - Build: :dev or :test"
echo ""
echo "3. Open browser demo:"
echo "   http://localhost:8081"
echo ""
echo "4. To stop all processes:"
echo "   npx shadow-cljs stop"
echo ""
echo "📖 See README.md for detailed development workflow!"
