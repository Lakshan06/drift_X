@echo off
echo ==========================================
echo  FIXING EMULATOR WI-FI INTERNET NOW
echo ==========================================
echo.
echo Please wait, this will take ~30 seconds...
echo.

:: Find adb
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
)

if not exist "%ADB%" (
    echo ERROR: Cannot find adb.exe
    echo Please make sure Android SDK is installed
    pause
    exit /b 1
)

echo Step 1/6: Resetting network...
"%ADB%" shell cmd connectivity airplane-mode enable
timeout /t 2 /nobreak >nul
"%ADB%" shell cmd connectivity airplane-mode disable
timeout /t 3 /nobreak >nul
echo DONE

echo Step 2/6: Restarting Wi-Fi...
"%ADB%" shell svc wifi disable
timeout /t 2 /nobreak >nul
"%ADB%" shell svc wifi enable
timeout /t 3 /nobreak >nul
echo DONE

echo Step 3/6: Setting Google DNS...
"%ADB%" shell "setprop net.dns1 8.8.8.8"
"%ADB%" shell "setprop net.dns2 8.8.4.4"
"%ADB%" shell "setprop net.rmnet0.dns1 8.8.8.8"
"%ADB%" shell "setprop net.rmnet0.dns2 8.8.4.4"
"%ADB%" shell "setprop net.eth0.dns1 8.8.8.8"
"%ADB%" shell "setprop net.eth0.dns2 8.8.4.4"
echo DONE

echo Step 4/6: Disabling captive portal...
"%ADB%" shell settings put global captive_portal_mode 0
"%ADB%" shell settings put global captive_portal_detection_enabled 0
echo DONE

echo Step 5/6: Clearing DNS cache...
"%ADB%" shell "ndc resolver clearnetdns eth0" 2>nul
"%ADB%" shell "ndc resolver clearnetdns wlan0" 2>nul
echo DONE

echo Step 6/6: Testing internet...
timeout /t 3 /nobreak >nul
"%ADB%" shell ping -c 4 8.8.8.8
if errorlevel 1 (
    echo.
    echo ==========================================
    echo  FAILED - Internet still not working
    echo ==========================================
    echo.
    echo Please try:
    echo 1. Restart your emulator
    echo 2. Check your PC has internet (ping 8.8.8.8)
    echo 3. Check Windows Firewall
    echo.
) else (
    echo.
    echo ==========================================
    echo  SUCCESS - Internet is now working!
    echo ==========================================
    echo.
    echo You can now:
    echo - Browse internet on emulator
    echo - Access Google Drive files
    echo - Use DriftGuardAI app
    echo.
)

pause
