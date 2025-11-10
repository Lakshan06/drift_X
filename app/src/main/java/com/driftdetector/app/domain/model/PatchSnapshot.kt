package com.driftdetector.app.domain.model

import java.time.Instant

/**
 * Extended snapshot of model and data state before patch application
 * Enables rollback to previous state if patch fails or causes issues
 * This extends the basic PatchSnapshot with additional monitoring metadata
 */
data class ExtendedPatchSnapshot(
    val id: String,
    val modelId: String,
    val timestamp: Instant,
    val prePatchDriftScore: Double,
    val prePatchDriftType: DriftType,
    val originalModelPath: String,
    val originalDataChecksum: String,
    val appliedPatches: List<Patch>,
    val validationMetrics: ExtendedValidationMetrics,
    val canRollback: Boolean = true
)

/**
 * Extended validation metrics for comprehensive patch safety assessment
 */
data class ExtendedValidationMetrics(
    val safetyScore: Double,
    val driftReduction: Double,
    val accuracyChange: Double,
    val performanceImpact: Double,
    val validationSampleSize: Int,
    val validationTimestamp: Instant
)

/**
 * Extended patch application status with approval workflow
 */
enum class ExtendedPatchStatus {
    PENDING_VALIDATION,
    PENDING_APPROVAL,
    APPROVED,
    APPLIED,
    ROLLED_BACK,
    FAILED,
    UNSAFE
}

/**
 * Patch application record with approval workflow
 */
data class PatchApplicationRecord(
    val id: String,
    val snapshotId: String,
    val modelId: String,
    val patches: List<Patch>,
    val status: ExtendedPatchStatus,
    val appliedAt: Instant?,
    val rolledBackAt: Instant?,
    val validationMetrics: ExtendedValidationMetrics,
    val failureReason: String? = null,
    val requiresApproval: Boolean
)

/**
 * Drift classification types (enhanced with temporal dimension)
 */
enum class DriftClassification {
    SUDDEN_COVARIATE,      // Sudden change in input distribution
    INCREMENTAL_COVARIATE, // Gradual change in input distribution
    SUDDEN_CONCEPT,        // Sudden change in X-Y relationship
    INCREMENTAL_CONCEPT,   // Gradual change in X-Y relationship
    SUDDEN_PRIOR,          // Sudden change in output distribution
    INCREMENTAL_PRIOR,     // Gradual change in output distribution
    MIXED,                 // Multiple drift types detected
    NO_DRIFT
}

/**
 * Extended drift result with classification and recommendations
 */
data class EnhancedDriftResult(
    val baseResult: DriftResult,
    val classification: DriftClassification,
    val severity: DriftSeverity,
    val trendAnalysis: TrendAnalysis,
    val recommendedActions: List<RecommendedAction>
)

/**
 * Drift severity levels for alerting
 */
enum class DriftSeverity {
    CRITICAL,   // > 0.4, immediate action required
    HIGH,       // 0.3-0.4, action required soon
    MODERATE,   // 0.2-0.3, monitoring recommended
    LOW,        // 0.1-0.2, normal variation
    MINIMAL     // < 0.1, no action needed
}

/**
 * Trend analysis over time for predictive alerting
 */
data class TrendAnalysis(
    val isIncreasing: Boolean,
    val rateOfChange: Double,
    val projectedDriftIn24h: Double,
    val historicalPattern: String
)

/**
 * Recommended action with priority and reasoning
 */
data class RecommendedAction(
    val action: String,
    val priority: ActionPriority,
    val reasoning: String,
    val estimatedImpact: Double
)

/**
 * Action priority levels for task management
 */
enum class ActionPriority {
    IMMEDIATE,  // Must do now
    HIGH,       // Within 1 hour
    MEDIUM,     // Within 24 hours
    LOW         // When convenient
}
