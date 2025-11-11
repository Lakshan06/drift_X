package com.driftdetector.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.drift.InstantDriftFixManager
import com.driftdetector.app.core.drift.InstantDriftFixManager.InstantFixResult
import com.driftdetector.app.core.drift.InstantDriftFixManager.ModelInfo
import com.driftdetector.app.core.drift.InstantDriftFixManager.PatchCandidate
import com.driftdetector.app.core.drift.InstantDriftFixManager.PatchedFilesResult
import com.driftdetector.app.domain.model.DriftResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for instant drift detection and patching workflow
 */
class InstantDriftFixViewModel(
    private val instantDriftFixManager: InstantDriftFixManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InstantDriftFixState>(
        InstantDriftFixState.Idle(
            hasFirstFile = false,
            firstFileName = null
        )
    )
    val uiState: StateFlow<InstantDriftFixState> = _uiState.asStateFlow()

    // Store uploaded files
    private var modelUri: Uri? = null
    private var modelFileName: String? = null
    private var dataUri: Uri? = null
    private var dataFileName: String? = null

    /**
     * Store first file (when user uploads only one file at a time)
     */
    fun storeFirstFile(file: Pair<Uri, String>) {
        // Determine if it's a model or data file based on extension
        val isModel = file.second.endsWith(".tflite") ||
                file.second.endsWith(".onnx") ||
                file.second.endsWith(".h5") ||
                file.second.endsWith(".pb")

        if (isModel) {
            modelUri = file.first
            modelFileName = file.second
        } else {
            dataUri = file.first
            dataFileName = file.second
        }

        _uiState.value = InstantDriftFixState.Idle(
            hasFirstFile = true,
            firstFileName = file.second
        )

        Timber.d("📁 First file stored: ${file.second}")
    }

    /**
     * Store both files and immediately start analysis
     */
    fun storeFilesAndAnalyze(file1: Pair<Uri, String>, file2: Pair<Uri, String>) {
        // Determine which is model and which is data
        val isFile1Model = file1.second.endsWith(".tflite") ||
                file1.second.endsWith(".onnx") ||
                file1.second.endsWith(".h5") ||
                file1.second.endsWith(".pb")

        if (isFile1Model) {
            modelUri = file1.first
            modelFileName = file1.second
            dataUri = file2.first
            dataFileName = file2.second
        } else {
            modelUri = file2.first
            modelFileName = file2.second
            dataUri = file1.first
            dataFileName = file1.second
        }

        Timber.d("📁 Files stored: model=${modelFileName}, data=${dataFileName}")

        // Immediately start analysis
        analyzeFiles()
    }

    /**
     * Store uploaded model and data files (legacy method for backward compatibility)
     */
    fun storeUploadedFiles(
        model: Pair<Uri, String>?,
        data: Pair<Uri, String>?
    ) {
        model?.let {
            modelUri = it.first
            modelFileName = it.second
        }
        data?.let {
            dataUri = it.first
            dataFileName = it.second
        }

        Timber.d("📁 Files stored: model=${modelFileName}, data=${dataFileName}")
    }

    /**
     * Step 1: Trigger instant drift analysis
     */
    fun analyzeFiles() {
        viewModelScope.launch {
            try {
                val model = modelUri ?: run {
                    _uiState.value = InstantDriftFixState.Error("No model file uploaded")
                    return@launch
                }
                val modelName = modelFileName ?: "model"
                val data = dataUri ?: run {
                    _uiState.value = InstantDriftFixState.Error("No data file uploaded")
                    return@launch
                }
                val dataName = dataFileName ?: "data"

                Timber.d("🔍 Starting instant drift analysis...")
                _uiState.value = InstantDriftFixState.Analyzing

                val result = instantDriftFixManager.analyzeFiles(
                    modelUri = model,
                    modelFileName = modelName,
                    dataUri = data,
                    dataFileName = dataName
                )

                if (result.success && result.driftResult != null) {
                    // FIXED: Safe null handling
                    val modelInfo = result.modelInfo
                    if (modelInfo == null) {
                        _uiState.value =
                            InstantDriftFixState.Error("Model information not available")
                        Timber.e("❌ Model info is null in analysis result")
                        return@launch
                    }

                    _uiState.value = InstantDriftFixState.AnalysisComplete(
                        modelInfo = modelInfo,
                        driftResult = result.driftResult,
                        patchCandidates = result.patches,
                        columnNames = result.columnNames
                    )
                    Timber.i(" Analysis complete: ${result.patches.size} patches available")
                } else {
                    _uiState.value = InstantDriftFixState.Error(
                        result.error ?: "Analysis failed"
                    )
                    Timber.e("❌ Analysis failed: ${result.error}")
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Analysis exception")
                _uiState.value = InstantDriftFixState.Error("Analysis failed: ${e.message}")
            }
        }
    }

    /**
     * Step 2: Apply selected patches and export files
     */
    fun applyPatchesAndExport(
        selectedPatchIds: Set<String>,
        analysisState: InstantDriftFixState.AnalysisComplete
    ) {
        viewModelScope.launch {
            try {
                Timber.d("🔨 Starting patch application with ${selectedPatchIds.size} selected patches")

                val model = modelUri ?: run {
                    val error = "Model file lost - please re-upload files"
                    Timber.e("❌ $error")
                    _uiState.value = InstantDriftFixState.Error(error)
                    return@launch
                }
                val modelName = modelFileName ?: "model"
                val data = dataUri ?: run {
                    val error = "Data file lost - please re-upload files"
                    Timber.e("❌ $error")
                    _uiState.value = InstantDriftFixState.Error(error)
                    return@launch
                }
                val dataName = dataFileName ?: "data"

                // Filter selected patches
                val selectedPatches = analysisState.patchCandidates.filter {
                    selectedPatchIds.contains(it.id)
                }

                Timber.d("📋 Selected patches: ${selectedPatches.map { it.title }}")

                if (selectedPatches.isEmpty()) {
                    val error = "No patches selected. Please select at least one patch to apply."
                    Timber.w("⚠️ $error")
                    _uiState.value = InstantDriftFixState.Error(error)
                    return@launch
                }

                Timber.d("🔨 Applying ${selectedPatches.size} patches...")
                _uiState.value = InstantDriftFixState.ApplyingPatches(selectedPatches)

                val result = instantDriftFixManager.applyPatchesAndExport(
                    modelUri = model,
                    modelFileName = modelName,
                    dataUri = data,
                    dataFileName = dataName,
                    selectedPatches = selectedPatches,
                    driftResult = analysisState.driftResult,
                    columnNames = analysisState.columnNames
                )

                if (result.success && result.patchedModelFile != null && result.patchedDataFile != null) {
                    _uiState.value = InstantDriftFixState.PatchesApplied(
                        originalModelInfo = analysisState.modelInfo,
                        originalDriftScore = analysisState.driftResult.driftScore,
                        patchedModelFile = result.patchedModelFile,
                        patchedDataFile = result.patchedDataFile,
                        appliedPatches = result.patchesApplied,
                        driftReduction = result.driftReduction
                    )
                    Timber.i("✅ Patches applied successfully: ${result.driftReduction.toInt()}% reduction")
                } else {
                    val errorMsg =
                        result.message.ifEmpty { "Patch application failed - unknown error" }
                    Timber.e("❌ Patch application failed: $errorMsg")
                    _uiState.value = InstantDriftFixState.Error(errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = "Failed to apply patches: ${e.message ?: e.javaClass.simpleName}"
                Timber.e(e, "❌ Patch application exception")
                _uiState.value = InstantDriftFixState.Error(errorMsg)
            }
        }
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        _uiState.value = InstantDriftFixState.Idle(
            hasFirstFile = false,
            firstFileName = null
        )
        modelUri = null
        modelFileName = null
        dataUri = null
        dataFileName = null
        Timber.d("🔄 Reset to idle state")
    }

    /**
     * Go back to previous state (for navigation)
     */
    fun goBack() {
        val currentState = _uiState.value
        when (currentState) {
            is InstantDriftFixState.AnalysisComplete -> {
                reset()
            }

            is InstantDriftFixState.ApplyingPatches -> {
                // Can't go back while applying
            }

            is InstantDriftFixState.PatchesApplied -> {
                reset()
            }

            is InstantDriftFixState.Error -> {
                reset()
            }

            else -> {
                reset()
            }
        }
    }
}

/**
 * UI state for instant drift fix workflow
 */
sealed class InstantDriftFixState {
    /**
     * Idle - waiting for user to upload files
     */
    data class Idle(
        val hasFirstFile: Boolean = false,
        val firstFileName: String? = null
    ) : InstantDriftFixState()

    /**
     * Analyzing - drift detection in progress
     */
    object Analyzing : InstantDriftFixState()

    /**
     * Analysis complete - showing results and patch candidates
     */
    data class AnalysisComplete(
        val modelInfo: ModelInfo,
        val driftResult: DriftResult,
        val patchCandidates: List<PatchCandidate>,
        val columnNames: List<String> = emptyList()
    ) : InstantDriftFixState()

    /**
     * Applying patches - patch application in progress
     */
    data class ApplyingPatches(
        val selectedPatches: List<PatchCandidate>
    ) : InstantDriftFixState()

    /**
     * Patches applied - showing download options
     */
    data class PatchesApplied(
        val originalModelInfo: ModelInfo,
        val originalDriftScore: Double,
        val patchedModelFile: File,
        val patchedDataFile: File,
        val appliedPatches: List<com.driftdetector.app.domain.model.Patch>,
        val driftReduction: Double
    ) : InstantDriftFixState()

    /**
     * Error state
     */
    data class Error(val message: String) : InstantDriftFixState()
}
