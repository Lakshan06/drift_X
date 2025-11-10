package com.driftdetector.app.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.toJSON
import com.driftdetector.app.domain.model.toReadableText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages patch export to various formats and destinations
 */
class PatchExportManager(private val context: Context) {

    companion object {
        private const val EXPORT_DIR = "DriftGuard/Patches"
        private const val JSON_EXTENSION = ".json"
        private const val TEXT_EXTENSION = ".txt"
    }

    /**
     * Export formats supported
     */
    enum class ExportFormat {
        JSON,       // Machine-readable JSON
        TEXT,       // Human-readable text
        BOTH        // Both formats
    }

    /**
     * Export result with file information
     */
    data class ExportResult(
        val success: Boolean,
        val message: String,
        val files: List<File> = emptyList(),
        val uris: List<Uri> = emptyList()
    )

    /**
     * Get the export directory, creating it if needed
     * Now saves to Downloads/DriftGuard/Patches for easy user access
     */
    private fun getExportDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val dir = File(downloadsDir, EXPORT_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
            Timber.d("📁 Created export directory: ${dir.absolutePath}")
        }
        return dir
    }

    /**
     * Generate filename for patch export
     */
    private fun generateFilename(patch: Patch, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val patchType = patch.patchType.name.lowercase()
        val shortId = patch.id.take(8)
        return "patch_${patchType}_${shortId}_$timestamp$extension"
    }

    /**
     * Export single patch to JSON format
     */
    suspend fun exportPatchToJson(patch: Patch): ExportResult = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDir()
            val filename = generateFilename(patch, JSON_EXTENSION)
            val file = File(exportDir, filename)

            // Convert patch to JSON
            val json = patch.toJSON()
            val jsonString = json.toString(2) // Pretty print with 2-space indent

            // Write to file
            FileOutputStream(file).use { output ->
                output.write(jsonString.toByteArray())
            }

            Timber.i("✅ Exported patch to JSON: ${file.absolutePath}")

            ExportResult(
                success = true,
                message = "Patch exported to JSON successfully",
                files = listOf(file)
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export patch to JSON")
            ExportResult(
                success = false,
                message = "Export failed: ${e.message}"
            )
        }
    }

    /**
     * Export single patch to human-readable text format
     */
    suspend fun exportPatchToText(patch: Patch): ExportResult = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDir()
            val filename = generateFilename(patch, TEXT_EXTENSION)
            val file = File(exportDir, filename)

            // Convert patch to readable text
            val text = patch.toReadableText()

            // Write to file
            FileOutputStream(file).use { output ->
                output.write(text.toByteArray())
            }

            Timber.i("✅ Exported patch to text: ${file.absolutePath}")

            ExportResult(
                success = true,
                message = "Patch exported to text successfully",
                files = listOf(file)
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export patch to text")
            ExportResult(
                success = false,
                message = "Export failed: ${e.message}"
            )
        }
    }

    /**
     * Export patch in specified format(s)
     */
    suspend fun exportPatch(
        patch: Patch,
        format: ExportFormat = ExportFormat.BOTH
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<ExportResult>()

            when (format) {
                ExportFormat.JSON -> {
                    results.add(exportPatchToJson(patch))
                }

                ExportFormat.TEXT -> {
                    results.add(exportPatchToText(patch))
                }

                ExportFormat.BOTH -> {
                    results.add(exportPatchToJson(patch))
                    results.add(exportPatchToText(patch))
                }
            }

            val allFiles = results.flatMap { it.files }
            val allSuccess = results.all { it.success }

            if (allSuccess) {
                ExportResult(
                    success = true,
                    message = "Exported ${allFiles.size} file(s) successfully",
                    files = allFiles
                )
            } else {
                val failedCount = results.count { !it.success }
                ExportResult(
                    success = false,
                    message = "$failedCount export(s) failed"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export patch")
            ExportResult(
                success = false,
                message = "Export failed: ${e.message}"
            )
        }
    }

    /**
     * Export multiple patches at once
     */
    suspend fun exportPatches(
        patches: List<Patch>,
        format: ExportFormat = ExportFormat.BOTH
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val allFiles = mutableListOf<File>()
            var successCount = 0
            var failedCount = 0

            patches.forEach { patch ->
                val result = exportPatch(patch, format)
                if (result.success) {
                    successCount++
                    allFiles.addAll(result.files)
                } else {
                    failedCount++
                }
            }

            Timber.i("📦 Exported ${patches.size} patches: $successCount success, $failedCount failed")

            ExportResult(
                success = failedCount == 0,
                message = "Exported $successCount of ${patches.size} patches successfully",
                files = allFiles
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export patches")
            ExportResult(
                success = false,
                message = "Batch export failed: ${e.message}"
            )
        }
    }

    /**
     * Share exported patch file via system share sheet
     */
    fun shareExportedFile(file: File, mimeType: String = "*/*"): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Drift Patch Export: ${file.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "Exported drift patch configuration file")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Intent.createChooser(shareIntent, "Share Patch Export")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to create share intent")
            null
        }
    }

    /**
     * Share multiple exported files
     */
    fun shareExportedFiles(files: List<File>, mimeType: String = "*/*"): Intent? {
        return try {
            val uris = files.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_SUBJECT, "Drift Patch Exports (${files.size} files)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Exported ${files.size} drift patch configuration files"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Intent.createChooser(shareIntent, "Share ${files.size} Patch Exports")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to create share intent for multiple files")
            null
        }
    }

    /**
     * Open file with appropriate app
     */
    fun openExportedFile(file: File, mimeType: String = "*/*"): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Intent.createChooser(openIntent, "Open with")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to create open intent")
            null
        }
    }

    /**
     * Get all exported patch files
     */
    fun getAllExportedFiles(): List<File> {
        return try {
            val exportDir = getExportDir()
            exportDir.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to list exported files")
            emptyList()
        }
    }

    /**
     * Delete an exported file
     */
    fun deleteExportedFile(file: File): Boolean {
        return try {
            val deleted = file.delete()
            if (deleted) {
                Timber.i("🗑️ Deleted exported file: ${file.name}")
            }
            deleted
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file: ${file.name}")
            false
        }
    }

    /**
     * Clear all exported files
     */
    fun clearAllExports(): Int {
        return try {
            val files = getAllExportedFiles()
            var deletedCount = 0
            files.forEach { file ->
                if (deleteExportedFile(file)) {
                    deletedCount++
                }
            }
            Timber.i("🗑️ Cleared $deletedCount exported files")
            deletedCount
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear exports")
            0
        }
    }

    /**
     * Get export directory path for display
     */
    fun getExportPath(): String {
        return getExportDir().absolutePath
    }

    /**
     * Get total size of all exports
     */
    fun getExportsTotalSize(): Long {
        return try {
            getAllExportedFiles().sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
