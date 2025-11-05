package com.driftdetector.app.data.local.dao

import androidx.room.*
import com.driftdetector.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DriftResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriftResult(driftResult: DriftResultEntity)

    @Query("SELECT * FROM drift_results WHERE modelId = :modelId ORDER BY timestamp DESC")
    fun getDriftResultsByModel(modelId: String): Flow<List<DriftResultEntity>>

    @Query("SELECT * FROM drift_results WHERE id = :id")
    suspend fun getDriftResultById(id: String): DriftResultEntity?

    @Query("SELECT * FROM drift_results WHERE modelId = :modelId AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getDriftResultsSince(modelId: String, startTime: Long): Flow<List<DriftResultEntity>>

    @Query("SELECT * FROM drift_results WHERE isDriftDetected = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDrifts(limit: Int = 50): Flow<List<DriftResultEntity>>

    @Query("DELETE FROM drift_results WHERE timestamp < :cutoffTime")
    suspend fun deleteOldResults(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM drift_results WHERE modelId = :modelId AND isDriftDetected = 1")
    suspend fun getDriftCount(modelId: String): Int
}

@Dao
interface MLModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: MLModelEntity)

    @Query("SELECT * FROM models WHERE isActive = 1")
    fun getActiveModels(): Flow<List<MLModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModelById(id: String): MLModelEntity?

    @Update
    suspend fun updateModel(model: MLModelEntity)

    @Query("UPDATE models SET isActive = 0 WHERE id = :modelId")
    suspend fun deactivateModel(modelId: String)

    @Delete
    suspend fun deleteModel(model: MLModelEntity)
}

@Dao
interface PatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatch(patch: PatchEntity)

    @Query("SELECT * FROM patches WHERE modelId = :modelId ORDER BY createdAt DESC")
    fun getPatchesByModel(modelId: String): Flow<List<PatchEntity>>

    @Query("SELECT * FROM patches WHERE id = :id")
    suspend fun getPatchById(id: String): PatchEntity?

    @Query("SELECT * FROM patches WHERE status = 'APPLIED' AND modelId = :modelId")
    fun getAppliedPatches(modelId: String): Flow<List<PatchEntity>>

    @Query("UPDATE patches SET status = :status, appliedAt = :appliedAt WHERE id = :patchId")
    suspend fun updatePatchStatus(patchId: String, status: String, appliedAt: Long?)

    @Query("UPDATE patches SET status = 'ROLLED_BACK', rolledBackAt = :rolledBackAt WHERE id = :patchId")
    suspend fun rollbackPatch(patchId: String, rolledBackAt: Long)

    @Delete
    suspend fun deletePatch(patch: PatchEntity)
}

@Dao
interface PatchSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PatchSnapshotEntity)

    @Query("SELECT * FROM patch_snapshots WHERE patchId = :patchId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(patchId: String): PatchSnapshotEntity?

    @Query("DELETE FROM patch_snapshots WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSnapshots(cutoffTime: Long)
}

@Dao
interface ModelPredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: ModelPredictionEntity)

    @Query("SELECT * FROM model_predictions WHERE modelId = :modelId AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getRecentPredictions(modelId: String, startTime: Long): Flow<List<ModelPredictionEntity>>

    @Query("DELETE FROM model_predictions WHERE timestamp < :cutoffTime")
    suspend fun deleteOldPredictions(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM model_predictions WHERE modelId = :modelId")
    suspend fun getPredictionCount(modelId: String): Int
}
