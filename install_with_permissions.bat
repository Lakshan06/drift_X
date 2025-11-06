@echo off
echo ========================================
echo  Installing DriftGuardAI with Storage Permissions
echo ========================================
echo.

:: Find adb in Android SDK
set "ADB="
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
) else if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" (
    set "ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
) else (
    echo ERROR: adb not found!
    echo Please install Android SDK or add adb to PATH
    pause
    exit /b 1
)

echo Found adb: %ADB%
echo.

:: Check if emulator is running
echo Checking for connected devices...
"%ADB%" devices
echo.

:: Install APK
echo Installing app...
"%ADB%" install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo.
    echo ERROR: Installation failed!
    echo Make sure emulator is running and USB debugging is enabled
    pause
    exit /b 1
)
echo.

:: Grant storage permissions
echo.
echo ========================================
echo  Granting Storage Permissions
echo ========================================
echo.

echo Granting READ_EXTERNAL_STORAGE...
"%ADB%" shell pm grant com.driftdetector.app android.permission.READ_EXTERNAL_STORAGE 2>nul

echo Granting WRITE_EXTERNAL_STORAGE...
"%ADB%" shell pm grant com.driftdetector.app android.permission.WRITE_EXTERNAL_STORAGE 2>nul

echo Granting READ_MEDIA_IMAGES (Android 13+)...
"%ADB%" shell pm grant com.driftdetector.app android.permission.READ_MEDIA_IMAGES 2>nul

echo Granting READ_MEDIA_VIDEO (Android 13+)...
"%ADB%" shell pm grant com.driftdetector.app android.permission.READ_MEDIA_VIDEO 2>nul

echo Granting READ_MEDIA_AUDIO (Android 13+)...
"%ADB%" shell pm grant com.driftdetector.app android.permission.READ_MEDIA_AUDIO 2>nul

echo Granting POST_NOTIFICATIONS...
"%ADB%" shell pm grant com.driftdetector.app android.permission.POST_NOTIFICATIONS 2>nul

echo.
echo ========================================
echo  Verification
echo ========================================
echo.

echo Checking granted permissions...
"%ADB%" shell dumpsys package com.driftdetector.app | findstr "permission"
echo.

echo ========================================
echo  SUCCESS!
echo ========================================
echo.
echo  The app is installed with storage permissions
echo  You can now access files from Google Drive!
echo.
echo  Next steps:
echo  1. Open the app on your emulator
echo  2. Go to Model Upload screen
echo  3. Select Local Files
echo  4. Browse to your files!
echo.
echo  If you still can't access files:
echo  - Open Settings on emulator
echo  - Go to Apps - DriftGuardAI - Permissions
echo  - Enable "Files and media" or "All files access"
echo.

pause
