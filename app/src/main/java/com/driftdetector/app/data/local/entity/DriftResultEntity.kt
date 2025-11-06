package com.driftdetector.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.driftdetector.app.domain.model.DriftType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "drift_results")
@TypeConverters(DriftResultConverters::class)
data class DriftResultEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val timestamp: Long,
    val driftType: DriftType,
    val driftScore: Double,
    val threshold: Double,
    val isDriftDetected: Boolean,
    val featureDriftsJson: String,
    val statisticalTestsJson: String,
    val metadataJson: String
)

@Entity(tableName = "models")
data class MLModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val modelPath: String,
    val inputFeaturesJson: String,
    val outputLabelsJson: String,
    val createdAt: Long,
    val lastUpdated: Long,
    val isActive: Boolean
)

@Entity(tableName = "patches")
@TypeConverters(PatchConverters::class)
data class PatchEntity(
    @PrimaryKey val id: String,
    val modelId: String,
    val driftResultId: String,
    val patchType: String,
    val status: String,
    val createdAt: Long,
    val appliedAt: Long?,
    val rolledBackAt: Long?,
    val configurationJson: String,
    val validationResultJson: String?,
    val metadataJson: String
)

@Entity(tableName = "patch_snapshots")
data class PatchSnapshotEntity(
    @PrimaryKey val id: String,
    val patchId: String,
    val timestamp: Long,
    val preApplyState: ByteArray,
    val postApplyState: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PatchSnapshotEntity
        if (id != other.id) return false
        if (patchId != other.patchId) return false
        if (timestamp != other.timestamp) return false
        if (!preApplyState.contentEquals(other.preApplyState)) return false
        if (!postApplyState.contentEquals(other.postApplyState)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + patchId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + preApplyState.contentHashCode()
        result = 31 * result + postApplyState.contentHashCode()
        return result
    }
}

@Entity(tableName = "model_predictions")
data class ModelPredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: String,
    val inputJson: String,
    val outputsJson: String,
    val predictedClass: Int?,
    val confidence: Float,
    val timestamp: Long
)

@Entity(tableName = "deactivated_models")
data class DeactivatedModelEntity(
    @PrimaryKey val id: String,
    val originalModelId: String,
    val name: String,
    val version: String,
    val modelPath: String,
    val deactivatedAt: Long,
    val deactivationReason: String, // "USER_DELETED", "HIGH_DRIFT", "REPLACED", "ERROR"
    val totalDriftsDetected: Int,
    val totalPatchesApplied: Int,
    val lastDriftScore: Double?,
    val driftHistoryJson: String, // List of drift results
    val patchHistoryJson: String, // List of patches
    val metadataJson: String, // Additional model metadata
    val canRestore: Boolean = true
)

class DriftResultConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromDriftType(value: DriftType): String = value.name

    @TypeConverter
    fun toDriftType(value: String): DriftType = DriftType.valueOf(value)
}

class PatchConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
