# 🚀 DriftGuard Backend - Deployment Monitoring Server

A **simple, lightweight WebSocket server** that demonstrates real-time ML deployment monitoring.

## ✨ Features

- ✅ **WebSocket Real-time Communication** - Bidirectional messaging
- ✅ **Drift Alert Broadcasting** - Simulates production drift detection
- ✅ **Telemetry Streaming** - Live model prediction metrics
- ✅ **Patch Deployment** - Demonstrates remote patch application
- ✅ **Model Subscriptions** - Subscribe to specific model updates
- ✅ **Auto-reconnection Support** - Handles network interruptions
- ✅ **Health Check Endpoint** - Monitor server status
- ✅ **Multiple Client Support** - Handle concurrent connections

## 🎯 Purpose

This backend is designed to **demonstrate** deployment monitoring features. It's **NOT** a
production-grade system - it's intentionally simple to show how the Android app integrates with
real-time monitoring.

## 📋 Requirements

- **Node.js** 14+ (Download from [nodejs.org](https://nodejs.org))
- **npm** (comes with Node.js)

## ⚡ Quick Start

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Start Server

```bash
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

### 3. Connect Android App

In your Android app, update the server URL:

**File:** `app/src/main/java/com/driftdetector/app/di/AppModule.kt`

```kotlin
@Provides
@Singleton
fun provideRealtimeClient(gson: Gson): RealtimeClient {
    return RealtimeClient(
        serverUrl = "ws://YOUR_IP_ADDRESS:8080", // Change this
        gson = gson
    )
}
```

**Important:**

- Use your computer's IP address (e.g., `ws://192.168.1.100:8080`)
- NOT `localhost` or `127.0.0.1` (Android emulator can't connect to these)

### 4. Find Your IP Address

**Windows:**

```bash
ipconfig
# Look for "IPv4 Address"
```

**Mac/Linux:**

```bash
ifconfig
# Look for inet address
```

## 📡 How It Works

### Connection Flow

```
Android App                    Backend Server
    |                               |
    |-------- Connect WS ---------->|
    |<------- Welcome --------------|
    |                               |
    |-------- Auth Token ---------->|
    |<----- Auth Success ------------|
    |                               |
    |---- Subscribe to Model ------>|
    |<-- Subscription Confirmed ----|
    |                               |
    |<------- Telemetry ------------|  (Every 5s)
    |<------ Drift Alert -----------|  (Every 15s if drift)
    |                               |
    |----- Deploy Patch ----------->|
    |<---- Patch Status ------------|
```

### Message Types

#### From Client → Server

| Type | Description | Data |
|------|-------------|------|
| `auth` | Authenticate connection | `{ token: string }` |
| `subscribe` | Subscribe to model | `{ modelId: string }` |
| `unsubscribe` | Unsubscribe from model | `{ modelId: string }` |
| `status_request` | Request model status | `{ modelId: string }` |
| `patch_command` | Deploy a patch | `{ modelId, patchId, action }` |
| `ping` | Heartbeat | `{}` |

#### From Server → Client

| Type | Description | Data |
|------|-------------|------|
| `welcome` | Connection established | `{ message, server_time }` |
| `auth_success` | Authentication OK | `{ message }` |
| `drift_alert` | Drift detected | `{ modelId, severity, driftScore, ... }` |
| `telemetry` | Model metrics | `{ modelId, prediction, confidence, ... }` |
| `patch_status` | Patch deployment status | `{ modelId, patchId, status }` |
| `models_list` | Available models | `{ models: [...] }` |

## 🎮 Testing the Server

### Test with `wscat` (WebSocket CLI)

```bash
# Install wscat
npm install -g wscat

# Connect to server
wscat -c ws://localhost:8080

# Send authentication
{"type":"auth","token":"demo-token"}

# Subscribe to a model
{"type":"subscribe","modelId":"fraud-detector-v1"}

# You'll receive drift alerts and telemetry automatically!
```

### Test with cURL (Health Check)

```bash
curl http://localhost:8080/health
```

Response:

```json
{
  "status": "healthy",
  "timestamp": 1704067200000,
  "connections": 1
}
```

## 🔧 Configuration

Edit `server.js` to customize:

```javascript
// Change port
const PORT = process.env.PORT || 8080;

// Adjust drift check frequency
const DRIFT_CHECK_INTERVAL = 15000; // milliseconds

// Adjust telemetry frequency
const TELEMETRY_INTERVAL = 5000; // milliseconds

// Drift occurrence probability
if (Math.random() < 0.3) { // 30% chance
```

## 🎯 Simulated Models

The server monitors 3 pre-configured models:

| Model ID | Name | Version | Status |
|----------|------|---------|--------|
| `fraud-detector-v1` | Fraud Detector v1 | 1.0 | deployed |
| `churn-predictor-v2` | Churn Predictor v2 | 2.0 | deployed |
| `credit-scorer-v1` | Credit Scorer v1 | 1.0 | deployed |

To add more models, edit the `deployedModels` Map in `server.js`.

## 📊 What Gets Simulated

### Drift Alerts (Every 15 seconds)

```json
{
  "type": "drift_alert",
  "data": {
    "modelId": "fraud-detector-v1",
    "timestamp": 1704067200000,
    "severity": "high",
    "driftScore": 0.75,
    "driftType": "distribution_shift",
    "affectedFeatures": ["feature_0", "feature_3"],
    "message": "HIGH drift detected in production",
    "recommendedAction": "Consider retraining or applying a patch"
  }
}
```

### Telemetry Streaming (Every 5 seconds)

```json
{
  "type": "telemetry",
  "data": {
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
    "latency": 25,
    "metadata": {
      "request_id": "uuid-here",
      "client_id": "production-api-1"
    }
  }
}
```

### Patch Deployment

When you deploy a patch from the Android app:

1. Server receives `patch_command`
2. Sends `patch_status: "deploying"`
3. Waits 3 seconds (simulating deployment)
4. Sends `patch_status: "deployed"`
5. Sends a drift alert showing improvement

## 🐛 Debugging

### Enable Verbose Logging

The server already logs all events to console:

- ✅ New connections
- 📩 Incoming messages
- 📡 Subscriptions
- ⚠️ Drift alerts
- 📊 Telemetry broadcasts
- ⏹️ Disconnections

### Common Issues

#### Android app can't connect

**Problem:** Using `localhost` or `127.0.0.1`

**Solution:** Use your computer's IP address (e.g., `192.168.1.100`)

#### Port already in use

**Problem:** Port 8080 is taken

**Solution:**

```bash
# Change port
PORT=8081 npm start
```

#### CORS issues

**Problem:** Browser blocking WebSocket

**Solution:** This is a WebSocket server, not HTTP. Use native WebSocket clients (like the Android
app).

## 🔒 Security Notes

⚠️ **This is a DEMO server** - not for production!

**What's missing for production:**

- Real authentication (uses dummy auth)
- HTTPS/WSS encryption
- Rate limiting
- Input validation
- Database persistence
- Horizontal scaling
- Load balancing
- Monitoring & logging infrastructure

## 📁 Project Structure

```
backend/
├── package.json          # Dependencies
├── server.js            # Main WebSocket server
├── README.md           # This file
└── node_modules/       # Installed packages
```

## 🚀 Deployment (Optional)

### Deploy to Render.com (Free)

1. Push to GitHub
2. Go to [render.com](https://render.com)
3. Create "New Web Service"
4. Connect GitHub repo
5. Set build command: `npm install`
6. Set start command: `npm start`
7. Click "Deploy"

Your server will be available at `wss://your-app.onrender.com`

### Deploy to Heroku

```bash
# Install Heroku CLI
npm install -g heroku

# Login
heroku login

# Create app
heroku create driftguard-backend

# Deploy
git push heroku main

# Open
heroku open
```

## 📞 Need Help?

### Check Server Status

```bash
# Is server running?
curl http://localhost:8080/health
```

### View Logs

```bash
# Server logs are in terminal
# Look for:
# ✅ = Success
# ⚠️ = Warning
# ❌ = Error
```

### Test Connection

```bash
# Use wscat to test
wscat -c ws://localhost:8080
```

## ✅ Verification Checklist

After setup, verify:

- [ ] Server starts without errors
- [ ] Health endpoint responds
- [ ] Android app can connect
- [ ] Drift alerts are received
- [ ] Telemetry streams correctly
- [ ] Patch deployment works
- [ ] Auto-reconnection works

## 🎓 Next Steps

1. **Start the server:** `npm start`
2. **Connect Android app** with your IP address
3. **Subscribe to a model** in the app
4. **Watch real-time monitoring** in action!
5. **Try deploying a patch** and see the response

## 📚 Related Documentation

- **Android App:** `../README.md`
- **Real-time Guide:** `../REALTIME_MONITORING_GUIDE.md`
- **Quick Start:** `../QUICK_START_REALTIME.md`

## 🎉 You're Ready!

Start the server and watch deployment monitoring in action! 🚀

---

**Built with ❤️ for seamless ML monitoring demos**
