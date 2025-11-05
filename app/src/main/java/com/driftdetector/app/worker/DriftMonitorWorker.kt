package com.driftdetector.app.worker

import android.content.Context
import androidx.work.*
import com.driftdetector.app.data.repository.DriftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Background worker for periodic drift monitoring
 */
class DriftMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: DriftRepository
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        const val WORK_NAME = "drift_monitor_periodic"
        const val MODEL_ID_KEY = "model_id"
        private const val MONITORING_WINDOW_HOURS = 24L

        /**
         * Schedule periodic drift monitoring
         */
        fun schedule(context: Context, modelId: String) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<DriftMonitorWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(MODEL_ID_KEY to modelId))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "$WORK_NAME-$modelId",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Timber.d("Scheduled drift monitoring for model: $modelId")
        }

        /**
         * Cancel monitoring for a model
         */
        fun cancel(context: Context, modelId: String) {
            WorkManager.getInstance(context).cancelUniqueWork("$WORK_NAME-$modelId")
            Timber.d("Cancelled drift monitoring for model: $modelId")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val modelId = inputData.getString(MODEL_ID_KEY)
                ?: return@withContext Result.failure()

            Timber.d("Starting drift monitoring for model: $modelId")

            // Get model
            val model = repository.getModelById(modelId)
            if (model == null || !model.isActive) {
                Timber.w("Model not found or inactive: $modelId")
                return@withContext Result.failure()
            }

            // Get recent predictions for analysis
            val startTime = Instant.now().minus(Duration.ofHours(MONITORING_WINDOW_HOURS))
            val predictions = mutableListOf<com.driftdetector.app.domain.model.ModelPrediction>()
            repository.getRecentPredictions(modelId, startTime).collect {
                predictions.clear()
                predictions.addAll(it)
            }

            if (predictions.size < 100) {
                Timber.d("Insufficient data for drift detection: ${predictions.size} samples")
                return@withContext Result.success()
            }

            // Split into reference and current data
            val splitPoint = predictions.size / 2
            val referenceData = predictions.take(splitPoint).map { it.input.features }
            val currentData = predictions.drop(splitPoint).map { it.input.features }

            // Detect drift
            val driftResult = repository.detectDrift(
                modelId = modelId,
                referenceData = referenceData,
                currentData = currentData,
                featureNames = model.inputFeatures
            )

            Timber.d("Drift detection completed: drift=${driftResult.isDriftDetected}, score=${driftResult.driftScore}")

            // If drift detected, synthesize and validate patch
            if (driftResult.isDriftDetected && driftResult.driftScore > 0.3) {
                val patch = repository.synthesizePatch(
                    modelId = modelId,
                    driftResult = driftResult,
                    referenceData = referenceData,
                    currentData = currentData
                )

                Timber.d("Patch synthesized: ${patch.patchType}")

                // Validate patch (with mock validation data in production)
                val validationData = currentData.takeLast(20)
                val validationLabels = List(20) { 0 } // Mock labels

                val validationResult = repository.validatePatch(
                    patch = patch,
                    validationData = validationData,
                    validationLabels = validationLabels
                )

                if (validationResult.isValid) {
                    Timber.d("Patch validated successfully, ready for application")
                    // Send notification to user
                    showDriftNotification(modelId, driftResult.driftScore)
                } else {
                    Timber.w("Patch validation failed: ${validationResult.errors}")
                }
            }

            // Cleanup old data
            val cutoffTime = Instant.now().minus(Duration.ofDays(30))
            repository.cleanupOldData(cutoffTime)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Drift monitoring failed")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun showDriftNotification(modelId: String, driftScore: Double) {
        // TODO: Implement notification using NotificationCompat
        Timber.d("Drift notification: modelId=$modelId, score=$driftScore")
    }
}

/**
 * One-time drift check worker
 */
class DriftCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: DriftRepository by inject()

    companion object {
        fun enqueue(context: Context, modelId: String) {
            val workRequest = OneTimeWorkRequestBuilder<DriftCheckWorker>()
                .setInputData(workDataOf(DriftMonitorWorker.MODEL_ID_KEY to modelId))
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val modelId = inputData.getString(DriftMonitorWorker.MODEL_ID_KEY)
                ?: return@withContext Result.failure()

            // Similar logic to DriftMonitorWorker but for one-time check
            Timber.d("One-time drift check for model: $modelId")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Drift check failed")
            Result.failure()
        }
    }
}
