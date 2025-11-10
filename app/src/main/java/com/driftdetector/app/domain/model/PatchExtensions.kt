package com.driftdetector.app.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Get icon for patch type
 */
fun PatchType.getIcon(): String {
    return when (this) {
        PatchType.FEATURE_CLIPPING -> "✂️"
        PatchType.FEATURE_REWEIGHTING -> "⚖️"
        PatchType.THRESHOLD_TUNING -> "🎯"
        PatchType.NORMALIZATION_UPDATE -> "📊"
        PatchType.ENSEMBLE_REWEIGHT -> "🔄"
        PatchType.CALIBRATION_ADJUST -> "🔧"
    }
}

/**
 * Get expected impact description
 */
fun PatchType.getExpectedImpact(): String {
    return when (this) {
        PatchType.FEATURE_CLIPPING -> "Expected: 30-60% drift reduction"
        PatchType.FEATURE_REWEIGHTING -> "Expected: 40-70% drift reduction"
        PatchType.THRESHOLD_TUNING -> "Expected: 20-40% drift reduction"
        PatchType.NORMALIZATION_UPDATE -> "Expected: 50-80% drift reduction"
        PatchType.ENSEMBLE_REWEIGHT -> "Expected: 35-65% drift reduction"
        PatchType.CALIBRATION_ADJUST -> "Expected: 25-50% drift reduction"
    }
}

/**
 * Get short summary of patch configuration
 */
fun PatchConfiguration.getShortSummary(): String {
    return when (this) {
        is PatchConfiguration.FeatureClipping ->
            "Clips ${featureIndices.size} features to prevent outliers"

        is PatchConfiguration.FeatureReweighting ->
            "Reweights ${featureIndices.size} features to reduce drift impact"

        is PatchConfiguration.ThresholdTuning ->
            "Adjusts decision threshold from $originalThreshold to $newThreshold"

        is PatchConfiguration.NormalizationUpdate ->
            "Updates normalization for ${featureIndices.size} features"
    }
}

/**
 * Get detailed explanation of what the patch does
 */
fun PatchConfiguration.getDetailedExplanation(): String {
    return when (this) {
        is PatchConfiguration.FeatureClipping -> buildString {
            appendLine("🔒 Feature Clipping Patch")
            appendLine()
            appendLine("This patch clips ${featureIndices.size} features that have drifted to prevent outliers from affecting predictions.")
            appendLine()
            appendLine("How it works:")
            appendLine("• Values below minimum threshold → clipped to minimum")
            appendLine("• Values above maximum threshold → clipped to maximum")
            appendLine("• Values within range → unchanged")
            appendLine()
            appendLine("Affected features: ${featureIndices.joinToString(", ") { "F$it" }}")
            appendLine()
            appendLine("This prevents extreme outliers from skewing model predictions while preserving normal data patterns.")
        }

        is PatchConfiguration.FeatureReweighting -> buildString {
            appendLine("⚖️ Feature Reweighting Patch")
            appendLine()
            appendLine("This patch adjusts the importance of ${featureIndices.size} features that have experienced drift.")
            appendLine()
            appendLine("How it works:")
            appendLine("• Drifted features get reduced weights")
            appendLine("• Stable features maintain their importance")
            appendLine("• Weights are normalized to maintain model balance")
            appendLine()
            appendLine("Affected features: ${featureIndices.joinToString(", ") { "F$it" }}")
            appendLine()
            appendLine("Average weight change: ${((newWeights.average() / originalWeights.average() - 1) * 100).toInt()}%")
            appendLine()
            appendLine("This reduces the impact of drifted features on predictions while maintaining overall model accuracy.")
        }

        is PatchConfiguration.ThresholdTuning -> buildString {
            appendLine("🎯 Threshold Tuning Patch")
            appendLine()
            appendLine("This patch adjusts the decision threshold to account for drift in the target distribution.")
            appendLine()
            appendLine("How it works:")
            appendLine("• Original threshold: $originalThreshold")
            appendLine("• New threshold: $newThreshold")
            appendLine("• Adjustment: ${String.format("%.4f", newThreshold - originalThreshold)}")
            appendLine()
            val direction =
                if (newThreshold > originalThreshold) "more conservative" else "more aggressive"
            appendLine("This makes the model $direction in its predictions to better match the current data distribution.")
        }

        is PatchConfiguration.NormalizationUpdate -> buildString {
            appendLine("📊 Normalization Update Patch")
            appendLine()
            appendLine("This patch updates normalization parameters for ${featureIndices.size} features to match the current data distribution.")
            appendLine()
            appendLine("How it works:")
            appendLine("• Recalculates mean and standard deviation")
            appendLine("• Applies new normalization to incoming data")
            appendLine("• Ensures features are on comparable scales")
            appendLine()
            appendLine("Affected features: ${featureIndices.joinToString(", ") { "F$it" }}")
            appendLine()
            appendLine("This realigns feature distributions to match training data characteristics, significantly reducing drift.")
        }
    }
}

