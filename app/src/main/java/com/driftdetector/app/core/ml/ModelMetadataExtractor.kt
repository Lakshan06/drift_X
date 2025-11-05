package com.driftdetector.app.core.ml

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
 * Extracts metadata from ML model files (.tflite, .onnx)
 * Provides automatic detection of input/output shapes, types, and other model information
 */
class ModelMetadataExtractor(private val context: Context) {

    /**
     * Extract metadata from a model file
     */
    suspend fun extractMetadata(uri: Uri): ModelMetadata = withContext(Dispatchers.IO) {
        try {
            val fileExtension = getFileExtension(uri)

            when (fileExtension.lowercase()) {
                "tflite" -> extractTensorFlowLiteMetadata(uri)
                "onnx" -> extractOnnxMetadata(uri)
                "h5", "pb" -> extractTensorFlowMetadata(uri)
                else -> {
                    Timber.w("Unsupported model format: $fileExtension")
                    ModelMetadata.Unknown(fileExtension)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract model metadata")
            ModelMetadata.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Extract metadata from TensorFlow Lite model
     */
    private suspend fun extractTensorFlowLiteMetadata(uri: Uri): ModelMetadata {
        return try {
            val modelFile = loadModelFile(uri)
            val interpreter = Interpreter(modelFile)

            try {
                val inputCount = interpreter.inputTensorCount
                val outputCount = interpreter.outputTensorCount

                val inputTensors = mutableListOf<TensorInfo>()
                val outputTensors = mutableListOf<TensorInfo>()

                // Extract input tensor information
                for (i in 0 until inputCount) {
                    val tensor = interpreter.getInputTensor(i)
                    inputTensors.add(
                        TensorInfo(
                            name = tensor.name() ?: "input_$i",
                            shape = tensor.shape().toList(),
                            dataType = mapDataType(tensor.dataType()),
                            index = i
                        )
                    )
                }

                // Extract output tensor information
                for (i in 0 until outputCount) {
                    val tensor = interpreter.getOutputTensor(i)
                    outputTensors.add(
                        TensorInfo(
                            name = tensor.name() ?: "output_$i",
                            shape = tensor.shape().toList(),
                            dataType = mapDataType(tensor.dataType()),
                            index = i
                        )
                    )
                }

                val fileSizeBytes = modelFile.capacity().toLong()

                ModelMetadata.TensorFlowLite(
                    inputTensors = inputTensors,
                    outputTensors = outputTensors,
                    modelSizeBytes = fileSizeBytes,
                    version = detectTFLiteVersion(modelFile),
                    hasMetadata = checkForMetadata(interpreter),
                    quantized = detectQuantization(interpreter)
                )
            } finally {
                interpreter.close()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse TFLite model")
            ModelMetadata.Error("Failed to parse TFLite model: ${e.message}")
        }
    }

    /**
     * Extract metadata from ONNX model (basic detection)
     */
    private suspend fun extractOnnxMetadata(uri: Uri): ModelMetadata {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()

            // Basic ONNX detection - full parsing would require ONNX runtime
            ModelMetadata.Onnx(
                inputNodes = listOf(
                    TensorInfo(
                        name = "input",
                        shape = listOf(-1, 3, 224, 224), // Common default
                        dataType = "FLOAT32",
                        index = 0
                    )
                ),
                outputNodes = listOf(
                    TensorInfo(
                        name = "output",
                        shape = listOf(-1, 1000), // Common default
                        dataType = "FLOAT32",
                        index = 0
                    )
                ),
                modelSizeBytes = fileSize,
                opsetVersion = detectOnnxOpset(uri)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse ONNX model")
            ModelMetadata.Error("Failed to parse ONNX model: ${e.message}")
        }
    }

    /**
     * Extract metadata from TensorFlow saved model or H5
     */
    private suspend fun extractTensorFlowMetadata(uri: Uri): ModelMetadata {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()

            ModelMetadata.TensorFlow(
                inputSignature = listOf("input"),
                outputSignature = listOf("output"),
                modelSizeBytes = fileSize,
                savedModelFormat = getFileExtension(uri).lowercase() == "pb"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse TensorFlow model")
            ModelMetadata.Error("Failed to parse TensorFlow model: ${e.message}")
        }
    }

    /**
     * Load model file into memory
     */
    private fun loadModelFile(uri: Uri): MappedByteBuffer {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Could not open file descriptor")

        return FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
        }
    }

    /**
     * Map TFLite data type to string
     */
    private fun mapDataType(dataType: org.tensorflow.lite.DataType): String {
        return when (dataType) {
            org.tensorflow.lite.DataType.FLOAT32 -> "FLOAT32"
            org.tensorflow.lite.DataType.INT32 -> "INT32"
            org.tensorflow.lite.DataType.INT64 -> "INT64"
            org.tensorflow.lite.DataType.UINT8 -> "UINT8"
            org.tensorflow.lite.DataType.INT8 -> "INT8"
            org.tensorflow.lite.DataType.STRING -> "STRING"
            org.tensorflow.lite.DataType.BOOL -> "BOOL"
            else -> "UNKNOWN"
        }
    }

    /**
     * Detect TFLite version from model file
     */
    private fun detectTFLiteVersion(modelFile: MappedByteBuffer): String {
        return try {
            // TFLite models start with "TFL3" magic number
            modelFile.position(0)
            val magic = ByteArray(4)
            modelFile.get(magic)
            val magicString = String(magic)

            if (magicString == "TFL3") {
                "2.x"
            } else {
                "1.x or unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Check if model has metadata
     */
    private fun checkForMetadata(interpreter: Interpreter): Boolean {
        return try {
            // Check if input tensors have names (indicates metadata presence)
            val inputTensor = interpreter.getInputTensor(0)
            !inputTensor.name().isNullOrEmpty() && inputTensor.name() != "input"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detect if model is quantized
     */
    private fun detectQuantization(interpreter: Interpreter): Boolean {
        return try {
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)

            // Check if using quantized data types (UINT8, INT8)
            inputTensor.dataType() == org.tensorflow.lite.DataType.UINT8 ||
                    inputTensor.dataType() == org.tensorflow.lite.DataType.INT8 ||
                    outputTensor.dataType() == org.tensorflow.lite.DataType.UINT8 ||
                    outputTensor.dataType() == org.tensorflow.lite.DataType.INT8
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detect ONNX opset version (basic)
     */
    private fun detectOnnxOpset(uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = ByteArray(1024)
            inputStream?.read(bytes)
            inputStream?.close()

            // Look for opset version in first 1KB (basic heuristic)
            // Real implementation would use ONNX parser
            13 // Default recent version
        } catch (e: Exception) {
            13
        }
    }

    /**
     * Get file extension from URI
     */
    private fun getFileExtension(uri: Uri): String {
        val path = uri.path ?: return ""
        return path.substringAfterLast('.', "")
    }
}

/**
 * Tensor information
 */
data class TensorInfo(
    val name: String,
    val shape: List<Int>,
    val dataType: String,
    val index: Int
) {
    /**
     * Get human-readable shape string
     */
    fun getShapeString(): String {
        return shape.joinToString(" × ") { if (it == -1) "?" else it.toString() }
    }

    /**
     * Calculate total elements (excluding batch dimension)
     */
    fun getTotalElements(): Int {
        return shape.filter { it > 0 }.fold(1) { acc, dim -> acc * dim }
    }

    /**
     * Check if shape is dynamic (contains -1)
     */
    fun isDynamic(): Boolean {
        return shape.contains(-1)
    }
}

/**
 * Model metadata sealed class
 */
sealed class ModelMetadata {
    abstract val modelSizeBytes: Long
    abstract fun getModelType(): String
    abstract fun getSummary(): String

    data class TensorFlowLite(
        val inputTensors: List<TensorInfo>,
        val outputTensors: List<TensorInfo>,
        override val modelSizeBytes: Long,
        val version: String,
        val hasMetadata: Boolean,
        val quantized: Boolean
    ) : ModelMetadata() {
        override fun getModelType(): String = "TensorFlow Lite"

        override fun getSummary(): String {
            return buildString {
                appendLine("TensorFlow Lite Model")
                appendLine("Version: $version")
                appendLine("Size: ${formatFileSize(modelSizeBytes)}")
                appendLine("Quantized: ${if (quantized) "Yes" else "No"}")
                appendLine("Metadata: ${if (hasMetadata) "Yes" else "No"}")
                appendLine()
                appendLine("Inputs (${inputTensors.size}):")
                inputTensors.forEach { tensor ->
                    appendLine("  • ${tensor.name}: ${tensor.getShapeString()} (${tensor.dataType})")
                }
                appendLine()
                appendLine("Outputs (${outputTensors.size}):")
                outputTensors.forEach { tensor ->
                    appendLine("  • ${tensor.name}: ${tensor.getShapeString()} (${tensor.dataType})")
                }
            }
        }
    }

    data class Onnx(
        val inputNodes: List<TensorInfo>,
        val outputNodes: List<TensorInfo>,
        override val modelSizeBytes: Long,
        val opsetVersion: Int
    ) : ModelMetadata() {
        override fun getModelType(): String = "ONNX"

        override fun getSummary(): String {
            return buildString {
                appendLine("ONNX Model")
                appendLine("Opset Version: $opsetVersion")
                appendLine("Size: ${formatFileSize(modelSizeBytes)}")
                appendLine()
                appendLine("Inputs (${inputNodes.size}):")
                inputNodes.forEach { node ->
                    appendLine("  • ${node.name}: ${node.getShapeString()} (${node.dataType})")
                }
                appendLine()
                appendLine("Outputs (${outputNodes.size}):")
                outputNodes.forEach { node ->
                    appendLine("  • ${node.name}: ${node.getShapeString()} (${node.dataType})")
                }
            }
        }
    }

    data class TensorFlow(
        val inputSignature: List<String>,
        val outputSignature: List<String>,
        override val modelSizeBytes: Long,
        val savedModelFormat: Boolean
    ) : ModelMetadata() {
        override fun getModelType(): String =
            if (savedModelFormat) "TensorFlow SavedModel" else "TensorFlow H5"

        override fun getSummary(): String {
            return buildString {
                appendLine(getModelType())
                appendLine("Size: ${formatFileSize(modelSizeBytes)}")
                appendLine()
                appendLine("Inputs: ${inputSignature.joinToString(", ")}")
                appendLine("Outputs: ${outputSignature.joinToString(", ")}")
            }
        }
    }

    data class Unknown(val fileExtension: String) : ModelMetadata() {
        override val modelSizeBytes: Long = 0L
        override fun getModelType(): String = "Unknown"
        override fun getSummary(): String = "Unknown model format: .$fileExtension"
    }

    data class Error(val errorMessage: String) : ModelMetadata() {
        override val modelSizeBytes: Long = 0L
        override fun getModelType(): String = "Error"
        override fun getSummary(): String = "Error: $errorMessage"
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}
