# 🌐 Emulator Internet Connectivity Guide

## 🔍 Check if Emulator has Internet Access

### Quick Test Commands

```bash
# Check if emulator is connected
adb devices

# Test internet connectivity from emulator
adb shell ping -c 4 8.8.8.8

# Test DNS resolution
adb shell ping -c 4 google.com

# Check Wi-Fi status
adb shell dumpsys wifi | findstr "Wi-Fi is"

# Check network connectivity
adb shell dumpsys connectivity | findstr "NetworkInfo"
```

---

## ✅ Expected Results

### If Internet is Working:

```bash
# Ping test should show:
PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.
64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=15.2 ms
64 bytes from 8.8.8.8: icmp_seq=2 ttl=117 time=14.8 ms
...
✅ 4 packets transmitted, 4 received, 0% packet loss
```

### If Internet is NOT Working:

```bash
# You'll see:
ping: sendto: Network is unreachable
❌ OR timeout errors
❌ OR 100% packet loss
```

---

## 🔧 Fix Internet Connection Issues

### Solution 1: Restart Emulator

**Easiest fix - Often resolves network issues:**

1. Close the emulator completely
2. In Android Studio: **Tools → Device Manager**
3. Click **Stop** on your emulator
4. Wait 5 seconds
5. Click **Start** to reboot
6. Test internet again

### Solution 2: Check Host Internet Connection

**Your computer must have internet:**

```bash
# Test your PC's internet
ping 8.8.8.8
ping google.com
```

If your PC doesn't have internet, the emulator won't either!

### Solution 3: Reset Emulator Network

**Using ADB:**

```bash
# Toggle airplane mode off/on to reset network
adb shell cmd connectivity airplane-mode enable
timeout 2
adb shell cmd connectivity airplane-mode disable

# OR restart network services
adb shell svc wifi disable
timeout 2
adb shell svc wifi enable
```

### Solution 4: Change Emulator Network Settings

**In Android Emulator:**

1. Open **Settings** on emulator
2. Go to **Network & Internet**
3. Tap **Internet** or **Wi-Fi**
4. Check if **AndroidWifi** is connected
5. If not connected:
    - Toggle Wi-Fi off/on
    - Forget network and reconnect

### Solution 5: Emulator Advanced Settings

**In Android Studio:**

1. **Tools → Device Manager**
2. Click **⋮** (three dots) next to your emulator
3. Select **Settings** or **Edit**
4. Go to **Show Advanced Settings**
5. Under **Network**:
    - Set **Network Latency**: None
    - Set **Network Speed**: Full

### Solution 6: Use Different Emulator Network Mode

**Cold boot with network reset:**

```bash
# Cold boot (fresh start)
emulator -avd <your_avd_name> -netdelay none -netspeed full -no-snapshot-load

# Example:
emulator -avd Pixel_5_API_34 -netdelay none -netspeed full
```

### Solution 7: Check Firewall/Antivirus

**Windows Firewall might block emulator:**

1. Open **Windows Security**
2. Go to **Firewall & network protection**
3. Click **Allow an app through firewall**
4. Find and enable:
    - **Android Emulator**
    - **qemu-system-x86_64.exe**
    - **adb.exe**

### Solution 8: Reset Emulator to Factory Settings

**Nuclear option - erases all data:**

1. **Android Studio → Tools → Device Manager**
2. Click **⋮** next to your emulator
3. Select **Wipe Data**
4. Confirm
5. Restart emulator

---

## 📊 Diagnostic Script

Save as `check_emulator_internet.bat`:

```batch
@echo off
echo ==========================================
echo  Emulator Internet Connectivity Check
echo ==========================================
echo.

:: Find adb
set "ADB="
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
) else (
    echo ERROR: adb not found!
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
echo.

echo [3/6] Testing DNS resolution (ping google.com)...
"%ADB%" shell ping -c 4 google.com
echo.

echo [4/6] Checking Wi-Fi status...
"%ADB%" shell dumpsys wifi | findstr "Wi-Fi is"
echo.

echo [5/6] Checking network info...
"%ADB%" shell dumpsys connectivity | findstr "NetworkInfo"
echo.

echo [6/6] Testing HTTPS connection...
"%ADB%" shell "curl -I https://www.google.com"
echo.

echo ==========================================
echo  Diagnosis Complete
echo ==========================================
echo.
echo If all tests passed, internet is working!
echo If any failed, try the fixes in CHECK_EMULATOR_INTERNET.md
echo.
pause
```

Run it:

```bash
check_emulator_internet.bat
```

---

## 🧪 Test Internet in Your App

### Test 1: Simple HTTP Request

Add this to your app to test connectivity:

```kotlin
// In your ViewModel or test class
fun testInternetConnection() {
    viewModelScope.launch {
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.connect()
            val responseCode = connection.responseCode
            
            if (responseCode == 200) {
                Timber.i("✅ Internet is working! Response: $responseCode")
            } else {
                Timber.w("⚠️ Unexpected response: $responseCode")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ No internet connection!")
        }
    }
}
```

