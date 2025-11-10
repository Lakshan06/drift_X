package com.driftdetector.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.backup.AutomaticBackupManager
import com.driftdetector.app.core.export.ModelExportManager
import com.driftdetector.app.core.export.PredictionResult
import com.driftdetector.app.core.security.EncryptionManager
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.presentation.screen.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import org.json.JSONArray
import android.net.Uri

/**
 * ViewModel for settings screen
 */
class SettingsViewModel(
    private val repository: DriftRepository,
    private val context: Context,
    private val encryptionManager: EncryptionManager
) : ViewModel(), KoinComponent {

    private val exportManager: ModelExportManager by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        calculateStorageUsed()
    }

    private fun loadSettings() {
        try {
            val prefs = encryptionManager.encryptedPreferences

            val themeMode = ThemeMode.valueOf(
                prefs.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name
            )

            Timber.d("🎨 Loading settings - Theme mode from prefs: $themeMode")

            _uiState.value = _uiState.value.copy(
                themeMode = themeMode,
                encryptionEnabled = prefs.getBoolean("encryption_enabled", true),
                differentialPrivacyEnabled = prefs.getBoolean(
                    "differential_privacy_enabled",
                    true
                ),
                cloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", false),
                driftMonitoringEnabled = prefs.getBoolean("drift_monitoring_enabled", true),
                monitoringIntervalMinutes = prefs.getInt("monitoring_interval_minutes", 30),
                driftThreshold = prefs.getFloat("drift_threshold", 0.3f),
                autoApplyPatches = prefs.getBoolean("auto_apply_patches", false),
                dataScientistMode = prefs.getBoolean("data_scientist_mode", false),
                driftAlertsEnabled = prefs.getBoolean("drift_alerts_enabled", true),
                patchNotificationsEnabled = prefs.getBoolean(
                    "patch_notifications_enabled",
                    true
                ),
                criticalAlertsOnly = prefs.getBoolean("critical_alerts_only", false),
                vibrateOnAlerts = prefs.getBoolean("vibrate_on_alerts", true),
                emailNotificationsEnabled = prefs.getBoolean("email_notifications_enabled", false),
                aiExplanationsEnabled = prefs.getBoolean("ai_explanations_enabled", true),
                dataRetentionDays = prefs.getInt("data_retention_days", 30),
                autoRegisterModels = prefs.getBoolean("auto_register_models", true),
                syncBaselineOnDeploy = prefs.getBoolean("sync_baseline_on_deploy", true),
                autoBackupModels = prefs.getBoolean("auto_backup_models", true)
            )

            Timber.d("🎨 Settings loaded - Current theme: ${_uiState.value.themeMode}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load settings")
        }
    }

    private fun savePreference(key: String, value: Any) {
        viewModelScope.launch {
            try {
                val prefs = encryptionManager.encryptedPreferences
                prefs.edit().apply {
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                    }
                    apply()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save preference: $key")
            }
        }
    }

    // Theme Settings
    fun updateTheme(theme: ThemeMode) {
        Timber.d("🎨 Theme update requested: $theme")
        Timber.d("🎨 Current theme: ${_uiState.value.themeMode}")
        _uiState.update { it.copy(themeMode = theme) }
        Timber.d("🎨 New theme state: ${_uiState.value.themeMode}")
        savePreference("theme_mode", theme.name)
        Timber.d("🎨 Theme preference saved: ${theme.name}")
    }

    // Privacy & Security
    fun toggleEncryption(enabled: Boolean) {
        _uiState.update { it.copy(encryptionEnabled = enabled) }
        savePreference("encryption_enabled", enabled)
    }

    fun toggleDifferentialPrivacy(enabled: Boolean) {
        _uiState.update { it.copy(differentialPrivacyEnabled = enabled) }
        savePreference("differential_privacy_enabled", enabled)
    }

    fun toggleCloudSync(enabled: Boolean) {
        _uiState.update { it.copy(cloudSyncEnabled = enabled) }
        savePreference("cloud_sync_enabled", enabled)
    }

    // Monitoring Settings
    fun toggleDriftMonitoring(enabled: Boolean) {
        _uiState.update { it.copy(driftMonitoringEnabled = enabled) }
        savePreference("drift_monitoring_enabled", enabled)
    }

    fun updateMonitoringInterval(minutes: Int) {
        _uiState.update { it.copy(monitoringIntervalMinutes = minutes) }
        savePreference("monitoring_interval_minutes", minutes)
    }

    fun updateDriftThreshold(threshold: Float) {
        _uiState.update { it.copy(driftThreshold = threshold) }
        savePreference("drift_threshold", threshold)
    }

    fun toggleAutoApplyPatches(enabled: Boolean) {
        _uiState.update { it.copy(autoApplyPatches = enabled) }
        savePreference("auto_apply_patches", enabled)
    }

    fun toggleDataScientistMode(enabled: Boolean) {
        _uiState.update { it.copy(dataScientistMode = enabled) }
        savePreference("data_scientist_mode", enabled)
    }

    // Notification Settings
    fun toggleDriftAlerts(enabled: Boolean) {
        _uiState.update { it.copy(driftAlertsEnabled = enabled) }
        savePreference("drift_alerts_enabled", enabled)
    }

    fun togglePatchNotifications(enabled: Boolean) {
        _uiState.update { it.copy(patchNotificationsEnabled = enabled) }
        savePreference("patch_notifications_enabled", enabled)
    }

    fun toggleCriticalAlertsOnly(enabled: Boolean) {
        _uiState.update { it.copy(criticalAlertsOnly = enabled) }
        savePreference("critical_alerts_only", enabled)
    }

    fun toggleVibrateOnAlerts(enabled: Boolean) {
        _uiState.update { it.copy(vibrateOnAlerts = enabled) }
        savePreference("vibrate_on_alerts", enabled)
    }

    fun toggleEmailNotifications(enabled: Boolean) {
        _uiState.update { it.copy(emailNotificationsEnabled = enabled) }
        savePreference("email_notifications_enabled", enabled)
    }

    // AI Settings
    fun toggleAIExplanations(enabled: Boolean) {
        _uiState.update { it.copy(aiExplanationsEnabled = enabled) }
        savePreference("ai_explanations_enabled", enabled)
    }

    fun openAIModelDownload() {
        // TODO: Implement AI model download dialog
        Timber.d("Opening AI model download")
    }

    // Data Management
    fun updateDataRetention(days: Int) {
        _uiState.update { it.copy(dataRetentionDays = days) }
        savePreference("data_retention_days", days)
    }

    fun clearOldData() {
        viewModelScope.launch {
            try {
                val cutoffTime =
                    Instant.now().minus(_uiState.value.dataRetentionDays.toLong(), ChronoUnit.DAYS)
                repository.cleanupOldData(cutoffTime)
                Timber.i("Cleared old data older than $cutoffTime")
                calculateStorageUsed()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear old data")
            }
        }
    }

    fun exportData() {
        showExportDialog()
    }

    /**
     * Export model configuration as JSON
     */
    private suspend fun exportModelConfiguration(model: MLModel): Result<String> = withContext(
        Dispatchers.IO
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val fileName = "model_config_${model.name}_$timestamp.json"

            // Use Downloads/DriftGuard/Data folder for user accessibility
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val exportDir = File(downloadsDir, "DriftGuard/Data")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, fileName)

            Timber.d("📝 Writing model config to: ${file.absolutePath}")

            val jsonObj = JSONObject().apply {
                put("exportType", "MODEL_CONFIGURATION")
                put("exportVersion", "1.0")
                put("modelId", model.id)
                put("modelName", model.name)
                put("version", model.version)
                put("modelPath", model.modelPath)
                put("inputFeatures", JSONArray(model.inputFeatures))
                put("outputLabels", JSONArray(model.outputLabels))
                put("createdAt", model.createdAt.toString())
                put("lastUpdated", model.lastUpdated.toString())
                put("isActive", model.isActive)
                put("exportedAt", timestamp)
                put("exportedBy", "DriftGuardAI v1.0")
                put(
                    "note",
                    "This is the model configuration export. For drift reports and predictions, ensure the model has been monitored and used."
                )
                put("instructions", JSONObject().apply {
                    put("openWith", "Any text editor or JSON viewer")
                    put("canImport", "Yes - Can be imported back into DriftGuardAI")
                    put("dataFormat", "JSON (JavaScript Object Notation)")
                })
            }

            // Write with pretty formatting (indent = 2 spaces)
            file.writeText(jsonObj.toString(2))

            // Verify file was created
            if (file.exists() && file.length() > 0) {
                // Notify media scanner so file appears immediately
                try {
                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = android.net.Uri.fromFile(file)
                    context.sendBroadcast(intent)
                    Timber.d("📢 Notified media scanner about: $fileName")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to notify media scanner")
                }

                Timber.i("✅ Exported model config: $fileName")
                Timber.i("   📂 Full path: ${file.absolutePath}")
                Timber.i("   📏 File size: ${file.length()} bytes")

                Result.success(fileName)
            } else {
                Timber.e("❌ Model config file was not created or is empty: ${file.absolutePath}")
                Result.failure(Exception("Failed to create model config file"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to export model configuration")
            Result.failure(e)
        }
    }

    fun clearExportStatus() {
        _uiState.update {
            it.copy(
                exportSuccess = false,
                exportError = null,
                lastExportedFiles = emptyList()
            )
        }
    }

    fun shareLastExport() {
        viewModelScope.launch {
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val driftGuardDir = File(downloadsDir, "DriftGuard")

                val files = driftGuardDir.walk()
                    .filter { it.isFile }
                    .filter { file ->
                        file.name.startsWith("predictions_") ||
                                file.name.startsWith("drift_report_") ||
                                file.name.startsWith("patch_comparison_") ||
                                file.name.startsWith("patch_") ||
                                file.name.startsWith("model_config_")
                    }
                    .sortedByDescending { it.lastModified() }
                    .toList()

                if (files.isEmpty()) {
                    Timber.w("⚠️ No export files found in Downloads/DriftGuard")
                    return@launch
                }

                // Share the most recent export
                val latestFile = files.first()
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    latestFile
                )

                val mimeType = when {
                    latestFile.name.endsWith(".csv") -> "text/csv"
                    latestFile.name.endsWith(".json") -> "application/json"
                    latestFile.name.endsWith(".txt") -> "text/plain"
                    else -> "*/*"
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "DriftGuard AI Export - ${latestFile.name}")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Exported data from DriftGuard AI\n\nAll files are saved in: Downloads/DriftGuard/"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share Export"))
                Timber.i("📤 Sharing export: ${latestFile.name}")

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to share export")
            }
        }
    }

    fun openExportLocation() {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val driftGuardDir = File(downloadsDir, "DriftGuard")

            // Ensure directory exists
            if (!driftGuardDir.exists()) {
                driftGuardDir.mkdirs()
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                driftGuardDir
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(Intent.createChooser(intent, "Open Export Folder"))
            } catch (e: Exception) {
                // Fallback: Open Downloads folder
                val downloadsIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"),
                        "vnd.android.document/directory"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(downloadsIntent)
            }

            Timber.i("📂 Opening export location: Downloads/DriftGuard")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to open export location")
        }
    }

    private fun calculateStorageUsed() {
        viewModelScope.launch {
            try {
                val dbPath = context.getDatabasePath("drift_database")
                val size = if (dbPath.exists()) {
                    dbPath.length() / (1024 * 1024) // Convert to MB
                } else {
                    0
                }

                _uiState.update { it.copy(storageUsed = "$size MB") }
            } catch (e: Exception) {
                Timber.e(e, "Failed to calculate storage")
                _uiState.update { it.copy(storageUsed = "Unknown") }
            }
        }
    }

    // Model Deployment Settings
    fun toggleAutoRegisterModels(enabled: Boolean) {
        _uiState.update { it.copy(autoRegisterModels = enabled) }
        savePreference("auto_register_models", enabled)
    }

    fun toggleSyncBaselineOnDeploy(enabled: Boolean) {
        _uiState.update { it.copy(syncBaselineOnDeploy = enabled) }
        savePreference("sync_baseline_on_deploy", enabled)
    }

    fun toggleAutoBackupModels(enabled: Boolean) {
        _uiState.update { it.copy(autoBackupModels = enabled) }
        savePreference("auto_backup_models", enabled)
    }

    // Export Data Methods
    fun showExportDialog() {
        viewModelScope.launch {
            try {
                Timber.d("📤 Opening export dialog...")

                // Load all models with their export status
                val models = repository.getActiveModels().first()
                val modelExportInfoList = mutableListOf<ModelExportInfo>()

                models.forEach { model ->
                    // Get patches for this model
                    val patches = repository.getPatchesByModel(model.id).first()
                    val patchedCount =
                        patches.count { it.status == com.driftdetector.app.domain.model.PatchStatus.APPLIED }
                    val ongoingCount = patches.count {
                        it.status == com.driftdetector.app.domain.model.PatchStatus.CREATED ||
                                it.status == com.driftdetector.app.domain.model.PatchStatus.VALIDATED
                    }

                    // Get drift results
                    val driftCount = repository.getDriftCount(model.id)

                    // Get predictions count (approximate from recent 30 days)
                    val startTime = Instant.now().minus(30, ChronoUnit.DAYS)
                    val predictions = repository.getRecentPredictions(model.id, startTime).first()

                    modelExportInfoList.add(
                        ModelExportInfo(
                            modelId = model.id,
                            modelName = model.name,
                            version = model.version,
                            hasPatches = patches.isNotEmpty(),
                            patchedCount = patchedCount,
                            ongoingCount = ongoingCount,
                            driftCount = driftCount,
                            predictionCount = predictions.size
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        showExportDialog = true,
                        availableModels = modelExportInfoList,
                        selectedModelIds = emptySet() // Reset selection
                    )
                }

                Timber.d("✅ Loaded ${modelExportInfoList.size} models for export")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load models for export")
                _uiState.update {
                    it.copy(exportError = "Failed to load models: ${e.message}")
                }
            }
        }
    }

    fun closeExportDialog() {
        _uiState.update {
            it.copy(
                showExportDialog = false,
                selectedModelIds = emptySet()
            )
        }
    }

    fun toggleModelSelection(modelId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedModelIds.contains(modelId)) {
                state.selectedModelIds - modelId
            } else {
                state.selectedModelIds + modelId
            }
            state.copy(selectedModelIds = newSelection)
        }
    }

    fun selectDownloadLocation(location: ExportLocation) {
        _uiState.update { it.copy(downloadLocation = location) }

        // If CUSTOM, open file picker
        if (location == ExportLocation.CUSTOM) {
            // TODO: Launch Android file picker
            Timber.d("Opening custom location picker...")
        }
    }

    fun exportSelectedModels() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedModelIds

                if (selectedIds.isEmpty()) {
                    _uiState.update {
                        it.copy(exportError = "Please select at least one model to export")
                    }
                    return@launch
                }

                // Close dialog and start export
                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        isExporting = true,
                        exportError = null
                    )
                }

                Timber.d("📤 Exporting ${selectedIds.size} selected models...")

                // Always use Downloads/DriftGuard folder for user accessibility
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val exportDir = File(downloadsDir, "DriftGuard/Data")

                // Log the exact path
                Timber.i("📁 Export directory: ${exportDir.absolutePath}")

                if (!exportDir.exists()) {
                    val created = exportDir.mkdirs()
                    Timber.i("📁 Created directory: $created")
                }

                // Verify directory is writable
                if (!exportDir.canWrite()) {
                    Timber.e("❌ Directory is NOT writable!")
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = "Cannot write to export directory: ${exportDir.absolutePath}"
                        )
                    }
                    return@launch
                }

                val exportedFiles = mutableListOf<String>()

                selectedIds.forEach { modelId ->
                    val modelInfo = _uiState.value.availableModels.find { it.modelId == modelId }
                    val model = repository.getActiveModels().first().find { it.id == modelId }

                    if (model != null) {
                        Timber.d("📊 Exporting model: ${model.name}")

                        // Always export model configuration first
                        val configResult = exportModelConfiguration(model)
                        configResult.onSuccess { fileName ->
                            exportedFiles.add(fileName)
                            Timber.i("✅ Exported model config: $fileName")
                        }.onFailure { error ->
                            Timber.e(error, "❌ Failed to export model config for ${model.name}")
                        }

                        // Export drift report if available
                        val driftResults = repository.getDriftResultsByModel(model.id).first()
                        val patches = repository.getPatchesByModel(model.id).first()

                        if (driftResults.isNotEmpty() || patches.isNotEmpty()) {
                            val driftReportResult = exportManager.exportDriftReport(
                                model = model,
                                driftResults = driftResults,
                                patches = patches
                            )

                            driftReportResult.onSuccess { exportResult ->
                                exportedFiles.add(exportResult.fileName)
                                Timber.i("✅ Exported drift report: ${exportResult.fileName}")
                                Timber.i("   📂 Full path: ${exportResult.file.absolutePath}")
                            }.onFailure { error ->
                                Timber.e(error, "❌ Failed to export drift report for ${model.name}")
                            }
                        } else {
                            // Create a sample drift report template
                            val timestamp =
                                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                            val fileName = "drift_report_${model.name}_$timestamp.json"
                            val file = File(exportDir, fileName)

                            try {
                                val sampleReport = JSONObject().apply {
                                    put("reportGeneratedAt", timestamp)
                                    put("model", JSONObject().apply {
                                        put("id", model.id)
                                        put("name", model.name)
                                        put("version", model.version)
                                        put("createdAt", model.createdAt.toString())
                                    })
                                    put("driftHistory", JSONArray())
                                    put("patchesApplied", JSONArray())
                                    put("summary", JSONObject().apply {
                                        put("totalDriftEvents", 0)
                                        put("driftDetectedCount", 0)
                                        put("totalPatchesApplied", 0)
                                        put(
                                            "note",
                                            "No drift data available yet. Start monitoring this model to generate drift reports."
                                        )
                                    })
                                }

                                file.writeText(sampleReport.toString(2))

                                // Notify media scanner
                                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                intent.data = android.net.Uri.fromFile(file)
                                context.sendBroadcast(intent)

                                exportedFiles.add(fileName)
                                Timber.i("✅ Exported sample drift report: $fileName")
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to create sample drift report")
                            }
                        }

                        // Export predictions if available
                        if (modelInfo != null && modelInfo.predictionCount > 0) {
                            val startTime = Instant.now().minus(30, ChronoUnit.DAYS)
                            val predictions =
                                repository.getRecentPredictions(model.id, startTime).first()

                            if (predictions.isNotEmpty()) {
                                val predictionResults = predictions.map { pred ->
                                    PredictionResult(
                                        timestamp = pred.timestamp,
                                        input = pred.input.features,
                                        prediction = pred.outputs,
                                        confidence = pred.confidence,
                                        modelVersion = model.version,
                                        patchId = null,
                                        driftScore = null
                                    )
                                }

                                val predictionsResult = exportManager.exportPredictionsToCsv(
                                    modelName = model.name,
                                    predictions = predictionResults,
                                    includeMetadata = true
                                )

                                predictionsResult.onSuccess { exportResult ->
                                    exportedFiles.add(exportResult.fileName)
                                    Timber.i("✅ Exported predictions: ${exportResult.fileName}")
                                    Timber.i("   📂 Full path: ${exportResult.file.absolutePath}")
                                }.onFailure { error ->
                                    Timber.e(
                                        error,
                                        "❌ Failed to export predictions for ${model.name}"
                                    )
                                }
                            }
                        } else {
                            // Create a sample CSV template
                            val timestamp =
                                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                            val fileName = "predictions_${model.name}_$timestamp.csv"
                            val file = File(exportDir, fileName)

                            try {
                                val csvContent = buildString {
                                    appendLine("# DriftGuardAI Predictions Export")
                                    appendLine("# Model: ${model.name}")
                                    appendLine("# Version: ${model.version}")
                                    appendLine("# Exported: $timestamp")
                                    appendLine("# Note: No prediction data available yet. Start using the model to generate predictions.")
                                    appendLine()
                                    appendLine("Timestamp,Input,Prediction,Confidence,Model Version,Patch Applied,Drift Score")
                                    appendLine("# Example: 2025-01-15T10:30:00Z,\"[0.5, 1.2, 3.4]\",\"[0, 1]\",0.95,v1.0,No,N/A")
                                }

                                file.writeText(csvContent)

                                // Notify media scanner
                                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                intent.data = android.net.Uri.fromFile(file)
                                context.sendBroadcast(intent)

                                exportedFiles.add(fileName)
                                Timber.i("✅ Exported sample predictions template: $fileName")
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to create sample predictions template")
                            }
                        }
                    }
                }

                if (exportedFiles.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = "No data available to export from selected models"
                        )
                    }
                } else {
                    val exportPath = "Downloads/DriftGuard/Data"
                    val fullPath = exportDir.absolutePath

                    Timber.i("✅ Export complete! ${exportedFiles.size} files exported to $exportPath")
                    Timber.i("   📂 Full path: $fullPath")

                    // Show toast notification
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "✅ ${exportedFiles.size} file(s) saved to:\n$exportPath",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }

                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = true,
                            lastExportedFiles = exportedFiles,
                            lastExportPath = exportPath
                        )
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Export failed")

                // Show error toast
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "❌ Export failed: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

}

