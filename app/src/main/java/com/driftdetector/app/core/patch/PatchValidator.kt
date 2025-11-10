package com.driftdetector.app.core.patch

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Validates patches before application to ensure safety
 */
class PatchValidator(
    private val patchEngine: PatchEngine,
    private val minAccuracy: Double = 0.6,
    private val maxPerformanceDelta: Double = 0.1,
    private val minSampleSize: Int = 10  // Minimum samples for reliable validation
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
            val warnings = mutableListOf<String>()

            // CHECK 1: Minimum sample size
            if (validationData.size < minSampleSize) {
                errors.add(
                    "Insufficient validation data: ${validationData.size} samples provided, " +
                            "minimum required: $minSampleSize samples for reliable validation"
                )
                return@withContext ValidationResult(
                    isValid = false,
                    validatedAt = Instant.now(),
                    metrics = createDefaultMetrics(),
                    errors = errors,
                    warnings = warnings
                )
            }

            // Apply patch to validation data
            val patchedData = validationData.map { input ->
                patchEngine.applyPatch(patch, input)
            }

            // Calculate metrics
            val metrics = calculateMetrics(patchedData, validationLabels, patch)

            // CHECK 2: Accuracy threshold
            if (metrics.accuracy < minAccuracy) {
                errors.add(
                    "Accuracy below minimum threshold: ${
                        String.format(
                            "%.4f",
                            metrics.accuracy
                        )
                    } " +
                            "< ${String.format("%.4f", minAccuracy)}. " +
                            "Patch may degrade model performance."
                )
            }

            // CHECK 3: Performance delta compared to baseline
            if (baselineMetrics != null) {
                val perfDelta = abs(metrics.accuracy - baselineMetrics.accuracy)
                if (perfDelta > maxPerformanceDelta) {
                    errors.add(
                        "Performance degradation too high: ${String.format("%.4f", perfDelta)} " +
                                "> ${String.format("%.4f", maxPerformanceDelta)}. " +
                                "Patch causes significant performance change."
                    )
                } else if (metrics.accuracy < baselineMetrics.accuracy) {
                    warnings.add(
                        "Patch reduces accuracy by ${
                            String.format(
                                "%.2f%%",
                                (baselineMetrics.accuracy - metrics.accuracy) * 100
                            )
                        }"
                    )
                }
            }

            // CHECK 4: Safety score
            if (metrics.safetyScore < 0.5) {
                errors.add(
                    "Safety score too low: ${String.format("%.4f", metrics.safetyScore)}. " +
                            "Minimum required: 0.5. Patch may be too aggressive."
                )
            } else if (metrics.safetyScore < 0.7) {
                warnings.add(
                    "Moderate safety score: ${String.format("%.4f", metrics.safetyScore)}. " +
                            "Monitor patch effects closely."
                )
            }

            // CHECK 5: Precision and Recall balance
            if (metrics.precision > 0 && metrics.recall > 0) {
                val balance = abs(metrics.precision - metrics.recall)
                if (balance > 0.3) {
                    warnings.add(
                        "Imbalanced precision (${String.format("%.4f", metrics.precision)}) " +
                                "and recall (${String.format("%.4f", metrics.recall)})"
                    )
                }
            }

            // CHECK 6: Confidence intervals
            val confidenceInterval = calculateConfidenceInterval(
                metrics.accuracy,
                validationData.size
            )

            Timber.d(
                "Validation complete: Accuracy=${String.format("%.4f", metrics.accuracy)} " +
                        "CI=[${String.format("%.4f", confidenceInterval.lower)}, " +
                        "${String.format("%.4f", confidenceInterval.upper)}]"
            )

            ValidationResult(
                isValid = errors.isEmpty(),
                validatedAt = Instant.now(),
                metrics = metrics.copy(
                    confidenceIntervalLower = confidenceInterval.lower,
                    confidenceIntervalUpper = confidenceInterval.upper
                ),
                errors = errors,
                warnings = warnings
            )
        } catch (e: Exception) {
            Timber.e(e, "Patch validation failed")
            ValidationResult(
                isValid = false,
                validatedAt = Instant.now(),
                metrics = createDefaultMetrics(),
                errors = listOf("Validation error: ${e.message}"),
                warnings = emptyList()
            )
        }
    }

    /**
     * Calculate confidence interval for accuracy using Wilson score interval
     */
    private fun calculateConfidenceInterval(
        accuracy: Double,
        sampleSize: Int,
        confidenceLevel: Double = 0.95
    ): ConfidenceInterval {
        // Z-score for 95% confidence
        val z = 1.96

        val p = accuracy
        val n = sampleSize.toDouble()

        // Wilson score interval (more accurate for small samples and extreme probabilities)
        val denominator = 1 + z * z / n
        val center = (p + z * z / (2 * n)) / denominator
        val margin = z * sqrt((p * (1 - p) / n + z * z / (4 * n * n))) / denominator

        return ConfidenceInterval(
            lower = (center - margin).coerceIn(0.0, 1.0),
            upper = (center + margin).coerceIn(0.0, 1.0)
        )
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

        // Calculate expected drift reduction based on patch type and effectiveness
        val driftReduction = calculateDriftReduction(patch, accuracy, safetyScore)

        return ValidationMetrics(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            driftScoreAfterPatch = 0.1, // Would be calculated with actual drift detection
            driftReduction = driftReduction,  // Now calculated dynamically
            performanceDelta = 0.0, // Would compare with baseline
            safetyScore = safetyScore,
            confidenceIntervalLower = 0.0,
            confidenceIntervalUpper = 0.0
        )
    }

    /**
     * Calculate expected drift reduction based on patch characteristics
     */
    private fun calculateDriftReduction(
        patch: Patch,
        accuracy: Double,
        safetyScore: Double
    ): Double {
        // Base drift reduction depends on patch type
        val baseReduction = when (patch.patchType) {
            PatchType.NORMALIZATION_UPDATE -> 0.70    // Highly effective
            PatchType.FEATURE_REWEIGHTING -> 0.60     // Very effective
            PatchType.FEATURE_CLIPPING -> 0.50        // Moderately effective
            PatchType.THRESHOLD_TUNING -> 0.40        // Less effective
            else -> 0.45
        }

        // Adjust by accuracy (higher accuracy = more effective patch)
        val accuracyFactor = (accuracy + 1.0) / 2.0  // Scale 0-1 to 0.5-1.0

        // Adjust by safety score (safer patches are typically more effective)
        val safetyFactor = (safetyScore + 1.0) / 2.0  // Scale 0-1 to 0.5-1.0

        // Calculate final drift reduction with adjustments
        val finalReduction = baseReduction * accuracyFactor * safetyFactor

        return finalReduction.coerceIn(0.2, 0.95)  // Ensure reasonable bounds
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
            safetyScore = 0.0,
            confidenceIntervalLower = 0.0,
            confidenceIntervalUpper = 0.0
        )
    }
}

data class ConfidenceInterval(val lower: Double, val upper: Double)
