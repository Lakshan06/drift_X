@echo off
echo ========================================
echo  DriftGuardAI - Quick Build for Demo
echo ========================================
echo.

echo [1/5] Cleaning previous build...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Clean failed, continuing anyway...
)

echo.
echo [2/5] Building debug APK (faster)...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED! Check errors above.
    pause
    exit /b 1
)

echo.
echo [3/5] Checking build artifacts...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✓ Debug APK found
) else (
    echo ✗ Debug APK not found
)

echo.
echo [4/5] Installing on connected device...
call gradlew installDebug
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Install failed. Install manually or connect device.
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo ✓ App installed successfully!
)

echo.
echo [5/5] Launch app...
echo Opening app on device...
adb shell am start -n com.driftdetector.app/.MainActivity

echo.
echo ========================================
echo  BUILD COMPLETE!
echo ========================================
echo.
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo ✓ NEXT STEPS:
echo   1. Test upload model
echo   2. Test drift detection  
echo   3. Test patch generation
echo   4. Practice demo flow
echo.
echo For release build, run:
echo   gradlew assembleRelease
echo.
pause
