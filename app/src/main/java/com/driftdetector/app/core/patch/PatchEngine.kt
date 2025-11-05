package com.driftdetector.app.core.patch

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant

/**
 * Engine for applying and rolling back reversible patches
 */
class PatchEngine {

    /**
     * Apply a patch to model inputs
     */
    suspend fun applyPatch(
        patch: Patch,
        input: FloatArray
    ): FloatArray = withContext(Dispatchers.Default) {
        try {
            when (val config = patch.configuration) {
                is PatchConfiguration.FeatureClipping -> applyFeatureClipping(input, config)
                is PatchConfiguration.FeatureReweighting -> applyFeatureReweighting(input, config)
                is PatchConfiguration.ThresholdTuning -> input // Threshold applied post-inference
                is PatchConfiguration.NormalizationUpdate -> applyNormalizationUpdate(input, config)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply patch")
            input // Return original on error
        }
    }

    /**
     * Apply feature clipping
     */
    private fun applyFeatureClipping(
        input: FloatArray,
        config: PatchConfiguration.FeatureClipping
    ): FloatArray {
        val result = input.copyOf()

        config.featureIndices.forEachIndexed { i, featureIdx ->
            if (featureIdx < result.size) {
                result[featureIdx] = result[featureIdx].coerceIn(
                    config.minValues[i],
                    config.maxValues[i]
                )
            }
        }

        return result
    }

    /**
     * Apply feature reweighting
     */
    private fun applyFeatureReweighting(
        input: FloatArray,
        config: PatchConfiguration.FeatureReweighting
    ): FloatArray {
        val result = input.copyOf()

        config.featureIndices.forEachIndexed { i, featureIdx ->
            if (featureIdx < result.size) {
                val weightRatio = config.newWeights[i] / config.originalWeights[i]
                result[featureIdx] = result[featureIdx] * weightRatio
            }
        }

        return result
    }

    /**
     * Apply normalization update
     */
    private fun applyNormalizationUpdate(
        input: FloatArray,
        config: PatchConfiguration.NormalizationUpdate
    ): FloatArray {
        val result = input.copyOf()

        config.featureIndices.forEachIndexed { i, featureIdx ->
            if (featureIdx < result.size) {
                // Denormalize using old parameters
                val denormalized = if (config.originalStds[i] > 0) {
                    result[featureIdx] * config.originalStds[i] + config.originalMeans[i]
                } else {
                    result[featureIdx] + config.originalMeans[i]
                }

                // Renormalize using new parameters
                result[featureIdx] = if (config.newStds[i] > 0) {
                    (denormalized - config.newMeans[i]) / config.newStds[i]
                } else {
                    denormalized - config.newMeans[i]
                }
            }
        }

        return result
    }

    /**
     * Apply threshold tuning to predictions
     */
    fun applyThresholdTuning(
        predictions: FloatArray,
        config: PatchConfiguration.ThresholdTuning
    ): FloatArray {
        // Shift predictions based on threshold change
        val thresholdDiff = config.newThreshold - config.originalThreshold
        return predictions.map { it - thresholdDiff }.toFloatArray()
    }

    /**
     * Rollback a patch (restore original input processing)
     */
    suspend fun rollbackPatch(
        patch: Patch,
        input: FloatArray
    ): FloatArray = withContext(Dispatchers.Default) {
        try {
            when (val config = patch.configuration) {
                is PatchConfiguration.FeatureClipping -> input // Clipping is reversible by not applying
                is PatchConfiguration.FeatureReweighting -> rollbackFeatureReweighting(
                    input,
                    config
                )

                is PatchConfiguration.ThresholdTuning -> input
                is PatchConfiguration.NormalizationUpdate -> rollbackNormalizationUpdate(
                    input,
                    config
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to rollback patch")
            input
        }
    }

    /**
     * Rollback feature reweighting
     */
    private fun rollbackFeatureReweighting(
        input: FloatArray,
        config: PatchConfiguration.FeatureReweighting
    ): FloatArray {
        val result = input.copyOf()

        config.featureIndices.forEachIndexed { i, featureIdx ->
            if (featureIdx < result.size) {
                val weightRatio = config.originalWeights[i] / config.newWeights[i]
                result[featureIdx] = result[featureIdx] * weightRatio
            }
        }

        return result
    }

    /**
     * Rollback normalization update
     */
    private fun rollbackNormalizationUpdate(
        input: FloatArray,
        config: PatchConfiguration.NormalizationUpdate
    ): FloatArray {
        val result = input.copyOf()

        config.featureIndices.forEachIndexed { i, featureIdx ->
            if (featureIdx < result.size) {
                // Denormalize using new parameters
                val denormalized = if (config.newStds[i] > 0) {
                    result[featureIdx] * config.newStds[i] + config.newMeans[i]
                } else {
                    result[featureIdx] + config.newMeans[i]
                }

                // Renormalize using old parameters
                result[featureIdx] = if (config.originalStds[i] > 0) {
                    (denormalized - config.originalMeans[i]) / config.originalStds[i]
                } else {
                    denormalized - config.originalMeans[i]
                }
            }
        }

        return result
    }

    /**
     * Create snapshot for rollback capability
     */
    fun createSnapshot(patch: Patch): PatchSnapshot {
        return PatchSnapshot(
            patchId = patch.id,
            timestamp = Instant.now(),
            preApplyState = serializeConfiguration(patch.configuration),
            postApplyState = byteArrayOf() // Will be filled after application
        )
    }

    /**
     * Serialize patch configuration for storage
     */
    private fun serializeConfiguration(config: PatchConfiguration): ByteArray {
        // Simple serialization - in production use Protocol Buffers or similar
        return when (config) {
            is PatchConfiguration.FeatureClipping -> {
                "CLIP:${config.featureIndices.joinToString(",")}"
            }

            is PatchConfiguration.FeatureReweighting -> {
                "REWEIGHT:${config.featureIndices.joinToString(",")}"
            }

            is PatchConfiguration.ThresholdTuning -> {
                "THRESHOLD:${config.originalThreshold}:${config.newThreshold}"
            }

            is PatchConfiguration.NormalizationUpdate -> {
                "NORM:${config.featureIndices.joinToString(",")}"
            }
        }.toByteArray()
    }
}
