package com.driftdetector.app.core.validation

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Comprehensive file validation and analysis system
 * Validates file format, structure, and compatibility before upload
 */
class FileValidator(private val context: Context) {

    /**
     * Validate a file before upload
     */
    suspend fun validateFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Validating file: $fileName")

                val fileType = detectFileType(fileName)

                when (fileType) {
                    FileType.MODEL -> validateModelFile(uri, fileName)
                    FileType.DATA -> validateDataFile(uri, fileName)
                    FileType.UNSUPPORTED -> ValidationResult.error(
                        "Unsupported file format",
                        "File '$fileName' is not a supported format.\n\n" +
                                "Supported formats:\n" +
                                "• Models: .tflite, .onnx, .h5, .pb\n" +
                                "• Data: .csv, .json, .parquet, .txt"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate file")
                ValidationResult.error(
                    "Validation failed",
                    "Could not validate file: ${e.message}"
                )
            }
        }

    /**
     * Validate model file structure and compatibility
     */
    private suspend fun validateModelFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Analyzing model file: $fileName")

                val extension = fileName.substringAfterLast(".", "").lowercase()

                when (extension) {
                    "tflite" -> validateTFLiteModel(uri, fileName)
                    "onnx" -> validateONNXModel(uri, fileName)
                    "h5", "pb", "pt", "pth" -> ValidationResult(
                        isValid = true,
                        fileType = FileType.MODEL,
                        warnings = listOf("Model format '$extension' is supported but validation is limited. TFLite models are recommended for best compatibility."),
                        details = ModelDetails(
                            format = extension,
                            fileName = fileName,
                            isOptimized = extension == "tflite"
                        )
                    )

                    else -> ValidationResult.error(
                        "Unknown model format",
                        "Model file format '$extension' is not recognized."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Model validation failed")
                ValidationResult.error(
                    "Model validation failed",
                    "Could not analyze model: ${e.message}"
                )
            }
        }

    /**
     * Validate TFLite model structure
     */
    private suspend fun validateTFLiteModel(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            var interpreter: Interpreter? = null

            try {
                Timber.d("📊 Analyzing TFLite model structure...")

                // Load model file
                val modelBuffer = loadModelBuffer(uri)

                // Check file size
                val fileSize = modelBuffer.capacity()
                if (fileSize == 0) {
                    return@withContext ValidationResult.error(
                        "Empty model file",
                        "The model file is empty or corrupted."
                    )
                }

                if (fileSize < 1024) {
                    return@withContext ValidationResult.error(
                        "Model file too small",
                        "Model file is only $fileSize bytes. This seems too small to be a valid model."
                    )
                }

                // Try to create interpreter
                interpreter = try {
                    Interpreter(modelBuffer)
                } catch (e: Exception) {
                    return@withContext ValidationResult.error(
                        "Invalid TFLite model",
                        "Could not load model: ${e.message}\n\nThe file may be corrupted or not a valid TFLite model."
                    )
                }

                // Extract model information
                val inputTensor = interpreter.getInputTensor(0)
                val outputTensor = interpreter.getOutputTensor(0)

                val inputShape = inputTensor.shape()
                val outputShape = outputTensor.shape()
                val inputDataType = inputTensor.dataType()
                val outputDataType = outputTensor.dataType()

                // Calculate model size
                val sizeMB = fileSize / (1024.0 * 1024.0)

                // Validation checks
                val warnings = mutableListOf<String>()

                if (inputShape.isEmpty() || inputShape[0] == 0) {
                    warnings.add("Input shape seems unusual: ${inputShape.contentToString()}")
                }

                if (outputShape.isEmpty() || outputShape[0] == 0) {
                    warnings.add("Output shape seems unusual: ${outputShape.contentToString()}")
                }

                if (sizeMB > 100) {
                    warnings.add(
                        "Model is large (${
                            String.format(
                                "%.2f",
                                sizeMB
                            )
                        } MB). This may impact performance."
                    )
                }

                if (inputDataType.toString() != "FLOAT32") {
                    warnings.add("Input type is ${inputDataType}. FLOAT32 is recommended for better compatibility.")
                }

                Timber.i("✅ TFLite model validated successfully")
                Timber.i("   Input shape: ${inputShape.contentToString()}")
                Timber.i("   Output shape: ${outputShape.contentToString()}")
                Timber.i("   Size: ${String.format("%.2f", sizeMB)} MB")

