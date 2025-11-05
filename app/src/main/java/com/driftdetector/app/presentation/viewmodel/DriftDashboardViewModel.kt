package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for drift dashboard screen
 */
class DriftDashboardViewModel(
    private val repository: DriftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriftDashboardState>(DriftDashboardState.Loading)
    val uiState: StateFlow<DriftDashboardState> = _uiState.asStateFlow()

    private val _selectedModel = MutableStateFlow<MLModel?>(null)
    val selectedModel: StateFlow<MLModel?> = _selectedModel.asStateFlow()

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

    /**
     * Generate a patch for a specific drift result
     */
    fun generatePatch(driftResult: DriftResult) {
        viewModelScope.launch {
            try {
                Timber.d("Generating patch for drift result: ${driftResult.id}")

                // Create sample reference and current data for patch synthesis
                // In production, these would come from actual data storage
                val referenceData = generateSampleData(driftResult.featureDrifts.size, 100)
                val currentData = generateSampleData(driftResult.featureDrifts.size, 100)

                val patch = repository.synthesizePatch(
                    modelId = driftResult.modelId,
                    driftResult = driftResult,
                    referenceData = referenceData,
                    currentData = currentData
                )

                Timber.i("Patch generated successfully: ${patch.id}")

                // Optionally validate the patch
                val validationData = generateSampleData(driftResult.featureDrifts.size, 50)
                val validationLabels = List(50) { (0..1).random() }

                val validationResult = repository.validatePatch(
                    patch = patch,
                    validationData = validationData,
                    validationLabels = validationLabels
                )

                Timber.i("Patch validated: ${validationResult.isValid}")

            } catch (e: Exception) {
                Timber.e(e, "Failed to generate patch")
                _uiState.value = DriftDashboardState.Error("Failed to generate patch: ${e.message}")
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
