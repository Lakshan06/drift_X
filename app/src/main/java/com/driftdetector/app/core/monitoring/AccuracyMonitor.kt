package com.driftdetector.app.core.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.Instant
import kotlin.math.sqrt

/**
 * High-precision accuracy monitoring system for ML models
 * Tracks comprehensive performance metrics with temporal analysis
 */
class AccuracyMonitor {

    private val _accuracyHistory = MutableStateFlow<List<AccuracySnapshot>>(emptyList())
    val accuracyHistory: Flow<List<AccuracySnapshot>> = _accuracyHistory.asStateFlow()

    private val _currentMetrics = MutableStateFlow<ModelMetrics?>(null)
    val currentMetrics: Flow<ModelMetrics?> = _currentMetrics.asStateFlow()

    // Rolling window for real-time accuracy calculation
    private val predictionWindow = mutableListOf<PredictionRecord>()
    private val maxWindowSize = 1000

    /**
     * Record a prediction with ground truth for accuracy calculation
     */
    fun recordPrediction(
        modelId: String,
        predictedClass: Int,
        actualClass: Int,
        confidence: Float,
        features: FloatArray,
        timestamp: Instant = Instant.now()
    ) {
        val record = PredictionRecord(
            modelId = modelId,
            predictedClass = predictedClass,
            actualClass = actualClass,
            confidence = confidence,
            features = features,
            timestamp = timestamp,
            isCorrect = predictedClass == actualClass
        )

        synchronized(predictionWindow) {
            predictionWindow.add(record)

            // Maintain rolling window
            if (predictionWindow.size > maxWindowSize) {
                predictionWindow.removeAt(0)
            }
        }

        // Update metrics for this model
        updateMetrics(modelId)
    }

    /**
     * Calculate comprehensive metrics for a model
     */
    private fun updateMetrics(modelId: String) {
        val modelPredictions = synchronized(predictionWindow) {
            predictionWindow.filter { it.modelId == modelId }
        }

        if (modelPredictions.isEmpty()) return

        // Calculate confusion matrix
        val confusionMatrix = calculateConfusionMatrix(modelPredictions)

        // Calculate metrics
        val accuracy = modelPredictions.count { it.isCorrect }.toDouble() / modelPredictions.size
        val precision = calculatePrecision(confusionMatrix)
        val recall = calculateRecall(confusionMatrix)
        val f1Score = calculateF1Score(precision, recall)
        val mcc = calculateMCC(confusionMatrix)
        val avgConfidence = modelPredictions.map { it.confidence.toDouble() }.average()

        // Calculate confidence calibration
        val calibrationError = calculateCalibrationError(modelPredictions)

        // Calculate temporal drift in accuracy
        val accuracyDrift = calculateAccuracyDrift(modelPredictions)

        val metrics = ModelMetrics(
            modelId = modelId,
            timestamp = Instant.now(),
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            matthewsCorrelation = mcc,
            averageConfidence = avgConfidence,
            calibrationError = calibrationError,
            confusionMatrix = confusionMatrix,
            totalPredictions = modelPredictions.size,
            correctPredictions = modelPredictions.count { it.isCorrect },
            accuracyDrift = accuracyDrift,
            rocAuc = calculateROCAUC(modelPredictions),
            precisionRecallAuc = calculatePRAUC(modelPredictions)
        )

        _currentMetrics.value = metrics

        // Add to history
        val snapshot = AccuracySnapshot(
            timestamp = Instant.now(),
            accuracy = accuracy,
            f1Score = f1Score,
            precision = precision,
            recall = recall,
            confidence = avgConfidence
        )

        val history = _accuracyHistory.value.toMutableList()
        history.add(snapshot)

        // Keep last 1000 snapshots
        if (history.size > 1000) {
            history.removeAt(0)
        }

        _accuracyHistory.value = history

        Timber.d("📊 Accuracy Metrics Updated for model $modelId:")
        Timber.d("   ✓ Accuracy: ${String.format("%.4f", accuracy * 100)}%")
        Timber.d("   ✓ Precision: ${String.format("%.4f", precision * 100)}%")
        Timber.d("   ✓ Recall: ${String.format("%.4f", recall * 100)}%")
        Timber.d("   ✓ F1 Score: ${String.format("%.4f", f1Score)}")
        Timber.d("   ✓ MCC: ${String.format("%.4f", mcc)}")
        Timber.d("   ✓ ROC-AUC: ${String.format("%.4f", metrics.rocAuc)}")
        Timber.d("   ✓ Calibration Error: ${String.format("%.4f", calibrationError)}")
    }

