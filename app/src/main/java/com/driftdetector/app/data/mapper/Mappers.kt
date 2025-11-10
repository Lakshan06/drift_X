package com.driftdetector.app.data.mapper

import com.driftdetector.app.data.local.entity.*
import com.driftdetector.app.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

private val gson = Gson()

// ========== DriftResult Mappers ==========

fun DriftResult.toEntity(): DriftResultEntity {
    return DriftResultEntity(
        id = id,
        modelId = modelId,
        timestamp = timestamp.toEpochMilli(),
        driftType = driftType,
        driftScore = driftScore,
        threshold = threshold,
        isDriftDetected = isDriftDetected,
        featureDriftsJson = gson.toJson(featureDrifts),
        statisticalTestsJson = gson.toJson(statisticalTests),
        metadataJson = gson.toJson(metadata)
    )
}

fun DriftResultEntity.toDomain(): DriftResult {
    val featureDriftsType = object : TypeToken<List<FeatureDrift>>() {}.type
    val statsTestsType = object : TypeToken<List<StatisticalTestResult>>() {}.type
    val metadataType = object : TypeToken<Map<String, Any>>() {}.type

    return DriftResult(
        id = id,
        modelId = modelId,
        timestamp = Instant.ofEpochMilli(timestamp),
        driftType = driftType,
        driftScore = driftScore,
        threshold = threshold,
        isDriftDetected = isDriftDetected,
        featureDrifts = gson.fromJson(featureDriftsJson, featureDriftsType),
        statisticalTests = gson.fromJson(statisticalTestsJson, statsTestsType),
        metadata = gson.fromJson(metadataJson, metadataType) ?: emptyMap()
    )
}

// ========== MLModel Mappers ==========

fun MLModel.toEntity(): MLModelEntity {
    return MLModelEntity(
        id = id,
        name = name,
        version = version,
        modelPath = modelPath,
        inputFeaturesJson = gson.toJson(inputFeatures),
        outputLabelsJson = gson.toJson(outputLabels),
        createdAt = createdAt.toEpochMilli(),
        lastUpdated = lastUpdated.toEpochMilli(),
        isActive = isActive
    )
}

fun MLModelEntity.toDomain(): MLModel {
    val stringListType = object : TypeToken<List<String>>() {}.type

    return MLModel(
        id = id,
        name = name,
        version = version,
        modelPath = modelPath,
        inputFeatures = gson.fromJson(inputFeaturesJson, stringListType),
        outputLabels = gson.fromJson(outputLabelsJson, stringListType),
        createdAt = Instant.ofEpochMilli(createdAt),
        lastUpdated = Instant.ofEpochMilli(lastUpdated),
        isActive = isActive
    )
}

// ========== Patch Mappers ==========

fun Patch.toEntity(): PatchEntity {
    return PatchEntity(
        id = id,
        modelId = modelId,
        driftResultId = driftResultId,
        patchType = patchType.name,
        status = status.name,
        createdAt = createdAt.toEpochMilli(),
        appliedAt = appliedAt?.toEpochMilli(),
        rolledBackAt = rolledBackAt?.toEpochMilli(),
        configurationJson = serializePatchConfiguration(configuration, patchType),
        validationResultJson = validationResult?.let { gson.toJson(it) },
        metadataJson = gson.toJson(metadata)
    )
}

fun PatchEntity.toDomain(): Patch {
    val patchTypeEnum = PatchType.valueOf(patchType)
    val configuration = deserializePatchConfiguration(configurationJson, patchTypeEnum)
    val validationResult = validationResultJson?.let {
        gson.fromJson(it, ValidationResult::class.java)
    }
    val metadataType = object : TypeToken<Map<String, Any>>() {}.type

    return Patch(
        id = id,
        modelId = modelId,
        driftResultId = driftResultId,
        patchType = patchTypeEnum,
        status = PatchStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAt),
        appliedAt = appliedAt?.let { Instant.ofEpochMilli(it) },
        rolledBackAt = rolledBackAt?.let { Instant.ofEpochMilli(it) },
        configuration = configuration,
        validationResult = validationResult,
        metadata = gson.fromJson(metadataJson, metadataType) ?: emptyMap()
    )
}

/**
 * Serialize PatchConfiguration based on type
 */