                ValidationResult(
                    isValid = true,
                    fileType = FileType.MODEL,
                    warnings = warnings.takeIf { it.isNotEmpty() },
                    details = ModelDetails(
                        format = "tflite",
                        fileName = fileName,
                        inputShape = inputShape,
                        outputShape = outputShape,
                        inputType = inputDataType.toString(),
                        outputType = outputDataType.toString(),
                        fileSizeMB = sizeMB,
                        tensorCount = interpreter.inputTensorCount + interpreter.outputTensorCount,
                        isOptimized = true
                    )
                )

            } catch (e: Exception) {
                Timber.e(e, "TFLite validation failed")
                ValidationResult.error(
                    "TFLite validation failed",
                    "Could not analyze TFLite model: ${e.message}"
                )
            } finally {
                interpreter?.close()
            }
        }

    /**
     * Validate ONNX model (basic validation)
     */
    private suspend fun validateONNXModel(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("📊 Analyzing ONNX model...")

                // Check file size
                val fileSize = getFileSize(uri)
                if (fileSize == 0L) {
                    return@withContext ValidationResult.error(
                        "Empty model file",
                        "The ONNX model file is empty."
                    )
                }

                val sizeMB = fileSize / (1024.0 * 1024.0)

                // Basic ONNX file signature check
                val header = ByteArray(8)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.read(header)
                }

                // ONNX files typically start with protobuf header
                val isValidONNX = header.isNotEmpty()

                if (!isValidONNX) {
                    return@withContext ValidationResult.error(
                        "Invalid ONNX file",
                        "File does not appear to be a valid ONNX model."
                    )
                }

                val warnings = mutableListOf<String>()
                warnings.add("ONNX support is experimental. For best compatibility, convert to TFLite format.")

                if (sizeMB > 100) {
                    warnings.add("Model is large (${String.format("%.2f", sizeMB)} MB).")
                }

                ValidationResult(
                    isValid = true,
                    fileType = FileType.MODEL,
                    warnings = warnings,
                    details = ModelDetails(
                        format = "onnx",
                        fileName = fileName,
                        fileSizeMB = sizeMB,
                        isOptimized = false
                    )
                )

            } catch (e: Exception) {
                Timber.e(e, "ONNX validation failed")
                ValidationResult.error(
                    "ONNX validation failed",
                    "Could not analyze ONNX model: ${e.message}"
                )
            }
        }

    /**
     * Validate data file structure and format
     */
    private suspend fun validateDataFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Analyzing data file: $fileName")

                val extension = fileName.substringAfterLast(".", "").lowercase()

                when (extension) {
                    "csv" -> validateCSVFile(uri, fileName)
                    "json" -> validateJSONFile(uri, fileName)
                    "parquet" -> validateParquetFile(uri, fileName)
                    "txt" -> validateTextFile(uri, fileName)
                    else -> ValidationResult.error(
                        "Unsupported data format",
                        "Data file format '$extension' is not supported.\n\n" +
                                "Supported formats: CSV, JSON, Parquet, TXT"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Data validation failed")
                ValidationResult.error(
                    "Data validation failed",
                    "Could not analyze data file: ${e.message}"
                )
            }
        }

    /**
     * Validate CSV file structure
     */
    private suspend fun validateCSVFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("📊 Analyzing CSV file structure...")

                val lines = mutableListOf<String>()
                var rowCount = 0
                var columnCount = 0
                var hasHeader = false

                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    // Read first few lines
                    for (i in 0 until 100) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            lines.add(line)
                            rowCount++
                        }
                    }

                    // Try to count total rows (may be slow for large files)
                    try {
                        reader.lineSequence().forEach { rowCount++ }
                    } catch (e: Exception) {
                        // File might be too large, use sample count
                    }
                }

                if (lines.isEmpty()) {
                    return@withContext ValidationResult.error(
                        "Empty data file",
                        "The CSV file is empty or contains no data."
                    )
                }

                // Analyze structure
                val firstLine = lines.first()
                val delimiter = detectDelimiter(firstLine)
                columnCount = firstLine.split(delimiter).size

                // Check if first row is header
                hasHeader = isLikelyHeader(firstLine, delimiter)

                // Check consistency
                val warnings = mutableListOf<String>()

                lines.forEachIndexed { index, line ->
                    val cols = line.split(delimiter).size
                    if (cols != columnCount) {
                        warnings.add("Row ${index + 1} has $cols columns, expected $columnCount. File may be malformed.")
                    }
                }

                if (rowCount < 10) {
                    warnings.add("File has very few rows ($rowCount). More data is recommended for accurate drift detection.")
                }

                if (columnCount < 2) {
                    warnings.add("File has only $columnCount column(s). Multi-feature data is recommended.")
                }

                val fileSizeMB = getFileSize(uri) / (1024.0 * 1024.0)

                Timber.i("✅ CSV file validated successfully")
                Timber.i("   Rows: $rowCount")
                Timber.i("   Columns: $columnCount")
                Timber.i("   Delimiter: $delimiter")
                Timber.i("   Has header: $hasHeader")

                ValidationResult(
                    isValid = true,
                    fileType = FileType.DATA,
                    warnings = warnings.takeIf { it.isNotEmpty() },
                    details = DataDetails(
                        format = "csv",
                        fileName = fileName,
                        rowCount = rowCount,
                        columnCount = columnCount,
                        hasHeader = hasHeader,
                        delimiter = delimiter.toString(),
                        fileSizeMB = fileSizeMB
                    )
                )

            } catch (e: Exception) {
                Timber.e(e, "CSV validation failed")
                ValidationResult.error(
                    "CSV validation failed",
                    "Could not analyze CSV file: ${e.message}"
                )
            }
        }

    /**
     * Validate JSON file structure
     */
    private suspend fun validateJSONFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("📊 Analyzing JSON file structure...")

                val content =
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()

                if (content.isNullOrBlank()) {
                    return@withContext ValidationResult.error(
                        "Empty JSON file",
                        "The JSON file is empty."
                    )
                }

                // Try to parse JSON
                val jsonObject = try {
                    org.json.JSONObject(content)
                } catch (e: Exception) {
                    try {
                        org.json.JSONArray(content)
                    } catch (e2: Exception) {
                        return@withContext ValidationResult.error(
                            "Invalid JSON",
                            "File is not valid JSON: ${e.message}"
                        )
                    }
                }

                val fileSizeMB = getFileSize(uri) / (1024.0 * 1024.0)

                Timber.i("✅ JSON file validated successfully")

                ValidationResult(
                    isValid = true,
                    fileType = FileType.DATA,
                    warnings = if (fileSizeMB > 10) {
                        listOf(
                            "Large JSON file (${
                                String.format(
                                    "%.2f",
                                    fileSizeMB
                                )
                            } MB). Consider using CSV for better performance."
                        )
                    } else null,
                    details = DataDetails(
                        format = "json",
                        fileName = fileName,
                        fileSizeMB = fileSizeMB
                    )
                )

            } catch (e: Exception) {
                Timber.e(e, "JSON validation failed")
                ValidationResult.error(
                    "JSON validation failed",
                    "Could not analyze JSON file: ${e.message}"
                )
            }
        }

    /**
     * Validate Parquet file
     */
    private suspend fun validateParquetFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                val fileSizeMB = getFileSize(uri) / (1024.0 * 1024.0)

                ValidationResult(
                    isValid = true,
                    fileType = FileType.DATA,
                    warnings = listOf("Parquet support is experimental. CSV is recommended for best compatibility."),
                    details = DataDetails(
                        format = "parquet",
                        fileName = fileName,
                        fileSizeMB = fileSizeMB
                    )
                )
            } catch (e: Exception) {
                ValidationResult.error(
                    "Parquet validation failed",
                    "Could not analyze Parquet file: ${e.message}"
                )
            }
        }

    /**
     * Validate text file
     */
    private suspend fun validateTextFile(uri: Uri, fileName: String): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                val lineCount =
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        reader.lineSequence().count()
                    } ?: 0

                if (lineCount == 0) {
                    return@withContext ValidationResult.error(
                        "Empty text file",
                        "The text file is empty."
                    )
                }

                val fileSizeMB = getFileSize(uri) / (1024.0 * 1024.0)

                ValidationResult(
                    isValid = true,
                    fileType = FileType.DATA,
                    warnings = listOf("Text file format may require additional configuration. CSV is recommended for structured data."),
                    details = DataDetails(
                        format = "txt",
                        fileName = fileName,
                        rowCount = lineCount,
                        fileSizeMB = fileSizeMB
                    )
                )
            } catch (e: Exception) {
                ValidationResult.error(
                    "Text validation failed",
                    "Could not analyze text file: ${e.message}"
                )
            }
        }

    /**
     * Validate compatibility between model and data
     */
    suspend fun validateCompatibility(
        modelUri: Uri,
        modelFileName: String,
        dataUri: Uri,
        dataFileName: String
    ): CompatibilityResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔍 Validating model-data compatibility...")

            // Validate both files first
            val modelValidation = validateFile(modelUri, modelFileName)
            val dataValidation = validateFile(dataUri, dataFileName)

            if (!modelValidation.isValid) {
                return@withContext CompatibilityResult(
                    isCompatible = false,
                    issues = listOf("Model validation failed: ${modelValidation.error}")
                )
            }

            if (!dataValidation.isValid) {
                return@withContext CompatibilityResult(
                    isCompatible = false,
                    issues = listOf("Data validation failed: ${dataValidation.error}")
                )
            }

            val issues = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Check feature count compatibility
            val modelDetails = modelValidation.details as? ModelDetails
            val dataDetails = dataValidation.details as? DataDetails

            if (modelDetails != null && dataDetails != null) {
                val modelInputSize = modelDetails.inputShape?.getOrNull(1) ?: 0
                val dataColumns = dataDetails.columnCount ?: 0

                if (modelInputSize > 0 && dataColumns > 0) {
                    val expectedFeatures =
                        if (dataDetails.hasHeader == true) dataColumns - 1 else dataColumns

                    if (modelInputSize != expectedFeatures) {
                        warnings.add(
                            "Model expects $modelInputSize features, but data has $expectedFeatures columns. " +
                                    "Make sure the data matches the model's input requirements."
                        )
                    }
                }
            }

            // All checks passed
            Timber.i("✅ Model and data are compatible")

            CompatibilityResult(
                isCompatible = true,
                issues = issues.takeIf { it.isNotEmpty() },
                warnings = warnings.takeIf { it.isNotEmpty() },
                modelDetails = modelDetails,
                dataDetails = dataDetails
            )

        } catch (e: Exception) {
            Timber.e(e, "Compatibility check failed")
            CompatibilityResult(
                isCompatible = false,
                issues = listOf("Compatibility check failed: ${e.message}")
            )
        }
    }

    // Helper methods

    private fun detectFileType(fileName: String): FileType {
        val extension = fileName.substringAfterLast(".", "").lowercase()

        return when (extension) {
            "tflite", "onnx", "h5", "pb", "pt", "pth" -> FileType.MODEL
            "csv", "json", "parquet", "txt" -> FileType.DATA
            else -> FileType.UNSUPPORTED
        }
    }

    private fun loadModelBuffer(uri: Uri): MappedByteBuffer {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open model file")

        return (inputStream as FileInputStream).channel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            inputStream.channel.size()
        )
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun detectDelimiter(line: String): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        return delimiters.maxByOrNull { delimiter ->
            line.count { it == delimiter }
        } ?: ','
    }

    private fun isLikelyHeader(firstLine: String, delimiter: Char): Boolean {
        val values = firstLine.split(delimiter)

        // If any value is non-numeric, likely a header
        return values.any { value ->
            value.trim().toDoubleOrNull() == null
        }
    }
}