/**
 * UI state for settings screen
 */
data class SettingsUiState(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.AUTO,

    // Privacy & Security
    val encryptionEnabled: Boolean = true,
    val differentialPrivacyEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = false,

    // Monitoring
    val driftMonitoringEnabled: Boolean = true,
    val monitoringIntervalMinutes: Int = 30,
    val driftThreshold: Float = 0.3f,
    val autoApplyPatches: Boolean = false,
    val dataScientistMode: Boolean = false,

    // Notifications
    val driftAlertsEnabled: Boolean = true,
    val patchNotificationsEnabled: Boolean = true,
    val criticalAlertsOnly: Boolean = false,
    val vibrateOnAlerts: Boolean = true,
    val emailNotificationsEnabled: Boolean = false,

    // AI
    val aiExplanationsEnabled: Boolean = true,
    val aiModelStatus: String = "Not Downloaded (Optional)",

    // Data Management
    val dataRetentionDays: Int = 30,

    // Model Deployment
    val autoRegisterModels: Boolean = true,
    val syncBaselineOnDeploy: Boolean = true,
    val autoBackupModels: Boolean = true,

    // Export State
    val showExportDialog: Boolean = false,
    val availableModels: List<ModelExportInfo> = emptyList(),
    val selectedModelIds: Set<String> = emptySet(),
    val downloadLocation: ExportLocation = ExportLocation.INTERNAL,
    val customDownloadPath: String? = null,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportError: String? = null,
    val lastExportedFiles: List<String> = emptyList(),
    val lastExportPath: String = "",

    // About
    val appVersion: String = "1.0.0",
    val storageUsed: String = "Calculating..."
)

/**
 * Model export information
 */
data class ModelExportInfo(
    val modelId: String,
    val modelName: String,
    val version: String,
    val hasPatches: Boolean,
    val patchedCount: Int,
    val ongoingCount: Int,
    val driftCount: Int,
    val predictionCount: Int
)

/**
 * Export location options
 */
enum class ExportLocation(val displayName: String) {
    INTERNAL("Internal Storage (App Data)"),
    DOWNLOADS("Downloads Folder"),
    CUSTOM("Custom Location")
}
