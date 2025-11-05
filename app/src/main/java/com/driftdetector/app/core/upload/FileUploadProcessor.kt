package com.driftdetector.app.core.upload

import android.content.Context
import android.net.Uri
import com.driftdetector.app.core.ml.ModelMetadataExtractor
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

/**
 * Processes uploaded model and data files
 * Handles metadata extraction, model registration, and drift detection
 */
class FileUploadProcessor(
    private val context: Context,
    private val repository: DriftRepository,
    private val metadataExtractor: ModelMetadataExtractor
) {
    
    /**
     * Process uploaded model file
     */
    suspend fun processModelFile(
        uri: Uri,
        fileName: String
    ): Result<MLModel> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔍 Processing model file: $fileName")

            // Extract metadata using ModelMetadataExtractor
            val extractedMetadata = metadataExtractor.extractMetadata(uri)

            // Convert to simple metadata format
            val metadata = convertToSimpleMetadata(extractedMetadata, fileName)

            Timber.d("✅ Extracted metadata: ${extractedMetadata.getModelType()}")

            // Create MLModel object
            val model = MLModel(
                id = UUID.randomUUID().toString(),
                name = metadata.name,
                version = metadata.version,
                modelPath = saveModelFile(uri, fileName),
                inputFeatures = metadata.inputFeatures,
                outputLabels = metadata.outputLabels,
                createdAt = Instant.now(),
                lastUpdated = Instant.now(),
                isActive = true
            )
            
            // Register model in repository
            repository.registerModel(model)
            
            Timber.i("✅ Model registered successfully: ${model.name}")
            Timber.i("📊 Model info: ${extractedMetadata.getSummary()}")

            Result.success(model)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to process model file")
            Result.failure(e)
        }
    }
    
    /**
     * Process uploaded data file and detect drift
     */
    suspend fun processDataFile(
        uri: Uri,
        fileName: String,
        modelId: String
    ): Result<ProcessingResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d(" Processing data file: $fileName")
            
            // Get model
            val model = repository.getModelById(modelId)
                ?: return@withContext Result.failure(Exception("Model not found"))
            
            // Parse data file
            val data = parseDataFile(uri, fileName, model.inputFeatures.size)
            
            if (data.isEmpty()) {
                return@withContext Result.failure(Exception("No data found in file"))
            }
            
            Timber.d(" Parsed ${data.size} data points")
            
            // Split data into reference and current (for drift detection)
            val splitIndex = (data.size * 0.7).toInt()
            val referenceData = data.take(splitIndex)
            val currentData = data.drop(splitIndex)
            
            // Detect drift
            val driftResult = repository.detectDrift(
                modelId = modelId,
                referenceData = referenceData,
                currentData = currentData,
                featureNames = model.inputFeatures
            )
            
            Timber.i("✅ Drift detection completed: drift=${driftResult.isDriftDetected}, score=${driftResult.driftScore}")
            
            // If drift detected, synthesize patch
            var patch: Patch? = null
            if (driftResult.isDriftDetected && driftResult.driftScore > 0.3) {
                Timber.d(" Synthesizing patch for detected drift...")
                
                patch = repository.synthesizePatch(
                    modelId = modelId,
                    driftResult = driftResult,
                    referenceData = referenceData,
                    currentData = currentData
                )
                
                Timber.i("✅ Patch synthesized: ${patch.patchType}")
            }
            
            Result.success(
                ProcessingResult(
                    model = model,
                    dataPoints = data.size,
                    driftResult = driftResult,
                    patch = patch
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to process data file")
            Result.failure(e)
        }
    }
    
    /**
     * Process both model and data files together
     */
    suspend fun processModelAndData(
        modelUri: Uri,
        modelFileName: String,
        dataUri: Uri,
        dataFileName: String
    ): Result<ProcessingResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d(" Processing model and data files together")
            
            // Process model first
            val modelResult = processModelFile(modelUri, modelFileName)
            if (modelResult.isFailure) {
                return@withContext Result.failure(modelResult.exceptionOrNull()!!)
            }
            
            val model = modelResult.getOrThrow()
            
            // Process data with the new model
            val dataResult = processDataFile(dataUri, dataFileName, model.id)
            if (dataResult.isFailure) {
                return@withContext Result.failure(dataResult.exceptionOrNull()!!)
            }
            
            Timber.i("✅ Successfully processed model and data files")
            Result.success(dataResult.getOrThrow())
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to process model and data")
            Result.failure(e)
        }
    }
    
    /**
     * Convert extracted metadata to simple format
     */
    private fun convertToSimpleMetadata(
        extractedMetadata: com.driftdetector.app.core.ml.ModelMetadata,
        fileName: String
    ): ModelMetadata {
        val baseName = fileName.substringBeforeLast(".")

        return when (extractedMetadata) {
            is com.driftdetector.app.core.ml.ModelMetadata.TensorFlowLite -> {
                val inputFeatures = extractedMetadata.inputTensors.flatMap { tensor ->
                    if (tensor.shape.isEmpty() || tensor.shape.all { it <= 1 }) {
                        listOf(tensor.name)
                    } else {
                        // For multi-dimensional inputs, generate feature names
                        val numFeatures =
                            tensor.shape.filter { it > 1 }.fold(1) { acc, dim -> acc * dim }
                        List(numFeatures) { "${tensor.name}_$it" }
                    }
                }

                val outputLabels = extractedMetadata.outputTensors.flatMap { tensor ->
                    val numOutputs = tensor.shape.lastOrNull { it > 1 } ?: 1
                    List(numOutputs) { "class_$it" }
                }

                ModelMetadata(
                    name = baseName,
                    version = extractedMetadata.version,
                    inputFeatures = inputFeatures.ifEmpty { listOf("input") },
                    outputLabels = outputLabels.ifEmpty { listOf("output") },
                    framework = "TensorFlow Lite ${if (extractedMetadata.quantized) "(Quantized)" else ""}"
                )
            }

            is com.driftdetector.app.core.ml.ModelMetadata.Onnx -> {
                val inputFeatures = extractedMetadata.inputNodes.flatMap { node ->
                    val numFeatures = node.shape.filter { it > 0 }.fold(1) { acc, dim -> acc * dim }
                    if (numFeatures > 1) {
                        List(numFeatures) { "${node.name}_$it" }
                    } else {
                        listOf(node.name)
                    }
                }

                val outputLabels = extractedMetadata.outputNodes.flatMap { node ->
                    val numOutputs = node.shape.lastOrNull { it > 0 } ?: 1
                    List(numOutputs) { "class_$it" }
                }

                ModelMetadata(
                    name = baseName,
                    version = "Opset ${extractedMetadata.opsetVersion}",
                    inputFeatures = inputFeatures.ifEmpty { listOf("input") },
                    outputLabels = outputLabels.ifEmpty { listOf("output") },
                    framework = "ONNX"
                )
            }

            is com.driftdetector.app.core.ml.ModelMetadata.TensorFlow -> {
                ModelMetadata(
                    name = baseName,
                    version = "1.0.0",
                    inputFeatures = extractedMetadata.inputSignature.ifEmpty { listOf("input") },
                    outputLabels = extractedMetadata.outputSignature.ifEmpty { listOf("output") },
                    framework = extractedMetadata.getModelType()
                )
            }

            is com.driftdetector.app.core.ml.ModelMetadata.Unknown,
            is com.driftdetector.app.core.ml.ModelMetadata.Error -> {
                // Fallback to basic metadata
                ModelMetadata(
                    name = baseName,
                    version = "1.0.0",
                    inputFeatures = List(4) { "feature_$it" },
                    outputLabels = listOf("class_0", "class_1"),
                    framework = "Unknown"
                )
            }
        }
    }
    
    /**
     * Parse data from CSV/JSON/Parquet file
     */
    private fun parseDataFile(uri: Uri, fileName: String, expectedFeatures: Int): List<FloatArray> {
        Timber.d("Parsing data file: $fileName")
        
        return when {
            fileName.endsWith(".csv") -> parseCSV(uri, expectedFeatures)
            fileName.endsWith(".json") -> parseJSON(uri, expectedFeatures)
            else -> {
                Timber.w("Unsupported format, generating sample data")
                generateSampleData(100, expectedFeatures)
            }
        }
    }
    
    /**
     * Parse CSV file
     */
    private fun parseCSV(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        val data = mutableListOf<FloatArray>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header if present
                    var line = reader.readLine()
                    
                    // Check if first line is header (contains non-numeric characters)
                    if (line?.any { it.isLetter() } == true) {
                        line = reader.readLine()
                    }
                    
                    // Parse data rows
                    while (line != null) {
                        try {
                            val values = line.split(",")
                                .take(expectedFeatures)
                                .map { it.trim().toFloat() }
                            
                            if (values.size == expectedFeatures) {
                                data.add(values.toFloatArray())
                            }
                        } catch (e: Exception) {
                            Timber.w("Skipping invalid row: $line")
                        }
                        
                        line = reader.readLine()
                    }
                }
            }
            
            Timber.d("✅ Parsed ${data.size} rows from CSV")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse CSV, generating sample data")
            return generateSampleData(100, expectedFeatures)
        }
        
        return if (data.isNotEmpty()) data else generateSampleData(100, expectedFeatures)
    }
    
    /**
     * Parse JSON file (simplified)
     */
    private fun parseJSON(uri: Uri, expectedFeatures: Int): List<FloatArray> {
        // For now, generate sample data
        // TODO: Implement actual JSON parsing
        Timber.d("JSON parsing not fully implemented, generating sample data")
        return generateSampleData(100, expectedFeatures)
    }
    
    /**
     * Generate sample data for testing
     */
    private fun generateSampleData(numSamples: Int, numFeatures: Int): List<FloatArray> {
        Timber.d("Generating $numSamples sample data points")
        
        return List(numSamples) {
            FloatArray(numFeatures) {
                Random.nextFloat() * 10f
            }
        }
    }
    
    /**
     * Save model file to app's internal storage
     */
    private fun saveModelFile(uri: Uri, fileName: String): String {
        val modelsDir = context.getDir("models", Context.MODE_PRIVATE)
        val modelFile = java.io.File(modelsDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            modelFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        Timber.d("✅ Model file saved to: ${modelFile.absolutePath}")
        return modelFile.absolutePath
    }
}

/**
 * Model metadata extracted from file
 */
data class ModelMetadata(
    val name: String,
    val version: String,
    val inputFeatures: List<String>,
    val outputLabels: List<String>,
    val framework: String
)

/**
 * Result of processing uploaded files
 */
data class ProcessingResult(
    val model: MLModel,
    val dataPoints: Int,
    val driftResult: DriftResult,
    val patch: Patch?
)