/**
 * Get detailed technical configuration as formatted text
 */
fun PatchConfiguration.getTechnicalDetails(): String {
    return when (this) {
        is PatchConfiguration.FeatureClipping -> buildString {
            appendLine("═══ FEATURE CLIPPING CONFIGURATION ═══")
            appendLine()
            appendLine("Total Features Affected: ${featureIndices.size}")
            appendLine()
            featureIndices.forEachIndexed { i, featureIdx ->
                appendLine("Feature $featureIdx:")
                appendLine("  Min Value: ${String.format("%.6f", minValues[i])}")
                appendLine("  Max Value: ${String.format("%.6f", maxValues[i])}")
                appendLine("  Range: ${String.format("%.6f", maxValues[i] - minValues[i])}")
                if (i < featureIndices.size - 1) appendLine()
            }
        }

        is PatchConfiguration.FeatureReweighting -> buildString {
            appendLine("═══ FEATURE REWEIGHTING CONFIGURATION ═══")
            appendLine()
            appendLine("Total Features Affected: ${featureIndices.size}")
            appendLine()
            featureIndices.forEachIndexed { i, featureIdx ->
                val change = ((newWeights[i] / originalWeights[i] - 1) * 100)
                val changeStr = if (change > 0) "+${
                    String.format(
                        "%.1f",
                        change
                    )
                }%" else "${String.format("%.1f", change)}%"
                appendLine("Feature $featureIdx:")
                appendLine("  Original Weight: ${String.format("%.6f", originalWeights[i])}")
                appendLine("  New Weight: ${String.format("%.6f", newWeights[i])}")
                appendLine("  Change: $changeStr")
                if (i < featureIndices.size - 1) appendLine()
            }
        }

        is PatchConfiguration.ThresholdTuning -> buildString {
            appendLine("═══ THRESHOLD TUNING CONFIGURATION ═══")
            appendLine()
            appendLine("Original Threshold: ${String.format("%.6f", originalThreshold)}")
            appendLine("New Threshold: ${String.format("%.6f", newThreshold)}")
            appendLine(
                "Absolute Change: ${
                    String.format(
                        "%.6f",
                        newThreshold - originalThreshold
                    )
                }"
            )
            appendLine(
                "Relative Change: ${
                    String.format(
                        "%.2f%%",
                        (newThreshold / originalThreshold - 1) * 100
                    )
                }"
            )
            appendLine()
            appendLine("Impact:")
            if (newThreshold > originalThreshold) {
                appendLine("  • Fewer positive predictions (more conservative)")
                appendLine("  • Higher precision, potentially lower recall")
            } else {
                appendLine("  • More positive predictions (more aggressive)")
                appendLine("  • Higher recall, potentially lower precision")
            }
        }

        is PatchConfiguration.NormalizationUpdate -> buildString {
            appendLine("═══ NORMALIZATION UPDATE CONFIGURATION ═══")
            appendLine()
            appendLine("Total Features Affected: ${featureIndices.size}")
            appendLine()
            featureIndices.forEachIndexed { i, featureIdx ->
                appendLine("Feature $featureIdx:")
                appendLine("  Original Mean: ${String.format("%.6f", originalMeans[i])}")
                appendLine("  New Mean: ${String.format("%.6f", newMeans[i])}")
                appendLine("  Mean Shift: ${String.format("%.6f", newMeans[i] - originalMeans[i])}")
                appendLine("  Original Std: ${String.format("%.6f", originalStds[i])}")
                appendLine("  New Std: ${String.format("%.6f", newStds[i])}")
                appendLine(
                    "  Std Ratio: ${
                        String.format(
                            "%.3fx",
                            newStds[i] / originalStds[i].coerceAtLeast(0.0001f)
                        )
                    }"
                )
                if (i < featureIndices.size - 1) appendLine()
            }
        }
    }
}

/**
 * Export patch configuration to JSON format
 */
