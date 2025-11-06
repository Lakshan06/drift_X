package com.driftdetector.app.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

/**
 * Automatic backup manager with progress tracking and user-friendly notifications
 * Backs up models, patches, drift results, and configurations
 */
class AutomaticBackupManager(
    private val context: Context,
    private val repository: DriftRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    /**
     * Perform automatic backup of all critical data
     */
    suspend fun performAutomaticBackup(
        includeModels: Boolean = true,
        includePatches: Boolean = true,
        includeDriftHistory: Boolean = true,
        includeSettings: Boolean = true,
        customDestination: Uri? = null
    ): Result<BackupResult> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress(0, "Initializing backup...")
            Timber.i("🔄 Starting automatic backup...")

            val timestamp = dateFormat.format(Date())
            val backupFileName = "driftguard_backup_$timestamp"
            
            // Create temporary directory for backup files
            val tempBackupDir = File(context.cacheDir, "backup_temp_$timestamp")
            tempBackupDir.mkdirs()

            var totalItems = 0
            var completedItems = 0
            val backupFiles = mutableListOf<File>()

            // Backup models
            if (includeModels) {
                _backupState.value = BackupState.InProgress(20, "Backing up models...")
                val modelsFile = backupModels(tempBackupDir)
                if (modelsFile != null) {
                    backupFiles.add(modelsFile)
                    completedItems++
                }
                totalItems++
            }

            // Backup patches
            if (includePatches) {
                _backupState.value = BackupState.InProgress(40, "Backing up patches...")
                val patchesFile = backupPatches(tempBackupDir)
                if (patchesFile != null) {
                    backupFiles.add(patchesFile)
                    completedItems++
                }
                totalItems++
            }

            // Backup drift history
            if (includeDriftHistory) {
                _backupState.value = BackupState.InProgress(60, "Backing up drift history...")
                val driftFile = backupDriftHistory(tempBackupDir)
                if (driftFile != null) {
                    backupFiles.add(driftFile)
                    completedItems++
                }
                totalItems++
            }

            // Backup settings
            if (includeSettings) {
                _backupState.value = BackupState.InProgress(80, "Backing up settings...")
                val settingsFile = backupSettings(tempBackupDir)
                if (settingsFile != null) {
                    backupFiles.add(settingsFile)
                    completedItems++
                }
                totalItems++
            }

            // Create zip archive
            _backupState.value = BackupState.InProgress(90, "Creating backup archive...")
            
            val zipFile = if (customDestination != null) {
                createZipToCustomLocation(backupFiles, "$backupFileName.zip", customDestination)
            } else {
                createZipFile(backupFiles, "$backupFileName.zip")
            }

            // Cleanup temp files
            tempBackupDir.deleteRecursively()

            val result = BackupResult(
                file = zipFile,
                timestamp = Date(),
                itemCount = completedItems,
                totalSize = zipFile.length(),
                backupType = BackupType.AUTOMATIC
            )

            _backupState.value = BackupState.Success(result)
            Timber.i("✅ Backup completed successfully: ${zipFile.name}")
            Timber.i("   Total items: $completedItems, Size: ${result.sizeMB} MB")

            Result.success(result)

        } catch (e: Exception) {
            Timber.e(e, "❌ Automatic backup failed")
            _backupState.value = BackupState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Backup all models and their metadata
     */
    private suspend fun backupModels(backupDir: File): File? {
        return try {
            val models = mutableListOf<MLModel>()
            repository.getActiveModels().collect { modelList ->
                models.addAll(modelList)
            }
            
            if (models.isEmpty()) {
                Timber.d("No models to backup")
                return null
            }

            val modelsFile = File(backupDir, "models_backup.json")
            val jsonArray = JSONArray()

            models.forEach { model ->
                jsonArray.put(JSONObject().apply {
                    put("id", model.id)
                    put("name", model.name)
                    put("version", model.version)
                    put("modelPath", model.modelPath)
                    put("createdAt", model.createdAt.toString())
                    put("inputFeatures", JSONArray(model.inputFeatures))
                    put("outputLabels", JSONArray(model.outputLabels))
                    put("isActive", model.isActive)
                })
            }

            val rootObj = JSONObject().apply {
                put("backupDate", Date().toString())
                put("modelCount", models.size)
                put("models", jsonArray)
            }

            modelsFile.writeText(rootObj.toString(2))
            Timber.d("✅ Backed up ${models.size} models")
            modelsFile

        } catch (e: Exception) {
            Timber.e(e, "Failed to backup models")
            null
        }
    }

    /**
     * Backup all patches and their configurations
     */
    private suspend fun backupPatches(backupDir: File): File? {
        return try {
            // Get all applied patches across all models
            val models = mutableListOf<MLModel>()
            repository.getActiveModels().collect { modelList ->
                models.addAll(modelList)
            }

            val allPatches = mutableListOf<Patch>()

            models.forEach { model ->
                repository.getPatchesByModel(model.id).collect { patches ->
                    allPatches.addAll(patches.filter { it.status == PatchStatus.APPLIED })
                }
            }

            if (allPatches.isEmpty()) {
                Timber.d("No patches to backup")
                return null
            }

            val patchesFile = File(backupDir, "patches_backup.json")
            val jsonArray = JSONArray()

            allPatches.forEach { patch ->
                jsonArray.put(JSONObject().apply {
                    put("id", patch.id)
                    put("modelId", patch.modelId)
                    put("patchType", patch.patchType.name)
                    put("status", patch.status.name)
                    put("createdAt", patch.createdAt.toString())
                    put("appliedAt", patch.appliedAt?.toString())
                    
                    // Include validation metrics if available
                    patch.validationResult?.let { validation ->
                        put("validationMetrics", JSONObject().apply {
                            put("accuracy", validation.metrics.accuracy)
                            put("safetyScore", validation.metrics.safetyScore)
                            put("driftReduction", validation.metrics.driftReduction)
                        })
                    }
                })
            }

            val rootObj = JSONObject().apply {
                put("backupDate", Date().toString())
                put("patchCount", allPatches.size)
                put("patches", jsonArray)
            }

            patchesFile.writeText(rootObj.toString(2))
            Timber.d("✅ Backed up ${allPatches.size} patches")
            patchesFile

        } catch (e: Exception) {
            Timber.e(e, "Failed to backup patches")
            null
        }
    }

    /**
     * Backup drift history
     */
    private suspend fun backupDriftHistory(backupDir: File): File? {
        return try {
            val models = mutableListOf<MLModel>()
            repository.getActiveModels().collect { modelList ->
                models.addAll(modelList)
            }

            val driftFile = File(backupDir, "drift_history_backup.json")
            val allDrifts = JSONArray()

            models.forEach { model ->
                repository.getDriftResultsByModel(model.id).collect { drifts ->
                    drifts.forEach { drift ->
                        allDrifts.put(JSONObject().apply {
                            put("modelId", model.id)
                            put("modelName", model.name)
                            put("timestamp", drift.timestamp.toString())
                            put("driftType", drift.driftType.name)
                            put("driftScore", drift.driftScore)
                            put("isDriftDetected", drift.isDriftDetected)
                            put("affectedFeatures", drift.featureDrifts.count { it.isDrifted })
                        })
                    }
                }
            }

            val rootObj = JSONObject().apply {
                put("backupDate", Date().toString())
                put("driftEventCount", allDrifts.length())
                put("driftHistory", allDrifts)
            }

            driftFile.writeText(rootObj.toString(2))
            Timber.d("✅ Backed up ${allDrifts.length()} drift events")
            driftFile

        } catch (e: Exception) {
            Timber.e(e, "Failed to backup drift history")
            null
        }
    }

    /**
     * Backup app settings and preferences
     */
    private suspend fun backupSettings(backupDir: File): File? {
        return try {
            val settingsFile = File(backupDir, "settings_backup.json")
            val prefs = context.getSharedPreferences("drift_guard_prefs", Context.MODE_PRIVATE)

            val settingsObj = JSONObject().apply {
                put("backupDate", Date().toString())
                put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                
                // Export all preferences
                val prefsMap = prefs.all
                val prefsJson = JSONObject()
                prefsMap.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> prefsJson.put(key, value)
                        is String -> prefsJson.put(key, value)
                        is Int -> prefsJson.put(key, value)
                        is Float -> prefsJson.put(key, value)
                        is Long -> prefsJson.put(key, value)
                    }
                }
                put("preferences", prefsJson)
            }

            settingsFile.writeText(settingsObj.toString(2))
            Timber.d("✅ Backed up settings")
            settingsFile

        } catch (e: Exception) {
            Timber.e(e, "Failed to backup settings")
            null
        }
    }

    /**
     * Create zip archive from backup files
     */
    private fun createZipFile(files: List<File>, zipFileName: String): File {
        val zipFile = File(context.getExternalFilesDir("backups"), zipFileName).apply {
            parentFile?.mkdirs()
        }

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            files.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        return zipFile
    }

    /**
     * Create zip archive to custom location (user-selected directory)
     */
    private fun createZipToCustomLocation(
        files: List<File>,
        zipFileName: String,
        destinationUri: Uri
    ): File {
        // First create temp zip
        val tempZip = createZipFile(files, zipFileName)
        
        // Copy to custom location
        try {
            val destDir = DocumentFile.fromTreeUri(context, destinationUri)
            val zipDoc = destDir?.createFile("application/zip", zipFileName)
            
            zipDoc?.let { doc ->
                context.contentResolver.openOutputStream(doc.uri)?.use { output ->
                    tempZip.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy to custom location, using default")
        }
        
        return tempZip
    }

    /**
     * Restore from backup
     */
    suspend fun restoreFromBackup(
        backupFile: File
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress(0, "Restoring from backup...")
            Timber.i("🔄 Starting restore from: ${backupFile.name}")

            // TODO: Implement restore logic
            // 1. Extract zip
            // 2. Parse JSON files
            // 3. Restore to database
            // 4. Update UI

            _backupState.value = BackupState.Idle
            Result.success(RestoreResult(itemsRestored = 0, timestamp = Date()))

        } catch (e: Exception) {
            Timber.e(e, "❌ Restore failed")
            _backupState.value = BackupState.Error(e.message ?: "Restore failed")
            Result.failure(e)
        }
    }

    /**
     * List available backups
     */
    suspend fun listAvailableBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val backupsDir = context.getExternalFilesDir("backups")
            backupsDir?.listFiles { file -> 
                file.extension == "zip" && file.name.startsWith("driftguard_backup_")
            }?.map { file ->
                BackupInfo(
                    file = file,
                    timestamp = Date(file.lastModified()),
                    size = file.length()
                )
            }?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to list backups")
            emptyList()
        }
    }

    /**
     * Delete old backups (keep last N backups)
     */
    suspend fun cleanupOldBackups(keepCount: Int = 5): Int = withContext(Dispatchers.IO) {
        try {
            val backups = listAvailableBackups()
            var deletedCount = 0

            if (backups.size > keepCount) {
                backups.drop(keepCount).forEach { backup ->
                    if (backup.file.delete()) {
                        deletedCount++
                    }
                }
            }

            Timber.i("✅ Cleaned up $deletedCount old backups")
            deletedCount

        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup backups")
            0
        }
    }
}

/**
 * Backup state for UI feedback
 */
sealed class BackupState {
    object Idle : BackupState()
    data class InProgress(val progress: Int, val message: String) : BackupState()
    data class Success(val result: BackupResult) : BackupState()
    data class Error(val message: String) : BackupState()
}

/**
 * Backup result
 */
data class BackupResult(
    val file: File,
    val timestamp: Date,
    val itemCount: Int,
    val totalSize: Long,
    val backupType: BackupType
) {
    val sizeMB: Double get() = totalSize / (1024.0 * 1024.0)
    val sizeFormatted: String get() = "%.2f MB".format(sizeMB)
}

/**
 * Restore result
 */
data class RestoreResult(
    val itemsRestored: Int,
    val timestamp: Date
)

/**
 * Backup type
 */
enum class BackupType {
    AUTOMATIC,
    MANUAL,
    SCHEDULED
}

/**
 * Backup info
 */
data class BackupInfo(
    val file: File,
    val timestamp: Date,
    val size: Long
) {
    val sizeMB: Double get() = size / (1024.0 * 1024.0)
    val sizeFormatted: String get() = "%.2f MB".format(sizeMB)
    val name: String get() = file.name
}
