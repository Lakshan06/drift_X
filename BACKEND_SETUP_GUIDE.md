# 🚀 Backend Setup Guide - DriftGuard Deployment Monitoring

## 📋 Overview

This guide will help you set up the **simple WebSocket backend** to demonstrate real-time deployment
monitoring with your DriftGuardAI Android app.

## ✨ What You'll Get

- ✅ Real-time drift alerts to your phone
- ✅ Live telemetry streaming from "deployed" models
- ✅ Patch deployment demonstrations
- ✅ WebSocket connection status
- ✅ Push notifications
- ✅ Model subscription management

## 🎯 Prerequisites

### 1. Node.js Installation

**Check if already installed:**

```bash
node --version
npm --version
```

**If not installed:**

- **Windows:** Download from [nodejs.org](https://nodejs.org) (LTS version)
- **Mac:** `brew install node`
- **Linux:** `sudo apt install nodejs npm`

### 2. Network Requirements

- Computer and Android device on **same WiFi network**
- Firewall allowing port `8080` (or chosen port)

## 🚀 Step-by-Step Setup

### Step 1: Start the Backend Server

**On Windows:**

```bash
cd backend
start.bat
```

**On Mac/Linux:**

```bash
cd backend
chmod +x start.sh
./start.sh
```

**Or manually:**

```bash
cd backend
npm install
npm start
```

You should see:

```
============================================================
  🚀 DriftGuard Deployment Monitoring Server
============================================================
  📡 WebSocket: ws://localhost:8080
  🏥 Health: http://localhost:8080/health
  📊 Monitoring 3 deployed models
============================================================
```

### Step 2: Find Your Computer's IP Address

**Windows:**

```bash
ipconfig
```

Look for `IPv4 Address` under your WiFi adapter (e.g., `192.168.1.100`)

**Mac:**

```bash
ifconfig | grep "inet "
```

**Linux:**

```bash
ip addr show
```

**Example IP:** `192.168.1.100`

### Step 3: Update Android App Configuration

#### Option A: Update Code (Permanent)

Open `app/src/main/java/com/driftdetector/app/di/AppModule.kt`

Find this section (around line 420):

```kotlin
single {
    try {
        Log.d("KOIN", "Creating RealtimeClient...")
        // Default server URL - can be configured via settings
        val serverUrl = "wss://api.driftdetector.example.com/realtime"
```

**Change to:**

```kotlin
single {
    try {
        Log.d("KOIN", "Creating RealtimeClient...")
        // Local development server
        val serverUrl = "ws://192.168.1.100:8080" // ← Your IP here!
```

**Important Notes:**

- Use `ws://` (NOT `wss://`) for local testing
- Replace `192.168.1.100` with YOUR computer's IP
- Don't use `localhost` or `127.0.0.1` - they won't work!

#### Option B: Settings Screen (Future Feature)

In a future update, you can configure this in the app's Settings screen without rebuilding.

### Step 4: Rebuild the Android App

```bash
# From project root
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Or in Android Studio:**

- Click "Run" (green play button)
- App will rebuild with new server URL

### Step 5: Test the Connection

1. **Open DriftGuardAI app**
2. **Grant notification permission** (if prompted)
3. **Go to Dashboard**
4. **Watch the backend console** - you should see:

```
✅ New client connected: <uuid>
📊 Total connections: 1
```

5. **In the app** - look for connection indicator (green dot)

## 🎮 How to Use

### Subscribe to Model Monitoring

1. Go to **Models** screen
2. Select a model
3. The app auto-subscribes when viewing model details
4. Backend console shows: `📡 Client subscribed to model: <modelId>`

### Receive Drift Alerts

The backend automatically sends drift alerts every 15 seconds (30% chance):

- **Low severity:** Green notification
- **Medium severity:** Yellow notification
- **High severity:** Orange notification
- **Critical severity:** Red notification

You'll see them both:

- In the app's dashboard
- As phone notifications
- In the backend console

### View Live Telemetry

The backend streams telemetry every 5 seconds:

- Real-time prediction data
- Feature values
- Confidence scores
- Latency metrics

### Deploy a Patch

1. Go to **Patches** screen
2. Select a patch
3. Tap "Deploy"
4. Backend console shows:
   ```
   🚀 Patch command received: deploy patch <id> for model <modelId>
   ```
5. Watch deployment progress in app
6. Receive confirmation notification

## 📊 What's Being Simulated

### Pre-configured Models

| Model ID | Name | Description |
|----------|------|-------------|
| `fraud-detector-v1` | Fraud Detector v1 | Credit card fraud detection |
| `churn-predictor-v2` | Churn Predictor v2 | Customer churn prediction |
| `credit-scorer-v1` | Credit Scorer v1 | Credit risk scoring |

### Drift Types

- **Distribution Shift:** Input data distribution changed
- **Concept Drift:** Relationship between features and target changed
- **Feature Drift:** Individual features drifted

### Telemetry Data

```json
{
  "modelId": "fraud-detector-v1",
  "timestamp": 1704067200000,
  "inputFeatures": {
    "feature_0": 0.42,
    "feature_1": 5.67,
    "feature_2": 89.12,
    "feature_3": 2.34
  },
  "prediction": 0.87,
  "confidence": 0.92,
  "latency": 25
}
```

## 🐛 Troubleshooting

### Issue: App Can't Connect

**Symptoms:**

- No connection indicator in app
- Backend shows no connections
- Notifications don't appear

**Solutions:**

1. **Check IP address is correct**
   ```bash
   # Verify server is running
   curl http://YOUR_IP:8080/health
   ```

2. **Check firewall**
   ```bash
   # Windows: Allow port in firewall
   # Settings → Firewall → Allow app → Node.js
   
   # Mac: System Preferences → Security → Firewall
   
   # Linux
   sudo ufw allow 8080
   ```

3. **Verify same WiFi network**
    - Phone and computer must be on same network
    - Not mobile data, not different WiFi

4. **Try different port**
   ```bash
   # Start server on different port
   PORT=8081 npm start
   
   # Update app config to match
   ```

### Issue: Server Won't Start

**Symptoms:**

- "Port already in use" error
- "Node not found" error

**Solutions:**

1. **Port in use:**
   ```bash
   # Find what's using port 8080
   # Windows:
   netstat -ano | findstr :8080
   
   # Mac/Linux:
   lsof -i :8080
   
   # Kill it or use different port
   PORT=8081 npm start
   ```

2. **Node not installed:**
    - Download from [nodejs.org](https://nodejs.org)
    - Restart terminal after install
    - Verify: `node --version`

### Issue: Notifications Not Appearing

**Symptoms:**

- Backend sends alerts
- App doesn't show notifications

**Solutions:**

1. **Grant notification permission**
    - Android Settings → Apps → DriftGuardAI → Notifications → Allow

2. **Check Do Not Disturb**
    - Disable Do Not Disturb mode

3. **Check app is in foreground**
    - Some devices need app in foreground for notifications

### Issue: Drift Alerts Too Frequent/Rare

**Solution:** Edit `backend/server.js`

```javascript
// Line 7-8
const DRIFT_CHECK_INTERVAL = 15000; // Change this (milliseconds)

// Line 445 - drift probability
if (Math.random() < 0.3) { // Change 0.3 to adjust probability
```

## 🔧 Advanced Configuration

### Change Server Port

**Option 1: Environment variable**

```bash
PORT=3000 npm start
```

**Option 2: Edit server.js**

```javascript
const PORT = process.env.PORT || 3000; // Change 8080 to 3000
```

### Add More Models

Edit `backend/server.js` around line 36:

```javascript
const deployedModels = new Map([
  ['fraud-detector-v1', { 
    name: 'Fraud Detector v1', 
    version: '1.0', 
    status: 'deployed',
    lastCheck: Date.now()
  }],
  // Add your model:
  ['my-model-id', {
    name: 'My Model',
    version: '1.0',
    status: 'deployed',
    lastCheck: Date.now()
  }]
]);
```

### Adjust Drift Severity

Edit `backend/server.js` around line 448:

```javascript
const driftScore = Math.random();
let severity = 'low';

if (driftScore > 0.7) severity = 'critical';      // Change thresholds
else if (driftScore > 0.5) severity = 'high';     // Adjust these
else if (driftScore > 0.3) severity = 'medium';   // As needed
```

### Enable HTTPS (WSS)

For production or remote testing:

```javascript
// Install dependencies
npm install https fs

// server.js
const https = require('https');
const fs = require('fs');

const server = https.createServer({
  cert: fs.readFileSync('path/to/cert.pem'),
  key: fs.readFileSync('path/to/key.pem')
}, app);

// Use wss:// in app config
```

## 🚀 Deploy to Cloud (Optional)

### Option 1: Render.com (Free, Easy)

1. Push code to GitHub
2. Go to [render.com](https://render.com) → Sign up
3. Click "New +" → "Web Service"
4. Connect GitHub repository
5. Settings:
    - **Build Command:** `npm install`
    - **Start Command:** `npm start`
    - **Environment:** Node
6. Click "Create Web Service"
7. Get URL: `wss://your-app.onrender.com`
8. Update app config with this URL

### Option 2: Heroku

```bash
# Install Heroku CLI
npm install -g heroku

# Login
heroku login

# Create app
cd backend
heroku create driftguard-backend

# Deploy
git init
git add .
git commit -m "Initial commit"
git push heroku main

# Get URL
heroku info
```

### Option 3: AWS EC2

1. Launch EC2 instance (Ubuntu)
2. SSH into instance
3. Install Node.js
4. Clone repo, run server
5. Configure security group for port 8080
6. Use public IP in app

## 📱 Testing Checklist

After setup, verify these work:

- [ ] Backend server starts without errors
- [ ] Health endpoint responds: `curl http://YOUR_IP:8080/health`
- [ ] Android app connects (check backend console)
- [ ] Notification permission granted in app
- [ ] Drift alerts appear as notifications
- [ ] Dashboard shows real-time data
- [ ] Patch deployment works
- [ ] Auto-reconnection works (stop/start server)

## 🎯 Usage Scenarios

### Scenario 1: Monitor During Development

```bash
# Terminal 1: Run backend
cd backend
npm start

# Terminal 2: Watch logs
# Backend logs show all activity

# Phone: Run app, subscribe to models
```

### Scenario 2: Demo to Stakeholders

1. Start backend
2. Open app on phone
3. Cast phone screen to TV/projector
4. Show real-time monitoring
5. Trigger patch deployment
6. Explain drift alerts

### Scenario 3: Test at Scale

```javascript
// Edit server.js to send more frequent alerts
const DRIFT_CHECK_INTERVAL = 5000; // 5 seconds
const TELEMETRY_INTERVAL = 2000;   // 2 seconds

// Increase drift probability
if (Math.random() < 0.8) { // 80% chance
```

## 📞 Getting Help

### Check Logs

**Backend logs:**

- Shows all connections, messages, alerts
- Look for errors marked with ❌

**Android logs:**

```bash
adb logcat | grep -E "Realtime|DriftGuard|WebSocket"
```

### Test with WebSocket Client

```bash
# Install wscat
npm install -g wscat

# Connect
wscat -c ws://YOUR_IP:8080

# Authenticate
{"type":"auth","token":"test-token"}

# Subscribe
{"type":"subscribe","modelId":"fraud-detector-v1"}

# Watch messages arrive!
```

### Common Error Messages

| Error | Meaning | Solution |
|-------|---------|----------|
| `ECONNREFUSED` | Can't connect to server | Check IP, port, firewall |
| `EADDRINUSE` | Port already used | Change port or kill process |
| `Authentication failed` | Invalid token | Use any token (demo accepts all) |
| `Network unreachable` | Wrong network | Ensure same WiFi |

## ✅ Success Indicators

When everything works, you'll see:

**Backend Console:**

```
✅ New client connected: abc-123
📡 Client abc-123 subscribed to model: fraud-detector-v1
⚠️  Drift alert broadcast for fraud-detector-v1: high
```

**Android App:**

- 🟢 Green connection indicator
- 📊 Real-time telemetry in dashboard
- 🔔 Drift alert notifications
- ✅ Patch deployment success messages

**Phone Notifications:**

- "High drift detected in Fraud Detector v1"
- "Patch deployed successfully"
- "Connected to DriftGuard Server"

## 🎉 You're Ready!

Your deployment monitoring backend is now running! The Android app will receive real-time drift
alerts, telemetry streams, and can deploy patches remotely.

## 📚 Related Documentation

- **Backend README:** `backend/README.md`
- **Real-time Monitoring Guide:** `REALTIME_MONITORING_GUIDE.md`
- **Quick Start:** `QUICK_START_REALTIME.md`
- **App README:** `README.md`

---

**Built with ❤️ for seamless ML monitoring demonstrations**
