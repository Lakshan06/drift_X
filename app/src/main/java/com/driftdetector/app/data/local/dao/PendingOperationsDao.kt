package com.driftdetector.app.data.local.dao

import androidx.room.*
import com.driftdetector.app.core.offline.OperationStatus
import com.driftdetector.app.core.offline.OperationType
import com.driftdetector.app.core.offline.PendingOperation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for pending operations
 */
@Dao
interface PendingOperationsDao {

    @Query("SELECT * FROM pending_operations WHERE status = 'PENDING' ORDER BY priority DESC, timestamp ASC")
    suspend fun getPendingOperations(): List<PendingOperation>

    @Query("SELECT * FROM pending_operations WHERE status = 'PENDING' ORDER BY priority DESC, timestamp ASC")
    fun observePendingOperations(): Flow<List<PendingOperation>>

    @Query("SELECT * FROM pending_operations WHERE type = :type AND status = 'PENDING'")
    suspend fun getPendingOperationsByType(type: OperationType): List<PendingOperation>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE status = 'PENDING'")
    fun getPendingOperationCount(): Flow<Int>

    @Query("SELECT * FROM pending_operations WHERE id = :id")
    suspend fun getOperationById(id: String): PendingOperation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<PendingOperation>)

    @Update
    suspend fun update(operation: PendingOperation)

    @Query("UPDATE pending_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: OperationStatus)

    @Query("UPDATE pending_operations SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatusWithError(id: String, status: OperationStatus, errorMessage: String)

    @Query("UPDATE pending_operations SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: String, retryCount: Int)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_operations WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM pending_operations WHERE status = 'FAILED' AND timestamp < :timestamp")
    suspend fun deleteOldFailed(timestamp: Long)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()
}
