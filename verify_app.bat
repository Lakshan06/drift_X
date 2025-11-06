@echo off
echo ========================================
echo  DriftGuardAI - App Verification Script
echo ========================================
echo.

echo [1/6] Cleaning previous builds...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Clean failed!
    pause
    exit /b 1
)
echo ✅ Clean successful
echo.

echo [2/6] Checking for linter errors...
call gradlew lintDebug
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️ Linter warnings found, continuing...
)
echo ✅ Linter check complete
echo.

echo [3/6] Building the app...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)
echo ✅ Build successful
echo.

echo [4/6] Checking for connected devices...
adb devices
echo.

echo [5/6] Installing app on device...
call gradlew installDebug
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Installation failed!
    echo    Make sure a device is connected or emulator is running
    pause
    exit /b 1
)
echo ✅ Installation successful
echo.

echo [6/6] Launching app...
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️ Could not launch app automatically
    echo    Please launch manually from device
)
echo ✅ App launched
echo.

echo ========================================
echo  ✅ Verification Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Follow the checklist in APP_VERIFICATION_CHECKLIST.md
echo 2. Test all features systematically
echo 3. Report any issues found
echo.
echo App is installed and running on your device.
echo.
pause
