package com.driftdetector.app.core.drift

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Core drift detection engine with PSI, KS test, and other algorithms
 */
class DriftDetector(
    private val psiThreshold: Double = 0.35,  // Increased from 0.2 to reduce false positives
    private val ksThreshold: Double = 0.10     // Increased from 0.05 to reduce false positives
) {

    /**
     * Detect drift between reference and current data
     */
    suspend fun detectDrift(
        modelId: String,
        referenceData: List<FloatArray>,
        currentData: List<FloatArray>,
        featureNames: List<String>
    ): DriftResult = withContext(Dispatchers.Default) {
        try {
            // Normalize data to prevent scale issues
            val normalizedRef = normalizeData(referenceData)
            val normalizedCur = normalizeData(currentData)

            val featureDrifts = mutableListOf<FeatureDrift>()
            val statisticalTests = mutableListOf<StatisticalTestResult>()

            // Detect drift for each feature
            for (featureIdx in featureNames.indices) {
                val refFeature = normalizedRef.map { it[featureIdx].toDouble() }
                val curFeature = normalizedCur.map { it[featureIdx].toDouble() }

                val psi = calculatePSI(refFeature, curFeature)
                val ksResult = performKSTest(refFeature, curFeature)
                val distributionShift = calculateDistributionShift(refFeature, curFeature)

                // Simple attribution score based on drift magnitude
                val attribution = psi / psiThreshold

                val isDrifted = psi > psiThreshold || ksResult.pValue < ksThreshold

                featureDrifts.add(
                    FeatureDrift(
                        featureName = featureNames[featureIdx],
                        featureIndex = featureIdx,
                        driftScore = psi,
                        psiScore = psi,
                        ksStatistic = ksResult.statistic,
                        pValue = ksResult.pValue,
                        isDrifted = isDrifted,
                        attribution = attribution,
                        distributionShift = distributionShift
                    )
                )

                statisticalTests.add(ksResult)
            }

            // Calculate overall drift score with weighted average
            val overallDriftScore = calculateWeightedDriftScore(featureDrifts)
            val isDriftDetected = featureDrifts.any { it.isDrifted }

            // Determine drift type
            val driftType = determineDriftType(featureDrifts, isDriftDetected)

            DriftResult(
                id = UUID.randomUUID().toString(),
                modelId = modelId,
                timestamp = Instant.now(),
                driftType = driftType,
                driftScore = overallDriftScore,
                threshold = psiThreshold,
                isDriftDetected = isDriftDetected,
                featureDrifts = featureDrifts,
                statisticalTests = statisticalTests
            )
        } catch (e: Exception) {
            Timber.e(e, "Drift detection failed")
            throw e
        }
    }

    /**
     * Normalize data to prevent scale issues
     */
    private fun normalizeData(data: List<FloatArray>): List<FloatArray> {
        if (data.isEmpty()) return data

        val numFeatures = data.first().size
        val normalizedData = mutableListOf<FloatArray>()

        // Calculate mean and std for each feature
        for (sample in data) {
            val normalized = FloatArray(numFeatures)
            for (i in 0 until numFeatures) {
                val featureValues = data.map { it[i].toDouble() }
                val mean = featureValues.average()
                val std = calculateStd(
                    featureValues,
                    mean
                ).coerceAtLeast(1e-6) // Prevent division by zero

                normalized[i] = ((sample[i] - mean) / std).toFloat()
            }
            normalizedData.add(normalized)
        }

        return normalizedData
    }

    /**
     * Calculate weighted drift score (gives more weight to features with higher drift)
     */
    private fun calculateWeightedDriftScore(featureDrifts: List<FeatureDrift>): Double {
        if (featureDrifts.isEmpty()) return 0.0

        // Use exponential weighting to emphasize high-drift features
        val weights = featureDrifts.map { drift ->
            val normalizedScore = (drift.psiScore ?: 0.0) / psiThreshold
            exp(normalizedScore)
        }

        val totalWeight = weights.sum()
        if (totalWeight == 0.0) return 0.0

        val weightedSum = featureDrifts.zip(weights).sumOf { (drift, weight) ->
            (drift.psiScore ?: 0.0) * weight
        }

        return weightedSum / totalWeight
    }

    /**
     * Calculate Population Stability Index (PSI)
     * PSI measures the shift in distributions
     */
    private fun calculatePSI(
        reference: List<Double>,
        current: List<Double>,
        bins: Int = 10
    ): Double {
        val refMin = reference.minOrNull() ?: 0.0
        val refMax = reference.maxOrNull() ?: 1.0
        val binWidth = (refMax - refMin) / bins

        var psi = 0.0

        for (i in 0 until bins) {
            val binStart = refMin + i * binWidth
            val binEnd = binStart + binWidth

            val refCount = reference.count { it >= binStart && it < binEnd }
            val curCount = current.count { it >= binStart && it < binEnd }

            val refPct = (refCount.toDouble() / reference.size).coerceAtLeast(0.0001)
            val curPct = (curCount.toDouble() / current.size).coerceAtLeast(0.0001)

            psi += (curPct - refPct) * ln(curPct / refPct)
        }

        return psi
    }

    /**
     * Perform Kolmogorov-Smirnov test
     * Tests if two samples come from the same distribution
     */
    private fun performKSTest(
        reference: List<Double>,
        current: List<Double>
    ): StatisticalTestResult {
        val sortedRef = reference.sorted()
        val sortedCur = current.sorted()

        var maxDiff = 0.0
        var refIdx = 0
        var curIdx = 0

        while (refIdx < sortedRef.size && curIdx < sortedCur.size) {
            val refCdf = refIdx.toDouble() / sortedRef.size
            val curCdf = curIdx.toDouble() / sortedCur.size

            maxDiff = maxOf(maxDiff, abs(refCdf - curCdf))

            if (sortedRef[refIdx] < sortedCur[curIdx]) {
                refIdx++
            } else {
                curIdx++
            }
        }

        // Approximate p-value calculation
        val n1 = reference.size
        val n2 = current.size
        val ne = (n1.toDouble() * n2) / (n1 + n2)
        val pValue = approximateKSPValue(maxDiff, ne)

        return StatisticalTestResult(
            testName = "Kolmogorov-Smirnov",
            statistic = maxDiff,
            pValue = pValue,
            threshold = ksThreshold,
            isPassed = pValue >= ksThreshold
        )
    }

    /**
     * Approximate p-value for KS test using Kolmogorov distribution
     */
    private fun approximateKSPValue(statistic: Double, ne: Double): Double {
        val lambda = (sqrt(ne) + 0.12 + 0.11 / sqrt(ne)) * statistic

        // Approximation of Kolmogorov distribution
        var sum = 0.0
        for (k in 1..10) {
            val sign = if (k % 2 == 0) 1.0 else -1.0
            sum += sign * kotlin.math.exp(-2.0 * k * k * lambda * lambda)
        }

        return (2.0 * sum).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate distribution shift metrics
     */
    private fun calculateDistributionShift(
        reference: List<Double>,
        current: List<Double>
    ): DistributionShift {
        val refMean = reference.average()
        val curMean = current.average()

        val refStd = calculateStd(reference, refMean)
        val curStd = calculateStd(current, curMean)

        val refMin = reference.minOrNull() ?: 0.0
        val curMin = current.minOrNull() ?: 0.0

        val refMax = reference.maxOrNull() ?: 0.0
        val curMax = current.maxOrNull() ?: 0.0

        val quantiles = listOf(0.25, 0.5, 0.75)
        val quantileShifts = quantiles.associateWith { q ->
            val refQ = calculateQuantile(reference.sorted(), q)
            val curQ = calculateQuantile(current.sorted(), q)
            curQ - refQ
        }

        return DistributionShift(
            meanShift = curMean - refMean,
            stdShift = curStd - refStd,
            minShift = curMin - refMin,
            maxShift = curMax - refMax,
            quantileShifts = quantileShifts
        )
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStd(data: List<Double>, mean: Double): Double {
        if (data.isEmpty()) return 0.0
        val variance = data.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    /**
     * Calculate quantile from sorted data
     */
    private fun calculateQuantile(sortedData: List<Double>, quantile: Double): Double {
        if (sortedData.isEmpty()) return 0.0
        val index = (sortedData.size * quantile).toInt().coerceIn(0, sortedData.size - 1)
        return sortedData[index]
    }

    /**
     * Determine the type of drift based on feature analysis
     * Enhanced logic to distinguish between different drift types
     */
    private fun determineDriftType(
        featureDrifts: List<FeatureDrift>,
        isDriftDetected: Boolean
    ): DriftType {
        if (!isDriftDetected) return DriftType.NO_DRIFT

        val driftedFeatures = featureDrifts.filter { it.isDrifted }
        if (driftedFeatures.isEmpty()) return DriftType.NO_DRIFT

        val driftedCount = driftedFeatures.size
        val totalCount = featureDrifts.size
        val driftRatio = driftedCount.toDouble() / totalCount

        // Analyze distribution shift patterns
        val meanShifts = driftedFeatures.map { abs(it.distributionShift.meanShift) }
        val stdShifts = driftedFeatures.map { abs(it.distributionShift.stdShift) }

        val avgMeanShift = meanShifts.average()
        val avgStdShift = stdShifts.average()

        // Check if distribution shapes changed (std shift) vs just location (mean shift)
        val shapeChangeRatio = if (avgMeanShift > 0.01) avgStdShift / avgMeanShift else avgStdShift

        // Check consistency of drift across features
        val driftScores = driftedFeatures.map { it.driftScore }
        val avgDriftScore = driftScores.average()
        val driftVariance =
            driftScores.map { (it - avgDriftScore) * (it - avgDriftScore) }.average()
        val driftConsistency =
            if (avgDriftScore > 0.01) sqrt(driftVariance) / avgDriftScore else 0.0

        Timber.d("🔍 Drift Analysis: ratio=$driftRatio, avgMean=$avgMeanShift, avgStd=$avgStdShift, shapeChange=$shapeChangeRatio, consistency=$driftConsistency")

        // IMPROVED LOGIC: More accurate drift type detection
        return when {
            // PRIOR_DRIFT: Output distribution changed
            // - Very few features drifted (< 20%)
            // - Localized drift (mean shift dominates)
            // - High consistency in affected features
            driftRatio < 0.20 && avgMeanShift > avgStdShift * 2.0 && driftConsistency < 0.5 -> {
                Timber.d("✅ Detected PRIOR_DRIFT: Few features ($driftRatio), localized shifts, consistent")
                DriftType.PRIOR_DRIFT
            }

            // CONCEPT_DRIFT: Relationship between X and Y changed
            // - Moderate feature drift (20-50%)
            // - High inconsistency OR significant shape changes
            driftRatio in 0.20..0.50 && (driftConsistency > 0.5 || shapeChangeRatio > 2.0) -> {
                Timber.d("✅ Detected CONCEPT_DRIFT: Moderate ratio ($driftRatio), inconsistent or shape change")
                DriftType.CONCEPT_DRIFT
            }

            // CONCEPT_DRIFT: Very inconsistent patterns
            // - Drift patterns vary wildly across features
            driftConsistency > 0.7 -> {
                Timber.d("✅ Detected CONCEPT_DRIFT: High inconsistency ($driftConsistency)")
                DriftType.CONCEPT_DRIFT
            }

            // COVARIATE_DRIFT: Input feature distributions changed
            // - Many features drifted (> 50%)
            // - Consistent drift patterns across features
            driftRatio > 0.50 && driftConsistency < 0.5 -> {
                Timber.d("✅ Detected COVARIATE_DRIFT: Many features ($driftRatio), consistent patterns")
                DriftType.COVARIATE_DRIFT
            }

            // COVARIATE_DRIFT: Significant comprehensive shifts
            // - INCREASED THRESHOLD from 0.1 to 0.3 (less sensitive)
            // - Both mean and variance shifts are substantial
            avgMeanShift > 0.3 && avgStdShift > 0.3 -> {
                Timber.d("✅ Detected COVARIATE_DRIFT: Significant mean ($avgMeanShift) and std ($avgStdShift) shifts")
                DriftType.COVARIATE_DRIFT
            }

            // Use drift ratio as final tiebreaker
            // - Prioritize based on percentage of drifted features
            driftRatio > 0.40 -> {
                Timber.d("✅ Detected COVARIATE_DRIFT: High drift ratio ($driftRatio)")
                DriftType.COVARIATE_DRIFT
            }

            driftRatio > 0.20 -> {
                Timber.d("✅ Detected CONCEPT_DRIFT: Moderate drift ratio ($driftRatio)")
                DriftType.CONCEPT_DRIFT
            }

            // Default to prior drift for edge cases with low feature involvement
            else -> {
                Timber.d("✅ Default to PRIOR_DRIFT: Low drift ratio ($driftRatio)")
                DriftType.PRIOR_DRIFT
            }
        }
    }

}
