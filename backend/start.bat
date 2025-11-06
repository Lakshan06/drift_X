@echo off
echo.
echo ============================================================
echo   DriftGuard Backend - Starting Server
echo ============================================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed!
    echo Please download and install Node.js from: https://nodejs.org
    echo.
    pause
    exit /b 1
)

echo [1/3] Checking Node.js version...
node --version
npm --version
echo.

REM Check if node_modules exists
if not exist "node_modules\" (
    echo [2/3] Installing dependencies...
    call npm install
    echo.
) else (
    echo [2/3] Dependencies already installed
    echo.
)

echo [3/3] Starting server...
echo.
echo ============================================================
echo   Server will start on http://localhost:8080
echo   Press Ctrl+C to stop the server
echo ============================================================
echo.

npm start
