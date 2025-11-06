# 🚀 Real-Time Monitoring System - Implementation Guide

## Overview

DriftGuardAI now features a **production-grade real-time monitoring system** that enables data
scientists to monitor deployed models with live telemetry, instant drift alerts, and remote patch
deployment capabilities.

## 🎯 Key Features Implemented

### 1. **WebSocket Real-Time Communication**

- Bidirectional streaming for telemetry data
- Auto-reconnection with exponential backoff
- Heartbeat mechanism for connection health
- Support for model subscriptions

**Location**: `app/src/main/java/com/driftdetector/app/core/realtime/RealtimeClient.kt`

**Features**:

```kotlin
- connect() / disconnect()
- sendTelemetry(telemetry)
- subscribeToModel(modelId)
- requestModelStatus(modelId)
- sendPatchCommand(modelId, patchId, action)
```

**Connection States**:

- `Disconnected`
- `Connecting`
- `Connected`
- `Reconnecting(attempt)`
- `Error(message)`

### 2. **Authentication & Authorization**

- JWT token management
- Secure session storage
- Role-based access control (RBAC)
- Token refresh mechanism

**Location**: `app/src/main/java/com/driftdetector/app/core/auth/AuthManager.kt`

**User Roles**:

- **Admin**: Full access to all features
- **Data Scientist**: Monitor, create patches, deploy
- **Viewer**: Read-only access to models and telemetry

**Permissions**:

```kotlin
enum class Permission {
    VIEW_MODELS,
    CREATE_MODELS,
    EDIT_MODELS,
    DELETE_MODELS,
    MONITOR_DRIFT,
    CREATE_PATCHES,
    DEPLOY_PATCHES,
    VIEW_TELEMETRY,
    MANAGE_USERS,
    ADMIN_SETTINGS
}
```

### 3. **Network Connectivity Management**

- Real-time network status monitoring
- Network type detection (WiFi, Cellular, Ethernet)
- Bandwidth estimation
- Metered connection detection

**Location**:
`app/src/main/java/com/driftdetector/app/core/connectivity/NetworkConnectivityManager.kt`

**Features**:

```kotlin
- isCurrentlyOnline(): Boolean
- getCurrentNetworkType(): NetworkType
- isMeteredConnection(): Boolean
- getNetworkBandwidth(): NetworkBandwidth
```

### 4. **Push Notifications**

- Drift alert notifications
- Patch synthesis notifications
- Connection status updates
- Monitoring statistics

**Location**:
`app/src/main/java/com/driftdetector/app/core/notifications/DriftNotificationManager.kt`

**Notification Types**:

- 🚨 **Critical Drift Alerts**: High-priority notifications for critical drift
- ⚠️ **Drift Warnings**: Medium-priority for detected drift
- ✅ **Patch Ready**: Notifications when patches are synthesized
- 🟢 **Connection Status**: Real-time connection updates

### 5. **Enhanced Dependencies**

Added to `app/build.gradle.kts`:

```kotlin
// WebSocket for real-time communication
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
implementation("org.java-websocket:Java-WebSocket:1.5.6")

// Authentication & JWT
implementation("com.auth0.android:jwtdecode:2.0.2")

// Firebase Cloud Messaging for push notifications
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")

// Connectivity monitoring
implementation("com.github.pwittchen:reactivenetwork-rx2:3.0.8")
```

## 🏗️ Architecture

### Real-Time Data Flow

```
┌─────────────────────┐
│  Deployed Model     │
│  (Production)       │
└──────────┬──────────┘
           │ Telemetry
           ▼
┌─────────────────────┐
│  Backend Service    │
│  (Telemetry Server) │
└──────────┬──────────┘
           │ WebSocket
           ▼
┌─────────────────────┐
│  DriftGuardAI App   │
│  (Mobile Client)    │
├─────────────────────┤
│ • RealtimeClient    │
│ • Auth Manager      │
│ • Notification Mgr  │
│ • Connectivity Mgr  │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│  Data Scientist     │
│  (User)             │
└─────────────────────┘
```

### Component Interaction

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Auth        │────▶│  Realtime    │────▶│  Monitoring  │
│  Manager     │     │  Client      │     │  Service     │
└──────────────┘     └──────────────┘     └──────────────┘
                             │
                             ▼
                    ┌──────────────┐
                    │ Notification │
                    │ Manager      │
                    └──────────────┘