    /**
     * Calculate confusion matrix
     */
    private fun calculateConfusionMatrix(predictions: List<PredictionRecord>): ConfusionMatrix {
        val classes = predictions.flatMap { listOf(it.predictedClass, it.actualClass) }.distinct()
        val numClasses = classes.maxOrNull()?.plus(1) ?: 2

        val matrix = Array(numClasses) { IntArray(numClasses) }

        predictions.forEach { pred ->
            if (pred.actualClass < numClasses && pred.predictedClass < numClasses) {
                matrix[pred.actualClass][pred.predictedClass]++
            }
        }

        return ConfusionMatrix(
            matrix = matrix,
            truePositives = if (numClasses == 2) matrix[1][1] else calculateMultiClassTP(matrix),
            trueNegatives = if (numClasses == 2) matrix[0][0] else calculateMultiClassTN(matrix),
            falsePositives = if (numClasses == 2) matrix[0][1] else calculateMultiClassFP(matrix),
            falseNegatives = if (numClasses == 2) matrix[1][0] else calculateMultiClassFN(matrix)
        )
    }

    /**
     * Calculate precision with proper handling of edge cases
     */
    private fun calculatePrecision(cm: ConfusionMatrix): Double {
        val tp = cm.truePositives.toDouble()
        val fp = cm.falsePositives.toDouble()
        return if (tp + fp > 0) tp / (tp + fp) else 0.0
    }

    /**
     * Calculate recall with proper handling of edge cases
     */
    private fun calculateRecall(cm: ConfusionMatrix): Double {
        val tp = cm.truePositives.toDouble()
        val fn = cm.falseNegatives.toDouble()
        return if (tp + fn > 0) tp / (tp + fn) else 0.0
    }

    /**
     * Calculate F1 score
     */
    private fun calculateF1Score(precision: Double, recall: Double): Double {
        return if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0.0
    }

