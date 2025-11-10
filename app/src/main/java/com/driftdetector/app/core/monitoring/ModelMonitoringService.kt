package com.driftdetector.app.core.monitoring

import android.content.Context
import com.driftdetector.app.core.notifications.DriftNotificationManager
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.random.Random

/**
 * Service that continuously monitors models for drift
 * Automatically detects drift and synthesizes patches
 */
class ModelMonitoringService(
    private val context: Context,
    private val repository: DriftRepository,
    private val notificationManager: DriftNotificationManager
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: Flow<Boolean> = _isMonitoring.asStateFlow()

    private val _monitoringStats = MutableStateFlow(MonitoringStats())
    val monitoringStats: Flow<MonitoringStats> = _monitoringStats.asStateFlow()

    private var monitoringJob: Job? = null

    /**
     * Start monitoring all active models
     */
    fun startMonitoring() {
        if (_isMonitoring.value) {
            Timber.w("Monitoring already running")
            return
        }

        Timber.i(" Starting model monitoring service")
        _isMonitoring.value = true

        monitoringJob = scope.launch {
            try {
                while (isActive) {
                    monitorActiveModels()
                    delay(30000) // Check every 30 seconds
                }
            } catch (e: Exception) {
                Timber.e(e, "Monitoring service error")
                _isMonitoring.value = false
            }
        }
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        Timber.i(" Stopping model monitoring service")
        monitoringJob?.cancel()
        _isMonitoring.value = false
    }

    /**
     * Monitor all active models
     */
    private suspend fun monitorActiveModels() {
        try {
            val models = repository.getActiveModels().first()

            if (models.isEmpty()) {
                Timber.d("No active models to monitor")
                return
            }

            Timber.d(" Monitoring ${models.size} active models")

            val stats = _monitoringStats.value
            _monitoringStats.value = stats.copy(
                modelsMonitored = models.size,
                lastCheckTime = System.currentTimeMillis()
            )

            // Monitor each model
            models.forEach { model ->
                monitorModel(model)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error monitoring models")
        }
    }

    /**
     * Monitor a single model
     */
    private suspend fun monitorModel(model: MLModel) {
        try {
            Timber.d(" Monitoring model: ${model.name}")

            // Generate reference and current data
            // In production, this would come from actual inference logs
            val referenceData = generateReferenceData(model)
            val currentData = generateCurrentData(model)

            // Detect drift
            val driftResult = repository.detectDrift(
                modelId = model.id,
                referenceData = referenceData,
                currentData = currentData,
                featureNames = model.inputFeatures
            )

            val stats = _monitoringStats.value
            _monitoringStats.value = stats.copy(
                totalChecks = stats.totalChecks + 1,
                lastCheckTime = System.currentTimeMillis()
            )

            if (driftResult.isDriftDetected) {
                Timber.w("⚠️ Drift detected in model: ${model.name}")
                Timber.w("   Drift score: ${driftResult.driftScore}")
                Timber.w("   Drift type: ${driftResult.driftType}")

                // Update stats
                _monitoringStats.value = stats.copy(
                    driftsDetected = stats.driftsDetected + 1
                )

                // Auto-synthesize patch if drift is significant
                if (driftResult.driftScore > 0.3) {
                    synthesizePatchForDrift(model, driftResult, referenceData, currentData)
                }
            } else {
                Timber.d("✅ No drift detected in model: ${model.name}")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error monitoring model: ${model.name}")
        }
    }

    /**
     * Automatically synthesize a patch for detected drift
     */
    private suspend fun synthesizePatchForDrift(
        model: MLModel,
        driftResult: DriftResult,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>
    ) {
        try {
            Timber.d("🔧 Auto-synthesizing patch for drift")

            val patch = repository.synthesizePatch(
                modelId = model.id,
                driftResult = driftResult,
                referenceData = referenceData,
                currentData = currentData
            )

            Timber.i("✅ Patch synthesized: ${patch.patchType}")
            Timber.i("   Safety score: ${patch.validationResult?.metrics?.safetyScore ?: 0.0}")

            val stats = _monitoringStats.value
            _monitoringStats.value = stats.copy(
                patchesSynthesized = stats.patchesSynthesized + 1
            )

            // Send notification about patch synthesis
            val safetyScore = patch.validationResult?.metrics?.safetyScore ?: 0.0
            notificationManager.showPatchSynthesized(
                modelName = model.name,
                patchType = patch.patchType.name.replace("_", " "),
                safetyScore = safetyScore
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to synthesize patch")
        }
    }

    /**
     * Generate reference data for a model
     * In production, this would be historical baseline data
     */
    private fun generateReferenceData(model: MLModel): List<FloatArray> {
        val numSamples = 100
        val numFeatures = model.inputFeatures.size

        return List(numSamples) {
            FloatArray(numFeatures) { featureIdx ->
                // Generate stable baseline data
                (featureIdx * 2.0f + Random.nextFloat() * 0.5f)
            }
        }
    }

    /**
     * Generate current data for a model (simulating drift)
     * In production, this would be recent inference data
     */
    private fun generateCurrentData(model: MLModel): List<FloatArray> {
        val numSamples = 100
        val numFeatures = model.inputFeatures.size

        // Simulate some drift by shifting the data slightly
        val driftFactor = if (Random.nextFloat() > 0.7) 0.8f else 0.2f

        return List(numSamples) {
            FloatArray(numFeatures) { featureIdx ->
                // Add drift to the data
                (featureIdx * 2.0f + Random.nextFloat() * 0.5f + driftFactor)
            }
        }
    }

    /**
     * Manually trigger drift check for a specific model
     */
    suspend fun checkModelNow(modelId: String): DriftResult? {
        return try {
            val model = repository.getModelById(modelId) ?: return null

            val referenceData = generateReferenceData(model)
            val currentData = generateCurrentData(model)

            repository.detectDrift(
                modelId = model.id,
                referenceData = referenceData,
                currentData = currentData,
                featureNames = model.inputFeatures
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to check model")
            null
        }
    }

    /**
     * Cleanup
     */
    fun shutdown() {
        stopMonitoring()
        scope.cancel()
    }
}

/**
 * Monitoring statistics
 */
data class MonitoringStats(
    val modelsMonitored: Int = 0,
    val totalChecks: Int = 0,
    val driftsDetected: Int = 0,
    val patchesSynthesized: Int = 0,
    val lastCheckTime: Long = 0L
)
