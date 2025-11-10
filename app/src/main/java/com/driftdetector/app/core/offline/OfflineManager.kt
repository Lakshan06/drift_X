package com.driftdetector.app.core.offline

import android.content.Context
import android.net.Uri
import com.driftdetector.app.core.upload.FileUploadProcessor
import com.driftdetector.app.data.local.dao.PendingOperationsDao
import com.driftdetector.app.data.repository.DriftRepository
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages offline operations and syncs them when network is available
 */
class OfflineManager(
    private val context: Context,
    private val pendingOperationsDao: PendingOperationsDao,
    private val networkMonitor: NetworkMonitor,
    private val repository: DriftRepository,
    private val fileUploadProcessor: FileUploadProcessor,
    private val conflictResolver: ConflictResolver,
    private val gson: Gson
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        observeNetworkChanges()
        observePendingOperations()
        Timber.d("🔄 OfflineManager initialized")
    }

    /**
     * Observe network connectivity and auto-sync when online
     */
    private fun observeNetworkChanges() {
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    Timber.i("🌐 Network available - starting auto-sync")
                    syncPendingOperations()
                } else {
                    Timber.d("📴 Network unavailable - queueing operations")
                }
            }
        }
    }

    /**
     * Observe pending operations count
     */
    private fun observePendingOperations() {
        scope.launch {
            pendingOperationsDao.getPendingOperationCount().collect { count ->
                _pendingCount.value = count
                Timber.d("📊 Pending operations: $count")
            }
        }
    }

    /**
     * Queue an operation for offline execution
     */
    suspend fun queueOperation(operation: PendingOperation) {
        try {
            pendingOperationsDao.insert(operation)
            Timber.i("➕ Queued operation: ${operation.type} (id: ${operation.id})")

            // Try to sync immediately if online
            if (networkMonitor.isCurrentlyOnline()) {
                syncPendingOperations()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to queue operation")
            throw e
        }
    }

    /**
     * Sync all pending operations
     */
    suspend fun syncPendingOperations() {
        if (!isSyncing.compareAndSet(false, true)) {
            Timber.d("⏳ Sync already in progress, skipping")
            return
        }

        try {
            _syncState.value = SyncState.Syncing(0, 0)

            val pending = pendingOperationsDao.getPendingOperations()
            if (pending.isEmpty()) {
                Timber.d("✅ No pending operations to sync")
                _syncState.value = SyncState.Idle
                return
            }

            Timber.i("🔄 Syncing ${pending.size} pending operations...")
            var completed = 0
            var failed = 0

            pending.forEachIndexed { index, operation ->
                _syncState.value = SyncState.Syncing(index + 1, pending.size)

                try {
                    executeOperation(operation)
                    pendingOperationsDao.updateStatus(operation.id, OperationStatus.COMPLETED)
                    completed++
                    Timber.d("✅ Completed operation: ${operation.type}")
                } catch (e: Exception) {
                    handleOperationFailure(operation, e)
                    failed++
                }

                delay(100) // Small delay between operations
            }

            // Cleanup completed operations
            pendingOperationsDao.deleteCompleted()

            val result = SyncResult(
                totalOperations = pending.size,
                completedOperations = completed,
                failedOperations = failed
            )

            _syncState.value = SyncState.Success(result)
            Timber.i("✅ Sync completed: $result")

            delay(3000) // Show success for 3 seconds
            _syncState.value = SyncState.Idle

        } catch (e: Exception) {
            Timber.e(e, "❌ Sync failed")
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            delay(3000)
            _syncState.value = SyncState.Idle
        } finally {
            isSyncing.set(false)
        }
    }

    /**
     * Execute a single operation
     */
    private suspend fun executeOperation(operation: PendingOperation) {
        Timber.d("⚡ Executing operation: ${operation.type}")

        // Mark as in progress
        pendingOperationsDao.updateStatus(operation.id, OperationStatus.IN_PROGRESS)

        when (operation.type) {
            OperationType.UPLOAD_MODEL -> executeModelUpload(operation)
            OperationType.UPLOAD_DATA -> executeDataUpload(operation)
            OperationType.APPLY_PATCH -> executeApplyPatch(operation)
            OperationType.VALIDATE_PATCH -> executeValidatePatch(operation)
            OperationType.SYNC_DRIFT_RESULTS -> executeSyncDrift(operation)
            OperationType.DELETE_MODEL -> executeDeleteModel(operation)
            OperationType.UPDATE_MODEL_STATUS -> executeUpdateModelStatus(operation)
            OperationType.ROLLBACK_PATCH -> executeRollbackPatch(operation)
            OperationType.EXPORT_DATA -> executeExportData(operation)
            OperationType.BACKUP_DATA -> executeBackupData(operation)
        }
    }

    /**
     * Handle operation failure with retry logic
     */
    private suspend fun handleOperationFailure(operation: PendingOperation, error: Exception) {
        val newRetryCount = operation.retryCount + 1

        if (newRetryCount >= operation.maxRetries) {
            // Max retries reached - mark as failed
            pendingOperationsDao.updateStatusWithError(
                operation.id,
                OperationStatus.FAILED,
                error.message ?: "Unknown error"
            )
            Timber.e(
                error,
                "❌ Operation failed after ${operation.maxRetries} retries: ${operation.type}"
            )
        } else {
            // Update retry count and try again later
            pendingOperationsDao.updateRetryCount(operation.id, newRetryCount)
            Timber.w("⚠️ Operation failed, retry $newRetryCount/${operation.maxRetries}: ${operation.type}")
        }
    }

    // ==================== Operation Executors ====================

    private suspend fun executeModelUpload(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, ModelUploadPayload::class.java)
        val result = fileUploadProcessor.processModelFile(
            uri = Uri.parse(payload.modelUri),
            fileName = payload.fileName
        )
        result.getOrThrow()
    }

    private suspend fun executeDataUpload(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, DataUploadPayload::class.java)
        val result = fileUploadProcessor.processModelAndData(
            modelUri = Uri.parse(payload.modelUri),
            modelFileName = payload.modelFileName,
            dataUri = Uri.parse(payload.dataUri),
            dataFileName = payload.dataFileName
        )
        result.getOrThrow()
    }

    private suspend fun executeApplyPatch(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, PatchPayload::class.java)
        repository.applyPatch(payload.patchId).getOrThrow()
    }

    private suspend fun executeValidatePatch(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, ValidatePatchPayload::class.java)
        // Get patch from repository
        val patch = repository.getModelById(payload.modelId)?.let { model ->
            // This is a stub - in real implementation, fetch patch by ID
            null // TODO: Implement patch retrieval
        }
        if (patch != null) {
            repository.validatePatch(
                patch = patch,
                validationData = payload.validationData,
                validationLabels = payload.validationLabels
            )
        }
    }

    private suspend fun executeSyncDrift(operation: PendingOperation) {
        // Sync drift results to backend (if backend exists)
        Timber.d("Syncing drift results...")
    }

    private suspend fun executeDeleteModel(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, ModelPayload::class.java)
        repository.deactivateModel(payload.modelId)
    }

    private suspend fun executeUpdateModelStatus(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, ModelStatusPayload::class.java)
        if (!payload.isActive) {
            repository.deactivateModel(payload.modelId)
        }
        // Note: No activate method in repository, only deactivate
    }

    private suspend fun executeRollbackPatch(operation: PendingOperation) {
        val payload = gson.fromJson(operation.payload, PatchPayload::class.java)
        repository.rollbackPatch(payload.patchId).getOrThrow()
    }

    private suspend fun executeExportData(operation: PendingOperation) {
        Timber.d("Exporting data...")
    }

    private suspend fun executeBackupData(operation: PendingOperation) {
        Timber.d("Backing up data...")
    }

    /**
     * Cancel a pending operation
     */
    suspend fun cancelOperation(operationId: String) {
        pendingOperationsDao.updateStatus(operationId, OperationStatus.CANCELLED)
        Timber.i("❌ Cancelled operation: $operationId")
    }

    /**
     * Retry all failed operations
     */
    suspend fun retryFailedOperations() {
        val failed = pendingOperationsDao.getPendingOperations()
            .filter { it.status == OperationStatus.FAILED }

        failed.forEach { operation ->
            val resetOperation = operation.copy(
                status = OperationStatus.PENDING,
                retryCount = 0,
                errorMessage = null
            )
            pendingOperationsDao.update(resetOperation)
        }

        Timber.i("🔄 Retrying ${failed.size} failed operations")
        syncPendingOperations()
    }

    /**
     * Clear all pending operations (use with caution!)
     */
    suspend fun clearAllPendingOperations() {
        pendingOperationsDao.deleteAll()
        Timber.w("🗑️ Cleared all pending operations")
    }

    /**
     * Cleanup old failed operations
     */
    suspend fun cleanupOldOperations(daysOld: Int = 7) {
        val timestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000)
        pendingOperationsDao.deleteOldFailed(timestamp)
        Timber.i("🧹 Cleaned up old failed operations")
    }

    fun shutdown() {
        scope.cancel()
        Timber.d("🛑 OfflineManager shutdown")
    }
}

