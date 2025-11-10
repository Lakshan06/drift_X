package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.error.ErrorHandler
import com.driftdetector.app.core.error.RetryPolicies
import com.driftdetector.app.core.error.retryWithExponentialBackoff
import com.driftdetector.app.core.export.PatchExportManager
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import com.driftdetector.app.domain.model.MLModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for patch management screen with multi-model support
 * Enhanced with error handling and retry logic
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class PatchManagementViewModel(
    private val repository: DriftRepository,
    private val exportManager: PatchExportManager,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow<PatchManagementState>(PatchManagementState.Loading)
    val uiState: StateFlow<PatchManagementState> = _uiState.asStateFlow()

    private val _allModels = MutableStateFlow<List<MLModel>>(emptyList())
    val allModels: StateFlow<List<MLModel>> = _allModels.asStateFlow()

    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var isProcessingOperation = false

    init {
        loadModelsAndPatches()
    }

    private fun loadModelsAndPatches() {
        viewModelScope.launch {
            try {
                repository.getActiveModels()
                    .debounce(300) // Prevent rapid updates
                    .distinctUntilChanged() // Only emit when data actually changes
                    .catch { error ->
                        Timber.e(error, "Error loading models")
                        errorHandler.handleError(error as? Exception ?: Exception(error))
                        emit(emptyList())
                    }
                    .collect { models ->
                        _allModels.value = models

                        if (models.isEmpty()) {
                            _uiState.value = PatchManagementState.Error("No models available")
                        } else {
                            // Auto-select first model if none selected
                            if (_selectedModelId.value == null ||
                                !models.any { it.id == _selectedModelId.value }
                            ) {
                                _selectedModelId.value = models.first().id
                            }
                            _selectedModelId.value?.let { loadPatches(it) }
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models")
                errorHandler.handleError(e)
                _uiState.value = PatchManagementState.Error(e.message ?: "Failed to load models")
            }
        }
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
        loadPatches(modelId)
    }

    fun loadPatches(modelId: String) {
        _selectedModelId.value = modelId
        viewModelScope.launch {
            try {
                Timber.d("📋 Loading patches for model: $modelId")
                repository.getPatchesByModel(modelId)
                    .debounce(300) // Prevent rapid updates
                    .distinctUntilChanged { old, new ->
                        // Only update if patches actually changed
                        old.size == new.size &&
                                old.zip(new).all { (a, b) ->
                                    a.id == b.id && a.status == b.status
                                }
                    }
                    .catch { error ->
                        Timber.e(error, "Error loading patches")
                        errorHandler.handleError(error as? Exception ?: Exception(error))
                        emit(emptyList())
                    }
                    .collect { patches ->
                        val appliedCount = patches.count { it.status == PatchStatus.APPLIED }
                        val currentState = _uiState.value
                        Timber.d("✅ Loaded ${patches.size} patches, ${appliedCount} applied")

                        _uiState.value = PatchManagementState.Success(
                            patches = patches,
                            appliedCount = appliedCount,
                            isProcessing = if (currentState is PatchManagementState.Success) currentState.isProcessing else false,
                            processingMessage = if (currentState is PatchManagementState.Success) currentState.processingMessage else null,
                            processingDetails = if (currentState is PatchManagementState.Success) currentState.processingDetails else null,
                            processingPatchId = if (currentState is PatchManagementState.Success) currentState.processingPatchId else null
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load patches")
                errorHandler.handleError(e)
                _uiState.value = PatchManagementState.Error(e.message ?: "Failed to load patches")
            }
        }
    }

    fun applyPatch(patchId: String) {
        // Prevent multiple simultaneous operations
        if (isProcessingOperation) {
            Timber.w("Operation already in progress, ignoring request")
            return
        }

        viewModelScope.launch {
            try {
                isProcessingOperation = true

                // Set processing state
                val currentState = _uiState.value
                if (currentState is PatchManagementState.Success) {
                    _uiState.value = currentState.copy(
                        isProcessing = true,
                        processingMessage = "Applying patch",
                        processingDetails = "Validating and applying changes...",
                        processingPatchId = patchId
                    )
                }

                // Simulate processing time for better UX
                delay(500)

                // Apply patch with retry logic
                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    repository.applyPatch(patchId)
                }

                result.fold(
                    onSuccess = { applyResult ->
                        applyResult.onSuccess {
                            Timber.d("✅ Patch applied successfully: $patchId")

                            // Log active preprocessing details
                            _selectedModelId.value?.let { modelId ->
                                val details = repository.getPreprocessingDetails(modelId)
                                Timber.i("📋 Active preprocessing after patch application:")
                                Timber.i(details)
                            }

                            // Clear processing state
                            if (currentState is PatchManagementState.Success) {
                                _uiState.value = currentState.copy(
                                    isProcessing = false,
                                    processingMessage = null,
                                    processingDetails = null,
                                    processingPatchId = null
                                )
                            }
                        }.onFailure { error ->
                            throw error
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Failed to apply patch")
                        errorHandler.handleError(error)

                        // Show error but keep UI stable
                        if (currentState is PatchManagementState.Success) {
                            _uiState.value = currentState.copy(
                                isProcessing = false,
                                processingMessage = "Failed: ${error.message}",
                                processingDetails = null,
                                processingPatchId = null
                            )

                            // Clear error message after 3 seconds
                            delay(3000)
                            _uiState.value = currentState.copy(
                                processingMessage = null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception applying patch")
                errorHandler.handleError(e)
                val currentState = _uiState.value
                if (currentState is PatchManagementState.Success) {
                    _uiState.value = currentState.copy(
                        isProcessing = false,
                        processingMessage = "Error: ${e.message}",
                        processingDetails = null,
                        processingPatchId = null
                    )
                }
            } finally {
                isProcessingOperation = false
            }
        }
    }

    fun rollbackPatch(patchId: String) {
        // Prevent multiple simultaneous operations
        if (isProcessingOperation) {
            Timber.w("Operation already in progress, ignoring request")
            return
        }

        viewModelScope.launch {
            try {
                isProcessingOperation = true

                // Set processing state
                val currentState = _uiState.value
                if (currentState is PatchManagementState.Success) {
                    _uiState.value = currentState.copy(
                        isProcessing = true,
                        processingMessage = "Rolling back patch",
                        processingDetails = "Restoring original model state...",
                        processingPatchId = patchId
                    )
                }

                // Simulate processing time for better UX
                delay(500)

                // Rollback patch with retry logic
                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    repository.rollbackPatch(patchId)
                }

                result.fold(
                    onSuccess = { rollbackResult ->
                        rollbackResult.onSuccess {
                            Timber.d("✅ Patch rolled back successfully: $patchId")

                            // Log active preprocessing details
                            _selectedModelId.value?.let { modelId ->
                                val details = repository.getPreprocessingDetails(modelId)
                                Timber.i("📋 Active preprocessing after patch rollback:")
                                Timber.i(details)
                            }

                            // Clear processing state
                            if (currentState is PatchManagementState.Success) {
                                _uiState.value = currentState.copy(
                                    isProcessing = false,
                                    processingMessage = null,
                                    processingDetails = null,
                                    processingPatchId = null
                                )
                            }
                        }.onFailure { error ->
                            throw error
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Failed to rollback patch")
                        errorHandler.handleError(error)

                        // Show error but keep UI stable
                        if (currentState is PatchManagementState.Success) {
                            _uiState.value = currentState.copy(
                                isProcessing = false,
                                processingMessage = "Failed: ${error.message}",
                                processingDetails = null,
                                processingPatchId = null
                            )

                            // Clear error message after 3 seconds
                            delay(3000)
                            _uiState.value = currentState.copy(
                                processingMessage = null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception rolling back patch")
                errorHandler.handleError(e)
                val currentState = _uiState.value
                if (currentState is PatchManagementState.Success) {
                    _uiState.value = currentState.copy(
                        isProcessing = false,
                        processingMessage = "Error: ${e.message}",
                        processingDetails = null,
                        processingPatchId = null
                    )
                }
            } finally {
                isProcessingOperation = false
            }
        }
    }

    /**
     * Export patch to JSON format
     */
    fun exportPatchToJson(patch: Patch) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting("Exporting to JSON...")

                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    exportManager.exportPatchToJson(patch)
                }.getOrThrow()

                if (result.success) {
                    Timber.i("✅ Patch exported to JSON: ${result.files.firstOrNull()?.name}")
                    _exportState.value = ExportState.Success(
                        message = result.message,
                        files = result.files
                    )
                } else {
                    _exportState.value = ExportState.Error(result.message)
                }

                // Reset after 3 seconds
                delay(3000)
                _exportState.value = ExportState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Failed to export patch to JSON")
                errorHandler.handleError(e)
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
                delay(3000)
                _exportState.value = ExportState.Idle
            }
        }
    }

    /**
     * Export patch to text format
     */
    fun exportPatchToText(patch: Patch) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting("Exporting to text...")

                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    exportManager.exportPatchToText(patch)
                }.getOrThrow()

                if (result.success) {
                    Timber.i("✅ Patch exported to text: ${result.files.firstOrNull()?.name}")
                    _exportState.value = ExportState.Success(
                        message = result.message,
                        files = result.files
                    )
                } else {
                    _exportState.value = ExportState.Error(result.message)
                }

                // Reset after 3 seconds
                delay(3000)
                _exportState.value = ExportState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Failed to export patch to text")
                errorHandler.handleError(e)
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
                delay(3000)
                _exportState.value = ExportState.Idle
            }
        }
    }

    /**
     * Export patch in both formats
     */
    fun exportPatchBoth(patch: Patch) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting("Exporting to JSON & text...")

                val result = retryWithExponentialBackoff(RetryPolicies.QUICK) {
                    exportManager.exportPatch(
                        patch,
                        PatchExportManager.ExportFormat.BOTH
                    )
                }.getOrThrow()

                if (result.success) {
                    Timber.i("✅ Patch exported to both formats: ${result.files.size} files")
                    _exportState.value = ExportState.Success(
                        message = result.message,
                        files = result.files
                    )
                } else {
                    _exportState.value = ExportState.Error(result.message)
                }

                // Reset after 3 seconds
                delay(3000)
                _exportState.value = ExportState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Failed to export patch")
                errorHandler.handleError(e)
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
                delay(3000)
                _exportState.value = ExportState.Idle
            }
        }
    }

    /**
     * Get export directory path
     */
    fun getExportPath(): String {
        return exportManager.getExportPath()
    }

    /**
     * Clear export state
     */
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }
}

/**
 * UI state for patch management
 */
sealed class PatchManagementState {
    object Loading : PatchManagementState()
    data class Success(
        val patches: List<Patch>,
        val appliedCount: Int,
        val isProcessing: Boolean = false,
        val processingMessage: String? = null,
        val processingDetails: String? = null,
        val processingPatchId: String? = null
    ) : PatchManagementState()

    data class Error(val message: String) : PatchManagementState()
}

/**
 * Export state for patch export operations
 */
sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val message: String) : ExportState()
    data class Success(val message: String, val files: List<File>) : ExportState()
    data class Error(val message: String) : ExportState()
}