private fun serializePatchConfiguration(
    configuration: PatchConfiguration,
    patchType: PatchType
): String {
    return when (configuration) {
        is PatchConfiguration.FeatureClipping -> gson.toJson(configuration)
        is PatchConfiguration.FeatureReweighting -> gson.toJson(configuration)
        is PatchConfiguration.ThresholdTuning -> gson.toJson(configuration)
        is PatchConfiguration.NormalizationUpdate -> gson.toJson(configuration)
    }
}

/**
 * Deserialize PatchConfiguration based on patch type
 * This prevents "abstract classes can't be instantiated" error
 */
private fun deserializePatchConfiguration(
    configurationJson: String,
    patchType: PatchType
): PatchConfiguration {
    return try {
        when (patchType) {
            PatchType.FEATURE_CLIPPING -> {
                gson.fromJson(configurationJson, PatchConfiguration.FeatureClipping::class.java)
            }

            PatchType.FEATURE_REWEIGHTING -> {
                gson.fromJson(configurationJson, PatchConfiguration.FeatureReweighting::class.java)
            }

            PatchType.THRESHOLD_TUNING -> {
                gson.fromJson(configurationJson, PatchConfiguration.ThresholdTuning::class.java)
            }

            PatchType.NORMALIZATION_UPDATE -> {
                gson.fromJson(configurationJson, PatchConfiguration.NormalizationUpdate::class.java)
            }

            PatchType.ENSEMBLE_REWEIGHT -> {
                // Fallback to FeatureReweighting for now
                gson.fromJson(configurationJson, PatchConfiguration.FeatureReweighting::class.java)
            }

            PatchType.CALIBRATION_ADJUST -> {
                // Fallback to ThresholdTuning for now
                gson.fromJson(configurationJson, PatchConfiguration.ThresholdTuning::class.java)
            }
        }
    } catch (e: Exception) {
        // Fallback to a default configuration if deserialization fails
        android.util.Log.e("Mappers", "Failed to deserialize PatchConfiguration: ${e.message}")
        // Return a safe default based on patch type
        when (patchType) {
            PatchType.FEATURE_CLIPPING -> PatchConfiguration.FeatureClipping(
                featureIndices = emptyList(),
                minValues = FloatArray(0),
                maxValues = FloatArray(0)
            )

            PatchType.FEATURE_REWEIGHTING, PatchType.ENSEMBLE_REWEIGHT -> PatchConfiguration.FeatureReweighting(
                featureIndices = emptyList(),
                originalWeights = FloatArray(0),
                newWeights = FloatArray(0)
            )

            PatchType.THRESHOLD_TUNING, PatchType.CALIBRATION_ADJUST -> PatchConfiguration.ThresholdTuning(
                originalThreshold = 0.5f,
                newThreshold = 0.5f
            )

            PatchType.NORMALIZATION_UPDATE -> PatchConfiguration.NormalizationUpdate(
                featureIndices = emptyList(),
                originalMeans = FloatArray(0),
                originalStds = FloatArray(0),
                newMeans = FloatArray(0),
                newStds = FloatArray(0)
            )
        }
    }
}

// ========== PatchSnapshot Mappers ==========

fun PatchSnapshot.toEntity(): PatchSnapshotEntity {
    return PatchSnapshotEntity(
        id = "$patchId-${timestamp.toEpochMilli()}",
        patchId = patchId,
        timestamp = timestamp.toEpochMilli(),
        preApplyState = preApplyState,
        postApplyState = postApplyState
    )
}

fun PatchSnapshotEntity.toDomain(): PatchSnapshot {
    return PatchSnapshot(
        patchId = patchId,
        timestamp = Instant.ofEpochMilli(timestamp),
        preApplyState = preApplyState,
        postApplyState = postApplyState
    )
}

// ========== ModelPrediction Mappers ==========

fun ModelPrediction.toEntity(modelId: String): ModelPredictionEntity {
    return ModelPredictionEntity(
        modelId = modelId,
        inputJson = gson.toJson(input),
        outputsJson = gson.toJson(outputs),
        predictedClass = predictedClass,
        confidence = confidence,
        timestamp = timestamp.toEpochMilli()
    )
}

fun ModelPredictionEntity.toDomain(): ModelPrediction {
    val input = gson.fromJson(inputJson, ModelInput::class.java)
    val outputsType = object : TypeToken<FloatArray>() {}.type
    val outputs: FloatArray = gson.fromJson(outputsJson, outputsType)

    return ModelPrediction(
        input = input,
        outputs = outputs,
        predictedClass = predictedClass,
        confidence = confidence,
        timestamp = Instant.ofEpochMilli(timestamp)
    )
}
