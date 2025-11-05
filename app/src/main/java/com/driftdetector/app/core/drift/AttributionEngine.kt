package com.driftdetector.app.core.drift

import com.driftdetector.app.domain.model.FeatureDrift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/**
 * SHAP-like attribution engine for explainable drift analysis
 * Adapted for mobile efficiency
 */
class AttributionEngine {

    /**
     * Calculate feature attributions for drift using simplified SHAP
     */
    suspend fun calculateAttributions(
        featureDrifts: List<FeatureDrift>,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>,
        numSamples: Int = 100
    ): Map<Int, Double> = withContext(Dispatchers.Default) {

        val attributions = mutableMapOf<Int, Double>()

        // Use simplified kernel SHAP approach
        for (drift in featureDrifts) {
            val idx = drift.featureIndex

            // Calculate marginal contribution
            val marginalContribution = calculateMarginalContribution(
                idx,
                referenceData,
                currentData,
                numSamples
            )

            // Normalize by drift score
            val normalizedAttribution = marginalContribution * drift.driftScore
            attributions[idx] = normalizedAttribution
        }

        // Normalize attributions to sum to 1
        val totalAttribution = attributions.values.sum()
        if (totalAttribution > 0) {
            attributions.mapValues { (_, value) -> value / totalAttribution }
        } else {
            attributions
        }
    }

    /**
     * Calculate marginal contribution of a feature to drift
     */
    private fun calculateMarginalContribution(
        featureIdx: Int,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>,
        numSamples: Int
    ): Double {
        var totalContribution = 0.0

        // Sample random subsets and measure impact
        repeat(numSamples) {
            val sampleSize = Random.nextInt(1, minOf(referenceData.size, 50))
            val refSample = referenceData.shuffled().take(sampleSize)
            val curSample = currentData.shuffled().take(sampleSize)

            // Calculate drift with and without feature
            val driftWithFeature = calculateFeatureDrift(
                refSample.map { it[featureIdx].toDouble() },
                curSample.map { it[featureIdx].toDouble() }
            )

            // Contribution is the absolute drift magnitude
            totalContribution += abs(driftWithFeature)
        }

        return totalContribution / numSamples
    }

    /**
     * Calculate drift for a single feature
     */
    private fun calculateFeatureDrift(
        reference: List<Double>,
        current: List<Double>
    ): Double {
        if (reference.isEmpty() || current.isEmpty()) return 0.0

        val refMean = reference.average()
        val curMean = current.average()
        val refStd = calculateStd(reference, refMean)

        // Normalized difference
        return if (refStd > 0) {
            abs(curMean - refMean) / refStd
        } else {
            abs(curMean - refMean)
        }
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStd(data: List<Double>, mean: Double): Double {
        if (data.isEmpty()) return 0.0
        val variance = data.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    /**
     * Calculate LIME-like local explanations
     */
    suspend fun explainLocalDrift(
        sample: FloatArray,
        referenceSamples: List<FloatArray>,
        numNeighbors: Int = 50
    ): Map<Int, Double> = withContext(Dispatchers.Default) {

        val explanations = mutableMapOf<Int, Double>()

        // Find nearest neighbors in reference data
        val neighbors = findNearestNeighbors(sample, referenceSamples, numNeighbors)

        // Calculate local feature importance
        for (featureIdx in sample.indices) {
            val localImportance = calculateLocalImportance(
                sample[featureIdx],
                neighbors.map { it[featureIdx] }
            )
            explanations[featureIdx] = localImportance
        }

        explanations
    }

    /**
     * Find k nearest neighbors
     */
    private fun findNearestNeighbors(
        sample: FloatArray,
        candidates: List<FloatArray>,
        k: Int
    ): List<FloatArray> {
        return candidates
            .map { candidate -> candidate to euclideanDistance(sample, candidate) }
            .sortedBy { it.second }
            .take(k)
            .map { it.first }
    }

    /**
     * Calculate Euclidean distance
     */
    private fun euclideanDistance(a: FloatArray, b: FloatArray): Double {
        return kotlin.math.sqrt(
            a.zip(b).sumOf { (x, y) -> (x - y).toDouble() * (x - y) }
        )
    }

    /**
     * Calculate local feature importance
     */
    private fun calculateLocalImportance(
        value: Float,
        neighborValues: List<Float>
    ): Double {
        if (neighborValues.isEmpty()) return 0.0

        val mean = neighborValues.average()
        val std = calculateStd(
            neighborValues.map { it.toDouble() },
            mean
        )

        return if (std > 0) {
            abs(value - mean) / std
        } else {
            abs(value - mean)
        }
    }
}