### Test 2: Check Network State

```kotlin
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
```

---

## 🎯 Specific to Your App

### Check WebSocket Connection

Since your app uses WebSocket for real-time monitoring:

```bash
# Test WebSocket connectivity
adb shell "curl -i -N -H 'Connection: Upgrade' -H 'Upgrade: websocket' http://192.168.1.100:8080"
```

### Check Backend Server Reachability

```bash
# Test if emulator can reach your backend
adb shell ping -c 4 192.168.1.100

# Test HTTP to backend
adb shell curl http://192.168.1.100:8080/health

# If you get response, internet is working!
```

---

## 🔍 Common Issues & Solutions

### Issue 1: "Network is unreachable"

**Cause**: Emulator network not initialized  
**Solution**: Restart emulator or toggle airplane mode

```bash
adb shell cmd connectivity airplane-mode enable
timeout 2
adb shell cmd connectivity airplane-mode disable
```

### Issue 2: "DNS resolution failed"

**Cause**: DNS servers not configured  
**Solution**: Manually set DNS

```bash
# Set Google DNS
adb shell "setprop net.dns1 8.8.8.8"
adb shell "setprop net.dns2 8.8.4.4"
```

### Issue 3: Emulator shows "No internet" warning

**Cause**: Captive portal detection failed  
**Solution**: Disable captive portal

```bash
adb shell settings put global captive_portal_mode 0
```

### Issue 4: Can ping but can't browse

**Cause**: DNS or proxy issues  
**Solution**: Check proxy settings

```bash
# Check proxy settings
adb shell settings get global http_proxy

# Clear proxy
adb shell settings put global http_proxy :0
```

---

## 📱 Enable Developer Options for Network Debugging

1. Open **Settings** on emulator
2. Scroll to **About phone** → **About emulated device**
3. Tap **Build number** 7 times
4. Go back to **Settings**
5. Open **System** → **Developer options**
6. Enable:
    - **Show network logs**
    - **Mobile data always active** (if available)

---

## 🌐 Alternative: Use Host Network

If emulator network doesn't work, use host PC's network:

### Option 1: ADB Reverse Proxy

```bash
# Forward port from emulator to your PC
adb reverse tcp:8080 tcp:8080

# Now emulator can access PC's localhost:8080
# In app, use: http://localhost:8080
```

### Option 2: Use 10.0.2.2 (Special IP)

In Android emulator, `10.0.2.2` points to host PC's `localhost`:

```kotlin
// Instead of:
val serverUrl = "http://192.168.1.100:8080"

// Use:
val serverUrl = "http://10.0.2.2:8080"
```

---

## ✅ Verification Checklist

After fixing, verify these work:

- [ ] `adb shell ping 8.8.8.8` succeeds
- [ ] `adb shell ping google.com` succeeds
- [ ] Wi-Fi shows as connected in Settings
- [ ] Can browse websites in Chrome on emulator
- [ ] Your app can make network requests
- [ ] WebSocket connection works to backend

---

## 🎉 Internet Working? Test Your App!

Once internet is working:

1. **Test WebSocket connection:**
    - Start backend: `cd backend && npm start`
    - Open DriftGuardAI app
    - Check dashboard for real-time data

2. **Test file access:**
    - Go to Model Upload
    - Try cloud storage options
    - Should work if internet is available

---

## 📞 Still Having Issues?

### View Network Logs

```bash
# Real-time network logs
adb logcat | findstr -i "network connectivity wifi internet"

# Detailed connectivity logs
adb logcat ConnectivityService:V *:S
```

### Full Network Diagnostic

```bash
# Complete network dump
adb shell dumpsys connectivity > network_dump.txt
adb shell dumpsys wifi >> network_dump.txt

# Check the file for issues
```

### Reset Everything

```bash
# Nuclear option - complete reset
1. Close emulator
2. Delete AVD and recreate it
3. Start fresh with internet enabled from the start
```

---

## 💡 Pro Tips

1. **Always check PC internet first** - Emulator inherits from host
2. **Use Google DNS (8.8.8.8)** - More reliable than default
3. **Cold boot for network issues** - Snapshots can save broken network state
4. **Check Windows Firewall** - Often blocks emulator network
5. **Use 10.0.2.2 for localhost** - Special IP for host PC in emulator

---

## 🆘 Quick Fix Summary

```bash
# Quick fix - run these in order:
adb shell cmd connectivity airplane-mode enable
timeout 2
adb shell cmd connectivity airplane-mode disable
timeout 2
adb shell ping -c 4 8.8.8.8

# If still broken:
# 1. Restart emulator
# 2. Check PC internet
# 3. Check firewall
# 4. Wipe emulator data
```

---

**Your app needs internet for:**

- ✅ Real-time WebSocket monitoring
- ✅ Cloud storage file access
- ✅ Backend server communication
- ✅ Google Drive integration

Make sure it's working before testing those features!
