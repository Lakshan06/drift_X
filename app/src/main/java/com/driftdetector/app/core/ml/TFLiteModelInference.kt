package com.driftdetector.app.core.ml

import android.content.Context
import com.driftdetector.app.domain.model.ModelInput
import com.driftdetector.app.domain.model.ModelPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant

/**
 * TensorFlow Lite model inference engine optimized for on-device execution
 */
class TFLiteModelInference(
    private val context: Context,
    private val useGpu: Boolean = true,
    private val numThreads: Int = 4
) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var modelLoaded = false

    /**
     * Load TFLite model from file
     */
    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelBuffer = loadModelFile(modelPath)

            val options = Interpreter.Options().apply {
                if (useGpu) {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                } else {
                    setNumThreads(numThreads)
                }
                setUseNNAPI(true) // Use Android Neural Networks API if available
            }

            interpreter = Interpreter(modelBuffer, options)
            modelLoaded = true

            Timber.d("Model loaded successfully: $modelPath")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load model: $modelPath")
            false
        }
    }

    /**
     * Run inference on input data
     */
    suspend fun predict(input: ModelInput): ModelPrediction = withContext(Dispatchers.Default) {
        if (!modelLoaded || interpreter == null) {
            throw IllegalStateException("Model not loaded")
        }

        try {
            val inputBuffer = prepareInputBuffer(input.features)
            val outputBuffer = prepareOutputBuffer()

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Extract outputs
            outputBuffer.rewind()
            val outputs = FloatArray(outputBuffer.capacity() / 4)
            outputBuffer.asFloatBuffer().get(outputs)

            // Get predicted class and confidence
            val predictedClass = outputs.indices.maxByOrNull { outputs[it] } ?: 0
            val confidence = outputs[predictedClass]

            ModelPrediction(
                input = input,
                outputs = outputs,
                predictedClass = predictedClass,
                confidence = confidence,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            Timber.e(e, "Inference failed")
            throw e
        }
    }

    /**
     * Batch prediction for multiple inputs
     */
    suspend fun predictBatch(inputs: List<ModelInput>): List<ModelPrediction> {
        return inputs.map { predict(it) }
    }

    /**
     * Get model input shape
     */
    fun getInputShape(): IntArray? {
        return interpreter?.getInputTensor(0)?.shape()
    }

    /**
     * Get model output shape
     */
    fun getOutputShape(): IntArray? {
        return interpreter?.getOutputTensor(0)?.shape()
    }

    /**
     * Prepare input buffer for TFLite
     */
    private fun prepareInputBuffer(features: FloatArray): ByteBuffer {
        val inputShape = getInputShape() ?: intArrayOf(1, features.size)
        val bufferSize = inputShape.reduce { acc, i -> acc * i } * 4 // 4 bytes per float

        val buffer = ByteBuffer.allocateDirect(bufferSize).apply {
            order(ByteOrder.nativeOrder())
        }

        features.forEach { buffer.putFloat(it) }
        buffer.rewind()

        return buffer
    }

    /**
     * Prepare output buffer for TFLite
     */
    private fun prepareOutputBuffer(): ByteBuffer {
        val outputShape = getOutputShape() ?: intArrayOf(1, 2)
        val bufferSize = outputShape.reduce { acc, i -> acc * i } * 4

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
     * Release resources
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        modelLoaded = false
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
