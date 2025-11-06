@echo off
echo ========================================
echo   VIEWING DEVICE LOGS (Press Ctrl+C to stop)
echo ========================================
echo.

set ANDROID_SDK=C:\Users\slaks\AppData\Local\Android\Sdk
set ADB=%ANDROID_SDK%\platform-tools\adb.exe

REM Clear old logs first
"%ADB%" logcat -c

echo Starting real-time log viewer...
echo Filtering for app logs: DriftDetector, AndroidRuntime, System.err
echo.

REM Show app logs
"%ADB%" logcat -s DriftDetector:V AndroidRuntime:E System.err:E *:E
