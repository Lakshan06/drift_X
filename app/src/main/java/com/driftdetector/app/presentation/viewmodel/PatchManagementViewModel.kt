package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for patch management screen
 */
class PatchManagementViewModel(
    private val repository: DriftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PatchManagementState>(PatchManagementState.Loading)
    val uiState: StateFlow<PatchManagementState> = _uiState.asStateFlow()

    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()

    fun loadPatches(modelId: String) {
        _selectedModelId.value = modelId
        viewModelScope.launch {
            try {
                repository.getPatchesByModel(modelId).collect { patches ->
                    val appliedCount = patches.count { it.status == PatchStatus.APPLIED }
                    _uiState.value = PatchManagementState.Success(
                        patches = patches,
                        appliedCount = appliedCount
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load patches")
                _uiState.value = PatchManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun applyPatch(patchId: String) {
        viewModelScope.launch {
            try {
                val result = repository.applyPatch(patchId)
                result.onSuccess {
                    Timber.d("Patch applied successfully: $patchId")
                    _selectedModelId.value?.let { loadPatches(it) }
                }.onFailure { error ->
                    Timber.e(error, "Failed to apply patch")
                    _uiState.value = PatchManagementState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to apply patch")
                _uiState.value = PatchManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun rollbackPatch(patchId: String) {
        viewModelScope.launch {
            try {
                val result = repository.rollbackPatch(patchId)
                result.onSuccess {
                    Timber.d("Patch rolled back successfully: $patchId")
                    _selectedModelId.value?.let { loadPatches(it) }
                }.onFailure { error ->
                    Timber.e(error, "Failed to rollback patch")
                    _uiState.value = PatchManagementState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to rollback patch")
                _uiState.value = PatchManagementState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI state for patch management
 */
sealed class PatchManagementState {
    object Loading : PatchManagementState()
    data class Success(
        val patches: List<Patch>,
        val appliedCount: Int
    ) : PatchManagementState()

    data class Error(val message: String) : PatchManagementState()
}
