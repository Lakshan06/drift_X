package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.error.ErrorHandler
import com.driftdetector.app.core.error.RetryPolicies
import com.driftdetector.app.core.error.retryWithExponentialBackoff
import com.driftdetector.app.core.patch.IntelligentPatchGenerator
import com.driftdetector.app.core.notifications.DriftNotificationManager
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for drift dashboard screen with intelligent auto-patching and multi-model support
 * Enhanced with error handling and retry logic
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class DriftDashboardViewModel(
    val repository: DriftRepository,
    private val intelligentPatchGenerator: IntelligentPatchGenerator,
    private val notificationManager: DriftNotificationManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriftDashboardState>(DriftDashboardState.Loading)
    val uiState: StateFlow<DriftDashboardState> = _uiState.asStateFlow()

    private val _allModels = MutableStateFlow<List<MLModel>>(emptyList())
    val allModels: StateFlow<List<MLModel>> = _allModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<MLModel?>(null)
    val selectedModel: StateFlow<MLModel?> = _selectedModel.asStateFlow()

    private val _patchGenerationState =
        MutableStateFlow<PatchGenerationState>(PatchGenerationState.Idle)
    val patchGenerationState: StateFlow<PatchGenerationState> = _patchGenerationState.asStateFlow()

    private val _autoPatchEnabled = MutableStateFlow(true)
    val autoPatchEnabled: StateFlow<Boolean> = _autoPatchEnabled.asStateFlow()

    init {
        // Set loading state first
        _uiState.value = DriftDashboardState.Loading

        // Load models in a safe way with error handling
        viewModelScope.launch {
            val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                loadActiveModels()
            }

            result.onFailure { error ->
                Timber.e(error, "Failed to initialize dashboard")
                errorHandler.handleError(error)
                _uiState.value = DriftDashboardState.Error(
                    error.message ?: "Failed to initialize dashboard"
                )
            }
        }
    }

    fun loadActiveModels() {
        viewModelScope.launch {
            try {
                Timber.d("Loading active models...")
                repository.getActiveModels()
                    .debounce(300) // Prevent rapid updates
                    .catch { error ->
                        Timber.e(error, "Error in models flow")
                        errorHandler.handleError(error)
                        emit(emptyList()) // Emit empty list to prevent crash
                    }
                    .collect { models ->
                        Timber.d("Found ${models.size} active models")
                        _allModels.value = models

                        if (models.isEmpty()) {
                            _uiState.value = DriftDashboardState.Empty
                        } else {
                            // Auto-select first model if none selected
                            if (_selectedModel.value == null ||
                                !models.any { it.id == _selectedModel.value?.id }
                            ) {
                                _selectedModel.value = models.first()
                            }
                            loadDriftResults()
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                errorHandler.handleError(e)
                _uiState.value = DriftDashboardState.Error(e.message ?: "Failed to load models")
            }
        }
    }

    fun selectModel(model: MLModel) {
        Timber.d("Switching to model: ${model.name}")
        _selectedModel.value = model
        loadDriftResults()
    }

    private fun loadDriftResults() {
        viewModelScope.launch {
            try {
                _selectedModel.value?.let { model ->
                    repository.getDriftResultsByModel(model.id)
                        .debounce(300) // Prevent rapid updates
                        .catch { error ->
                            Timber.e(error, "Error loading drift results")
                            errorHandler.handleError(error)
                            emit(emptyList())
                        }
                        .collect { results ->
                            val driftCount = repository.getDriftCount(model.id)
                            _uiState.value = DriftDashboardState.Success(
                                model = model,
                                driftResults = results,
                                totalDrifts = driftCount,
                                allModelsCount = _allModels.value.size
                            )
                        }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load drift results")
                errorHandler.handleError(e)
                _uiState.value =
                    DriftDashboardState.Error(e.message ?: "Failed to load drift results")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DriftDashboardState.Loading

            val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                loadDriftResults()
            }

            result.onFailure { error ->
                errorHandler.handleError(error)
                _uiState.value = DriftDashboardState.Error(
                    error.message ?: "Failed to refresh"
                )
            }
        }
    }

    fun toggleAutoPatch() {
        _autoPatchEnabled.value = !_autoPatchEnabled.value
        Timber.i("Auto-patch ${if (_autoPatchEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Generate comprehensive patches for a drift result with intelligent strategies
     * Enhanced with error handling and retry logic
     */
    fun generatePatch(driftResult: DriftResult) {
        viewModelScope.launch {
            try {
                _patchGenerationState.value = PatchGenerationState.Loading

                Timber.d("🔧 Generating intelligent patches for drift result: ${driftResult.id}")
                Timber.d("   Drift Type: ${driftResult.driftType}")
                Timber.d("   Drift Score: ${driftResult.driftScore}")

                // Create sample reference and current data for patch synthesis
                val referenceData = generateSampleData(driftResult.featureDrifts.size, 100)
                val currentData = generateSampleData(driftResult.featureDrifts.size, 100)

                // Use retry logic for patch generation
                val patchResult = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    intelligentPatchGenerator.generateComprehensivePatches(
                        modelId = driftResult.modelId,
                        driftResult = driftResult,
                        referenceData = referenceData,
                        currentData = currentData
                    )
                }

                if (patchResult.isFailure) {
                    val error =
                        patchResult.exceptionOrNull() ?: Exception("Patch generation failed")
                    Timber.e(error, "Failed to generate patches")
                    errorHandler.handleError(error)
                    _patchGenerationState.value =
                        PatchGenerationState.Error(error.message ?: "Failed to generate patches")
                    return@launch
                }

                val patches = patchResult.getOrThrow()
                Timber.i("✅ Generated ${patches.size} comprehensive patches")

                // Get model name for notifications
                val model = repository.getModelById(driftResult.modelId)

                // Validate and potentially auto-apply patches
                var successCount = 0
                var failCount = 0
                val safePatches = mutableListOf<Patch>()

                patches.forEach { patch ->
                    try {
                        // Validate each patch with retry logic
                        val validationData = generateSampleData(driftResult.featureDrifts.size, 50)
                        val validationLabels = List(50) { (0..1).random() }

                        val validationResult = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                            repository.validatePatch(
                                patch = patch,
                                validationData = validationData,
                                validationLabels = validationLabels
                            )
                        }.getOrThrow()

                        val effectiveness = validationResult.metrics.driftReduction
                        Timber.i("   Patch ${patch.patchType}: valid=${validationResult.isValid}, safety=${validationResult.metrics.safetyScore}, effectiveness=$effectiveness")

                        // Save patch to database with validation result
                        val validatedPatch = patch.copy(
                            status = if (validationResult.isValid) com.driftdetector.app.domain.model.PatchStatus.VALIDATED else com.driftdetector.app.domain.model.PatchStatus.CREATED,
                            validationResult = validationResult
                        )
                        
                        repository.savePatch(validatedPatch)
                        Timber.i("   💾 Patch saved to database: ${validatedPatch.patchType}")

                        if (validationResult.isValid) {
                            safePatches.add(validatedPatch)
                        }

                        // Auto-apply if enabled and patch is safe
                        if (_autoPatchEnabled.value &&
                            validationResult.isValid &&
                            validationResult.metrics.safetyScore > 0.7 &&
                            effectiveness > 0.1
                        ) {
                            // Apply patch with retry logic
                            val applyResult = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                                repository.applyPatch(validatedPatch.id)
                            }

                            if (applyResult.isSuccess && applyResult.getOrNull()?.isSuccess == true) {
                                successCount++
                                Timber.i("   ✅ Auto-applied patch: ${validatedPatch.patchType}")
                            } else {
                                failCount++
                                Timber.w("   ❌ Failed to apply patch: ${validatedPatch.patchType}")
                            }
                        }

                    } catch (e: Exception) {
                        Timber.e(e, "Failed to validate/apply patch: ${patch.patchType}")
                        errorHandler.handleError(e)
                        failCount++
                    }
                }

                // Send ONE summary notification instead of spamming
                if (safePatches.isNotEmpty()) {
                    model?.let {
                        notificationManager.showPatchesSynthesizedSummary(
                            modelName = it.name,
                            totalPatches = safePatches.size,
                            appliedPatches = successCount
                        )
                    }
                }

                _patchGenerationState.value = PatchGenerationState.Success(
                    totalGenerated = patches.size,
                    autoApplied = successCount,
                    failed = failCount
                )

                // Refresh to show updated patches
                refresh()

            } catch (e: Exception) {
                Timber.e(e, "Failed to generate patches")
                errorHandler.handleError(e)
                _patchGenerationState.value =
                    PatchGenerationState.Error(e.message ?: "Failed to generate patches")
            }
        }
    }

    /**
     * Manually apply a specific patch with retry logic
     */
    fun applyPatch(patchId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Applying patch: $patchId")

                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    repository.applyPatch(patchId)
                }

                result.fold(
                    onSuccess = { applyResult ->
                        if (applyResult.isSuccess) {
                            Timber.i("✅ Patch applied successfully")
                            refresh()
                        } else {
                            Timber.e("❌ Failed to apply patch")
                            errorHandler.handleError(Exception("Failed to apply patch"))
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Error applying patch")
                        errorHandler.handleError(error)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error applying patch")
                errorHandler.handleError(e)
            }
        }
    }

    /**
     * Rollback a patch with retry logic
     */
    fun rollbackPatch(patchId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Rolling back patch: $patchId")

                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    repository.rollbackPatch(patchId)
                }

                result.fold(
                    onSuccess = { rollbackResult ->
                        if (rollbackResult.isSuccess) {
                            Timber.i("✅ Patch rolled back successfully")
                            refresh()
                        } else {
                            Timber.e("❌ Failed to rollback patch")
                            errorHandler.handleError(Exception("Failed to rollback patch"))
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Error rolling back patch")
                        errorHandler.handleError(error)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error rolling back patch")
                errorHandler.handleError(e)
            }
        }
    }

    /**
     * Generate sample data for demonstration
     * In production, this would be replaced with actual stored data
     */
    private fun generateSampleData(numFeatures: Int, numSamples: Int): List<FloatArray> {
        return List(numSamples) {
            FloatArray(numFeatures) { (Math.random() * 10).toFloat() }
        }
    }

    /**
     * Get model summary statistics with error handling
     */
    fun getModelSummary(modelId: String): Flow<ModelSummary> = flow {
        try {
            val driftCount = repository.getDriftCount(modelId)
            val patches = repository.getPatchesByModel(modelId).first()
            val appliedPatches =
                patches.count { it.status == com.driftdetector.app.domain.model.PatchStatus.APPLIED }

            emit(
                ModelSummary(
                    totalDrifts = driftCount,
                    totalPatches = patches.size,
                    appliedPatches = appliedPatches
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get model summary")
            errorHandler.handleError(e)
            emit(ModelSummary(0, 0, 0))
        }
    }.catch { error ->
        Timber.e(error, "Error in model summary flow")
        errorHandler.handleError(error as? Exception ?: Exception(error))
        emit(ModelSummary(0, 0, 0))
    }
}

/**
 * UI state for drift dashboard
 */
sealed class DriftDashboardState {
    object Loading : DriftDashboardState()
    object Empty : DriftDashboardState()
    data class Success(
        val model: MLModel,
        val driftResults: List<DriftResult>,
        val totalDrifts: Int,
        val allModelsCount: Int = 1
    ) : DriftDashboardState()

    data class Error(val message: String) : DriftDashboardState()
}

sealed class PatchGenerationState {
    object Idle : PatchGenerationState()
    object Loading : PatchGenerationState()
    data class Success(
        val totalGenerated: Int,
        val autoApplied: Int,
        val failed: Int
    ) : PatchGenerationState()
    data class Error(val message: String) : PatchGenerationState()
}

/**
 * Model summary statistics
 */
data class ModelSummary(
    val totalDrifts: Int,
    val totalPatches: Int,
    val appliedPatches: Int
)
