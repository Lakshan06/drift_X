package com.driftdetector.app.core.analytics

import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.DriftType
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced analytics engine for drift analysis and forecasting
 */
class AnalyticsEngine {

    /**
     * Generate comprehensive analytics for a model
     */
    fun generateModelAnalytics(
        model: MLModel,
        driftResults: List<DriftResult>,
        patches: List<Patch>,
        dateRange: DateRange
    ): ModelAnalytics {
        Timber.d("📊 Generating analytics for model: ${model.name}")

        val filteredDrifts = filterByDateRange(driftResults, dateRange)
        val filteredPatches = filterPatchesByDateRange(patches, dateRange)

        return ModelAnalytics(
            modelId = model.id,
            modelName = model.name,
            dateRange = dateRange,

            // Drift metrics
            totalDriftEvents = filteredDrifts.size,
            driftDetectionRate = calculateDriftRate(filteredDrifts),
            averageDriftScore = filteredDrifts.map { it.driftScore }.average()
                .takeIf { !it.isNaN() } ?: 0.0,
            maxDriftScore = filteredDrifts.maxOfOrNull { it.driftScore } ?: 0.0,
            minDriftScore = filteredDrifts.minOfOrNull { it.driftScore } ?: 0.0,
            driftScoreStdDev = calculateStdDev(filteredDrifts.map { it.driftScore }),

            // Drift type distribution
            driftTypeDistribution = calculateDriftTypeDistribution(filteredDrifts),

            // Patch metrics
            totalPatches = filteredPatches.size,
            appliedPatches = filteredPatches.count { it.status == PatchStatus.APPLIED },
            failedPatches = filteredPatches.count { it.status == PatchStatus.FAILED },
            averagePatchSafetyScore = calculateAverageSafetyScore(filteredPatches),
            patchSuccessRate = calculatePatchSuccessRate(filteredPatches),

            // Trend analysis
            driftTrend = calculateDriftTrend(filteredDrifts),
            forecastedDrift = forecastDrift(filteredDrifts, daysAhead = 7),

            // Feature analysis
            topDriftedFeatures = identifyTopDriftedFeatures(filteredDrifts, limit = 5),

            // Time-based metrics
            driftsByDay = groupDriftsByDay(filteredDrifts),
            driftsByWeek = groupDriftsByWeek(filteredDrifts),
            driftsByMonth = groupDriftsByMonth(filteredDrifts),

            // Health score
            modelHealthScore = calculateModelHealthScore(filteredDrifts, filteredPatches),

            // Recommendations
            recommendations = generateRecommendations(filteredDrifts, filteredPatches, model)
        )
    }

    /**
     * Compare multiple models
     */
    fun compareModels(
        modelsWithData: List<ModelWithData>
    ): ModelComparison {
        Timber.d("📊 Comparing ${modelsWithData.size} models")

        return ModelComparison(
            models = modelsWithData.map { it.model },
            comparisons = modelsWithData.map { data ->
                ModelComparisonData(
                    modelId = data.model.id,
                    modelName = data.model.name,
                    totalDrifts = data.driftResults.size,
                    avgDriftScore = data.driftResults.map { it.driftScore }.average()
                        .takeIf { !it.isNaN() } ?: 0.0,
                    driftRate = calculateDriftRate(data.driftResults),
                    patchesApplied = data.patches.count { it.status == PatchStatus.APPLIED },
                    healthScore = calculateModelHealthScore(data.driftResults, data.patches),
                    lastDriftDate = data.driftResults.maxOfOrNull { it.timestamp }
                )
            },
            bestPerformingModel = findBestPerformingModel(modelsWithData),
            worstPerformingModel = findWorstPerformingModel(modelsWithData)
        )
    }

    /**
     * Generate time-series data for charting
     */
    fun generateTimeSeriesData(
        driftResults: List<DriftResult>,
        dateRange: DateRange,
        granularity: TimeGranularity
    ): List<TimeSeriesDataPoint> {
        val filtered = filterByDateRange(driftResults, dateRange)

        return when (granularity) {
            TimeGranularity.HOURLY -> groupByHour(filtered)
            TimeGranularity.DAILY -> groupByDay(filtered)
            TimeGranularity.WEEKLY -> groupByWeek(filtered)
            TimeGranularity.MONTHLY -> groupByMonth(filtered)
        }
    }

