package com.driftdetector.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val fileType: String, // "MODEL" or "DATA"
    val filePath: String,
    val fileSize: Long,
    val uploadTimestamp: Long,
    val lastAccessedTimestamp: Long,
    val modelId: String?, // If this is a data file linked to a model
    val isPinned: Boolean = false,
    val metadata: String // JSON string of additional metadata
)

@Entity(tableName = "user_tasks")
data class UserTaskEntity(
    @PrimaryKey
    val id: String,
    val taskType: String, // "UPLOAD", "DRIFT_DETECTION", "PATCH_GENERATION", "PATCH_APPLICATION"
    val status: String, // "IN_PROGRESS", "COMPLETED", "FAILED", "PAUSED"
    val progress: Float, // 0.0 to 1.0
    val startTimestamp: Long,
    val lastUpdatedTimestamp: Long,
    val completedTimestamp: Long?,
    val metadata: String, // JSON string with task-specific data
    val errorMessage: String?
)

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val lastActiveModelId: String?,
    val lastActiveDataFileId: String?,
    val dashboardState: String // JSON string with dashboard preferences
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey
    val key: String,
    val value: String, // JSON or simple string value
    val lastUpdated: Long
)
