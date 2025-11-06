@echo off
echo ==========================================
echo  Fix Emulator Internet Connection
echo ==========================================
echo.

:: Find adb
set "ADB="
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
) else if exist "%ANDROID_SDK_ROOT%\platform-tools\adb.exe" (
    set "ADB=%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
) else (
    echo ERROR: adb not found!
    pause
    exit /b 1
)

echo Found adb: %ADB%
echo.

echo [Step 1/5] Checking current internet status...
"%ADB%" shell ping -c 2 8.8.8.8 >nul 2>&1
if errorlevel 1 (
    echo ❌ Internet not working - applying fixes...
) else (
    echo ✅ Internet already working!
    goto :end
)
echo.

echo [Step 2/5] Toggling airplane mode to reset network...
"%ADB%" shell cmd connectivity airplane-mode enable
timeout /t 2 /nobreak >nul
"%ADB%" shell cmd connectivity airplane-mode disable
timeout /t 3 /nobreak >nul
echo ✓ Airplane mode toggled
echo.

echo [Step 3/5] Restarting Wi-Fi...
"%ADB%" shell svc wifi disable
timeout /t 2 /nobreak >nul
"%ADB%" shell svc wifi enable
timeout /t 3 /nobreak >nul
echo ✓ Wi-Fi restarted
echo.

echo [Step 4/5] Setting Google DNS servers...
"%ADB%" shell "setprop net.dns1 8.8.8.8"
"%ADB%" shell "setprop net.dns2 8.8.4.4"
echo ✓ DNS configured
echo.

echo [Step 5/5] Disabling captive portal check...
"%ADB%" shell settings put global captive_portal_mode 0
echo ✓ Captive portal disabled
echo.

echo ==========================================
echo  Testing Internet Connection
echo ==========================================
echo.

echo Testing ping to 8.8.8.8...
"%ADB%" shell ping -c 4 8.8.8.8
if errorlevel 1 (
    echo.
    echo ❌ Internet still not working!
    echo.
    echo Additional steps to try:
    echo 1. Restart your emulator
    echo 2. Check your PC's internet connection
    echo 3. Check Windows Firewall settings
    echo 4. Try: emulator -avd YOUR_AVD_NAME -netdelay none -netspeed full
    echo.
) else (
    echo.
    echo ==========================================
    echo  ✅ SUCCESS! Internet is now working!
    echo ==========================================
    echo.
)

:end
pause
