package com.driftdetector.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.export.ModelExportManager
import com.driftdetector.app.core.export.PredictionResult
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.presentation.screen.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * ViewModel for settings screen
 */
class SettingsViewModel(
    private val repository: DriftRepository,
    private val context: Context
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
            val prefs = context.getSharedPreferences("drift_detector_prefs", Context.MODE_PRIVATE)

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
                val prefs =
                    context.getSharedPreferences("drift_detector_prefs", Context.MODE_PRIVATE)
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
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, exportError = null) }
                Timber.d("📤 Starting data export...")

                // Get all active models
                val models = repository.getActiveModels().first()

                if (models.isEmpty()) {
                    Timber.w("⚠️ No models found to export")
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = "No models found. Please upload a model first."
                        )
                    }
                    return@launch
                }

                val exportedFiles = mutableListOf<String>()

                models.forEach { model ->
                    try {
                        Timber.d("📊 Exporting data for model: ${model.name}")

                        // Get drift results
                        val driftResults = repository.getDriftResultsByModel(model.id).first()

                        // Get patches
                        val patches = repository.getPatchesByModel(model.id).first()

                        // Get recent predictions (last 7 days)
                        val startTime = Instant.now().minus(7, ChronoUnit.DAYS)
                        val predictions =
                            repository.getRecentPredictions(model.id, startTime).first()

                        // Export drift report (JSON)
                        if (driftResults.isNotEmpty()) {
                            val driftReportResult = exportManager.exportDriftReport(
                                model = model,
                                driftResults = driftResults,
                                patches = patches
                            )

                            driftReportResult.onSuccess { exportResult ->
                                Timber.i("✅ Exported drift report: ${exportResult.fileName}")
                                exportedFiles.add(exportResult.fileName)
                            }
                        }

                        // Export predictions (CSV)
                        if (predictions.isNotEmpty()) {
                            val predictionResults = predictions.map { pred ->
                                PredictionResult(
                                    timestamp = pred.timestamp,
                                    input = pred.input.features,
                                    prediction = pred.outputs,
                                    confidence = pred.confidence,
                                    modelVersion = model.version,
                                    patchId = null, // TODO: Track patch ID with predictions
                                    driftScore = null // TODO: Track drift score with predictions
                                )
                            }

                            val predictionsResult = exportManager.exportPredictionsToCsv(
                                modelName = model.name,
                                predictions = predictionResults,
                                includeMetadata = true
                            )

                            predictionsResult.onSuccess { exportResult ->
                                Timber.i("✅ Exported predictions: ${exportResult.fileName}")
                                exportedFiles.add(exportResult.fileName)
                            }
                        }

                    } catch (e: Exception) {
                        Timber.e(e, "❌ Failed to export data for model: ${model.name}")
                    }
                }

                if (exportedFiles.isEmpty()) {
                    Timber.w("⚠️ No data available to export")
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportError = "No data available to export. Models need to have predictions or drift results."
                        )
                    }
                } else {
                    Timber.i("✅ Export complete! Files: ${exportedFiles.joinToString(", ")}")
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = true,
                            lastExportedFiles = exportedFiles
                        )
                    }

                    // Show export location
                    val exportDir = context.getExternalFilesDir(null)?.absolutePath
                    Timber.i("📁 Exported files location: $exportDir")
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Export failed")
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = "Export failed: ${e.message}"
                    )
                }
            }
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
                val exportDir = context.getExternalFilesDir(null)
                val files = exportDir?.listFiles()?.filter { file ->
                    file.name.startsWith("predictions_") ||
                            file.name.startsWith("drift_report_") ||
                            file.name.startsWith("patch_comparison_")
                }?.sortedByDescending { it.lastModified() }

                if (files.isNullOrEmpty()) {
                    Timber.w("⚠️ No export files found")
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
                    else -> "*/*"
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "DriftGuard AI Export - ${latestFile.name}")
                    putExtra(Intent.EXTRA_TEXT, "Exported data from DriftGuard AI")
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
            val exportDir = context.getExternalFilesDir(null)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        exportDir!!
                    ),
                    "resource/folder"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Export Folder"))
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
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportError: String? = null,
    val lastExportedFiles: List<String> = emptyList(),

    // About
    val appVersion: String = "1.0.0",
    val storageUsed: String = "Calculating..."
)
