# 🚀 Quick Start - Real-Time Monitoring

Get up and running with DriftGuardAI's real-time monitoring in 5 minutes!

## 📋 Prerequisites

- ✅ Android Studio installed
- ✅ Android device/emulator (API 26+)
- ✅ Backend telemetry server (or use demo server)

## ⚡ Quick Setup

### Step 1: Build & Run

```bash
# Clone the repository (if not already done)
git clone https://github.com/your-org/driftguardai.git
cd driftguardai

# Build the app
./gradlew assembleDebug

# Or open in Android Studio and click Run
```

### Step 2: Configure Backend URL (Optional)

Edit `app/src/main/java/com/driftdetector/app/di/AppModule.kt`:

```kotlin
// Change this to your backend URL
val serverUrl = "wss://your-backend.com/api/v1/realtime"
```

### Step 3: Request Notification Permission

On Android 13+, the app will auto-request permission. Just click "Allow" when prompted.

## 🎯 Basic Usage

### 1. Start Monitoring

```kotlin
class YourViewModel(
    private val enhancedMonitoring: EnhancedMonitoringService
) : ViewModel() {
    
    fun startMonitoring() {
        viewModelScope.launch {
            enhancedMonitoring.start()
        }
    }
}
```

### 2. Subscribe to Models

```kotlin
viewModelScope.launch {
    enhancedMonitoring.subscribeToModel("model-123")
}
```

### 3. Monitor Drift Alerts

```kotlin
viewModelScope.launch {
    realtimeClient.driftAlerts.collect { alert ->
        alert?.let {
            println("Drift detected: ${it.driftScore}")
        }
    }
}
```

### 4. Deploy a Patch

```kotlin
viewModelScope.launch {
    enhancedMonitoring.deployPatch("model-123", "patch-456")
}
```

## 🔐 Demo Authentication

The app includes demo authentication for testing:

```kotlin
// Credentials
Email: demo@driftguard.ai
Password: demo123

// Or authenticate programmatically
authManager.login("demo@driftguard.ai", "demo123")
```

## 📡 Test Without Backend

The app works without a backend using mock data:

1. Local drift detection continues to work
2. Patch synthesis functions locally
3. Notifications are shown for local events
4. WebSocket gracefully handles connection failures

## 🔧 Configuration Options

### Change Server URL

```kotlin
// In AppModule.kt
val serverUrl = "wss://your-server.com/realtime"
```

### Customize Drift Thresholds

```kotlin
// In AppModule.kt
DriftDetector(
    psiThreshold = 0.2,  // Change this
    ksThreshold = 0.05    // And this
)
```

### Adjust Monitoring Frequency

```kotlin
// In ModelMonitoringService.kt
delay(30000) // Change to desired interval (milliseconds)
```

## 📊 View Real-Time Stats

Access monitoring statistics in your UI:

```kotlin
enhancedMonitoring.getStatistics().collect { stats ->
    println("Active subscriptions: ${stats.activeSubscriptions}")
    println("Connection state: ${stats.connectionState}")
    println("Network state: ${stats.networkState}")
}
```

## 🔔 Notification Testing

Test notifications manually:

```kotlin
notificationManager.showDriftAlert(
    modelId = "test-model",
    modelName = "Test Model",
    driftScore = 0.75,
    severity = "high"
)
```

## 🐛 Debugging

### Enable Verbose Logging

```bash
adb logcat | grep -E "DriftGuard|Realtime|Auth|Notification|Monitoring"
```

### Check Connection State

```kotlin
when (realtimeClient.connectionState.value) {
    ConnectionState.Connected -> println("✅ Connected")
    is ConnectionState.Error -> println("❌ Error")
    else -> println("⏳ Connecting...")
}
```

### Verify Authentication

```kotlin
val isAuth = authManager.isAuthenticated()
println("Authenticated: $isAuth")
```

### Check Network Status

```kotlin
val isOnline = connectivityManager.isCurrentlyOnline()
val networkType = connectivityManager.getCurrentNetworkType()
println("Online: $isOnline, Type: $networkType")
```

## 📱 Try It Out

### Scenario 1: Monitor a Model

```kotlin
// 1. Start service
enhancedMonitoring.start()

// 2. Subscribe
enhancedMonitoring.subscribeToModel("fraud-detector")

// 3. Watch for alerts
realtimeClient.driftAlerts.collect { alert ->
    notificationManager.showDriftAlert(...)
}
```

### Scenario 2: Deploy a Patch

```kotlin
// 1. Get drift result
val driftResult = repository.getDriftResults().first()

// 2. Synthesize patch
val patch = repository.synthesizePatch(...)

// 3. Deploy remotely
enhancedMonitoring.deployPatch(modelId, patch.id)
```

### Scenario 3: Handle Offline Mode

```kotlin
// Monitor network state
connectivityManager.networkState.collect { state ->
    when (state) {
        NetworkState.Available -> {
            // Reconnect and sync
            realtimeClient.connect()
        }
        NetworkState.Lost -> {
            // Switch to offline mode
            useLocalData()
        }
    }
}
```

## ✅ Verification Checklist

After setup, verify these work:

- [ ] App launches without crashes
- [ ] Notification permission granted
- [ ] WebSocket connects (if backend available)
- [ ] Models are listed
- [ ] Drift detection works
- [ ] Patches can be synthesized
- [ ] Notifications appear
- [ ] Offline mode works

## 🎓 Next Steps

1. **Read Full Guide**: See `REALTIME_MONITORING_GUIDE.md`
2. **Setup Backend**: Deploy WebSocket server
3. **Integrate SDK**: Add to model deployment
4. **Customize**: Adjust thresholds and UI
5. **Deploy**: Ship to production!

## 📞 Need Help?

- **Documentation**: `REALTIME_MONITORING_GUIDE.md`
- **Architecture**: `REALTIME_FEATURES_IMPLEMENTATION_SUMMARY.md`
- **Summary**: `PRODUCTION_READY_SUMMARY.md`

## 🎉 You're Ready!

DriftGuardAI is now monitoring your models in real-time! 🚀

---

**Built with ❤️ for seamless ML monitoring**