    /**
     * Calculate Matthews Correlation Coefficient (MCC)
     * Better metric for imbalanced datasets
     */
    private fun calculateMCC(cm: ConfusionMatrix): Double {
        val tp = cm.truePositives.toDouble()
        val tn = cm.trueNegatives.toDouble()
        val fp = cm.falsePositives.toDouble()
        val fn = cm.falseNegatives.toDouble()

        val numerator = (tp * tn) - (fp * fn)
        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))

        return if (denominator > 0) numerator / denominator else 0.0
    }

    /**
     * Calculate calibration error (Expected Calibration Error - ECE)
     * Measures how well predicted probabilities match actual outcomes
     */
    private fun calculateCalibrationError(predictions: List<PredictionRecord>): Double {
        val numBins = 10
        val bins = Array(numBins) { mutableListOf<PredictionRecord>() }

        // Distribute predictions into bins based on confidence
        predictions.forEach { pred ->
            val binIndex = ((pred.confidence * numBins).toInt()).coerceIn(0, numBins - 1)
            bins[binIndex].add(pred)
        }

        // Calculate ECE
        var ece = 0.0
        predictions.size.let { total ->
            bins.forEach { bin ->
                if (bin.isNotEmpty()) {
                    val avgConfidence = bin.map { it.confidence.toDouble() }.average()
                    val avgAccuracy = bin.count { it.isCorrect }.toDouble() / bin.size
                    ece += (bin.size.toDouble() / total) * Math.abs(avgConfidence - avgAccuracy)
                }
            }
        }

        return ece
    }

    /**
     * Calculate temporal accuracy drift
     * Detects if model accuracy is degrading over time
     */
    private fun calculateAccuracyDrift(predictions: List<PredictionRecord>): Double {
        if (predictions.size < 10) return 0.0

        // Split into early and recent predictions
        val splitPoint = predictions.size / 2
        val earlyPredictions = predictions.take(splitPoint)
        val recentPredictions = predictions.drop(splitPoint)

        val earlyAccuracy =
            earlyPredictions.count { it.isCorrect }.toDouble() / earlyPredictions.size
        val recentAccuracy =
            recentPredictions.count { it.isCorrect }.toDouble() / recentPredictions.size

        return earlyAccuracy - recentAccuracy // Positive means degradation
    }

    /**
     * Calculate ROC-AUC score
     */
    private fun calculateROCAUC(predictions: List<PredictionRecord>): Double {
        if (predictions.size < 2) return 0.5

        // Sort by confidence (descending)
        val sorted = predictions.sortedByDescending { it.confidence }

        var auc = 0.0
        var tpr = 0.0 // True Positive Rate
        var fpr = 0.0 // False Positive Rate

        val totalPositives = predictions.count { it.actualClass == 1 }.toDouble()
        val totalNegatives = predictions.count { it.actualClass == 0 }.toDouble()

        if (totalPositives == 0.0 || totalNegatives == 0.0) return 0.5

        var prevFpr = 0.0
        var prevTpr = 0.0

        sorted.forEach { pred ->
            if (pred.actualClass == 1) {
                tpr += 1.0 / totalPositives
            } else {
                fpr += 1.0 / totalNegatives
                // Trapezoidal rule for AUC
                auc += (fpr - prevFpr) * (tpr + prevTpr) / 2.0
                prevFpr = fpr
                prevTpr = tpr
            }
        }

        return auc.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate Precision-Recall AUC
     */
    private fun calculatePRAUC(predictions: List<PredictionRecord>): Double {
        if (predictions.size < 2) return 0.5

        val sorted = predictions.sortedByDescending { it.confidence }

        var auc = 0.0
        var tp = 0.0
        var fp = 0.0
        var prevRecall = 0.0
        var prevPrecision = 1.0

        val totalPositives = predictions.count { it.actualClass == 1 }.toDouble()

        if (totalPositives == 0.0) return 0.0

        sorted.forEach { pred ->
            if (pred.actualClass == 1) {
                tp += 1.0
            } else {
                fp += 1.0
            }

            val recall = tp / totalPositives
            val precision = if (tp + fp > 0) tp / (tp + fp) else 0.0

            // Trapezoidal rule
            auc += (recall - prevRecall) * (precision + prevPrecision) / 2.0

            prevRecall = recall
            prevPrecision = precision
        }

        return auc.coerceIn(0.0, 1.0)
    }

    // Helper functions for multi-class metrics
    private fun calculateMultiClassTP(matrix: Array<IntArray>): Int {
        return matrix.indices.sumOf { matrix[it][it] }
    }

    private fun calculateMultiClassTN(matrix: Array<IntArray>): Int {
        val total = matrix.sumOf { it.sum() }
        val tp = calculateMultiClassTP(matrix)
        val fp = calculateMultiClassFP(matrix)
        val fn = calculateMultiClassFN(matrix)
        return total - tp - fp - fn
    }

    private fun calculateMultiClassFP(matrix: Array<IntArray>): Int {
        return matrix.indices.sumOf { row ->
            matrix[row].indices.filter { col -> col != row }.sumOf { col -> matrix[row][col] }
        }
    }

    private fun calculateMultiClassFN(matrix: Array<IntArray>): Int {
        return matrix.indices.sumOf { col ->
            matrix.indices.filter { row -> row != col }.sumOf { row -> matrix[row][col] }
        }
    }

    /**
     * Get accuracy trend (improving, stable, degrading)
     */
    fun getAccuracyTrend(): AccuracyTrend {
        val history = _accuracyHistory.value
        if (history.size < 10) return AccuracyTrend.INSUFFICIENT_DATA

        val recent = history.takeLast(10)
        val older = history.dropLast(10).takeLast(10)

        if (older.isEmpty()) return AccuracyTrend.INSUFFICIENT_DATA

        val recentAvg = recent.map { it.accuracy }.average()
        val olderAvg = older.map { it.accuracy }.average()

        val change = recentAvg - olderAvg

        return when {
            change > 0.02 -> AccuracyTrend.IMPROVING
            change < -0.02 -> AccuracyTrend.DEGRADING
            else -> AccuracyTrend.STABLE
        }
    }

    /**
     * Clear history for a model
     */
    fun clearHistory(modelId: String) {
        synchronized(predictionWindow) {
            predictionWindow.removeAll { it.modelId == modelId }
        }
        _accuracyHistory.value = emptyList()
        _currentMetrics.value = null
    }

    /**
     * Get performance summary
     */
    fun getPerformanceSummary(modelId: String): PerformanceSummary? {
        val metrics = _currentMetrics.value ?: return null
        if (metrics.modelId != modelId) return null

        val trend = getAccuracyTrend()
        val history = _accuracyHistory.value

        return PerformanceSummary(
            currentAccuracy = metrics.accuracy,
            trend = trend,
            dataPoints = history.size,
            avgAccuracy = history.map { it.accuracy }.average(),
            minAccuracy = history.minOfOrNull { it.accuracy } ?: 0.0,
            maxAccuracy = history.maxOfOrNull { it.accuracy } ?: 0.0,
            standardDeviation = calculateStandardDeviation(history.map { it.accuracy })
        )
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
}

/**
 * Record of a single prediction with ground truth
 */
data class PredictionRecord(
    val modelId: String,
    val predictedClass: Int,
    val actualClass: Int,
    val confidence: Float,
    val features: FloatArray,
    val timestamp: Instant,
    val isCorrect: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PredictionRecord
        if (modelId != other.modelId) return false
        if (predictedClass != other.predictedClass) return false
        if (actualClass != other.actualClass) return false
        if (confidence != other.confidence) return false
        if (!features.contentEquals(other.features)) return false
        if (timestamp != other.timestamp) return false
        if (isCorrect != other.isCorrect) return false
        return true
    }

    override fun hashCode(): Int {
        var result = modelId.hashCode()
        result = 31 * result + predictedClass
        result = 31 * result + actualClass
        result = 31 * result + confidence.hashCode()
        result = 31 * result + features.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isCorrect.hashCode()
        return result
    }
}