/**
 * File type enum
 */
enum class FileType {
    MODEL,
    DATA,
    UNSUPPORTED
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val fileType: FileType? = null,
    val error: String? = null,
    val errorDetails: String? = null,
    val warnings: List<String>? = null,
    val details: FileDetails? = null
) {
    companion object {
        fun error(error: String, details: String) = ValidationResult(
            isValid = false,
            error = error,
            errorDetails = details
        )
    }
}

/**
 * File details (base class)
 */
sealed class FileDetails

/**
 * Model file details
 */
data class ModelDetails(
    val format: String,
    val fileName: String,
    val inputShape: IntArray? = null,
    val outputShape: IntArray? = null,
    val inputType: String? = null,
    val outputType: String? = null,
    val fileSizeMB: Double = 0.0,
    val tensorCount: Int = 0,
    val isOptimized: Boolean = false
) : FileDetails() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelDetails
        if (format != other.format) return false
        if (fileName != other.fileName) return false
        if (inputShape != null) {
            if (other.inputShape == null) return false
            if (!inputShape.contentEquals(other.inputShape)) return false
        } else if (other.inputShape != null) return false
        if (outputShape != null) {
            if (other.outputShape == null) return false
            if (!outputShape.contentEquals(other.outputShape)) return false
        } else if (other.outputShape != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + (inputShape?.contentHashCode() ?: 0)
        result = 31 * result + (outputShape?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Data file details
 */
data class DataDetails(
    val format: String,
    val fileName: String,
    val rowCount: Int? = null,
    val columnCount: Int? = null,
    val hasHeader: Boolean? = null,
    val delimiter: String? = null,
    val fileSizeMB: Double = 0.0
) : FileDetails()

/**
 * Compatibility check result
 */
data class CompatibilityResult(
    val isCompatible: Boolean,
    val issues: List<String>? = null,
    val warnings: List<String>? = null,
    val modelDetails: ModelDetails? = null,
    val dataDetails: DataDetails? = null
)
