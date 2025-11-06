package com.driftdetector.app.data.local.dao

import androidx.room.*
import com.driftdetector.app.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFileEntity)

    @Query("SELECT * FROM recent_files ORDER BY lastAccessedTimestamp DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 10): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE fileType = :fileType ORDER BY lastAccessedTimestamp DESC LIMIT :limit")
    fun getRecentFilesByType(fileType: String, limit: Int = 10): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE isPinned = 1 ORDER BY lastAccessedTimestamp DESC")
    fun getPinnedFiles(): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE id = :id")
    suspend fun getRecentFileById(id: String): RecentFileEntity?

    @Query("UPDATE recent_files SET lastAccessedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)

    @Query("UPDATE recent_files SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteRecentFile(id: String)

    @Query("DELETE FROM recent_files WHERE uploadTimestamp < :cutoffTime AND isPinned = 0")
    suspend fun deleteOldFiles(cutoffTime: Long)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAllRecentFiles()
}

@Dao
interface UserTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: com.driftdetector.app.data.local.entity.UserTaskEntity)

    @Query("SELECT * FROM user_tasks ORDER BY lastUpdatedTimestamp DESC")
    fun getAllTasks(): Flow<List<com.driftdetector.app.data.local.entity.UserTaskEntity>>

    @Query("SELECT * FROM user_tasks WHERE status IN ('IN_PROGRESS', 'PAUSED') ORDER BY lastUpdatedTimestamp DESC")
    fun getActiveTasks(): Flow<List<com.driftdetector.app.data.local.entity.UserTaskEntity>>

    @Query("SELECT * FROM user_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): com.driftdetector.app.data.local.entity.UserTaskEntity?

    @Query("UPDATE user_tasks SET status = :status, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE user_tasks SET progress = :progress, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateTaskProgress(id: String, progress: Float, timestamp: Long)

    @Query("UPDATE user_tasks SET status = :status, completedTimestamp = :timestamp, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun completeTask(id: String, status: String, timestamp: Long)

    @Query("UPDATE user_tasks SET status = 'FAILED', errorMessage = :errorMessage, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun failTask(id: String, errorMessage: String, timestamp: Long)

    @Query("DELETE FROM user_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM user_tasks WHERE status = 'COMPLETED' AND completedTimestamp < :cutoffTime")
    suspend fun deleteCompletedTasks(cutoffTime: Long)
}

@Dao
interface UserSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: com.driftdetector.app.data.local.entity.UserSessionEntity)

    @Query("SELECT * FROM user_sessions ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getLatestSession(): com.driftdetector.app.data.local.entity.UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): com.driftdetector.app.data.local.entity.UserSessionEntity?

    @Query("UPDATE user_sessions SET endTimestamp = :timestamp WHERE id = :id")
    suspend fun endSession(id: String, timestamp: Long)

    @Query("UPDATE user_sessions SET lastActiveModelId = :modelId WHERE id = :id")
    suspend fun updateActiveModel(id: String, modelId: String)

    @Query("UPDATE user_sessions SET lastActiveDataFileId = :dataFileId WHERE id = :id")
    suspend fun updateActiveDataFile(id: String, dataFileId: String)

    @Query("UPDATE user_sessions SET dashboardState = :state WHERE id = :id")
    suspend fun updateDashboardState(id: String, state: String)

    @Query("DELETE FROM user_sessions WHERE startTimestamp < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long)
}

@Dao
interface AppStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(state: com.driftdetector.app.data.local.entity.AppStateEntity)

    @Query("SELECT * FROM app_state WHERE `key` = :key")
    suspend fun getState(key: String): com.driftdetector.app.data.local.entity.AppStateEntity?

    @Query("SELECT * FROM app_state")
    fun getAllStates(): Flow<List<com.driftdetector.app.data.local.entity.AppStateEntity>>

    @Query("DELETE FROM app_state WHERE `key` = :key")
    suspend fun deleteState(key: String)

    @Query("DELETE FROM app_state")
    suspend fun deleteAllStates()
}
