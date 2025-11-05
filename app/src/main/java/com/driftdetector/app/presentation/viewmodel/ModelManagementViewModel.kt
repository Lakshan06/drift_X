package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.MLModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.UUID

/**
 * ViewModel for model management screen
 */
class ModelManagementViewModel(
    private val repository: DriftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModelManagementState>(ModelManagementState.Loading)
    val uiState: StateFlow<ModelManagementState> = _uiState.asStateFlow()

    init {
        // Set loading state explicitly
        _uiState.value = ModelManagementState.Loading

        // Load models safely
        viewModelScope.launch {
            try {
                loadModels()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize model management")
                _uiState.value = ModelManagementState.Error(
                    e.message ?: "Failed to load models"
                )
            }
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            try {
                Timber.d("Loading all models...")
                repository.getActiveModels().collect { models ->
                    Timber.d("Loaded ${models.size} models")
                    if (models.isEmpty()) {
                        _uiState.value = ModelManagementState.Success(emptyList())
                    } else {
                        _uiState.value = ModelManagementState.Success(models)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                _uiState.value = ModelManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun registerModel(model: MLModel) {
        viewModelScope.launch {
            try {
                repository.registerModel(model)
                Timber.d("Model registered: ${model.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to register model")
                _uiState.value = ModelManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun registerModel(
        name: String,
        version: String,
        modelPath: String,
        inputFeatures: List<String>,
        outputLabels: List<String>
    ) {
        viewModelScope.launch {
            try {
                val model = MLModel(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    version = version,
                    modelPath = modelPath,
                    inputFeatures = inputFeatures,
                    outputLabels = outputLabels,
                    createdAt = Instant.now(),
                    lastUpdated = Instant.now(),
                    isActive = true
                )
                repository.registerModel(model)
                Timber.d("Model registered: ${model.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to register model")
                _uiState.value = ModelManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deactivateModel(modelId: String) {
        viewModelScope.launch {
            try {
                repository.deactivateModel(modelId)
                Timber.d("Model deactivated: $modelId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to deactivate model")
                _uiState.value = ModelManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI state for model management
 */
sealed class ModelManagementState {
    object Loading : ModelManagementState()
    object Empty : ModelManagementState()
    data class Success(val models: List<MLModel>) : ModelManagementState()
    data class Error(val message: String) : ModelManagementState()
}
