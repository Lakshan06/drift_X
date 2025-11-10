@echo off
setlocal enabledelayedexpansion
echo ========================================
echo DriftGuardAI Connection Diagnostic
echo ========================================
echo.

echo Step 1: Checking if backend is running...
echo.
curl -s http://localhost:8080/health > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] Backend is NOT running!
    echo.
    echo Start the backend first:
    echo   cd backend
    echo   npm start
    goto :end
) else (
    echo [OK] Backend is running
)

echo.
echo Step 2: Checking if device is connected...
echo.
adb devices | findstr "device" > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] No Android device found!
    echo.
    echo Connect your phone via USB or WiFi
    goto :end
) else (
    echo [OK] Device connected
    adb devices
)

echo.
echo Step 3: Checking backend connection status...
echo.
curl -s http://localhost:8080/health
echo.

echo.
echo Step 4: Rebuild and install app...
echo.
echo This will take about 30 seconds...
echo.

cd C:\drift_X
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] Build failed!
    goto :end
)

echo.
echo Installing app...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] Installation failed!
    goto :end
)

echo.
echo [SUCCESS] App rebuilt and installed!
echo.

echo.
echo Step 5: Starting app and monitoring logs...
echo.
echo Opening app on phone...
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
timeout /t 3 > nul

echo.
echo Watching logs for connection attempts...
echo Press Ctrl+C to stop
echo.

adb logcat -s KOIN:D Realtime:D DriftGuard:D OkHttp:D

:end
echo.
echo ========================================
echo Diagnostic Complete
echo ========================================
pause
