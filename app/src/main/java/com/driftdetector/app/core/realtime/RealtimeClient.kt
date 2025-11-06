package com.driftdetector.app.core.realtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for real-time telemetry streaming and monitoring
 * Handles bidirectional communication with backend deployment services
 */
class RealtimeClient(
    private val serverUrl: String,
    private val authToken: String? = null,
    private val gson: Gson = Gson()
) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryEvents = MutableStateFlow<TelemetryEvent?>(null)
    val telemetryEvents: StateFlow<TelemetryEvent?> = _telemetryEvents.asStateFlow()

    private val _driftAlerts = MutableStateFlow<DriftAlert?>(null)
    val driftAlerts: StateFlow<DriftAlert?> = _driftAlerts.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.i("🟢 WebSocket connected")
            _connectionState.value = ConnectionState.Connected

            // Send authentication message
            authToken?.let { token ->
                val authMessage = mapOf(
                    "type" to "auth",
                    "token" to token
                )
                sendMessage(authMessage)
            }

            // Cancel any reconnect attempts
            reconnectJob?.cancel()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                handleMessage(text)
            } catch (e: Exception) {
                Timber.e(e, "Error handling WebSocket message")
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.w("WebSocket closing: $code - $reason")
            _connectionState.value = ConnectionState.Disconnecting
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.w("WebSocket closed: $code - $reason")
            _connectionState.value = ConnectionState.Disconnected

            // Attempt reconnection if not a normal closure
            if (code != 1000) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.e(t, "WebSocket failure: ${response?.message}")
            _connectionState.value = ConnectionState.Error(t.message ?: "Unknown error")

            // Attempt reconnection
            scheduleReconnect()
        }
    }

    /**
     * Connect to WebSocket server
     */
    fun connect() {
        if (_connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Connecting
        ) {
            Timber.w("Already connected or connecting")
            return
        }

        try {
            _connectionState.value = ConnectionState.Connecting

            val requestBuilder = Request.Builder()
                .url(serverUrl)

            // Add authentication header if available
            authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            webSocket = client.newWebSocket(request, listener)

            Timber.i("WebSocket connection initiated")
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect WebSocket")
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            scheduleReconnect()
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Send telemetry data to server
     */
    fun sendTelemetry(telemetry: ModelTelemetry) {
        val message = mapOf(
            "type" to "telemetry",
            "data" to telemetry
        )
        sendMessage(message)
    }

    /**
     * Subscribe to model monitoring
     */
    fun subscribeToModel(modelId: String) {
        val message = mapOf(
            "type" to "subscribe",
            "modelId" to modelId
        )
        sendMessage(message)
    }

    /**
     * Unsubscribe from model monitoring
     */
    fun unsubscribeFromModel(modelId: String) {
        val message = mapOf(
            "type" to "unsubscribe",
            "modelId" to modelId
        )
        sendMessage(message)
    }

    /**
     * Request model status
     */
    fun requestModelStatus(modelId: String) {
        val message = mapOf(
            "type" to "status_request",
            "modelId" to modelId
        )
        sendMessage(message)
    }

    /**
     * Send patch application command
     */
    fun sendPatchCommand(modelId: String, patchId: String, action: String) {
        val message = mapOf(
            "type" to "patch_command",
            "modelId" to modelId,
            "patchId" to patchId,
            "action" to action
        )
        sendMessage(message)
    }

    private fun sendMessage(message: Any) {
        try {
            val json = gson.toJson(message)
            val success = webSocket?.send(json) ?: false

            if (!success) {
                Timber.w("Failed to send message - socket may be busy")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending WebSocket message")
        }
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, Map::class.java)
            val type = message["type"] as? String

            when (type) {
                "telemetry" -> {
                    val data = message["data"]
                    val event = gson.fromJson(gson.toJson(data), TelemetryEvent::class.java)
                    _telemetryEvents.value = event
                    Timber.d("📊 Received telemetry event: ${event.modelId}")
                }

                "drift_alert" -> {
                    val data = message["data"]
                    val alert = gson.fromJson(gson.toJson(data), DriftAlert::class.java)
                    _driftAlerts.value = alert
                    Timber.w("⚠️ Drift alert: ${alert.modelId} - ${alert.severity}")
                }

                "status_update" -> {
                    Timber.d("Status update received: $message")
                }

                "auth_success" -> {
                    Timber.i("✅ Authentication successful")
                }

                "auth_failed" -> {
                    Timber.e("❌ Authentication failed")
                    _connectionState.value = ConnectionState.Error("Authentication failed")
                    disconnect()
                }

                "error" -> {
                    val errorMsg = message["message"] as? String ?: "Unknown error"
                    Timber.e("Server error: $errorMsg")
                }

                "pong" -> {
                    // Heartbeat response
                    Timber.v("Heartbeat received")
                }

                else -> {
                    Timber.w("Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing WebSocket message: $text")
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()

        reconnectJob = scope.launch {
            var retryCount = 0
            val maxRetries = 5
            val baseDelay = 2000L

            while (retryCount < maxRetries && isActive) {
                retryCount++
                val delay = baseDelay * (1 shl (retryCount - 1)) // Exponential backoff

                Timber.i("Reconnecting in ${delay}ms (attempt $retryCount/$maxRetries)")
                _connectionState.value = ConnectionState.Reconnecting(retryCount)

                delay(delay)

                if (isActive) {
                    connect()

                    // Wait a bit to see if connection succeeds
                    delay(3000)

                    if (_connectionState.value is ConnectionState.Connected) {
                        Timber.i("✅ Reconnection successful")
                        break
                    }
                }
            }

            if (retryCount >= maxRetries) {
                Timber.e("Failed to reconnect after $maxRetries attempts")
                _connectionState.value = ConnectionState.Error("Max reconnection attempts reached")
            }
        }
    }

    /**
     * Send periodic heartbeat
     */
    fun startHeartbeat() {
        scope.launch {
            while (isActive) {
                delay(30000) // Every 30 seconds

                if (_connectionState.value is ConnectionState.Connected) {
                    sendMessage(mapOf("type" to "ping"))
                }
            }
        }
    }

    /**
     * Cleanup
     */
    fun shutdown() {
        reconnectJob?.cancel()
        scope.cancel()
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}

/**
 * Connection states
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnecting : ConnectionState()
    data class Reconnecting(val attempt: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Telemetry event from deployed model
 */
data class TelemetryEvent(
    val modelId: String,
    val timestamp: Long,
    val inputFeatures: Map<String, Double>,
    val prediction: Double,
    val confidence: Double,
    val latency: Long,
    val metadata: Map<String, Any>? = null
)

/**
 * Drift alert from monitoring system
 */
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

/**
 * Model telemetry data to send
 */
data class ModelTelemetry(
    val modelId: String,
    val timestamp: Long,
    val metrics: Map<String, Double>,
    val featureStats: Map<String, FeatureStats>,
    val errorRate: Double,
    val latency: LatencyStats
)

data class FeatureStats(
    val mean: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double
)

data class LatencyStats(
    val p50: Long,
    val p95: Long,
    val p99: Long,
    val mean: Long
)