    /**
     * Calculate drift detection rate (% of data showing drift)
     */
    private fun calculateDriftRate(driftResults: List<DriftResult>): Double {
        if (driftResults.isEmpty()) return 0.0
        return driftResults.count { it.isDriftDetected }.toDouble() / driftResults.size
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    /**
     * Calculate drift type distribution
     */
    private fun calculateDriftTypeDistribution(driftResults: List<DriftResult>): Map<DriftType, Int> {
        return driftResults.groupBy { it.driftType }.mapValues { it.value.size }
    }

    /**
     * Calculate average safety score for patches
     */
    private fun calculateAverageSafetyScore(patches: List<Patch>): Double {
        val scores = patches.mapNotNull { it.validationResult?.metrics?.safetyScore }
        return scores.average().takeIf { !it.isNaN() } ?: 0.0
    }

    /**
     * Calculate patch success rate
     */
    private fun calculatePatchSuccessRate(patches: List<Patch>): Double {
        if (patches.isEmpty()) return 0.0
        val applied = patches.count { it.status == PatchStatus.APPLIED }
        val total = patches.count { it.status != PatchStatus.CREATED }
        return if (total > 0) applied.toDouble() / total else 0.0
    }

    /**
     * Calculate drift trend (increasing, decreasing, stable)
     */
    private fun calculateDriftTrend(driftResults: List<DriftResult>): DriftTrend {
        if (driftResults.size < 3) return DriftTrend.INSUFFICIENT_DATA

        val sortedDrifts = driftResults.sortedBy { it.timestamp }
        val recentScores = sortedDrifts.takeLast(10).map { it.driftScore }
        val olderScores = sortedDrifts.dropLast(10).takeLast(10).map { it.driftScore }

        if (olderScores.isEmpty()) return DriftTrend.INSUFFICIENT_DATA

        val recentAvg = recentScores.average()
        val olderAvg = olderScores.average()
        val difference = recentAvg - olderAvg

        return when {
            difference > 0.1 -> DriftTrend.INCREASING
            difference < -0.1 -> DriftTrend.DECREASING
            else -> DriftTrend.STABLE
        }
    }

    /**
     * Forecast future drift using linear regression
     */
    private fun forecastDrift(
        driftResults: List<DriftResult>,
        daysAhead: Int
    ): List<DriftForecast> {
        if (driftResults.size < 3) return emptyList()

        val sortedDrifts = driftResults.sortedBy { it.timestamp }
        val timestamps = sortedDrifts.map { it.timestamp.toEpochMilli().toDouble() }
        val scores = sortedDrifts.map { it.driftScore }

        // Simple linear regression
        val (slope, intercept) = linearRegression(timestamps, scores)

        val lastTimestamp = sortedDrifts.last().timestamp
        val forecasts = mutableListOf<DriftForecast>()

        for (day in 1..daysAhead) {
            val futureTimestamp = lastTimestamp.plus(day.toLong(), ChronoUnit.DAYS)
            val predictedScore =
                (slope * futureTimestamp.toEpochMilli() + intercept).coerceIn(0.0, 1.0)
            val confidence = calculateForecastConfidence(driftResults, day)

            forecasts.add(
                DriftForecast(
                    date = futureTimestamp,
                    predictedDriftScore = predictedScore,
                    confidence = confidence,
                    isLikelyDrift = predictedScore > 0.5
                )
            )
        }

        return forecasts
    }

    /**
     * Linear regression for forecasting
     */
    private fun linearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { it.first * it.second }
        val sumX2 = x.sumOf { it * it }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        return Pair(slope, intercept)
    }

    /**
     * Calculate forecast confidence
     */
    private fun calculateForecastConfidence(
        driftResults: List<DriftResult>,
        daysAhead: Int
    ): Double {
        // Confidence decreases with distance and data variance
        val variance = calculateStdDev(driftResults.map { it.driftScore })
        val baseConfidence = 1.0 - (daysAhead * 0.1)
        val variancePenalty = variance * 0.5
        return (baseConfidence - variancePenalty).coerceIn(0.0, 1.0)
    }

