package com.driftdetector.app.domain.model

import java.time.Instant

/**
 * Represents a reversible patch for drift mitigation
 */
data class Patch(
    val id: String,
    val modelId: String,
    val driftResultId: String,
    val patchType: PatchType,
    val status: PatchStatus,
    val createdAt: Instant,
    val appliedAt: Instant?,
    val rolledBackAt: Instant?,
    val configuration: PatchConfiguration,
    val validationResult: ValidationResult?,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of patches that can be generated
 */
enum class PatchType {
    FEATURE_CLIPPING,       // Clip feature values to reference distribution bounds
    FEATURE_REWEIGHTING,    // Adjust feature weights/importance
    THRESHOLD_TUNING,       // Adjust decision thresholds
    NORMALIZATION_UPDATE,   // Update normalization parameters
    ENSEMBLE_REWEIGHT,      // Reweight ensemble components
    CALIBRATION_ADJUST      // Adjust probability calibration
}

/**
 * Patch lifecycle status
 */
enum class PatchStatus {
    CREATED,
    VALIDATED,
    APPLIED,
    FAILED,
    ROLLED_BACK
}

/**
 * Configuration for a specific patch
 */
sealed class PatchConfiguration {
    data class FeatureClipping(
        val featureIndices: List<Int>,
        val minValues: FloatArray,
        val maxValues: FloatArray
    ) : PatchConfiguration() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FeatureClipping
            if (featureIndices != other.featureIndices) return false
            if (!minValues.contentEquals(other.minValues)) return false
            if (!maxValues.contentEquals(other.maxValues)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = featureIndices.hashCode()
            result = 31 * result + minValues.contentHashCode()
            result = 31 * result + maxValues.contentHashCode()
            return result
        }
    }

    data class FeatureReweighting(
        val featureIndices: List<Int>,
        val originalWeights: FloatArray,
        val newWeights: FloatArray
    ) : PatchConfiguration() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FeatureReweighting
            if (featureIndices != other.featureIndices) return false
            if (!originalWeights.contentEquals(other.originalWeights)) return false
            if (!newWeights.contentEquals(other.newWeights)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = featureIndices.hashCode()
            result = 31 * result + originalWeights.contentHashCode()
            result = 31 * result + newWeights.contentHashCode()
            return result
        }
    }

    data class ThresholdTuning(
        val originalThreshold: Float,
        val newThreshold: Float,
        val classIndex: Int = 0
    ) : PatchConfiguration()

    data class NormalizationUpdate(
        val featureIndices: List<Int>,
        val originalMeans: FloatArray,
        val originalStds: FloatArray,
        val newMeans: FloatArray,
        val newStds: FloatArray
    ) : PatchConfiguration() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as NormalizationUpdate
            if (featureIndices != other.featureIndices) return false
            if (!originalMeans.contentEquals(other.originalMeans)) return false
            if (!originalStds.contentEquals(other.originalStds)) return false
            if (!newMeans.contentEquals(other.newMeans)) return false
            if (!newStds.contentEquals(other.newStds)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = featureIndices.hashCode()
            result = 31 * result + originalMeans.contentHashCode()
            result = 31 * result + originalStds.contentHashCode()
            result = 31 * result + newMeans.contentHashCode()
            result = 31 * result + newStds.contentHashCode()
            return result
        }
    }
}

/**
 * Result of patch validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val validatedAt: Instant,
    val metrics: ValidationMetrics,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Metrics for patch validation
 */
data class ValidationMetrics(
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val driftScoreAfterPatch: Double,
    val driftReduction: Double,
    val performanceDelta: Double,
    val safetyScore: Double,  // Safety metric to prevent degradation
    val confidenceIntervalLower: Double = 0.0,
    val confidenceIntervalUpper: Double = 0.0
)

/**
 * Historical snapshot for rollback capability
 */
data class PatchSnapshot(
    val patchId: String,
    val timestamp: Instant,
    val preApplyState: ByteArray,
    val postApplyState: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PatchSnapshot
        if (patchId != other.patchId) return false
        if (timestamp != other.timestamp) return false
        if (!preApplyState.contentEquals(other.preApplyState)) return false
        if (!postApplyState.contentEquals(other.postApplyState)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = patchId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + preApplyState.contentHashCode()
        result = 31 * result + postApplyState.contentHashCode()
        return result
    }
}

// ==================== Extension Functions for Human-Readable Descriptions ====================

/**
 * Get a user-friendly description of what this patch type does
 */
fun PatchType.getDescription(): String {
    return when (this) {
        PatchType.FEATURE_CLIPPING -> "Clips outlier values to safe ranges based on reference data"
        PatchType.FEATURE_REWEIGHTING -> "Adjusts feature importance to reduce impact of drifted features"
        PatchType.THRESHOLD_TUNING -> "Optimizes decision thresholds to account for data distribution changes"
        PatchType.NORMALIZATION_UPDATE -> "Updates normalization parameters to align with current data distribution"
        PatchType.ENSEMBLE_REWEIGHT -> "Rebalances ensemble model components for better performance"
        PatchType.CALIBRATION_ADJUST -> "Recalibrates prediction probabilities to maintain accuracy"
    }
}

/**
 * Get a brief explanation suitable for notifications or quick views
 */
fun Patch.getBriefDescription(): String {
    return "${patchType.getIcon()} ${
        patchType.name.replace(
            "_",
            " "
        )
    }: ${configuration.getShortSummary()}"
}
