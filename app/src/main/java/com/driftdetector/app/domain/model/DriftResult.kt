package com.driftdetector.app.domain.model

import java.time.Instant

/**
 * Represents the result of a drift detection analysis
 */
data class DriftResult(
    val id: String,
    val modelId: String,
    val timestamp: Instant,
    val driftType: DriftType,
    val driftScore: Double,
    val threshold: Double,
    val isDriftDetected: Boolean,
    val featureDrifts: List<FeatureDrift>,
    val statisticalTests: List<StatisticalTestResult>,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of drift that can be detected
 */
enum class DriftType {
    CONCEPT_DRIFT,      // Change in P(Y|X) - relationship between features and target
    COVARIATE_DRIFT,    // Change in P(X) - distribution of input features
    PRIOR_DRIFT,        // Change in P(Y) - distribution of target variable
    NO_DRIFT
}

/**
 * Drift information for individual features
 */
data class FeatureDrift(
    val featureName: String,
    val featureIndex: Int,
    val driftScore: Double,
    val psiScore: Double?,     // Population Stability Index
    val ksStatistic: Double?,  // Kolmogorov-Smirnov statistic
    val pValue: Double?,
    val isDrifted: Boolean,
    val attribution: Double,    // SHAP-like attribution score
    val distributionShift: DistributionShift
)

/**
 * Information about distribution changes
 */
data class DistributionShift(
    val meanShift: Double,
    val stdShift: Double,
    val minShift: Double,
    val maxShift: Double,
    val quantileShifts: Map<Double, Double> = emptyMap()
)

/**
 * Result of statistical tests
 */
data class StatisticalTestResult(
    val testName: String,
    val statistic: Double,
    val pValue: Double,
    val threshold: Double,
    val isPassed: Boolean
)

/**
 * Model metadata
 */
data class MLModel(
    val id: String,
    val name: String,
    val version: String,
    val modelPath: String,
    val inputFeatures: List<String>,
    val outputLabels: List<String>,
    val createdAt: Instant,
    val lastUpdated: Instant,
    val isActive: Boolean
)

/**
 * Data point for model inference
 */
data class ModelInput(
    val features: FloatArray,
    val featureNames: List<String>,
    val timestamp: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelInput
        if (!features.contentEquals(other.features)) return false
        if (featureNames != other.featureNames) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = features.contentHashCode()
        result = 31 * result + featureNames.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Prediction result from model
 */
data class ModelPrediction(
    val input: ModelInput,
    val outputs: FloatArray,
    val predictedClass: Int?,
    val confidence: Float,
    val timestamp: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelPrediction
        if (input != other.input) return false
        if (!outputs.contentEquals(other.outputs)) return false
        if (predictedClass != other.predictedClass) return false
        if (confidence != other.confidence) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = input.hashCode()
        result = 31 * result + outputs.contentHashCode()
        result = 31 * result + (predictedClass ?: 0)
        result = 31 * result + confidence.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