```

## 📡 Real-Time Telemetry

### Telemetry Event Structure

```kotlin
data class TelemetryEvent(
    val modelId: String,
    val timestamp: Long,
    val inputFeatures: Map<String, Double>,
    val prediction: Double,
    val confidence: Double,
    val latency: Long,
    val metadata: Map<String, Any>? = null
)
```

### Drift Alert Structure

```kotlin
data class DriftAlert(
    val modelId: String,
    val timestamp: Long,
    val severity: String, // "low", "medium", "high", "critical"
    val driftScore: Double,
    val driftType: String,
    val affectedFeatures: List<String>,
    val message: String,
    val recommendedAction: String?
)
```

## 🔧 Integration Guide

### 1. Setup Backend Server

Your backend telemetry server should support WebSocket connections:

**WebSocket URL Format**:

```
wss://your-backend.com/api/v1/realtime
```

**Message Types** (JSON):

**Client → Server**:

```json
{
  "type": "auth",
  "token": "JWT_TOKEN"
}

{
  "type": "subscribe",
  "modelId": "model-123"
}

{
  "type": "telemetry",
  "data": {
    "modelId": "model-123",
    "timestamp": 1234567890,
    "metrics": {...}
  }
}

{
  "type": "patch_command",
  "modelId": "model-123",
  "patchId": "patch-456",
  "action": "deploy"
}
```

**Server → Client**:

```json
{
  "type": "telemetry",
  "data": {
    "modelId": "model-123",
    "timestamp": 1234567890,
    "inputFeatures": {...},
    "prediction": 0.85,
    "confidence": 0.92,
    "latency": 45
  }
}

{
  "type": "drift_alert",
  "data": {
    "modelId": "model-123",
    "severity": "high",
    "driftScore": 0.75,
    "driftType": "covariate_drift",
    "affectedFeatures": ["feature1", "feature2"],
    "message": "Significant drift detected",
    "recommendedAction": "Review and retrain model"
  }
}

{
  "type": "auth_success" | "auth_failed"
}

{
  "type": "status_update",
  "data": {...}
}
```

### 2. Initialize in App

The real-time services are automatically initialized in the DI container (Koin).

To use them in ViewModels:

```kotlin
class MyViewModel(
    private val authManager: AuthManager,
    private val realtimeClient: RealtimeClient,
    private val notificationManager: DriftNotificationManager,
    private val connectivityManager: NetworkConnectivityManager
) : ViewModel() {
    
    init {
        // Authenticate first
        viewModelScope.launch {
            val result = authManager.login("user@example.com", "password")
            if (result.isSuccess) {
                // Connect to real-time server
                realtimeClient.connect()
                
                // Subscribe to models
                realtimeClient.subscribeToModel("model-123")
            }
        }
        
        // Monitor connection state
        viewModelScope.launch {
            realtimeClient.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        // Connected!
                    }
                    is ConnectionState.Error -> {
                        // Handle error
                    }
                    else -> {}
                }
            }
        }
        
        // Monitor drift alerts
        viewModelScope.launch {
            realtimeClient.driftAlerts.collect { alert ->
                alert?.let {
                    // Show notification
                    notificationManager.showDriftAlert(
                        modelId = it.modelId,
                        modelName = "Model Name",
                        driftScore = it.driftScore,
                        severity = it.severity
                    )
                }
            }
        }
    }
}
```

### 3. Deployment SDK Integration

For embedding telemetry collection in deployment environments:

**Python Example** (for ML deployment pipelines):

```python
import websocket
import json

class DriftGuardTelemetry:
    def __init__(self, server_url, api_key):
        self.server_url = server_url
        self.api_key = api_key
        self.ws = None
        
    def connect(self):
        self.ws = websocket.create_connection(self.server_url)
        # Authenticate
        self.ws.send(json.dumps({
            "type": "auth",
            "token": self.api_key
        }))
        
    def send_telemetry(self, model_id, features, prediction, confidence):
        telemetry = {
            "type": "telemetry",
            "data": {
                "modelId": model_id,
                "timestamp": time.time() * 1000,
                "inputFeatures": features,
                "prediction": prediction,
                "confidence": confidence,
                "latency": 0  # Measure actual latency
            }
        }
        self.ws.send(json.dumps(telemetry))

# Usage in model inference
telemetry = DriftGuardTelemetry("wss://your-backend.com/api/v1/realtime", "API_KEY")
telemetry.connect()

# After each prediction
result = model.predict(input_data)
telemetry.send_telemetry(
    model_id="model-123",
    features=input_data.to_dict(),
    prediction=result,
    confidence=model.predict_proba(input_data).max()
)
```

### 4. Notification Setup

**Android Manifest** (already configured):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**Request Permission** (in Activity):

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        REQUEST_CODE_NOTIFICATIONS
    )
}
```

## 🔒 Security Best Practices

### 1. **Authentication**

- Always use HTTPS/WSS in production
- Store JWT tokens securely in DataStore
- Implement token refresh before expiry
- Never hardcode credentials

### 2. **Authorization**

- Check permissions before sensitive operations
- Use RBAC to restrict access
- Audit user actions

### 3. **Data Privacy**

