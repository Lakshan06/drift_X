package com.driftdetector.app.data.repository

import com.driftdetector.app.core.drift.AttributionEngine
import com.driftdetector.app.core.drift.DriftDetector
import com.driftdetector.app.core.ml.TFLiteModelInference
import com.driftdetector.app.core.patch.PatchEngine
import com.driftdetector.app.core.patch.PatchSynthesizer
import com.driftdetector.app.core.patch.PatchValidator
import com.driftdetector.app.data.local.dao.*
import com.driftdetector.app.data.local.entity.*
import com.driftdetector.app.data.mapper.toDomain
import com.driftdetector.app.data.mapper.toEntity
import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant

/**
 * Main repository for drift detection and patch management
 */
class DriftRepository(
    private val driftResultDao: DriftResultDao,
    private val mlModelDao: MLModelDao,
    private val patchDao: PatchDao,
    private val patchSnapshotDao: PatchSnapshotDao,
    private val modelPredictionDao: ModelPredictionDao,
    private val driftDetector: DriftDetector,
    private val patchSynthesizer: PatchSynthesizer,
    private val patchValidator: PatchValidator,
    private val patchEngine: PatchEngine,
    private val attributionEngine: AttributionEngine
) {

    // ========== Drift Detection ==========

    suspend fun detectDrift(
        modelId: String,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>,
        featureNames: List<String>
    ): DriftResult {
        val result = driftDetector.detectDrift(modelId, referenceData, currentData, featureNames)
        driftResultDao.insertDriftResult(result.toEntity())
        return result
    }

    fun getDriftResultsByModel(modelId: String): Flow<List<DriftResult>> {
        return driftResultDao.getDriftResultsByModel(modelId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun getRecentDrifts(limit: Int = 50): Flow<List<DriftResult>> {
        return driftResultDao.getRecentDrifts(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getDriftCount(modelId: String): Int {
        return driftResultDao.getDriftCount(modelId)
    }

    // ========== Attribution ==========

    suspend fun calculateAttributions(
        featureDrifts: List<FeatureDrift>,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>
    ): Map<Int, Double> {
        return attributionEngine.calculateAttributions(featureDrifts, referenceData, currentData)
    }

    // ========== Patch Management ==========

    suspend fun synthesizePatch(
        modelId: String,
        driftResult: DriftResult,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>
    ): Patch {
        val patch =
            patchSynthesizer.synthesizePatch(modelId, driftResult, referenceData, currentData)
        patchDao.insertPatch(patch.toEntity())
        return patch
    }

    suspend fun validatePatch(
        patch: Patch,
        validationData: List<FloatArray>,
        validationLabels: List<Int>,
        baselineMetrics: ValidationMetrics? = null
    ): ValidationResult {
        return patchValidator.validate(patch, validationData, validationLabels, baselineMetrics)
    }

    suspend fun applyPatch(patchId: String): Result<Patch> {
        return try {
            val patchEntity = patchDao.getPatchById(patchId)
                ?: return Result.failure(Exception("Patch not found"))

            val patch = patchEntity.toDomain()

            // Create snapshot before applying
            val snapshot = patchEngine.createSnapshot(patch)
            patchSnapshotDao.insertSnapshot(snapshot.toEntity())

            // Update patch status
            patchDao.updatePatchStatus(
                patchId,
                PatchStatus.APPLIED.name,
                Instant.now().toEpochMilli()
            )

            Result.success(patch.copy(status = PatchStatus.APPLIED, appliedAt = Instant.now()))
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply patch")
            Result.failure(e)
        }
    }

    suspend fun rollbackPatch(patchId: String): Result<Patch> {
        return try {
            val patchEntity = patchDao.getPatchById(patchId)
                ?: return Result.failure(Exception("Patch not found"))

            patchDao.rollbackPatch(patchId, Instant.now().toEpochMilli())

            val patch = patchEntity.toDomain()
            Result.success(patch.copy(status = PatchStatus.ROLLED_BACK))
        } catch (e: Exception) {
            Timber.e(e, "Failed to rollback patch")
            Result.failure(e)
        }
    }

    fun getPatchesByModel(modelId: String): Flow<List<Patch>> {
        return patchDao.getPatchesByModel(modelId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun getAppliedPatches(modelId: String): Flow<List<Patch>> {
        return patchDao.getAppliedPatches(modelId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    // ========== Model Management ==========

    suspend fun registerModel(model: MLModel) {
        mlModelDao.insertModel(model.toEntity())
    }

    fun getActiveModels(): Flow<List<MLModel>> {
        return mlModelDao.getActiveModels()
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getModelById(modelId: String): MLModel? {
        return mlModelDao.getModelById(modelId)?.toDomain()
    }

    suspend fun deactivateModel(modelId: String) {
        mlModelDao.deactivateModel(modelId)
    }

    // ========== Predictions ==========

    suspend fun savePrediction(prediction: ModelPrediction, modelId: String) {
        modelPredictionDao.insertPrediction(prediction.toEntity(modelId))
    }

    fun getRecentPredictions(modelId: String, startTime: Instant): Flow<List<ModelPrediction>> {
        return modelPredictionDao.getRecentPredictions(modelId, startTime.toEpochMilli())
            .map { entities -> entities.map { it.toDomain() } }
    }

    // ========== Cleanup ==========

    suspend fun cleanupOldData(cutoffTime: Instant) {
        val cutoffMillis = cutoffTime.toEpochMilli()
        driftResultDao.deleteOldResults(cutoffMillis)
        patchSnapshotDao.deleteOldSnapshots(cutoffMillis)
        modelPredictionDao.deleteOldPredictions(cutoffMillis)
    }
}
