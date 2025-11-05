package com.driftdetector.app.core.drift

import com.driftdetector.app.domain.model.DriftType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Unit tests for DriftDetector
 */
class DriftDetectorTest {

    private lateinit var driftDetector: DriftDetector

    @Before
    fun setup() {
        driftDetector = DriftDetector(psiThreshold = 0.2, ksThreshold = 0.05)
    }

    @Test
    fun `detectDrift should identify no drift in identical distributions`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 0.0, std = 1.0, samples = 1000, features = 3)
        val currentData = generateNormalData(mean = 0.0, std = 1.0, samples = 1000, features = 3)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("feature1", "feature2", "feature3")
        )

        // Assert
        assertFalse("Should not detect drift in identical distributions", result.isDriftDetected)
        assertEquals(DriftType.NO_DRIFT, result.driftType)
        assertTrue("Drift score should be low", result.driftScore < 0.2)
    }

    @Test
    fun `detectDrift should identify drift in shifted distributions`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 0.0, std = 1.0, samples = 1000, features = 3)
        val currentData = generateNormalData(mean = 2.0, std = 1.0, samples = 1000, features = 3)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("feature1", "feature2", "feature3")
        )

        // Assert
        assertTrue("Should detect drift in shifted distributions", result.isDriftDetected)
        assertTrue("Drift score should be high", result.driftScore > 0.2)
        assertNotEquals(DriftType.NO_DRIFT, result.driftType)
    }

    @Test
    fun `detectDrift should compute PSI correctly`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 0.0, std = 1.0, samples = 500, features = 2)
        val currentData = generateNormalData(mean = 1.0, std = 1.2, samples = 500, features = 2)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("feature1", "feature2")
        )

        // Assert
        assertTrue("PSI should be computed for each feature", result.featureDrifts.isNotEmpty())
        result.featureDrifts.forEach { featureDrift ->
            assertNotNull("PSI score should not be null", featureDrift.psiScore)
            assertTrue("PSI score should be positive", featureDrift.psiScore!! >= 0.0)
        }
    }

    @Test
    fun `detectDrift should perform KS test`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 0.0, std = 1.0, samples = 300, features = 2)
        val currentData = generateNormalData(mean = 0.5, std = 1.0, samples = 300, features = 2)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("feature1", "feature2")
        )

        // Assert
        assertTrue("Statistical tests should be performed", result.statisticalTests.isNotEmpty())
        result.statisticalTests.forEach { test ->
            assertEquals("Test should be KS test", "Kolmogorov-Smirnov", test.testName)
            assertTrue("KS statistic should be between 0 and 1", test.statistic in 0.0..1.0)
            assertTrue("p-value should be between 0 and 1", test.pValue in 0.0..1.0)
        }
    }

    @Test
    fun `detectDrift should identify covariate drift when many features drift`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 0.0, std = 1.0, samples = 500, features = 5)
        val currentData = generateNormalData(mean = 2.0, std = 1.0, samples = 500, features = 5)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("f1", "f2", "f3", "f4", "f5")
        )

        // Assert
        assertTrue("Should detect drift", result.isDriftDetected)
        assertEquals(
            "Should identify as covariate drift",
            DriftType.COVARIATE_DRIFT,
            result.driftType
        )
    }

    @Test
    fun `detectDrift should calculate distribution shifts`() = runBlocking {
        // Arrange
        val referenceData = generateNormalData(mean = 5.0, std = 2.0, samples = 400, features = 2)
        val currentData = generateNormalData(mean = 7.0, std = 3.0, samples = 400, features = 2)

        // Act
        val result = driftDetector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = currentData,
            featureNames = listOf("feature1", "feature2")
        )

        // Assert
        result.featureDrifts.forEach { featureDrift ->
            assertTrue(
                "Mean shift should be positive",
                featureDrift.distributionShift.meanShift > 0
            )
            assertTrue("Std shift should be positive", featureDrift.distributionShift.stdShift > 0)
            assertNotNull(
                "Quantile shifts should be computed",
                featureDrift.distributionShift.quantileShifts
            )
            assertTrue(
                "Should have quantile shifts",
                featureDrift.distributionShift.quantileShifts.isNotEmpty()
            )
        }
    }

    // Helper function to generate normal distributed data
    private fun generateNormalData(
        mean: Double,
        std: Double,
        samples: Int,
        features: Int
    ): List<FloatArray> {
        return List(samples) {
            FloatArray(features) {
                (nextGaussian() * std + mean).toFloat()
            }
        }
    }

    // Box-Muller transform to generate normally distributed random numbers
    private fun nextGaussian(): Double {
        val u1 = Random.nextDouble()
        val u2 = Random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }
}
