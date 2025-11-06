package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.patch.IntelligentPatchGenerator
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for drift dashboard screen with intelligent auto-patching
 */
class DriftDashboardViewModel(
    private val repository: DriftRepository,
    private val intelligentPatchGenerator: IntelligentPatchGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriftDashboardState>(DriftDashboardState.Loading)
    val uiState: StateFlow<DriftDashboardState> = _uiState.asStateFlow()

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

        // Load models in a safe way
        viewModelScope.launch {
            try {
                loadActiveModels()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize dashboard")
                _uiState.value = DriftDashboardState.Error(
                    e.message ?: "Failed to initialize dashboard"
                )
            }
        }
    }

    fun loadActiveModels() {
        viewModelScope.launch {
            try {
                Timber.d("Loading active models...")
                repository.getActiveModels().collect { models ->
                    Timber.d("Found ${models.size} active models")
                    if (models.isEmpty()) {
                        _uiState.value = DriftDashboardState.Empty
                    } else {
                        if (_selectedModel.value == null && models.isNotEmpty()) {
                            _selectedModel.value = models.first()
                        }
                        loadDriftResults()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                _uiState.value = DriftDashboardState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectModel(model: MLModel) {
        _selectedModel.value = model
        loadDriftResults()
    }

    private fun loadDriftResults() {
        viewModelScope.launch {
            try {
                _selectedModel.value?.let { model ->
                    repository.getDriftResultsByModel(model.id).collect { results ->
                        val driftCount = repository.getDriftCount(model.id)
                        _uiState.value = DriftDashboardState.Success(
                            model = model,
                            driftResults = results,
                            totalDrifts = driftCount
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load drift results")
                _uiState.value = DriftDashboardState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        loadDriftResults()
    }

    fun toggleAutoPatch() {
        _autoPatchEnabled.value = !_autoPatchEnabled.value
        Timber.i("Auto-patch ${if (_autoPatchEnabled.value) "enabled" else "disabled"}")
    }

    /**
     * Generate comprehensive patches for a drift result with intelligent strategies
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

                // Use intelligent patch generator
                val patches = intelligentPatchGenerator.generateComprehensivePatches(
                    modelId = driftResult.modelId,
                    driftResult = driftResult,
                    referenceData = referenceData,
                    currentData = currentData
                )

                Timber.i("✅ Generated ${patches.size} comprehensive patches")

                // Validate and potentially auto-apply patches
                var successCount = 0
                var failCount = 0

                patches.forEach { patch ->
                    try {
                        // Validate each patch
                        val validationData = generateSampleData(driftResult.featureDrifts.size, 50)
                        val validationLabels = List(50) { (0..1).random() }

                        val validationResult = repository.validatePatch(
                            patch = patch,
                            validationData = validationData,
                            validationLabels = validationLabels
                        )

                        Timber.i("   Patch ${patch.patchType}: valid=${validationResult.isValid}, safety=${validationResult.metrics.safetyScore}")

                        // Auto-apply if enabled and patch is safe
                        if (_autoPatchEnabled.value &&
                            validationResult.isValid &&
                            validationResult.metrics.safetyScore > 0.7 &&
                            validationResult.metrics.driftReduction > 0.1
                        ) {

                            val applyResult = repository.applyPatch(patch.id)
                            if (applyResult.isSuccess) {
                                successCount++
                                Timber.i("   ✅ Auto-applied patch: ${patch.patchType}")
                            } else {
                                failCount++
                                Timber.w("   ❌ Failed to apply patch: ${patch.patchType}")
                            }
                        }

                    } catch (e: Exception) {
                        Timber.e(e, "Failed to validate/apply patch: ${patch.patchType}")
                        failCount++
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
                _patchGenerationState.value =
                    PatchGenerationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Manually apply a specific patch
     */
    fun applyPatch(patchId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Applying patch: $patchId")
                val result = repository.applyPatch(patchId)

                if (result.isSuccess) {
                    Timber.i("✅ Patch applied successfully")
                    refresh()
                } else {
                    Timber.e("❌ Failed to apply patch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error applying patch")
            }
        }
    }

    /**
     * Rollback a patch
     */
    fun rollbackPatch(patchId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Rolling back patch: $patchId")
                val result = repository.rollbackPatch(patchId)

                if (result.isSuccess) {
                    Timber.i("✅ Patch rolled back successfully")
                    refresh()
                } else {
                    Timber.e("❌ Failed to rollback patch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rolling back patch")
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
        val totalDrifts: Int
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
