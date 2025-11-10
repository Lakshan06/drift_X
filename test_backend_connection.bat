@echo off
echo ========================================
echo DriftGuardAI - Backend Connection Test
echo ========================================
echo.

echo Step 1: Testing Backend Health Endpoint...
echo.
curl -s http://localhost:8080/health
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] Backend is NOT running or not accessible!
    echo.
    echo Please start the backend:
    echo   cd backend
    echo   npm start
    goto :end
) else (
    echo.
    echo [SUCCESS] Backend is running!
)

echo.
echo ========================================
echo Step 2: Check Your Computer's IP Address
echo ========================================
echo.

for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4 Address"') do (
    set IP=%%a
    set IP=!IP:~1!
    echo Found IP Address: !IP!
)

echo.
echo ========================================
echo Step 3: Test from Phone
echo ========================================
echo.
echo Open your phone's browser and go to:
echo   http://!IP!:8080/health
echo.
echo If you see {"status":"ok"} = Backend is reachable!
echo.

echo ========================================
echo Step 4: Check App Configuration
echo ========================================
echo.
echo Make sure app\src\main\java\com\driftdetector\app\di\AppModule.kt has:
echo   val serverUrl = "ws://!IP!:8080"
echo.
echo ⚠️  NOT "ws://localhost:8080"
echo ⚠️  NOT "ws://127.0.0.1:8080"
echo.

echo ========================================
echo Step 5: Monitor Backend Console
echo ========================================
echo.
echo After opening the DriftGuardAI app, check backend console for:
echo   ✅ New client connected: abc-123...
echo.
echo If you see this message = APP IS CONNECTED! 🎉
echo.

:end
echo ========================================
echo Test Complete!
echo ========================================
pause