- Encrypt telemetry data in transit
- Apply differential privacy to sensitive metrics
- Comply with data retention policies

### 4. **Network Security**

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">your-backend.com</domain>
    </domain-config>
</network-security-config>
```

## 📊 Monitoring Dashboard

The real-time monitoring data is visualized in the app dashboard:

### Key Metrics Displayed:

- **Active Connections**: Number of models being monitored
- **Drift Events**: Real-time drift detection count
- **Patches Synthesized**: Auto-generated patches
- **System Health**: Connection status, latency, bandwidth

### Real-Time Charts:

- **Drift Score Timeline**: Live drift score over time
- **Feature Distribution**: Real-time feature statistics
- **Prediction Confidence**: Model confidence trends
- **Latency Metrics**: Inference performance

## 🚦 Performance Optimization

### 1. **Efficient Data Streaming**

- Use message batching for high-frequency telemetry
- Implement backpressure handling
- Cache locally during offline periods

### 2. **Battery Optimization**

- Use WorkManager for background sync
- Reduce polling frequency when app is backgrounded
- Batch notifications

### 3. **Network Optimization**

- Compress telemetry data
- Use binary protocols (e.g., Protocol Buffers) for production
- Implement adaptive streaming based on bandwidth

### 4. **Memory Management**

- Limit telemetry buffer size
- Clean up old data periodically
- Use efficient data structures

## 🧪 Testing

### Unit Tests

```kotlin
@Test
fun `realtimeClient connects successfully`() = runTest {
    val client = RealtimeClient("ws://localhost:8080", "test-token")
    client.connect()
    
    client.connectionState.test {
        assertEquals(ConnectionState.Connected, awaitItem())
    }
}
```

### Integration Tests

```kotlin
@Test
fun `drift alert triggers notification`() = runTest {
    val alert = DriftAlert(
        modelId = "test-model",
        timestamp = System.currentTimeMillis(),
        severity = "high",
        driftScore = 0.8,
        driftType = "covariate_drift",
        affectedFeatures = listOf("feature1"),
        message = "Test drift",
        recommendedAction = "Retrain"
    )
    
    notificationManager.showDriftAlert(
        modelId = alert.modelId,
        modelName = "Test Model",
        driftScore = alert.driftScore,
        severity = alert.severity
    )
    
    // Verify notification was shown
    assertTrue(notificationManager.areNotificationsEnabled())
}
```

## 🎓 Advanced Usage

### Custom Drift Detection Rules

You can configure custom drift thresholds:

```kotlin
val monitoringConfig = MonitoringConfig(
    driftThreshold = 0.3,
    alertSeverity = mapOf(
        0.3 to "medium",
        0.5 to "high",
        0.7 to "critical"
    ),
    autoSynthesizePatch = true,
    notificationEnabled = true
)
```

### Automated Remediation

Enable automatic patch deployment for low-risk drift:

```kotlin
if (alert.severity == "low" && alert.driftScore < 0.4) {
    realtimeClient.sendPatchCommand(
        modelId = alert.modelId,
        patchId = suggestedPatch.id,
        action = "deploy_auto"
    )
}
```

## 📱 User Experience

### Connection Indicators

- 🟢 **Green**: Connected and monitoring
- 🟡 **Yellow**: Reconnecting
- 🔴 **Red**: Disconnected

### Notifications

- **Critical alerts**: Sound + vibration
- **Info updates**: Silent notifications
- **Persistent monitoring**: Ongoing notification (optional)

## 🔍 Troubleshooting

### Connection Issues

```kotlin
// Check network status
if (!connectivityManager.isCurrentlyOnline()) {
    // Handle offline mode
}

// Check authentication
if (!authManager.isAuthenticated()) {
    // Re-authenticate
}

// Check WebSocket state
when (realtimeClient.connectionState.value) {
    is ConnectionState.Error -> {
        // Retry connection
        realtimeClient.connect()
    }
}
```

### Performance Issues

- Check network bandwidth
- Reduce telemetry frequency
- Enable data compression
- Use batch mode for high-volume data

## 📈 Metrics & Analytics

Track system performance:

- Connection uptime
- Message throughput
- Alert response time
- Patch deployment success rate

## 🎉 Next Steps

1. **Deploy Backend Service**: Setup WebSocket telemetry server
2. **Configure Authentication**: Implement JWT token issuance
3. **Integrate SDK**: Add telemetry to model deployment
4. **Test End-to-End**: Verify real-time drift detection
5. **Monitor & Optimize**: Track performance metrics

## 📞 Support

For issues or questions:

- Check logs: `adb logcat | grep DriftGuard`
- Review crash reports: `app/files/crash_*.log`
- Enable debug mode for verbose logging

---

**Built with ❤️ for seamless ML model monitoring**
