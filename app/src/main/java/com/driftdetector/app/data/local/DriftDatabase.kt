package com.driftdetector.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.driftdetector.app.data.local.dao.*
import com.driftdetector.app.data.local.entity.*

@Database(
    entities = [
        DriftResultEntity::class,
        MLModelEntity::class,
        PatchEntity::class,
        PatchSnapshotEntity::class,
        ModelPredictionEntity::class,
        RecentFileEntity::class,
        UserTaskEntity::class,
        UserSessionEntity::class,
        AppStateEntity::class,
        DeactivatedModelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DriftDatabase : RoomDatabase() {
    abstract fun driftResultDao(): DriftResultDao
    abstract fun mlModelDao(): MLModelDao
    abstract fun patchDao(): PatchDao
    abstract fun patchSnapshotDao(): PatchSnapshotDao
    abstract fun modelPredictionDao(): ModelPredictionDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun userTaskDao(): UserTaskDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun appStateDao(): AppStateDao
    abstract fun deactivatedModelDao(): DeactivatedModelDao

    companion object {
        const val DATABASE_NAME = "drift_detector.db"
    }
}
