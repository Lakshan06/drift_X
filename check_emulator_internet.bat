@echo off
echo ==========================================
echo  Emulator Internet Connectivity Check
echo ==========================================
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

echo [1/6] Checking connected devices...
"%ADB%" devices
echo.

echo [2/6] Testing internet connectivity (ping 8.8.8.8)...
"%ADB%" shell ping -c 4 8.8.8.8
if errorlevel 1 (
    echo ❌ FAILED - No internet connection!
) else (
    echo ✅ PASSED - Internet is working!
)
echo.

echo [3/6] Testing DNS resolution (ping google.com)...
"%ADB%" shell ping -c 4 google.com
if errorlevel 1 (
    echo ❌ FAILED - DNS not working!
) else (
    echo ✅ PASSED - DNS is working!
)
echo.

echo [4/6] Checking Wi-Fi status...
"%ADB%" shell dumpsys wifi | findstr "Wi-Fi is"
echo.

echo [5/6] Checking network info...
"%ADB%" shell dumpsys connectivity | findstr "NetworkInfo"
echo.

echo [6/6] Testing HTTPS connection...
"%ADB%" shell "curl -I https://www.google.com" 2>nul
if errorlevel 1 (
    echo ⚠️ curl not available or HTTPS connection failed
) else (
    echo ✅ HTTPS connection works!
)
echo.

echo ==========================================
echo  Diagnosis Complete
echo ==========================================
echo.
echo If all tests passed, internet is working!
echo If any failed, see CHECK_EMULATOR_INTERNET.md for fixes
echo.
echo Quick fixes to try:
echo 1. Restart emulator
echo 2. Toggle airplane mode: adb shell cmd connectivity airplane-mode enable
echo 3. Check your PC's internet connection
echo.
pause
