package com.driftdetector.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.cloud.CloudProvider
import com.driftdetector.app.core.cloud.CloudStorageManager
import com.driftdetector.app.core.upload.FileUploadProcessor
import com.driftdetector.app.core.upload.ProcessingResult
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

enum class UploadMethod {
    LOCAL_FILE,
    CLOUD_STORAGE,
    URL_IMPORT,
    DRAG_DROP
}

data class UploadedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val format: String? = null,
    val isModel: Boolean = false,
    val uri: Uri? = null,
    val metadata: Map<String, String> = emptyMap(),
    val processed: Boolean = false,
    val processingStatus: String? = null
)

data class ModelUploadState(
    val selectedMethod: UploadMethod? = null,
    val uploadedFiles: List<UploadedFile> = emptyList(),
    val isUploading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val processedModel: MLModel? = null,
    val detectedDrift: DriftResult? = null,
    val synthesizedPatch: Patch? = null
)

class ModelUploadViewModel(
    private val fileUploadProcessor: FileUploadProcessor,
    private val context: Context,
    private val cloudStorageManager: CloudStorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelUploadState())
    val uiState: StateFlow<ModelUploadState> = _uiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    // Store uploaded files temporarily
    private var modelFile: UploadedFile? = null
    private var dataFile: UploadedFile? = null

    fun selectMethod(method: UploadMethod) {
        Timber.d(" Upload method selected: $method")
        _uiState.value = _uiState.value.copy(
            selectedMethod = method,
            error = null
        )
    }

    fun uploadFiles(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                Timber.d(" Starting upload for ${uris.size} files")
                _uiState.value = _uiState.value.copy(
                    isUploading = true,
                    error = null,
                    successMessage = null
                )

                // Simulate upload progress
                for (i in 0..100 step 10) {
                    _uploadProgress.value = i / 100f
                    delay(100)
                }

                // Process each file with real file info from ContentResolver
                val newFiles = uris.mapIndexed { index, uri ->
                    val fileName = extractFileName(uri)
                    val fileSize = getFileSize(uri)
                    val format = extractFormat(fileName)
                    val isModel = isModelFile(fileName)

                    val file = UploadedFile(
                        name = fileName,
                        size = formatFileSize(fileSize),
                        format = format,
                        isModel = isModel,
                        uri = uri,
                        metadata = mapOf(
                            "uploadDate" to System.currentTimeMillis().toString(),
                            "index" to index.toString(),
                            "fileSize" to fileSize.toString()
                        ),
                        processed = false,
                        processingStatus = "UPLOADED"
                    )

                    // Store model/data file references
                    if (isModel) {
                        modelFile = file
                    } else {
                        dataFile = file
                    }

                    file
                }

                _uiState.value = _uiState.value.copy(
                    uploadedFiles = _uiState.value.uploadedFiles + newFiles,
                    isUploading = false,
                    successMessage = "✅ Uploaded ${newFiles.size} file(s) successfully!"
                )

                _uploadProgress.value = 1f
                delay(500)
                _uploadProgress.value = 0f

                Timber.i("✅ Successfully uploaded ${newFiles.size} files")

                // Auto-process if we have both model and data
                if (modelFile != null && dataFile != null) {
                    delay(500) // Small delay for UI feedback
                    processFilesAutomatically()
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to upload files")
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Failed to upload files: ${e.message}"
                )
                _uploadProgress.value = 0f
            }
        }
    }

    /**
     * Process uploaded files - run drift detection and synthesize patches
     */
    private fun processFilesAutomatically() {
        viewModelScope.launch {
            try {
                val model = modelFile
                val data = dataFile

                if (model == null || data == null || model.uri == null || data.uri == null) {
                    Timber.w("Cannot process: missing model or data file")
                    return@launch
                }

                Timber.d(" Auto-processing files...")
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    error = null
                )

                // Show processing progress
                for (i in 0..100 step 5) {
                    _uploadProgress.value = i / 100f
                    delay(50)
                }

                // Process model and data together
                val result = fileUploadProcessor.processModelAndData(
                    modelUri = model.uri,
                    modelFileName = model.name,
                    dataUri = data.uri,
                    dataFileName = data.name
                )

                _uploadProgress.value = 0f

                if (result.isSuccess) {
                    val processingResult = result.getOrThrow()

                    // Update state with results
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processedModel = processingResult.model,
                        detectedDrift = processingResult.driftResult,
                        synthesizedPatch = processingResult.patch,
                        successMessage = buildSuccessMessage(processingResult),
                        uploadedFiles = _uiState.value.uploadedFiles.map {
                            if (it.id == model.id || it.id == data.id) {
                                it.copy(
                                    processed = true,
                                    processingStatus = "✅ Processed successfully"
                                )
                            } else it
                        }
                    )

                    Timber.i("✅ Files processed successfully!")

                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "❌ Failed to process files")
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Processing failed: ${error?.message}"
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to process files")
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Processing failed: ${e.message}"
                )
                _uploadProgress.value = 0f
            }
        }
    }

    private fun buildSuccessMessage(result: ProcessingResult): String {
        val sb = StringBuilder()
        sb.appendLine("✅ Processing Complete!")
        sb.appendLine()
        sb.appendLine(" Model: ${result.model.name}")
        sb.appendLine(" Data Points: ${result.dataPoints}")
        sb.appendLine()

        if (result.driftResult.isDriftDetected) {
            sb.appendLine("⚠️ Drift Detected!")
            sb.appendLine("   Score: ${String.format("%.3f", result.driftResult.driftScore)}")
            sb.appendLine("   Type: ${result.driftResult.driftType}")

            if (result.patch != null) {
                sb.appendLine()
                sb.appendLine("️ Patch Synthesized!")
                sb.appendLine("   Type: ${result.patch.patchType}")
                sb.appendLine(
                    "   Safety: ${
                        String.format(
                            "%.2f",
                            result.patch.validationResult?.metrics?.safetyScore ?: 0.0
                        )
                    }"
                )
            }
        } else {
            sb.appendLine("✅ No Drift Detected")
            sb.appendLine("   Model is performing well!")
        }

        return sb.toString()
    }

    fun connectCloudStorage(provider: String) {
        viewModelScope.launch {
            try {
                Timber.d("☁️ Connecting to cloud storage: $provider")
                _uiState.value = _uiState.value.copy(
                    error = null,
                    isUploading = true
                )

                // Map string to CloudProvider enum
                val cloudProvider = when (provider.lowercase()) {
                    "google_drive" -> CloudProvider.GOOGLE_DRIVE
                    "dropbox" -> CloudProvider.DROPBOX
                    "onedrive" -> CloudProvider.ONEDRIVE
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Unknown cloud provider: $provider",
                            isUploading = false
                        )
                        return@launch
                    }
                }

                // Attempt connection
                val authResult = cloudStorageManager.connectProvider(cloudProvider)

                if (authResult.success) {
                    Timber.i("✅ Connected to $provider")

                    // List available files
                    val filesResult = cloudStorageManager.listFiles(
                        provider = cloudProvider,
                        accessToken = authResult.accessToken ?: "",
                        fileTypes = listOf(".tflite", ".onnx", ".csv", ".json", ".parquet")
                    )

                    if (filesResult.isSuccess) {
                        val cloudFiles = filesResult.getOrDefault(emptyList())
                        Timber.d(" Found ${cloudFiles.size} files in $provider")

                        _uiState.value = _uiState.value.copy(
                            successMessage = "✅ Connected to $provider! Found ${cloudFiles.size} compatible files.\n\nCloud file selection UI coming soon - for now, files are listed in logs.",
                            isUploading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Connected but failed to list files: ${filesResult.exceptionOrNull()?.message}",
                            isUploading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to connect: ${authResult.error}",
                        isUploading = false
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to connect to cloud storage")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to connect to $provider: ${e.message}",
                    isUploading = false
                )
            }
        }
    }

    fun importFromUrl(url: String) {
        viewModelScope.launch {
            try {
                Timber.d(" Importing from URL: $url")
                _uiState.value = _uiState.value.copy(
                    isUploading = true,
                    error = null
                )

                // Simulate download progress
                for (i in 0..100 step 5) {
                    _uploadProgress.value = i / 100f
                    delay(50)
                }

                // Extract filename from URL
                val fileName = url.substringAfterLast("/")
                val format = extractFormat(fileName)
                val isModel = isModelFile(fileName)

                val newFile = UploadedFile(
                    name = fileName,
                    size = "3.2 MB", // Simulated
                    format = format,
                    isModel = isModel,
                    uri = Uri.parse(url),
                    metadata = mapOf(
                        "source" to "url",
                        "url" to url,
                        "downloadDate" to System.currentTimeMillis().toString()
                    ),
                    processed = false
                )

                _uiState.value = _uiState.value.copy(
                    uploadedFiles = _uiState.value.uploadedFiles + newFile,
                    isUploading = false,
                    successMessage = "File imported from URL!"
                )

                _uploadProgress.value = 1f
                delay(500)
                _uploadProgress.value = 0f

                Timber.i("✅ Successfully imported from URL")

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to import from URL")
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Failed to import from URL: ${e.message}"
                )
                _uploadProgress.value = 0f
            }
        }
    }

    fun removeFile(fileId: String) {
        Timber.d("️ Removing file: $fileId")

        // Clear model/data file references if removed
        if (modelFile?.id == fileId) {
            modelFile = null
        }
        if (dataFile?.id == fileId) {
            dataFile = null
        }

        _uiState.value = _uiState.value.copy(
            uploadedFiles = _uiState.value.uploadedFiles.filter { it.id != fileId }
        )
    }

    fun configureFile(fileId: String) {
        Timber.d("⚙️ Configuring file: $fileId")
        // TODO: Open configuration dialog/screen
        _uiState.value = _uiState.value.copy(
            successMessage = "Configuration feature coming soon!"
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null
        )
    }

    /**
     * Handle drag and drop files
     */
    fun onFilesDropped(uris: List<Uri>) {
        Timber.d(" Files dropped: ${uris.size}")
        uploadFiles(uris)
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) {
            Timber.w(e, "Failed to get file size")
            0L
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun extractFileName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.lastPathSegment ?: "unknown_file"
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract filename")
            uri.lastPathSegment ?: "unknown_file"
        }
    }

    private fun extractFormat(fileName: String): String? {
        val extension = fileName.substringAfterLast(".", "")
        return if (extension.isNotEmpty()) ".$extension" else null
    }

    private fun isModelFile(fileName: String): Boolean {
        val modelExtensions = listOf(".tflite", ".onnx", ".h5", ".pb", ".pt", ".pth")
        return modelExtensions.any { fileName.endsWith(it, ignoreCase = true) }
    }
}
