package com.driftdetector.app.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.driftdetector.app.domain.model.*
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages export of model outputs, predictions, drift reports, and patch results
 */
class ModelExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    /**
     * Export model predictions to CSV
     */
    suspend fun exportPredictionsToCsv(
        modelName: String,
        predictions: List<PredictionResult>,
        includeMetadata: Boolean = true
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = "predictions_${modelName}_$timestamp.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            CSVWriter(FileWriter(file)).use { writer ->
                // Write header
                val header = mutableListOf("Timestamp", "Input", "Prediction", "Confidence")
                if (includeMetadata) {
                    header.addAll(listOf("Model Version", "Patch Applied", "Drift Score"))
                }
                writer.writeNext(header.toTypedArray())

                // Write predictions
                predictions.forEach { pred ->
                    val row = mutableListOf(
                        pred.timestamp.toString(),
                        pred.input.contentToString(),
                        pred.prediction.contentToString(),
                        pred.confidence.toString()
                    )

                    if (includeMetadata) {
                        row.addAll(
                            listOf(
                            pred.modelVersion ?: "N/A",
                            pred.patchId?.let { "Yes" } ?: "No",
                            pred.driftScore?.toString() ?: "N/A"
                        ))
                    }

                    writer.writeNext(row.toTypedArray())
                }
            }

            Timber.i("✅ Exported ${predictions.size} predictions to CSV: $fileName")
            Result.success(ExportResult(file, ExportFormat.CSV, predictions.size))

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export predictions to CSV")
            Result.failure(e)
        }
    }

    /**
     * Export model predictions to JSON
     */
    suspend fun exportPredictionsToJson(
        modelName: String,
        predictions: List<PredictionResult>
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = "predictions_${modelName}_$timestamp.json"
            val file = File(context.getExternalFilesDir(null), fileName)

            val jsonArray = JSONArray()
            predictions.forEach { pred ->
                val jsonObj = JSONObject().apply {
                    put("timestamp", pred.timestamp.toString())
                    put("input", JSONArray(pred.input.toList()))
                    put("prediction", JSONArray(pred.prediction.toList()))
                    put("confidence", pred.confidence)
                    put("modelVersion", pred.modelVersion)
                    put("patchApplied", pred.patchId != null)
                    put("patchId", pred.patchId)
                    put("driftScore", pred.driftScore)
                }
                jsonArray.put(jsonObj)
            }

            val rootObj = JSONObject().apply {
                put("modelName", modelName)
                put("exportedAt", timestamp)
                put("totalPredictions", predictions.size)
                put("predictions", jsonArray)
            }

            file.writeText(rootObj.toString(2))

            Timber.i("✅ Exported ${predictions.size} predictions to JSON: $fileName")
            Result.success(ExportResult(file, ExportFormat.JSON, predictions.size))

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export predictions to JSON")
            Result.failure(e)
        }
    }

    /**
     * Export patch comparison (before vs after)
     */
    suspend fun exportPatchComparison(
        patch: Patch,
        beforePredictions: List<PredictionResult>,
        afterPredictions: List<PredictionResult>,
        performanceMetrics: PatchPerformanceMetrics
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = "patch_comparison_${patch.id}_$timestamp.json"
            val file = File(context.getExternalFilesDir(null), fileName)

            val jsonObj = JSONObject().apply {
                put("patchId", patch.id)
                put("patchType", patch.patchType.name)
                put("modelId", patch.modelId)
                put("appliedAt", patch.appliedAt?.toString())
                put("exportedAt", timestamp)

                // Performance comparison
                put("performanceMetrics", JSONObject().apply {
                    put("accuracyBefore", performanceMetrics.accuracyBefore)
                    put("accuracyAfter", performanceMetrics.accuracyAfter)
                    put("accuracyImprovement", performanceMetrics.accuracyImprovement)
                    put("driftScoreBefore", performanceMetrics.driftScoreBefore)
                    put("driftScoreAfter", performanceMetrics.driftScoreAfter)
                    put("driftReduction", performanceMetrics.driftReduction)
                })

                // Before predictions
                put("beforePredictions", JSONArray().apply {
                    beforePredictions.take(100).forEach { pred ->
                        put(JSONObject().apply {
                            put("input", JSONArray(pred.input.toList()))
                            put("prediction", JSONArray(pred.prediction.toList()))
                            put("confidence", pred.confidence)
                        })
                    }
                })

                // After predictions
                put("afterPredictions", JSONArray().apply {
                    afterPredictions.take(100).forEach { pred ->
                        put(JSONObject().apply {
                            put("input", JSONArray(pred.input.toList()))
                            put("prediction", JSONArray(pred.prediction.toList()))
                            put("confidence", pred.confidence)
                        })
                    }
                })
            }

            file.writeText(jsonObj.toString(2))

            Timber.i("✅ Exported patch comparison: $fileName")
            Result.success(
                ExportResult(
                    file,
                    ExportFormat.JSON,
                    beforePredictions.size + afterPredictions.size
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export patch comparison")
            Result.failure(e)
        }
    }

    /**
     * Export comprehensive drift report
     */
    suspend fun exportDriftReport(
        model: MLModel,
        driftResults: List<DriftResult>,
        patches: List<Patch>
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = "drift_report_${model.name}_$timestamp.json"
            val file = File(context.getExternalFilesDir(null), fileName)

            val jsonObj = JSONObject().apply {
                put("reportGeneratedAt", timestamp)
                put("model", JSONObject().apply {
                    put("id", model.id)
                    put("name", model.name)
                    put("version", model.version)
                    put("createdAt", model.createdAt.toString())
                })

                // Drift history
                put("driftHistory", JSONArray().apply {
                    driftResults.forEach { drift ->
                        put(JSONObject().apply {
                            put("timestamp", drift.timestamp.toString())
                            put("driftType", drift.driftType.name)
                            put("driftScore", drift.driftScore)
                            put("isDriftDetected", drift.isDriftDetected)
                            put("threshold", drift.threshold)

                            // Feature drifts
                            put("featureDrifts", JSONArray().apply {
                                drift.featureDrifts.forEach { feature ->
                                    put(JSONObject().apply {
                                        put("featureName", feature.featureName)
                                        put("driftScore", feature.driftScore)
                                        put("psiScore", feature.psiScore)
                                        put("ksStatistic", feature.ksStatistic)
                                        put("pValue", feature.pValue)
                                        put("isDrifted", feature.isDrifted)
                                    })
                                }
                            })
                        })
                    }
                })

                // Patches applied
                put("patchesApplied", JSONArray().apply {
                    patches.forEach { patch ->
                        put(JSONObject().apply {
                            put("patchId", patch.id)
                            put("patchType", patch.patchType.name)
                            put("status", patch.status.name)
                            put("createdAt", patch.createdAt.toString())
                            put("appliedAt", patch.appliedAt?.toString())
                            put("rolledBackAt", patch.rolledBackAt?.toString())
                        })
                    }
                })

                // Summary statistics
                put("summary", JSONObject().apply {
                    put("totalDriftEvents", driftResults.size)
                    put("driftDetectedCount", driftResults.count { it.isDriftDetected })
                    put("averageDriftScore", driftResults.map { it.driftScore }.average())
                    put("maxDriftScore", driftResults.maxOfOrNull { it.driftScore })
                    put("totalPatchesApplied", patches.count { it.status == PatchStatus.APPLIED })
                    put(
                        "totalPatchesRolledBack",
                        patches.count { it.status == PatchStatus.ROLLED_BACK })
                })
            }

            file.writeText(jsonObj.toString(2))

            Timber.i("✅ Exported drift report: $fileName")
            Result.success(ExportResult(file, ExportFormat.JSON, driftResults.size))

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to export drift report")
            Result.failure(e)
        }
    }

    /**
     * Export model outputs with all metadata
     */
    suspend fun exportModelOutputs(
        model: MLModel,
        predictions: List<PredictionResult>,
        format: ExportFormat = ExportFormat.CSV
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        when (format) {
            ExportFormat.CSV -> exportPredictionsToCsv(
                model.name,
                predictions,
                includeMetadata = true
            )

            ExportFormat.JSON -> exportPredictionsToJson(model.name, predictions)
        }
    }

    /**
     * Share exported file via Android Share Sheet
     */
    fun shareExportedFile(exportResult: ExportResult): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportResult.file
        )

        val mimeType = when (exportResult.format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.JSON -> "application/json"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DriftGuardAI Export - ${exportResult.file.name}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Exported ${exportResult.recordCount} records from DriftGuardAI"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Get export file URI for direct access
     */
    fun getExportFileUri(exportResult: ExportResult): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportResult.file
        )
    }

    /**
     * Delete old export files (cleanup)
     */
    suspend fun cleanupOldExports(daysOld: Int = 7): Int = withContext(Dispatchers.IO) {
        try {
            val exportsDir = context.getExternalFilesDir(null)
            var deletedCount = 0

            exportsDir?.listFiles()?.forEach { file ->
                if (file.isFile && isExportFile(file)) {
                    val ageInDays =
                        (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60 * 24)
                    if (ageInDays > daysOld) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }

            Timber.i("✅ Cleaned up $deletedCount old export files")
            deletedCount

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to cleanup old exports")
            0
        }
    }

    private fun isExportFile(file: File): Boolean {
        return file.name.startsWith("predictions_") ||
                file.name.startsWith("patch_comparison_") ||
                file.name.startsWith("drift_report_")
    }
}

/**
 * Prediction result with metadata
 */
data class PredictionResult(
    val timestamp: java.time.Instant,
    val input: FloatArray,
    val prediction: FloatArray,
    val confidence: Float,
    val modelVersion: String? = null,
    val patchId: String? = null,
    val driftScore: Double? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictionResult

        if (timestamp != other.timestamp) return false
        if (!input.contentEquals(other.input)) return false
        if (!prediction.contentEquals(other.prediction)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + input.contentHashCode()
        result = 31 * result + prediction.contentHashCode()
        return result
    }
}

/**
 * Patch performance metrics
 */
data class PatchPerformanceMetrics(
    val accuracyBefore: Double,
    val accuracyAfter: Double,
    val accuracyImprovement: Double,
    val driftScoreBefore: Double,
    val driftScoreAfter: Double,
    val driftReduction: Double
)

/**
 * Export result
 */
data class ExportResult(
    val file: File,
    val format: ExportFormat,
    val recordCount: Int
) {
    val fileName: String get() = file.name
    val fileSizeBytes: Long get() = file.length()
    val fileSizeMB: Double get() = fileSizeBytes / (1024.0 * 1024.0)
}

/**
 * Export format
 */
enum class ExportFormat {
    CSV,
    JSON
}