fun PatchConfiguration.toJSON(): JSONObject {
    return when (this) {
        is PatchConfiguration.FeatureClipping -> JSONObject().apply {
            put("type", "feature_clipping")
            put("feature_count", featureIndices.size)
            put("features", JSONArray().apply {
                featureIndices.forEachIndexed { i, idx ->
                    put(JSONObject().apply {
                        put("feature_index", idx)
                        put("min_value", minValues[i].toDouble())
                        put("max_value", maxValues[i].toDouble())
                        put("range", (maxValues[i] - minValues[i]).toDouble())
                    })
                }
            })
        }

        is PatchConfiguration.FeatureReweighting -> JSONObject().apply {
            put("type", "feature_reweighting")
            put("feature_count", featureIndices.size)
            put("features", JSONArray().apply {
                featureIndices.forEachIndexed { i, idx ->
                    put(JSONObject().apply {
                        put("feature_index", idx)
                        put("original_weight", originalWeights[i].toDouble())
                        put("new_weight", newWeights[i].toDouble())
                        put(
                            "weight_change_percent",
                            ((newWeights[i] / originalWeights[i] - 1) * 100).toDouble()
                        )
                    })
                }
            })
        }

        is PatchConfiguration.ThresholdTuning -> JSONObject().apply {
            put("type", "threshold_tuning")
            put("original_threshold", originalThreshold.toDouble())
            put("new_threshold", newThreshold.toDouble())
            put("absolute_change", (newThreshold - originalThreshold).toDouble())
            put(
                "relative_change_percent",
                ((newThreshold / originalThreshold - 1) * 100).toDouble()
            )
        }

        is PatchConfiguration.NormalizationUpdate -> JSONObject().apply {
            put("type", "normalization_update")
            put("feature_count", featureIndices.size)
            put("features", JSONArray().apply {
                featureIndices.forEachIndexed { i, idx ->
                    put(JSONObject().apply {
                        put("feature_index", idx)
                        put("original_mean", originalMeans[i].toDouble())
                        put("new_mean", newMeans[i].toDouble())
                        put("mean_shift", (newMeans[i] - originalMeans[i]).toDouble())
                        put("original_std", originalStds[i].toDouble())
                        put("new_std", newStds[i].toDouble())
                        put(
                            "std_ratio",
                            (newStds[i] / originalStds[i].coerceAtLeast(0.0001f)).toDouble()
                        )
                    })
                }
            })
        }
    }
}

/**
 * Export complete patch to JSON including metadata
 */
fun Patch.toJSON(): JSONObject {
    return JSONObject().apply {
        put("patch_id", id)
        put("model_id", modelId)
        put("drift_result_id", driftResultId)
        put("patch_type", patchType.name)
        put("status", status.name)
        put("created_at", createdAt.toString())
        appliedAt?.let { put("applied_at", it.toString()) }
        rolledBackAt?.let { put("rolled_back_at", it.toString()) }
        put("configuration", configuration.toJSON())
        validationResult?.let {
            put("validation", JSONObject().apply {
                put("is_valid", it.isValid)
                put("accuracy", it.metrics.accuracy)
                put("f1_score", it.metrics.f1Score)
                put("safety_score", it.metrics.safetyScore)
                put("drift_reduction", it.metrics.driftReduction)
                put("performance_delta", it.metrics.performanceDelta)
                if (it.errors.isNotEmpty()) {
                    put("errors", JSONArray(it.errors))
                }
            })
        }
        metadata?.let { meta ->
            put("metadata", JSONObject().apply {
                meta.forEach { (key, value) ->
                    put(key, value)
                }
            })
        }
    }
}

/**
 * Export patch to human-readable text format
 */
fun Patch.toReadableText(): String {
    return buildString {
        appendLine("╔═══════════════════════════════════════════════════════════════╗")
        appendLine("║               DRIFT PATCH CONFIGURATION                       ║")
        appendLine("╚═══════════════════════════════════════════════════════════════╝")
        appendLine()
        appendLine("PATCH INFORMATION")
        appendLine("─".repeat(65))
        appendLine("Patch ID: $id")
        appendLine("Model ID: $modelId")
        appendLine("Drift Result ID: $driftResultId")
        appendLine("Type: ${patchType.name}")
        appendLine("Status: ${status.name}")
        appendLine("Created: $createdAt")
        appliedAt?.let { appendLine("Applied: $it") }
        rolledBackAt?.let { appendLine("Rolled Back: $it") }
        appendLine()

        // Metadata
        if (!metadata.isNullOrEmpty()) {
            appendLine("METADATA")
            appendLine("─".repeat(65))
            metadata.forEach { (key, value) ->
                appendLine("$key: $value")
            }
            appendLine()
        }

        // Configuration
        appendLine("CONFIGURATION")
        appendLine("─".repeat(65))
        appendLine(configuration.getTechnicalDetails())
        appendLine()

        // Validation
        validationResult?.let { validation ->
            appendLine("VALIDATION RESULTS")
            appendLine("─".repeat(65))
            appendLine("Valid: ${if (validation.isValid) "YES ✓" else "NO ✗"}")
            appendLine("Accuracy: ${String.format("%.4f", validation.metrics.accuracy)}")
            appendLine("F1 Score: ${String.format("%.4f", validation.metrics.f1Score)}")
            appendLine("Safety Score: ${String.format("%.4f", validation.metrics.safetyScore)}")
            appendLine(
                "Drift Reduction: ${
                    String.format(
                        "%.2f%%",
                        validation.metrics.driftReduction * 100
                    )
                }"
            )
            appendLine(
                "Performance Delta: ${
                    String.format(
                        "%.4f",
                        validation.metrics.performanceDelta
                    )
                }"
            )
            if (validation.errors.isNotEmpty()) {
                appendLine()
                appendLine("Errors:")
                validation.errors.forEach { error ->
                    appendLine("  • $error")
                }
            }
            appendLine()
        }

        appendLine("═".repeat(65))
        appendLine("End of patch configuration")
        appendLine("═".repeat(65))
    }
}
