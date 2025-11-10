package com.driftdetector.app.core.ml

import android.content.Context
import com.driftdetector.app.core.patch.RealPatchApplicator
import com.driftdetector.app.domain.model.ModelInput
import com.driftdetector.app.domain.model.ModelPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import kotlin.math.min

/**
 * TensorFlow Lite model inference engine optimized for on-device execution
 * OPTIMIZATIONS: GPU delegation, NNAPI, batch processing, buffer reuse, patch preprocessing
 */
class TFLiteModelInference(
    private val context: Context,
    private val useGpu: Boolean = true,
    private val numThreads: Int = 4
) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var modelLoaded = false
    private var currentModelId: String? = null

    // Buffer cache for reuse (reduces allocation overhead)
    private var inputBufferCache: ByteBuffer? = null
    private var outputBufferCache: ByteBuffer? = null

    // Model metadata cache
    private var cachedInputShape: IntArray? = null
    private var cachedOutputShape: IntArray? = null

    // Patch applicator for preprocessing
    private val patchApplicator = RealPatchApplicator(context)

    /**
     * Load TFLite model from file with optimized settings
     */
    suspend fun loadModel(modelPath: String, modelId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
        try {
            Timber.d("⚡ Loading model with optimizations: GPU=$useGpu, Threads=$numThreads")
            val startTime = System.currentTimeMillis()

            currentModelId = modelId

            val modelBuffer = loadModelFile(modelPath)

            val options = Interpreter.Options().apply {
                // GPU acceleration (if available)
                if (useGpu) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Timber.d("✅ GPU delegation enabled")
                    } catch (e: Exception) {
                        Timber.w("⚠️ GPU not available, falling back to CPU: ${e.message}")
                    }
                } else {
                    setNumThreads(numThreads)
                }

                // Use NNAPI if available (Android Neural Networks API)
                try {
                    nnApiDelegate = NnApiDelegate()
                    addDelegate(nnApiDelegate)
                    Timber.d("✅ NNAPI enabled")
                } catch (e: Exception) {
                    Timber.d("ℹ️ NNAPI not available")
                }

                // Memory optimization
                setAllowBufferHandleOutput(true)
            }

            interpreter = Interpreter(modelBuffer, options)
            modelLoaded = true

            // Cache model shapes
            cachedInputShape = interpreter?.getInputTensor(0)?.shape()
            cachedOutputShape = interpreter?.getOutputTensor(0)?.shape()

            // Pre-allocate buffers
            inputBufferCache = prepareInputBuffer(FloatArray(getInputSize()))
            outputBufferCache = prepareOutputBuffer()

            val loadTime = System.currentTimeMillis() - startTime
            Timber.d("✅ Model loaded successfully in ${loadTime}ms")
            Timber.d("📊 Input shape: ${cachedInputShape?.contentToString()}")
            Timber.d("📊 Output shape: ${cachedOutputShape?.contentToString()}")

            // Check for active patches
            if (currentModelId != null && patchApplicator.hasActivePreprocessing(currentModelId!!)) {
                Timber.d("🔧 Model has active preprocessing patches")
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load model: $modelPath")
            false
        }
    }

    /**
     * Run inference on input data - OPTIMIZED WITH PATCH PREPROCESSING
     */
    suspend fun predict(input: ModelInput): ModelPrediction = withContext(Dispatchers.Default) {
        if (!modelLoaded || interpreter == null) {
            throw IllegalStateException("Model not loaded")
        }

        try {
            val startTime = System.nanoTime()

            // Apply preprocessing from active patches
            val preprocessedFeatures = if (currentModelId != null) {
                patchApplicator.applyPreprocessing(currentModelId!!, input.features)
            } else {
                input.features
            }

            // Reuse buffers if possible
            val inputBuffer = inputBufferCache?.also { it.rewind() }
                ?: prepareInputBuffer(preprocessedFeatures)
            val outputBuffer = outputBufferCache?.also { it.rewind() }
                ?: prepareOutputBuffer()

            // Fill input buffer with preprocessed data
            preprocessedFeatures.forEach { inputBuffer.putFloat(it) }
            inputBuffer.rewind()

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Extract outputs
            outputBuffer.rewind()
            val outputs = FloatArray(outputBuffer.capacity() / 4)
            outputBuffer.asFloatBuffer().get(outputs)

            // Get predicted class and confidence
            val predictedClass = outputs.indices.maxByOrNull { outputs[it] } ?: 0
            val confidence = outputs[predictedClass]

            val inferenceTime = (System.nanoTime() - startTime) / 1_000_000.0
            Timber.v("⚡ Inference completed in %.2fms".format(inferenceTime))

            ModelPrediction(
                input = input,
                outputs = outputs,
                predictedClass = predictedClass,
                confidence = confidence,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Inference failed")
            throw e
        }
    }

    /**
     * Batch prediction for multiple inputs - HIGHLY OPTIMIZED WITH PREPROCESSING
     * Uses parallel processing for large batches
     */
    suspend fun predictBatch(inputs: List<ModelInput>): List<ModelPrediction> =
        withContext(Dispatchers.Default) {
            if (inputs.isEmpty()) return@withContext emptyList()

            val startTime = System.nanoTime()
            Timber.d("⚡ Starting batch inference for ${inputs.size} inputs")

            // For small batches, process sequentially (less overhead)
            val results = if (inputs.size < 10) {
                inputs.map { predict(it) }
            } else {
                // For large batches, use parallel processing
                // Split into chunks to avoid overwhelming the system
                val chunkSize = 50
                inputs.chunked(chunkSize).flatMap { chunk ->
                    chunk.map { input ->
                        async {
                            predict(input)
                        }
                    }.awaitAll()
                }
            }

            val totalTime = (System.nanoTime() - startTime) / 1_000_000.0
            val avgTime = totalTime / inputs.size
            Timber.d(
                "✅ Batch inference complete: ${inputs.size} predictions in %.2fms (avg: %.2fms/prediction)".format(
                    totalTime,
                    avgTime
                )
            )

            results
        }

    /**
     * Get model input shape - uses cached value
     */
    fun getInputShape(): IntArray? {
        return cachedInputShape ?: interpreter?.getInputTensor(0)?.shape()?.also {
            cachedInputShape = it
        }
    }

    /**
     * Get model output shape - uses cached value
     */
    fun getOutputShape(): IntArray? {
        return cachedOutputShape ?: interpreter?.getOutputTensor(0)?.shape()?.also {
            cachedOutputShape = it
        }
    }

    /**
     * Get input size from shape
     */
    private fun getInputSize(): Int {
        val shape = getInputShape() ?: return 1
        return shape.reduce { acc, i -> acc * i }
    }

    /**
     * Get output size from shape
     */
    private fun getOutputSize(): Int {
        val shape = getOutputShape() ?: return 2
        return shape.reduce { acc, i -> acc * i }
    }

    /**
     * Prepare input buffer for TFLite - OPTIMIZED
     */
    private fun prepareInputBuffer(features: FloatArray): ByteBuffer {
        val bufferSize = getInputSize() * 4 // 4 bytes per float

        val buffer = ByteBuffer.allocateDirect(bufferSize).apply {
            order(ByteOrder.nativeOrder())
        }

        // Only put data up to buffer capacity
        val maxFeatures = min(features.size, bufferSize / 4)
        for (i in 0 until maxFeatures) {
            buffer.putFloat(features[i])
        }
        buffer.rewind()

        return buffer
    }

    /**
     * Prepare output buffer for TFLite - OPTIMIZED
     */
    private fun prepareOutputBuffer(): ByteBuffer {
        val bufferSize = getOutputSize() * 4

        return ByteBuffer.allocateDirect(bufferSize).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Load model file from storage
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val file = context.getFileStreamPath(modelPath)
        val inputStream = FileInputStream(file)
        val fileChannel = inputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Load model from assets
     */
    private fun loadModelFromAssets(fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Release resources and cleanup
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        nnApiDelegate?.close()
        interpreter = null
        gpuDelegate = null
        nnApiDelegate = null
        inputBufferCache = null
        outputBufferCache = null
        cachedInputShape = null
        cachedOutputShape = null
        modelLoaded = false
        Timber.d("🧹 Model inference resources released")
    }

    /**
     * Get model metadata
     */
    fun getModelInfo(): ModelInfo? {
        return if (modelLoaded) {
            ModelInfo(
                inputShape = getInputShape(),
                outputShape = getOutputShape(),
                inputTensorCount = interpreter?.inputTensorCount ?: 0,
                outputTensorCount = interpreter?.outputTensorCount ?: 0
            )
        } else null
    }

}

/**
 * Model information container
 */
data class ModelInfo(
    val inputShape: IntArray?,
    val outputShape: IntArray?,
    val inputTensorCount: Int,
    val outputTensorCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelInfo
        if (inputShape != null) {
            if (other.inputShape == null) return false
            if (!inputShape.contentEquals(other.inputShape)) return false
        } else if (other.inputShape != null) return false
        if (outputShape != null) {
            if (other.outputShape == null) return false
            if (!outputShape.contentEquals(other.outputShape)) return false
        } else if (other.outputShape != null) return false
        if (inputTensorCount != other.inputTensorCount) return false
        if (outputTensorCount != other.outputTensorCount) return false
        return true
    }

    override fun hashCode(): Int {
        var result = inputShape?.contentHashCode() ?: 0
        result = 31 * result + (outputShape?.contentHashCode() ?: 0)
        result = 31 * result + inputTensorCount
        result = 31 * result + outputTensorCount
        return result
    }
}
