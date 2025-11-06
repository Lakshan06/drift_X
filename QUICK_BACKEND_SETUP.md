# ⚡ Quick Backend Setup - 5 Minutes

Get your DriftGuard monitoring backend running in 5 minutes!

## 🎯 What You Need

- Node.js installed ([Download here](https://nodejs.org))
- Computer and phone on same WiFi

## 🚀 3 Simple Steps

### 1. Start Backend

```bash
cd backend
npm install
npm start
```

### 2. Get Your IP Address

**Windows:**

```bash
ipconfig
# Look for "IPv4 Address" (e.g., 192.168.1.100)
```

**Mac/Linux:**

```bash
ifconfig | grep "inet "
# Look for your local IP
```

### 3. Update Android App

Edit `app/src/main/java/com/driftdetector/app/di/AppModule.kt` (line ~420):

```kotlin
// Change this line:
val serverUrl = "wss://api.driftdetector.example.com/realtime"

// To this (with YOUR IP):
val serverUrl = "ws://192.168.1.100:8080"
```

**Important:** Use `ws://` not `wss://`, and use YOUR computer's IP!

### 4. Rebuild & Run

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or click "Run" in Android Studio.

## ✅ Test It

1. Open app on phone
2. Check backend console - should see: `✅ New client connected`
3. Go to Models → Select a model
4. Wait 15 seconds - drift alert appears! 🎉

## 🎮 What Works Now

- ✅ Real-time drift alerts every 15s
- ✅ Live telemetry streaming every 5s
- ✅ Patch deployment simulation
- ✅ Push notifications
- ✅ WebSocket reconnection

## 🐛 Not Working?

**Can't connect:**

- Verify IP address is correct
- Check both devices on same WiFi
- Allow port 8080 in firewall

**Port already in use:**
```bash
PORT=8081 npm start
# Update app config to match
```

## 📚 Need More Help?

See full guide: `BACKEND_SETUP_GUIDE.md`

---

**That's it! You're monitoring deployments in real-time! 🚀**

