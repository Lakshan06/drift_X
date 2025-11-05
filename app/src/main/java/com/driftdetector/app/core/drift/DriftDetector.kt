package com.driftdetector.app.core.drift

import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Core drift detection engine with PSI, KS test, and other algorithms
 */
class DriftDetector(
    private val psiThreshold: Double = 0.2,
    private val ksThreshold: Double = 0.05
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
            val featureDrifts = mutableListOf<FeatureDrift>()
            val statisticalTests = mutableListOf<StatisticalTestResult>()

            // Detect drift for each feature
            for (featureIdx in featureNames.indices) {
                val refFeature = referenceData.map { it[featureIdx].toDouble() }
                val curFeature = currentData.map { it[featureIdx].toDouble() }

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

            // Calculate overall drift score (average PSI)
            val overallDriftScore = featureDrifts.map { it.psiScore ?: 0.0 }.average()
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
     */
    private fun determineDriftType(
        featureDrifts: List<FeatureDrift>,
        isDriftDetected: Boolean
    ): DriftType {
        if (!isDriftDetected) return DriftType.NO_DRIFT

        // Simple heuristic: if many features drift, it's covariate drift
        val driftedCount = featureDrifts.count { it.isDrifted }
        val driftRatio = driftedCount.toDouble() / featureDrifts.size

        return when {
            driftRatio > 0.5 -> DriftType.COVARIATE_DRIFT
            driftRatio > 0.2 -> DriftType.CONCEPT_DRIFT
            else -> DriftType.PRIOR_DRIFT
        }
    }
}
