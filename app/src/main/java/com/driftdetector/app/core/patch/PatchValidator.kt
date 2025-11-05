package com.driftdetector.app.core.patch

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import kotlin.math.abs

/**
 * Validates patches before application to ensure safety
 */
class PatchValidator(
    private val patchEngine: PatchEngine,
    private val minAccuracy: Double = 0.7,
    private val maxPerformanceDelta: Double = 0.1
) {

    /**
     * Validate a patch on validation data
     */
    suspend fun validate(
        patch: Patch,
        validationData: List<FloatArray>,
        validationLabels: List<Int>,
        baselineMetrics: ValidationMetrics? = null
    ): ValidationResult = withContext(Dispatchers.Default) {
        try {
            val errors = mutableListOf<String>()

            // Apply patch to validation data
            val patchedData = validationData.map { input ->
                patchEngine.applyPatch(patch, input)
            }

            // Calculate metrics
            val metrics = calculateMetrics(patchedData, validationLabels, patch)

            // Safety checks
            if (metrics.accuracy < minAccuracy) {
                errors.add("Accuracy below minimum threshold: ${metrics.accuracy} < $minAccuracy")
            }

            if (baselineMetrics != null) {
                val perfDelta = abs(metrics.accuracy - baselineMetrics.accuracy)
                if (perfDelta > maxPerformanceDelta) {
                    errors.add("Performance degradation too high: $perfDelta > $maxPerformanceDelta")
                }
            }

            if (metrics.safetyScore < 0.8) {
                errors.add("Safety score too low: ${metrics.safetyScore}")
            }

            ValidationResult(
                isValid = errors.isEmpty(),
                validatedAt = Instant.now(),
                metrics = metrics,
                errors = errors
            )
        } catch (e: Exception) {
            Timber.e(e, "Patch validation failed")
            ValidationResult(
                isValid = false,
                validatedAt = Instant.now(),
                metrics = createDefaultMetrics(),
                errors = listOf("Validation error: ${e.message}")
            )
        }
    }

    /**
     * Calculate validation metrics
     */
    private fun calculateMetrics(
        patchedData: List<FloatArray>,
        labels: List<Int>,
        patch: Patch
    ): ValidationMetrics {
        // Simplified metrics calculation
        // In production, would run through actual model inference

        val predictions = patchedData.map { data ->
            // Mock prediction - get highest value index
            data.indices.maxByOrNull { data[it] } ?: 0
        }

        // Calculate confusion matrix
        var truePositives = 0
        var falsePositives = 0
        var trueNegatives = 0
        var falseNegatives = 0

        predictions.zip(labels).forEach { (pred, actual) ->
            when {
                pred == 1 && actual == 1 -> truePositives++
                pred == 1 && actual == 0 -> falsePositives++
                pred == 0 && actual == 0 -> trueNegatives++
                pred == 0 && actual == 1 -> falseNegatives++
            }
        }

        val accuracy = if (predictions.isNotEmpty()) {
            predictions.zip(labels).count { it.first == it.second }.toDouble() / predictions.size
        } else 0.0

        val precision = if (truePositives + falsePositives > 0) {
            truePositives.toDouble() / (truePositives + falsePositives)
        } else 0.0

        val recall = if (truePositives + falseNegatives > 0) {
            truePositives.toDouble() / (truePositives + falseNegatives)
        } else 0.0

        val f1Score = if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0.0

        // Safety score based on patch type and magnitude
        val safetyScore = calculateSafetyScore(patch, accuracy)

        return ValidationMetrics(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            driftScoreAfterPatch = 0.1, // Would be calculated with actual drift detection
            driftReduction = 0.5, // Mock value
            performanceDelta = 0.0, // Would compare with baseline
            safetyScore = safetyScore
        )
    }

    /**
     * Calculate safety score for patch
     */
    private fun calculateSafetyScore(patch: Patch, accuracy: Double): Double {
        var score = accuracy

        // Penalize aggressive patches
        when (val config = patch.configuration) {
            is PatchConfiguration.FeatureClipping -> {
                // Check clipping range
                val ranges = config.maxValues.zip(config.minValues) { max, min -> max - min }
                val avgRange = ranges.average()
                if (avgRange < 0.5) score *= 0.9 // Narrow clipping is risky
            }

            is PatchConfiguration.FeatureReweighting -> {
                // Check weight changes
                val weightChanges = config.newWeights.zip(config.originalWeights) { new, old ->
                    if (old > 0) abs(new - old) / old else 0f
                }
                val maxChange = weightChanges.maxOrNull() ?: 0f
                if (maxChange > 0.5) score *= 0.85 // Large reweighting is risky
            }

            is PatchConfiguration.ThresholdTuning -> {
                val thresholdChange = abs(config.newThreshold - config.originalThreshold)
                if (thresholdChange > 0.2) score *= 0.9
            }

            is PatchConfiguration.NormalizationUpdate -> {
                // Generally safe
                score *= 0.95
            }
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Create default metrics for error cases
     */
    private fun createDefaultMetrics(): ValidationMetrics {
        return ValidationMetrics(
            accuracy = 0.0,
            precision = 0.0,
            recall = 0.0,
            f1Score = 0.0,
            driftScoreAfterPatch = 1.0,
            driftReduction = 0.0,
            performanceDelta = 0.0,
            safetyScore = 0.0
        )
    }
}
