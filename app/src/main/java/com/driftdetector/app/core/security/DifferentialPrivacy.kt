package com.driftdetector.app.core.security

import kotlin.math.ln
import kotlin.math.exp
import kotlin.random.Random

/**
 * Differential privacy mechanisms for privacy-preserving metadata sync
 */
class DifferentialPrivacy(
    private val epsilon: Double = 0.5,  // Privacy budget
    private val delta: Double = 1e-5    // Privacy parameter
) {

    /**
     * Add Laplace noise to numeric data
     * Uses Laplace mechanism for differential privacy
     */
    fun addLaplaceNoise(value: Double, sensitivity: Double = 1.0): Double {
        val scale = sensitivity / epsilon
        val noise = sampleLaplace(scale)
        return value + noise
    }

    /**
     * Add Gaussian noise to numeric data
     * Uses Gaussian mechanism for differential privacy
     */
    fun addGaussianNoise(value: Double, sensitivity: Double = 1.0): Double {
        val scale = sensitivity * kotlin.math.sqrt(2 * ln(1.25 / delta)) / epsilon
        val noise = Random.nextDouble() * scale
        return value + noise
    }

    /**
     * Sample from Laplace distribution
     */
    private fun sampleLaplace(scale: Double): Double {
        val u = Random.nextDouble() - 0.5
        return -scale * sign(u) * ln(1 - 2 * kotlin.math.abs(u))
    }

    private fun sign(x: Double): Double = if (x >= 0) 1.0 else -1.0

    /**
     * Apply randomized response for boolean data
     */
    fun randomizedResponse(value: Boolean): Boolean {
        val p = exp(epsilon) / (1 + exp(epsilon))
        return if (Random.nextDouble() < p) value else !value
    }

    /**
     * Apply local differential privacy to a count/metric
     */
    fun privatizeCount(count: Int, maxCount: Int): Int {
        val sensitivity = 1.0
        val noisyCount = addLaplaceNoise(count.toDouble(), sensitivity)
        return noisyCount.toInt().coerceIn(0, maxCount)
    }

    /**
     * Apply differential privacy to a histogram
     */
    fun privatizeHistogram(histogram: Map<String, Int>): Map<String, Int> {
        val sensitivity = 1.0
        return histogram.mapValues { (_, count) ->
            val noisyCount = addLaplaceNoise(count.toDouble(), sensitivity)
            noisyCount.toInt().coerceAtLeast(0)
        }
    }

    /**
     * Apply differential privacy to feature statistics
     */
    fun privatizeStatistics(mean: Double, std: Double, count: Int): PrivateStatistics {
        // Add calibrated noise based on sensitivity
        val meanSensitivity = std / kotlin.math.sqrt(count.toDouble())
        val stdSensitivity = std / kotlin.math.sqrt(2.0 * count)

        return PrivateStatistics(
            mean = addLaplaceNoise(mean, meanSensitivity),
            std = addLaplaceNoise(std, stdSensitivity).coerceAtLeast(0.0),
            count = privatizeCount(count, Int.MAX_VALUE)
        )
    }

    /**
     * Apply k-anonymity to categorize drift scores
     */
    fun anonymizeDriftScore(score: Double): String {
        return when {
            score < 0.1 -> "LOW"
            score < 0.3 -> "MODERATE"
            score < 0.6 -> "HIGH"
            else -> "CRITICAL"
        }
    }
}

/**
 * Container for differentially private statistics
 */
data class PrivateStatistics(
    val mean: Double,
    val std: Double,
    val count: Int
)

/**
 * Privacy budget manager to track epsilon spending
 */
class PrivacyBudgetManager(
    private val totalEpsilon: Double = 1.0
) {
    private var spentEpsilon: Double = 0.0

    fun spendBudget(epsilon: Double): Boolean {
        return if (spentEpsilon + epsilon <= totalEpsilon) {
            spentEpsilon += epsilon
            true
        } else {
            false
        }
    }

    fun getRemainingBudget(): Double = totalEpsilon - spentEpsilon

    fun reset() {
        spentEpsilon = 0.0
    }

    fun isExhausted(): Boolean = spentEpsilon >= totalEpsilon
}
