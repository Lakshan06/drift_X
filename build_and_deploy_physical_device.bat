@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo   BUILD AND DEPLOY TO PHYSICAL DEVICE
echo ========================================
echo.

REM Set Android SDK path
set ANDROID_SDK=C:\Users\slaks\AppData\Local\Android\Sdk
set ADB=%ANDROID_SDK%\platform-tools\adb.exe
set GRADLEW=gradlew.bat

echo [1/6] Checking Android SDK...
if not exist "%ADB%" (
    echo ERROR: ADB not found at %ADB%
    echo Please install Android SDK Platform Tools
    pause
    exit /b 1
)
echo ✓ Android SDK found

echo.
echo [2/6] Checking for connected devices...
"%ADB%" devices -l
echo.
echo Is your device listed above with "device" status?
echo If you see "unauthorized", please accept the USB debugging prompt on your phone!
echo.
pause

echo.
echo [3/6] Cleaning previous builds...
call %GRADLEW% clean
if errorlevel 1 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)
echo ✓ Clean successful

echo.
echo [4/6] Building Debug APK...
echo This may take a few minutes on first build...
call %GRADLEW% assembleDebug --warning-mode all
if errorlevel 1 (
    echo ERROR: Build failed
    echo Check the error messages above
    pause
    exit /b 1
)
echo ✓ Build successful!

echo.
echo [5/6] Installing APK on device...
"%ADB%" install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo ERROR: Installation failed
    echo Make sure USB debugging is enabled on your device
    pause
    exit /b 1
)
echo ✓ Installation successful!

echo.
echo [6/6] Launching app...
"%ADB%" shell am start -n com.driftdetector.app/.presentation.MainActivity
if errorlevel 1 (
    echo WARNING: Failed to launch app automatically
    echo Please launch it manually from your device
)

echo.
echo ========================================
echo   DEPLOYMENT COMPLETE! 
echo ========================================
echo.
echo Your app is now installed on your physical device!
echo.
echo NEXT STEPS:
echo 1. The app should now be running on your device
echo 2. To view real-time logs, run: view_device_logs.bat
echo 3. To reinstall after changes, run this script again
echo.
echo DEBUGGING TIPS:
echo - View logs: adb logcat -s DriftDetector
echo - Clear app data: adb shell pm clear com.driftdetector.app
echo - Uninstall: adb uninstall com.driftdetector.app
echo.
pause
