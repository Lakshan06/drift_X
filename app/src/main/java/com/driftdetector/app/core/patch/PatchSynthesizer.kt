package com.driftdetector.app.core.patch

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.*
import kotlin.math.abs

/**
 * Synthesizes reversible patches for drift mitigation
 */
class PatchSynthesizer {

    /**
     * Generate patches based on drift analysis
     */
    suspend fun synthesizePatch(
        modelId: String,
        driftResult: DriftResult,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>
    ): Patch = withContext(Dispatchers.Default) {
        try {
            // Select patch type based on drift characteristics
            val patchType = selectPatchType(driftResult)

            // Generate configuration based on type
            val configuration = when (patchType) {
                PatchType.FEATURE_CLIPPING -> generateFeatureClipping(driftResult, referenceData)
                PatchType.FEATURE_REWEIGHTING -> generateFeatureReweighting(driftResult)
                PatchType.THRESHOLD_TUNING -> generateThresholdTuning(driftResult, currentData)
                PatchType.NORMALIZATION_UPDATE -> generateNormalizationUpdate(
                    driftResult,
                    referenceData,
                    currentData
                )

                else -> generateFeatureClipping(driftResult, referenceData)
            }

            Patch(
                id = UUID.randomUUID().toString(),
                modelId = modelId,
                driftResultId = driftResult.id,
                patchType = patchType,
                status = PatchStatus.CREATED,
                createdAt = Instant.now(),
                appliedAt = null,
                rolledBackAt = null,
                configuration = configuration,
                validationResult = null
            )
        } catch (e: Exception) {
            Timber.e(e, "Patch synthesis failed")
            throw e
        }
    }

    /**
     * Select appropriate patch type based on drift characteristics
     */
    private fun selectPatchType(driftResult: DriftResult): PatchType {
        return when (driftResult.driftType) {
            DriftType.COVARIATE_DRIFT -> {
                // For covariate drift, clip or normalize features
                if (hasOutliers(driftResult.featureDrifts)) {
                    PatchType.FEATURE_CLIPPING
                } else {
                    PatchType.NORMALIZATION_UPDATE
                }
            }

            DriftType.CONCEPT_DRIFT -> {
                // For concept drift, reweight features or adjust thresholds
                if (hasSignificantAttributionChanges(driftResult.featureDrifts)) {
                    PatchType.FEATURE_REWEIGHTING
                } else {
                    PatchType.THRESHOLD_TUNING
                }
            }

            DriftType.PRIOR_DRIFT -> PatchType.THRESHOLD_TUNING
            DriftType.NO_DRIFT -> PatchType.THRESHOLD_TUNING
        }
    }

    /**
     * Check if features have outliers
     */
    private fun hasOutliers(featureDrifts: List<FeatureDrift>): Boolean {
        return featureDrifts.any { drift ->
            abs(drift.distributionShift.maxShift) > 3.0 * drift.distributionShift.stdShift ||
                    abs(drift.distributionShift.minShift) > 3.0 * drift.distributionShift.stdShift
        }
    }

    /**
     * Check if feature attributions changed significantly
     */
    private fun hasSignificantAttributionChanges(featureDrifts: List<FeatureDrift>): Boolean {
        return featureDrifts.any { it.attribution > 1.5 }
    }

    /**
     * Generate feature clipping patch
     */
    private fun generateFeatureClipping(
        driftResult: DriftResult,
        referenceData: List<FloatArray>
    ): PatchConfiguration.FeatureClipping {
        val driftedFeatures = driftResult.featureDrifts.filter { it.isDrifted }
        val indices = driftedFeatures.map { it.featureIndex }

        val minValues = FloatArray(indices.size)
        val maxValues = FloatArray(indices.size)

        indices.forEachIndexed { i, featureIdx ->
            val featureValues = referenceData.map { it[featureIdx] }
            val sortedValues = featureValues.sorted()

            // Use 1st and 99th percentile for clipping
            minValues[i] = sortedValues[(sortedValues.size * 0.01).toInt()]
            maxValues[i] = sortedValues[(sortedValues.size * 0.99).toInt()]
        }

        return PatchConfiguration.FeatureClipping(
            featureIndices = indices,
            minValues = minValues,
            maxValues = maxValues
        )
    }

    /**
     * Generate feature reweighting patch
     */
    private fun generateFeatureReweighting(
        driftResult: DriftResult
    ): PatchConfiguration.FeatureReweighting {
        val driftedFeatures = driftResult.featureDrifts.filter { it.isDrifted }
        val indices = driftedFeatures.map { it.featureIndex }

        val originalWeights = FloatArray(indices.size) { 1.0f }
        val newWeights = FloatArray(indices.size)

        // Adjust weights inversely proportional to drift score
        driftedFeatures.forEachIndexed { i, drift ->
            // Reduce weight for drifted features
            newWeights[i] = (1.0 / (1.0 + drift.driftScore)).toFloat()
        }

        // Normalize weights
        val sumWeights = newWeights.sum()
        if (sumWeights > 0) {
            for (i in newWeights.indices) {
                newWeights[i] = newWeights[i] / sumWeights * originalWeights.size
            }
        }

        return PatchConfiguration.FeatureReweighting(
            featureIndices = indices,
            originalWeights = originalWeights,
            newWeights = newWeights
        )
    }

    /**
     * Generate threshold tuning patch
     */
    private fun generateThresholdTuning(
        driftResult: DriftResult,
        currentData: List<FloatArray>
    ): PatchConfiguration.ThresholdTuning {
        val originalThreshold = 0.5f

        // Adjust threshold based on drift score
        val adjustment = (driftResult.driftScore * 0.1).toFloat()
        val newThreshold = (originalThreshold + adjustment).coerceIn(0.0f, 1.0f)

        return PatchConfiguration.ThresholdTuning(
            originalThreshold = originalThreshold,
            newThreshold = newThreshold
        )
    }

    /**
     * Generate normalization update patch
     */
    private fun generateNormalizationUpdate(
        driftResult: DriftResult,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>
    ): PatchConfiguration.NormalizationUpdate {
        val driftedFeatures = driftResult.featureDrifts.filter { it.isDrifted }
        val indices = driftedFeatures.map { it.featureIndex }

        val originalMeans = FloatArray(indices.size)
        val originalStds = FloatArray(indices.size)
        val newMeans = FloatArray(indices.size)
        val newStds = FloatArray(indices.size)

        indices.forEachIndexed { i, featureIdx ->
            val refValues = referenceData.map { it[featureIdx].toDouble() }
            val curValues = currentData.map { it[featureIdx].toDouble() }

            originalMeans[i] = refValues.average().toFloat()
            originalStds[i] = calculateStd(refValues, originalMeans[i].toDouble()).toFloat()

            newMeans[i] = curValues.average().toFloat()
            newStds[i] = calculateStd(curValues, newMeans[i].toDouble()).toFloat()
        }

        return PatchConfiguration.NormalizationUpdate(
            featureIndices = indices,
            originalMeans = originalMeans,
            originalStds = originalStds,
            newMeans = newMeans,
            newStds = newStds
        )
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStd(data: List<Double>, mean: Double): Double {
        if (data.isEmpty()) return 0.0
        val variance = data.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
