#!/bin/bash

echo ""
echo "============================================================"
echo "  DriftGuard Backend - Starting Server"
echo "============================================================"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "[ERROR] Node.js is not installed!"
    echo "Please download and install Node.js from: https://nodejs.org"
    echo ""
    exit 1
fi

echo "[1/3] Checking Node.js version..."
node --version
npm --version
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "[2/3] Installing dependencies..."
    npm install
    echo ""
else
    echo "[2/3] Dependencies already installed"
    echo ""
fi

echo "[3/3] Starting server..."
echo ""
echo "============================================================"
echo "  Server will start on http://localhost:8080"
echo "  Press Ctrl+C to stop the server"
echo "============================================================"
echo ""

npm start