    /**
     * Identify top drifted features
     */
    private fun identifyTopDriftedFeatures(
        driftResults: List<DriftResult>,
        limit: Int
    ): List<FeatureDriftSummary> {
        val featureDriftMap = mutableMapOf<String, MutableList<Double>>()

        driftResults.forEach { drift ->
            drift.featureDrifts.forEach { featureDrift ->
                featureDriftMap.getOrPut(featureDrift.featureName) { mutableListOf() }
                    .add(featureDrift.driftScore)
            }
        }

        return featureDriftMap.entries
            .map { (featureName, scores) ->
                FeatureDriftSummary(
                    featureName = featureName,
                    avgDriftScore = scores.average(),
                    maxDriftScore = scores.maxOrNull() ?: 0.0,
                    driftFrequency = scores.count { it > 0.5 },
                    totalOccurrences = scores.size
                )
            }
            .sortedByDescending { it.avgDriftScore }
            .take(limit)
    }

    /**
     * Group drifts by day
     */
    private fun groupDriftsByDay(driftResults: List<DriftResult>): Map<String, List<DriftResult>> {
        return driftResults.groupBy { drift ->
            drift.timestamp.truncatedTo(ChronoUnit.DAYS).toString()
        }
    }

    /**
     * Group drifts by week
     */
    private fun groupDriftsByWeek(driftResults: List<DriftResult>): Map<String, List<DriftResult>> {
        return driftResults.groupBy { drift ->
            val weekStart = drift.timestamp.minus(
                drift.timestamp.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value.toLong() - 1,
                ChronoUnit.DAYS
            )
            weekStart.truncatedTo(ChronoUnit.DAYS).toString()
        }
    }

