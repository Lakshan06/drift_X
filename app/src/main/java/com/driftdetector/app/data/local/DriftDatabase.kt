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
        ModelPredictionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DriftDatabase : RoomDatabase() {
    abstract fun driftResultDao(): DriftResultDao
    abstract fun mlModelDao(): MLModelDao
    abstract fun patchDao(): PatchDao
    abstract fun patchSnapshotDao(): PatchSnapshotDao
    abstract fun modelPredictionDao(): ModelPredictionDao

    companion object {
        const val DATABASE_NAME = "drift_detector.db"
    }
}