// ==================== Payload Data Classes ====================

data class ModelUploadPayload(
    val modelUri: String,
    val fileName: String
)

data class DataUploadPayload(
    val modelUri: String,
    val modelFileName: String,
    val dataUri: String,
    val dataFileName: String
)

data class PatchPayload(
    val patchId: String,
    val referenceData: List<FloatArray>,
    val currentData: List<FloatArray>
)

data class ModelPayload(
    val modelId: String
)

data class ModelStatusPayload(
    val modelId: String,
    val isActive: Boolean
)

data class ValidatePatchPayload(
    val modelId: String,
    val validationData: List<FloatArray>,
    val validationLabels: List<Int>
)

// ==================== Sync State ====================

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val current: Int, val total: Int) : SyncState() {
        val progress: Float get() = if (total > 0) current.toFloat() / total else 0f
    }

    data class Success(val result: SyncResult) : SyncState()
    data class Error(val message: String) : SyncState()
}

data class SyncResult(
    val totalOperations: Int,
    val completedOperations: Int,
    val failedOperations: Int
) {
    val successRate: Float get() = if (totalOperations > 0) completedOperations.toFloat() / totalOperations else 0f

    override fun toString(): String {
        return "SyncResult(total=$totalOperations, completed=$completedOperations, failed=$failedOperations, successRate=${
            String.format(
                "%.1f%%",
                successRate * 100
            )
        })"
    }
}
