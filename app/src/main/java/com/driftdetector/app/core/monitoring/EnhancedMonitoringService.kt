package com.driftdetector.app.core.monitoring

import android.content.Context
import com.driftdetector.app.core.auth.AuthManager
import com.driftdetector.app.core.connectivity.NetworkConnectivityManager
import com.driftdetector.app.core.connectivity.NetworkState
import com.driftdetector.app.core.notifications.DriftNotificationManager
import com.driftdetector.app.core.realtime.ConnectionState
import com.driftdetector.app.core.realtime.RealtimeClient
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.MLModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Enhanced monitoring service that integrates:
 * - Real-time WebSocket connectivity
 * - Network state management
 * - Push notifications
 * - Authentication
 * - Telemetry streaming
 */
class EnhancedMonitoringService(
    private val context: Context,
    private val repository: DriftRepository,
    private val authManager: AuthManager,
    private val realtimeClient: RealtimeClient,
    private val connectivityManager: NetworkConnectivityManager,
    private val notificationManager: DriftNotificationManager
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Stopped)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _monitoredModels = MutableStateFlow<Set<String>>(emptySet())
    val monitoredModels: StateFlow<Set<String>> = _monitoredModels.asStateFlow()

    private var monitoringJob: Job? = null
    private var connectionMonitorJob: Job? = null
    private var alertMonitorJob: Job? = null

    /**
     * Start enhanced monitoring service
     */
    suspend fun start() {
        if (_serviceState.value is ServiceState.Running) {
            Timber.w("Service already running")
            return
        }

        Timber.i("🚀 Starting Enhanced Monitoring Service")
        _serviceState.value = ServiceState.Starting

        try {
            // Start network monitoring
            connectivityManager.startMonitoring()

            // Authenticate if needed
            if (!authManager.isAuthenticated()) {
                Timber.i("Authenticating...")
                // In production, get credentials from secure storage
                // For now, auto-login with demo account
                val result = authManager.login("demo@driftguard.ai", "demo123")
                if (result.isFailure) {
                    throw Exception("Authentication failed")
                }
            }

            // Connect to real-time server if online
            if (connectivityManager.isCurrentlyOnline()) {
                connectToRealtimeServer()
            }

            // Start monitoring jobs
            startMonitoringJobs()

            _serviceState.value = ServiceState.Running
            Timber.i("✅ Enhanced Monitoring Service started")

        } catch (e: Exception) {
            Timber.e(e, "Failed to start monitoring service")
            _serviceState.value = ServiceState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Stop enhanced monitoring service
     */
    fun stop() {
        Timber.i("⏹️ Stopping Enhanced Monitoring Service")

        monitoringJob?.cancel()
        connectionMonitorJob?.cancel()
        alertMonitorJob?.cancel()

        realtimeClient.disconnect()
        connectivityManager.stopMonitoring()
        notificationManager.clearMonitoringStatus()

        _serviceState.value = ServiceState.Stopped
        Timber.i("✅ Enhanced Monitoring Service stopped")
    }

    /**
     * Subscribe to a model for monitoring
     */
    suspend fun subscribeToModel(modelId: String) {
        try {
            val model = repository.getModelById(modelId)
            if (model != null) {
                realtimeClient.subscribeToModel(modelId)
                _monitoredModels.value = _monitoredModels.value + modelId
                Timber.i("📡 Subscribed to model: ${model.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to model: $modelId")
        }
    }

    /**
     * Unsubscribe from a model
     */
    fun unsubscribeFromModel(modelId: String) {
        realtimeClient.unsubscribeFromModel(modelId)
        _monitoredModels.value = _monitoredModels.value - modelId
        Timber.i("📡 Unsubscribed from model: $modelId")
    }

    /**
     * Get monitoring statistics
     */
    fun getStatistics(): Flow<MonitoringStatistics> {
        return combine(
            serviceState,
            realtimeClient.connectionState,
            connectivityManager.networkState,
            monitoredModels
        ) { service, connection, network, models ->
            MonitoringStatistics(
                serviceState = service,
                connectionState = connection,
                networkState = network,
                activeSubscriptions = models.size,
                isOnline = connectivityManager.isCurrentlyOnline()
            )
        }
    }

    private suspend fun connectToRealtimeServer() {
        try {
            val token = authManager.getAccessToken()
            // Update realtime client with auth token
            // Note: In a full implementation, RealtimeClient would have a method to update token
            realtimeClient.connect()
            realtimeClient.startHeartbeat()

            Timber.i("✅ Connected to real-time server")
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to real-time server")
        }
    }

    private fun startMonitoringJobs() {
        // Monitor connection state
        connectionMonitorJob = scope.launch {
            realtimeClient.connectionState.collect { state ->
                handleConnectionStateChange(state)
            }
        }

        // Monitor drift alerts
        alertMonitorJob = scope.launch {
            realtimeClient.driftAlerts.collect { alert ->
                alert?.let { handleDriftAlert(it) }
            }
        }

        // Monitor network state
        scope.launch {
            connectivityManager.networkState.collect { state ->
                handleNetworkStateChange(state)
            }
        }

        // Monitor telemetry events
        scope.launch {
            realtimeClient.telemetryEvents.collect { event ->
                event?.let { handleTelemetryEvent(it) }
            }
        }

        // Auto-subscribe to active models
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val activeModels = repository.getActiveModels().first()
                    activeModels.forEach { model ->
                        if (!_monitoredModels.value.contains(model.id)) {
                            subscribeToModel(model.id)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error auto-subscribing to models")
                }

                delay(60000) // Check every minute
            }
        }
    }

    private fun handleConnectionStateChange(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                Timber.i("🟢 Real-time connection established")
                notificationManager.showConnectionStatus(true, "DriftGuard Server")
            }

            is ConnectionState.Disconnected -> {
                Timber.w("⚫ Real-time connection lost")
                notificationManager.showConnectionStatus(false, "DriftGuard Server")
            }

            is ConnectionState.Reconnecting -> {
                Timber.i("🟡 Reconnecting (attempt ${state.attempt})...")
            }

            is ConnectionState.Error -> {
                Timber.e("❌ Connection error: ${state.message}")
            }

            else -> {}
        }
    }

    private suspend fun handleDriftAlert(alert: com.driftdetector.app.core.realtime.DriftAlert) {
        try {
            Timber.w("⚠️ Drift alert received: ${alert.modelId} - ${alert.severity}")

            val model = repository.getModelById(alert.modelId)
            val modelName = model?.name ?: alert.modelId

            // Show notification
            notificationManager.showDriftAlert(
                modelId = alert.modelId,
                modelName = modelName,
                driftScore = alert.driftScore,
                severity = alert.severity
            )

            // Store drift result in database
            // This would typically create a DriftResult entity and save it

        } catch (e: Exception) {
            Timber.e(e, "Error handling drift alert")
        }
    }

    private fun handleNetworkStateChange(state: NetworkState) {
        when (state) {
            is NetworkState.Available -> {
                Timber.i("🟢 Network available")

                // Reconnect to real-time server if not connected
                if (realtimeClient.connectionState.value !is ConnectionState.Connected) {
                    scope.launch {
                        delay(2000) // Wait a bit for network to stabilize
                        connectToRealtimeServer()
                    }
                }
            }

            is NetworkState.Lost -> {
                Timber.w("⚫ Network lost")
                // WebSocket will auto-reconnect when network returns
            }

            is NetworkState.Unavailable -> {
                Timber.w("⚫ Network unavailable")
            }

            else -> {}
        }
    }

    private fun handleTelemetryEvent(event: com.driftdetector.app.core.realtime.TelemetryEvent) {
        // Process telemetry event
        Timber.d("📊 Telemetry: ${event.modelId} - Prediction: ${event.prediction}")

        // Store telemetry for analysis
        // Update statistics
        // Check for anomalies
    }

    /**
     * Manually trigger drift check for a model
     */
    suspend fun checkModelNow(modelId: String) {
        try {
            realtimeClient.requestModelStatus(modelId)
            Timber.i("🔍 Requested status for model: $modelId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to request model status")
        }
    }

    /**
     * Send patch deployment command
     */
    suspend fun deployPatch(modelId: String, patchId: String) {
        try {
            realtimeClient.sendPatchCommand(modelId, patchId, "deploy")
            Timber.i("🚀 Sent patch deployment command: $patchId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send patch command")
        }
    }

    /**
     * Cleanup resources
     */
    fun shutdown() {
        stop()
        scope.cancel()
    }
}

/**
 * Service state
 */
sealed class ServiceState {
    object Stopped : ServiceState()
    object Starting : ServiceState()
    object Running : ServiceState()
    data class Error(val message: String) : ServiceState()
}

/**
 * Monitoring statistics
 */
data class MonitoringStatistics(
    val serviceState: ServiceState,
    val connectionState: ConnectionState,
    val networkState: NetworkState,
    val activeSubscriptions: Int,
    val isOnline: Boolean
)
