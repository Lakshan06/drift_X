package com.driftdetector.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.presentation.screen.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
) : ViewModel() {

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
                monitoringIntervalMinutes = prefs.getInt("monitoring_interval_minutes", 30),
                driftThreshold = prefs.getFloat("drift_threshold", 0.3f),
                autoApplyPatches = prefs.getBoolean("auto_apply_patches", false),
                driftAlertsEnabled = prefs.getBoolean("drift_alerts_enabled", true),
                patchNotificationsEnabled = prefs.getBoolean(
                    "patch_notifications_enabled",
                    true
                ),
                criticalAlertsOnly = prefs.getBoolean("critical_alerts_only", false),
                aiExplanationsEnabled = prefs.getBoolean("ai_explanations_enabled", true),
                dataRetentionDays = prefs.getInt("data_retention_days", 30)
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
        // TODO: Implement data export
        Timber.d("Exporting data")
    }

    fun openPrivacyPolicy() {
        // TODO: Open privacy policy
        Timber.d("Opening privacy policy")
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
    val monitoringIntervalMinutes: Int = 30,
    val driftThreshold: Float = 0.3f,
    val autoApplyPatches: Boolean = false,

    // Notifications
    val driftAlertsEnabled: Boolean = true,
    val patchNotificationsEnabled: Boolean = true,
    val criticalAlertsOnly: Boolean = false,

    // AI
    val aiExplanationsEnabled: Boolean = true,
    val aiModelStatus: String = "Not Downloaded (Optional)",

    // Data Management
    val dataRetentionDays: Int = 30,

    // About
    val appVersion: String = "1.0.0",
    val storageUsed: String = "Calculating..."
)
