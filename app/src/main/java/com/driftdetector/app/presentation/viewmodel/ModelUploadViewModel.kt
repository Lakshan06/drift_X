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

                // Take persistent URI permissions for offline access (Google Drive, etc.)
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Timber.d("✅ Persistent permission granted for: $uri")
                    } catch (e: SecurityException) {
                        Timber.w(
                            e,
                            "⚠️ Could not take persistent permission for: $uri (may not support it)"
                        )
                        // Continue anyway - file may still be accessible
                    }
                }

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

                // Check what we have and process accordingly
                when {
                    // Both model and data - full processing
                    modelFile != null && dataFile != null -> {
                        delay(500)
                        processFilesAutomatically()
                    }
                    // Only model - register it to database
                    modelFile != null && dataFile == null -> {
                        delay(500)
                        processModelOnly()
                    }
                    // Only data - needs a model first
                    modelFile == null && dataFile != null -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "✅ Data file uploaded!\n\n⚠️ Please upload a model file to begin drift detection."
                        )
                    }
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

    /**
     * Process model-only upload - register to database without data
     */
    private fun processModelOnly() {
        viewModelScope.launch {
            try {
                val model = modelFile
                if (model == null || model.uri == null) {
                    Timber.w("Cannot process: missing model file")
                    return@launch
                }

                Timber.d(" Processing model file only...")
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    error = null
                )

                // Show processing progress
                for (i in 0..100 step 5) {
                    _uploadProgress.value = i / 100f
                    delay(30)
                }

                // Process and register model
                val result = fileUploadProcessor.processModelFile(
                    uri = model.uri,
                    fileName = model.name
                )

                _uploadProgress.value = 0f

                if (result.isSuccess) {
                    val registeredModel = result.getOrThrow()

                    // Update state with registered model
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processedModel = registeredModel,
                        successMessage = buildModelOnlySuccessMessage(registeredModel),
                        uploadedFiles = _uiState.value.uploadedFiles.map {
                            if (it.id == model.id) {
                                it.copy(
                                    processed = true,
                                    processingStatus = "✅ Model registered & ready"
                                )
                            } else it
                        }
                    )

                    Timber.i("✅ Model registered successfully: ${registeredModel.name}")

                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "❌ Failed to process model")
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Failed to register model: ${error?.message}"
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to process model")
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to register model: ${e.message}"
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

    private fun buildModelOnlySuccessMessage(model: MLModel): String {
        val sb = StringBuilder()
        sb.appendLine("✅ Model Registered Successfully!")
        sb.appendLine()
        sb.appendLine(" Model: ${model.name}")
        sb.appendLine("️ Version: ${model.version}")
        sb.appendLine("📊 Input Features: ${model.inputFeatures.size}")
        sb.appendLine("️ Output Labels: ${model.outputLabels.size}")
        sb.appendLine()
        sb.appendLine("📌 NEXT STEPS:")
        sb.appendLine("1. Upload a dataset (.csv, .json, .parquet)")
        sb.appendLine("2. System will detect drift automatically")
        sb.appendLine("3. Patches will be generated if needed")
        sb.appendLine()
        sb.appendLine("Your model is now active and ready to monitor!")
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
                Timber.d("🌐 Importing from URL: $url")
                _uiState.value = _uiState.value.copy(
                    isUploading = true,
                    error = null,
                    successMessage = null
                )

                // Validate URL
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Invalid URL. Must start with http:// or https://"
                    )
                    return@launch
                }

                // Extract filename from URL
                val fileName = url.substringAfterLast("/").substringBefore("?")
                if (fileName.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Could not extract filename from URL"
                    )
                    return@launch
                }

                val format = extractFormat(fileName)
                val isModel = isModelFile(fileName)

                // Show progress
                for (i in 0..30 step 10) {
                    _uploadProgress.value = i / 100f
                    delay(100)
                }

                // Download file using Android DownloadManager
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle("Downloading $fileName")
                    setDescription("DriftGuardAI - Downloading model/data file")
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                        "DriftGuardAI_$fileName"
                    )
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(false)
                }

                val downloadId = downloadManager.enqueue(request)
                Timber.d("📥 Download started with ID: $downloadId")

                // Monitor download progress
                var downloading = true
                while (downloading) {
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val statusIndex =
                            cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)

                        val bytesDownloadedIndex =
                            cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex =
                            cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)

                        when (status) {
                            android.app.DownloadManager.STATUS_RUNNING -> {
                                if (bytesTotal > 0) {
                                    val progress =
                                        (bytesDownloaded.toFloat() / bytesTotal.toFloat()) * 0.7f + 0.3f
                                    _uploadProgress.value = progress
                                }
                                delay(500)
                            }

                            android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                                _uploadProgress.value = 1f
                                downloading = false

                                // Get downloaded file URI
                                val uriIndex =
                                    cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_URI)
                                if (uriIndex < 0) {
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        error = "Could not get downloaded file location"
                                    )
                                    return@launch
                                }
                                val uriString = cursor.getString(uriIndex)
                                val fileUri = Uri.parse(uriString)

                                // Get actual file size
                                val actualSize = getFileSize(fileUri)

                                val newFile = UploadedFile(
                                    name = fileName,
                                    size = formatFileSize(actualSize),
                                    format = format,
                                    isModel = isModel,
                                    uri = fileUri,
                                    metadata = mapOf(
                                        "source" to "url",
                                        "url" to url,
                                        "downloadDate" to System.currentTimeMillis().toString(),
                                        "downloadId" to downloadId.toString()
                                    ),
                                    processed = false,
                                    processingStatus = "UPLOADED"
                                )

                                // Store model/data file references
                                if (isModel) {
                                    modelFile = newFile
                                } else {
                                    dataFile = newFile
                                }

                                _uiState.value = _uiState.value.copy(
                                    uploadedFiles = _uiState.value.uploadedFiles + newFile,
                                    isUploading = false,
                                    successMessage = "✅ Successfully downloaded file from URL!"
                                )

                                Timber.i("✅ Successfully imported from URL: $fileName")

                                // Auto-process if we have both model and data
                                if (modelFile != null && dataFile != null) {
                                    delay(500)
                                    processFilesAutomatically()
                                }
                            }

                            android.app.DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                val reasonIndex =
                                    cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON)
                                val reason = cursor.getInt(reasonIndex)
                                _uiState.value = _uiState.value.copy(
                                    isUploading = false,
                                    error = "Download failed (code: $reason). Check your internet connection and try again."
                                )
                                Timber.e("❌ Download failed with reason: $reason")
                            }

                            android.app.DownloadManager.STATUS_PAUSED -> {
                                delay(500)
                            }
                        }
                    } else {
                        downloading = false
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            error = "Download was cancelled or failed"
                        )
                    }
                    cursor.close()
                }

                _uploadProgress.value = 0f

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
