package com.driftdetector.app.core.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents an operation that was performed offline and needs to be synced
 */
@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: OperationType,
    val payload: String, // JSON serialized data
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val status: OperationStatus = OperationStatus.PENDING,
    val errorMessage: String? = null,
    val priority: Int = 0 // Higher priority = executed first
)

/**
 * Types of operations that can be queued
 */
enum class OperationType {
    UPLOAD_MODEL,
    UPLOAD_DATA,
    APPLY_PATCH,
    VALIDATE_PATCH,
    SYNC_DRIFT_RESULTS,
    DELETE_MODEL,
    UPDATE_MODEL_STATUS,
    ROLLBACK_PATCH,
    EXPORT_DATA,
    BACKUP_DATA
}

/**
 * Status of pending operations
 */
enum class OperationStatus {
    PENDING,      // Waiting to be executed
    IN_PROGRESS,  // Currently executing
    COMPLETED,    // Successfully completed
    FAILED,       // Failed after max retries
    CANCELLED     // Cancelled by user
}