/**
 * Confusion matrix with derived metrics
 */
data class ConfusionMatrix(
    val matrix: Array<IntArray>,
    val truePositives: Int,
    val trueNegatives: Int,
    val falsePositives: Int,
    val falseNegatives: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ConfusionMatrix
        if (!matrix.contentDeepEquals(other.matrix)) return false
        if (truePositives != other.truePositives) return false
        if (trueNegatives != other.trueNegatives) return false
        if (falsePositives != other.falsePositives) return false
        if (falseNegatives != other.falseNegatives) return false
        return true
    }

    override fun hashCode(): Int {
        var result = matrix.contentDeepHashCode()
        result = 31 * result + truePositives
        result = 31 * result + trueNegatives
        result = 31 * result + falsePositives
        result = 31 * result + falseNegatives
        return result
    }
}

/**
 * Comprehensive model metrics at a point in time
 */
data class ModelMetrics(
    val modelId: String,
    val timestamp: Instant,
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val matthewsCorrelation: Double,
    val averageConfidence: Double,
    val calibrationError: Double,
    val confusionMatrix: ConfusionMatrix,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val accuracyDrift: Double,
    val rocAuc: Double,
    val precisionRecallAuc: Double
)

/**
 * Snapshot of accuracy metrics at a specific time
 */
data class AccuracySnapshot(
    val timestamp: Instant,
    val accuracy: Double,
    val f1Score: Double,
    val precision: Double,
    val recall: Double,
    val confidence: Double
)

/**
 * Accuracy trend classification
 */
enum class AccuracyTrend {
    IMPROVING,
    STABLE,
    DEGRADING,
    INSUFFICIENT_DATA
}

/**
 * Performance summary for UI display
 */
data class PerformanceSummary(
    val currentAccuracy: Double,
    val trend: AccuracyTrend,
    val dataPoints: Int,
    val avgAccuracy: Double,
    val minAccuracy: Double,
    val maxAccuracy: Double,
    val standardDeviation: Double
)
