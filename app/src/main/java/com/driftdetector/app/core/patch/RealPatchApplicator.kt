package com.driftdetector.app.core.patch

import android.content.Context
import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * Applies patches as preprocessing transformations during model inference
 * This actually modifies model behavior by transforming inputs
 */
class RealPatchApplicator(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Apply preprocessing from active patches to model input
     */
    suspend fun applyPreprocessing(modelId: String, input: FloatArray): FloatArray =
        withContext(Dispatchers.IO) {
            try {
                val preprocessor = loadPreprocessor(modelId) ?: return@withContext input

                var processedInput = input.copyOf()
                var modificationsApplied = 0

                // Apply clipping rules
                preprocessor.clippingRules?.forEach { rule ->
                    if (rule.featureIndex < processedInput.size) {
                        val originalValue = processedInput[rule.featureIndex]
                        processedInput[rule.featureIndex] = processedInput[rule.featureIndex]
                            .coerceIn(rule.minValue, rule.maxValue)

                        if (originalValue != processedInput[rule.featureIndex]) {
                            Timber.v("   📌 Clipped feature[${rule.featureIndex}]: $originalValue → ${processedInput[rule.featureIndex]} (range: ${rule.minValue} to ${rule.maxValue})")
                            modificationsApplied++
                        }
                    }
                }

                // Apply reweighting rules
                preprocessor.reweightingRules?.forEach { rule ->
                    if (rule.featureIndex < processedInput.size) {
                        val originalValue = processedInput[rule.featureIndex]
                        val weightRatio = rule.newWeight / rule.originalWeight
                        processedInput[rule.featureIndex] *= weightRatio
                        Timber.v("   ⚖️ Reweighted feature[${rule.featureIndex}]: $originalValue → ${processedInput[rule.featureIndex]} (ratio: $weightRatio)")
                        modificationsApplied++
                    }
                }

                // Apply normalization rules
                preprocessor.normalizationRules?.forEach { rule ->
                    if (rule.featureIndex < processedInput.size) {
                        val originalValue = processedInput[rule.featureIndex]

                        // Denormalize using old parameters
                        val denormalized = if (rule.originalStd > 0) {
                            processedInput[rule.featureIndex] * rule.originalStd + rule.originalMean
                        } else {
                            processedInput[rule.featureIndex] + rule.originalMean
                        }

                        // Renormalize using new parameters
                        processedInput[rule.featureIndex] = if (rule.newStd > 0) {
                            (denormalized - rule.newMean) / rule.newStd
                        } else {
                            denormalized - rule.newMean
                        }

                        Timber.v("   📊 Renormalized feature[${rule.featureIndex}]: $originalValue → ${processedInput[rule.featureIndex]}")
                        Timber.v("      Old: μ=${rule.originalMean}, σ=${rule.originalStd} | New: μ=${rule.newMean}, σ=${rule.newStd}")
                        modificationsApplied++
                    }
                }

                if (modificationsApplied > 0) {
                    Timber.d("✅ Applied $modificationsApplied preprocessing modifications for model $modelId")
                }

                processedInput
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to apply preprocessing, using original input")
                input
            }
        }

    /**
     * Save patch as preprocessing configuration
     */
    suspend fun savePatchAsPreprocessing(
        modelId: String,
        patch: Patch
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingPreprocessor = loadPreprocessor(modelId)
            val updatedPreprocessor = when (val config = patch.configuration) {
                is PatchConfiguration.FeatureClipping -> {
                    val clippingRules = config.featureIndices.mapIndexed { i, featureIdx ->
                        FeatureClippingRule(
                            featureIndex = featureIdx,
                            minValue = config.minValues[i],
                            maxValue = config.maxValues[i]
                        )
                    }

                    Timber.d("   📌 Adding ${clippingRules.size} clipping rules:")
                    clippingRules.forEach { rule ->
                        Timber.d("      • Feature[${rule.featureIndex}]: clip to [${rule.minValue}, ${rule.maxValue}]")
                    }

                    existingPreprocessor?.copy(
                        clippingRules = (existingPreprocessor.clippingRules.orEmpty() + clippingRules)
                    ) ?: ModelPreprocessor(clippingRules = clippingRules)
                }

                is PatchConfiguration.FeatureReweighting -> {
                    val reweightingRules = config.featureIndices.mapIndexed { i, featureIdx ->
                        FeatureReweightingRule(
                            featureIndex = featureIdx,
                            originalWeight = config.originalWeights[i],
                            newWeight = config.newWeights[i]
                        )
                    }

                    Timber.d("   ⚖️ Adding ${reweightingRules.size} reweighting rules:")
                    reweightingRules.forEach { rule ->
                        val ratio = rule.newWeight / rule.originalWeight
                        Timber.d("      • Feature[${rule.featureIndex}]: weight ${rule.originalWeight} → ${rule.newWeight} (×$ratio)")
                    }

                    existingPreprocessor?.copy(
                        reweightingRules = (existingPreprocessor.reweightingRules.orEmpty() + reweightingRules)
                    ) ?: ModelPreprocessor(reweightingRules = reweightingRules)
                }

                is PatchConfiguration.NormalizationUpdate -> {
                    val normalizationRules = config.featureIndices.mapIndexed { i, featureIdx ->
                        NormalizationRule(
                            featureIndex = featureIdx,
                            originalMean = config.originalMeans[i],
                            originalStd = config.originalStds[i],
                            newMean = config.newMeans[i],
                            newStd = config.newStds[i]
                        )
                    }

                    Timber.d("   📊 Adding ${normalizationRules.size} normalization rules:")
                    normalizationRules.forEach { rule ->
                        Timber.d("      • Feature[${rule.featureIndex}]:")
                        Timber.d("         Old: μ=${rule.originalMean}, σ=${rule.originalStd}")
                        Timber.d("         New: μ=${rule.newMean}, σ=${rule.newStd}")
                    }

                    existingPreprocessor?.copy(
                        normalizationRules = (existingPreprocessor.normalizationRules.orEmpty() + normalizationRules)
                    ) ?: ModelPreprocessor(normalizationRules = normalizationRules)
                }

                is PatchConfiguration.ThresholdTuning -> {
                    // Threshold tuning doesn't affect preprocessing
                    Timber.d("   🎯 Threshold tuning patch: ${config.originalThreshold} → ${config.newThreshold}")
                    Timber.d("      (No preprocessing rules to save for threshold tuning)")
                    existingPreprocessor ?: ModelPreprocessor()
                }
            }

            savePreprocessor(modelId, updatedPreprocessor)

            // Log summary of all active preprocessing
            val totalRules = (updatedPreprocessor.clippingRules?.size ?: 0) +
                    (updatedPreprocessor.reweightingRules?.size ?: 0) +
                    (updatedPreprocessor.normalizationRules?.size ?: 0)

            Timber.i("✅ Saved patch as preprocessing for model $modelId")
            Timber.i("   📋 Total active preprocessing rules: $totalRules")
            Timber.i("      Clipping: ${updatedPreprocessor.clippingRules?.size ?: 0}")
            Timber.i("      Reweighting: ${updatedPreprocessor.reweightingRules?.size ?: 0}")
            Timber.i("      Normalization: ${updatedPreprocessor.normalizationRules?.size ?: 0}")

            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save patch as preprocessing")
            false
        }
    }

    /**
     * Remove patch preprocessing
     */
    suspend fun removePatchPreprocessing(
        modelId: String,
        patch: Patch
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val preprocessor = loadPreprocessor(modelId) ?: return@withContext false

            val updatedPreprocessor = when (val config = patch.configuration) {
                is PatchConfiguration.FeatureClipping -> {
                    val indicesToRemove = config.featureIndices.toSet()
                    preprocessor.copy(
                        clippingRules = preprocessor.clippingRules?.filterNot {
                            it.featureIndex in indicesToRemove
                        }
                    )
                }

                is PatchConfiguration.FeatureReweighting -> {
                    val indicesToRemove = config.featureIndices.toSet()
                    preprocessor.copy(
                        reweightingRules = preprocessor.reweightingRules?.filterNot {
                            it.featureIndex in indicesToRemove
                        }
                    )
                }

                is PatchConfiguration.NormalizationUpdate -> {
                    val indicesToRemove = config.featureIndices.toSet()
                    preprocessor.copy(
                        normalizationRules = preprocessor.normalizationRules?.filterNot {
                            it.featureIndex in indicesToRemove
                        }
                    )
                }

                is PatchConfiguration.ThresholdTuning -> preprocessor
            }

            savePreprocessor(modelId, updatedPreprocessor)
            Timber.d("✅ Removed patch preprocessing for model $modelId")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to remove patch preprocessing")
            false
        }
    }

    /**
     * Clear all preprocessing for a model
     */
    suspend fun clearPreprocessing(modelId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getPreprocessorFile(modelId)
            if (file.exists()) {
                file.delete()
                Timber.d("✅ Cleared all preprocessing for model $modelId")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear preprocessing")
            false
        }
    }

    /**
     * Load preprocessor configuration from file
     */
    private fun loadPreprocessor(modelId: String): ModelPreprocessor? {
        return try {
            val file = getPreprocessorFile(modelId)
            if (!file.exists()) return null

            val jsonString = file.readText()
            json.decodeFromString<ModelPreprocessor>(jsonString)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load preprocessor for model $modelId")
            null
        }
    }

    /**
     * Save preprocessor configuration to file
     */
    private fun savePreprocessor(modelId: String, preprocessor: ModelPreprocessor) {
        val file = getPreprocessorFile(modelId)
        file.parentFile?.mkdirs()

        val jsonString = json.encodeToString(preprocessor)
        file.writeText(jsonString)
    }

    /**
     * Get preprocessor file for a model
     */
    private fun getPreprocessorFile(modelId: String): File {
        return File(context.filesDir, "preprocessors/${modelId}_preprocessor.json")
    }

    /**
     * Check if model has active preprocessing
     */
    fun hasActivePreprocessing(modelId: String): Boolean {
        return getPreprocessorFile(modelId).exists()
    }

    /**
     * Get summary of active preprocessing for a model
     */
    fun getActivePreprocessingSummary(modelId: String): PreprocessingSummary {
        val preprocessor = loadPreprocessor(modelId) ?: return PreprocessingSummary()

        return PreprocessingSummary(
            hasPreprocessing = true,
            clippingCount = preprocessor.clippingRules?.size ?: 0,
            reweightingCount = preprocessor.reweightingRules?.size ?: 0,
            normalizationCount = preprocessor.normalizationRules?.size ?: 0,
            clippingRules = preprocessor.clippingRules ?: emptyList(),
            reweightingRules = preprocessor.reweightingRules ?: emptyList(),
            normalizationRules = preprocessor.normalizationRules ?: emptyList()
        )
    }

    /**
     * Get detailed preprocessing info as human-readable string
     */
    fun getPreprocessingDetails(modelId: String): String {
        val summary = getActivePreprocessingSummary(modelId)

        if (!summary.hasPreprocessing) {
            return "No active preprocessing"
        }

        val sb = StringBuilder()
        sb.appendLine("Active Preprocessing Rules:")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (summary.clippingCount > 0) {
            sb.appendLine("\n📌 Clipping Rules (${summary.clippingCount}):")
            summary.clippingRules.forEach { rule ->
                sb.appendLine("   • Feature ${rule.featureIndex}: [${rule.minValue}, ${rule.maxValue}]")
            }
        }

        if (summary.reweightingCount > 0) {
            sb.appendLine("\n⚖️ Reweighting Rules (${summary.reweightingCount}):")
            summary.reweightingRules.forEach { rule ->
                val ratio = rule.newWeight / rule.originalWeight
                sb.appendLine("   • Feature ${rule.featureIndex}: ×${String.format("%.3f", ratio)}")
            }
        }

        if (summary.normalizationCount > 0) {
            sb.appendLine("\n📊 Normalization Rules (${summary.normalizationCount}):")
            summary.normalizationRules.forEach { rule ->
                sb.appendLine("   • Feature ${rule.featureIndex}:")
                sb.appendLine(
                    "      μ: ${
                        String.format(
                            "%.3f",
                            rule.originalMean
                        )
                    } → ${String.format("%.3f", rule.newMean)}"
                )
                sb.appendLine(
                    "      σ: ${
                        String.format(
                            "%.3f",
                            rule.originalStd
                        )
                    } → ${String.format("%.3f", rule.newStd)}"
                )
            }
        }

        sb.appendLine("\n━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Total: ${summary.totalRules} rules active")

        return sb.toString()
    }
}

/**
 * Model preprocessing configuration stored as JSON
 */
@Serializable
data class ModelPreprocessor(
    val clippingRules: List<FeatureClippingRule>? = null,
    val reweightingRules: List<FeatureReweightingRule>? = null,
    val normalizationRules: List<NormalizationRule>? = null
)

@Serializable
data class FeatureClippingRule(
    val featureIndex: Int,
    val minValue: Float,
    val maxValue: Float
)

@Serializable
data class FeatureReweightingRule(
    val featureIndex: Int,
    val originalWeight: Float,
    val newWeight: Float
)

@Serializable
data class NormalizationRule(
    val featureIndex: Int,
    val originalMean: Float,
    val originalStd: Float,
    val newMean: Float,
    val newStd: Float
)

@Serializable
data class PreprocessingSummary(
    val hasPreprocessing: Boolean = false,
    val clippingCount: Int = 0,
    val reweightingCount: Int = 0,
    val normalizationCount: Int = 0,
    val clippingRules: List<FeatureClippingRule> = emptyList(),
    val reweightingRules: List<FeatureReweightingRule> = emptyList(),
    val normalizationRules: List<NormalizationRule> = emptyList()
) {
    val totalRules: Int = clippingCount + reweightingCount + normalizationCount
}