    /**
     * Group drifts by month
     */
    private fun groupDriftsByMonth(driftResults: List<DriftResult>): Map<String, List<DriftResult>> {
        return driftResults.groupBy { drift ->
            "${drift.timestamp.atZone(java.time.ZoneId.systemDefault()).year}-${
                drift.timestamp.atZone(
                    java.time.ZoneId.systemDefault()
                ).monthValue
            }"
        }
    }

    /**
     * Calculate model health score (0-100)
     */
    private fun calculateModelHealthScore(
        driftResults: List<DriftResult>,
        patches: List<Patch>
    ): Double {
        if (driftResults.isEmpty()) return 100.0

        val recentDrifts = driftResults.sortedByDescending { it.timestamp }.take(20)
        val driftRate = recentDrifts.count { it.isDriftDetected }.toDouble() / recentDrifts.size
        val avgDriftScore = recentDrifts.map { it.driftScore }.average()

        val patchSuccessRate = calculatePatchSuccessRate(patches)
        val avgSafetyScore = calculateAverageSafetyScore(patches)

        // Health score formula: weighted combination of metrics
        val driftPenalty = (driftRate * 30) + (avgDriftScore * 30)
        val patchBonus = (patchSuccessRate * 20) + (avgSafetyScore * 20)

        return (100.0 - driftPenalty + patchBonus).coerceIn(0.0, 100.0)
    }

    /**
     * Generate actionable recommendations
     */
    private fun generateRecommendations(
        driftResults: List<DriftResult>,
        patches: List<Patch>,
        model: MLModel
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // High drift detection
        val recentDrifts = driftResults.sortedByDescending { it.timestamp }.take(10)
        if (recentDrifts.count { it.isDriftDetected } >= 7) {
            recommendations.add(
                Recommendation(
                    priority = RecommendationPriority.HIGH,
                    title = "Frequent Drift Detected",
                    description = "Model is experiencing drift in 70%+ of recent predictions",
                    actionItems = listOf(
                        "Review recent data distribution changes",
                        "Consider retraining the model",
                        "Apply recommended patches immediately"
                    )
                )
            )
        }

        // Increasing drift trend
        val trend = calculateDriftTrend(driftResults)
        if (trend == DriftTrend.INCREASING) {
            recommendations.add(
                Recommendation(
                    priority = RecommendationPriority.MEDIUM,
                    title = "Drift Trend Increasing",
                    description = "Drift scores are trending upward over time",
                    actionItems = listOf(
                        "Monitor model closely",
                        "Prepare for model retraining",
                        "Review data pipeline for issues"
                    )
                )
            )
        }

        // Failed patches
        val failedPatches = patches.count { it.status == PatchStatus.FAILED }
        if (failedPatches >= 3) {
            recommendations.add(
                Recommendation(
                    priority = RecommendationPriority.HIGH,
                    title = "Multiple Patch Failures",
                    description = "$failedPatches patches have failed to apply",
                    actionItems = listOf(
                        "Review patch failure logs",
                        "Consider manual model update",
                        "Check model compatibility"
                    )
                )
            )
        }

        // Top drifted features
        val topFeatures = identifyTopDriftedFeatures(driftResults, 3)
        if (topFeatures.isNotEmpty() && topFeatures.first().avgDriftScore > 0.7) {
            recommendations.add(
                Recommendation(
                    priority = RecommendationPriority.MEDIUM,
                    title = "High-Drift Features Identified",
                    description = "Features ${
                        topFeatures.take(3).joinToString { it.featureName }
                    } show significant drift",
                    actionItems = listOf(
                        "Investigate these features in detail",
                        "Consider feature engineering",
                        "Apply feature-specific patches"
                    )
                )
            )
        }

        // No recent activity
        val daysSinceLastDrift = if (driftResults.isNotEmpty()) {
            ChronoUnit.DAYS.between(driftResults.maxOf { it.timestamp }, Instant.now())
        } else {
            0
        }

        if (daysSinceLastDrift > 30) {
            recommendations.add(
                Recommendation(
                    priority = RecommendationPriority.LOW,
                    title = "No Recent Drift Monitoring",
                    description = "Model hasn't been tested for drift in over 30 days",
                    actionItems = listOf(
                        "Run drift detection with recent data",
                        "Ensure monitoring is active",
                        "Schedule regular drift checks"
                    )
                )
            )
        }

        return recommendations.sortedByDescending { it.priority }
    }

    // Helper functions for time-series grouping
    private fun groupByHour(drifts: List<DriftResult>): List<TimeSeriesDataPoint> {
        return drifts.groupBy { it.timestamp.truncatedTo(ChronoUnit.HOURS) }
            .map { (timestamp, group) ->
                TimeSeriesDataPoint(
                    timestamp = timestamp,
                    avgDriftScore = group.map { it.driftScore }.average(),
                    count = group.size,
                    driftDetected = group.count { it.isDriftDetected }
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun groupByDay(drifts: List<DriftResult>): List<TimeSeriesDataPoint> {
        return drifts.groupBy { it.timestamp.truncatedTo(ChronoUnit.DAYS) }
            .map { (timestamp, group) ->
                TimeSeriesDataPoint(
                    timestamp = timestamp,
                    avgDriftScore = group.map { it.driftScore }.average(),
                    count = group.size,
                    driftDetected = group.count { it.isDriftDetected }
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun groupByWeek(drifts: List<DriftResult>): List<TimeSeriesDataPoint> {
        return drifts.groupBy {
            val weekStart = it.timestamp.minus(
                it.timestamp.atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value.toLong() - 1,
                ChronoUnit.DAYS
            ).truncatedTo(ChronoUnit.DAYS)
            weekStart
        }
            .map { (timestamp, group) ->
                TimeSeriesDataPoint(
                    timestamp = timestamp,
                    avgDriftScore = group.map { it.driftScore }.average(),
                    count = group.size,
                    driftDetected = group.count { it.isDriftDetected }
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun groupByMonth(drifts: List<DriftResult>): List<TimeSeriesDataPoint> {
        return drifts.groupBy {
            Instant.ofEpochSecond(
                it.timestamp.atZone(java.time.ZoneId.systemDefault())
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .toEpochSecond()
            )
        }
            .map { (timestamp, group) ->
                TimeSeriesDataPoint(
                    timestamp = timestamp,
                    avgDriftScore = group.map { it.driftScore }.average(),
                    count = group.size,
                    driftDetected = group.count { it.isDriftDetected }
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun filterByDateRange(
        drifts: List<DriftResult>,
        dateRange: DateRange
    ): List<DriftResult> {
        return drifts.filter { it.timestamp in dateRange.start..dateRange.end }
    }

    private fun filterPatchesByDateRange(patches: List<Patch>, dateRange: DateRange): List<Patch> {
        return patches.filter { it.createdAt in dateRange.start..dateRange.end }
    }

    private fun findBestPerformingModel(modelsWithData: List<ModelWithData>): String? {
        return modelsWithData.maxByOrNull {
            calculateModelHealthScore(it.driftResults, it.patches)
        }?.model?.id
    }

    private fun findWorstPerformingModel(modelsWithData: List<ModelWithData>): String? {
        return modelsWithData.minByOrNull {
            calculateModelHealthScore(it.driftResults, it.patches)
        }?.model?.id
    }
}

// ==================== Data Classes ====================

data class ModelAnalytics(
    val modelId: String,
    val modelName: String,
    val dateRange: DateRange,

    // Drift metrics
    val totalDriftEvents: Int,
    val driftDetectionRate: Double,
    val averageDriftScore: Double,
    val maxDriftScore: Double,
    val minDriftScore: Double,
    val driftScoreStdDev: Double,
    val driftTypeDistribution: Map<DriftType, Int>,

    // Patch metrics
    val totalPatches: Int,
    val appliedPatches: Int,
    val failedPatches: Int,
    val averagePatchSafetyScore: Double,
    val patchSuccessRate: Double,

    // Trend analysis
    val driftTrend: DriftTrend,
    val forecastedDrift: List<DriftForecast>,

    // Feature analysis
    val topDriftedFeatures: List<FeatureDriftSummary>,

    // Time-based metrics
    val driftsByDay: Map<String, List<DriftResult>>,
    val driftsByWeek: Map<String, List<DriftResult>>,
    val driftsByMonth: Map<String, List<DriftResult>>,

    // Health score
    val modelHealthScore: Double,

    // Recommendations
    val recommendations: List<Recommendation>
)

data class ModelComparison(
    val models: List<MLModel>,
    val comparisons: List<ModelComparisonData>,
    val bestPerformingModel: String?,
    val worstPerformingModel: String?
)

data class ModelComparisonData(
    val modelId: String,
    val modelName: String,
    val totalDrifts: Int,
    val avgDriftScore: Double,
    val driftRate: Double,
    val patchesApplied: Int,
    val healthScore: Double,
    val lastDriftDate: Instant?
)

data class ModelWithData(
    val model: MLModel,
    val driftResults: List<DriftResult>,
    val patches: List<Patch>
)

data class DriftForecast(
    val date: Instant,
    val predictedDriftScore: Double,
    val confidence: Double,
    val isLikelyDrift: Boolean
)

data class FeatureDriftSummary(
    val featureName: String,
    val avgDriftScore: Double,
    val maxDriftScore: Double,
    val driftFrequency: Int,
    val totalOccurrences: Int
)

data class TimeSeriesDataPoint(
    val timestamp: Instant,
    val avgDriftScore: Double,
    val count: Int,
    val driftDetected: Int
)

data class Recommendation(
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val actionItems: List<String>
)

data class DateRange(
    val start: Instant,
    val end: Instant
) {
    companion object {
        fun last7Days(): DateRange = DateRange(
            start = Instant.now().minus(7, ChronoUnit.DAYS),
            end = Instant.now()
        )

        fun last30Days(): DateRange = DateRange(
            start = Instant.now().minus(30, ChronoUnit.DAYS),
            end = Instant.now()
        )

        fun last90Days(): DateRange = DateRange(
            start = Instant.now().minus(90, ChronoUnit.DAYS),
            end = Instant.now()
        )

        fun custom(start: Instant, end: Instant): DateRange = DateRange(start, end)
    }
}

enum class DriftTrend {
    INCREASING,
    DECREASING,
    STABLE,
    INSUFFICIENT_DATA
}

enum class TimeGranularity {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class RecommendationPriority {
    HIGH,
    MEDIUM,
    LOW
}
