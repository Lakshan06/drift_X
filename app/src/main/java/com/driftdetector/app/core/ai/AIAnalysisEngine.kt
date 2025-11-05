package com.driftdetector.app.core.ai

import android.content.Context
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.Patch
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * AI-powered analysis engine using RunAnywhere SDK
 * Provides natural language explanations for drift detection and patch recommendations
 */
class AIAnalysisEngine(private val context: Context) {

    private var isInitialized = false
    private var currentModelId: String? = null

    /**
     * Check if RunAnywhere SDK is available and initialized
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try to access RunAnywhere SDK
            val sdkClass = Class.forName("com.runanywhere.sdk.public.RunAnywhere")
            isInitialized && sdkClass != null && RunAnywhereInitializer.isInitialized()
        } catch (e: ClassNotFoundException) {
            Timber.d("RunAnywhere SDK not found in classpath (AAR files not present)")
            false
        } catch (e: NoClassDefFoundError) {
            Timber.d("RunAnywhere SDK classes not found")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error checking RunAnywhere SDK availability")
            false
        }
    }

    /**
     * Initialize the AI engine (called from Application class)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Timber.d("Initializing AI Analysis Engine with RunAnywhere SDK")

            // Check if SDK is available before trying to initialize
            val sdkAvailable = try {
                Class.forName("com.runanywhere.sdk.public.RunAnywhere")
                true
            } catch (e: Exception) {
                Timber.w("RunAnywhere SDK not available - using fallback explanations only")
                false
            }

            if (!sdkAvailable) {
                isInitialized = true
                Timber.i("AI Analysis Engine initialized (fallback mode - no SDK)")
                return@withContext
            }

            // Initialize RunAnywhere SDK
            try {
                RunAnywhereInitializer.initialize(context)
                Timber.i("RunAnywhere SDK initialized successfully")
            } catch (e: Exception) {
                Timber.w(e, "RunAnywhere SDK initialization failed, using fallback mode")
            }

            isInitialized = true
            Timber.i("AI Analysis Engine initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AI Analysis Engine")
            isInitialized = true // Still initialize in fallback mode
        }
    }

    /**
     * Generate natural language explanation for drift detection
     */
    suspend fun explainDrift(driftResult: DriftResult): String = withContext(Dispatchers.Default) {
        if (!isInitialized || !isAvailable()) {
            return@withContext generateFallbackDriftExplanation(driftResult)
        }

        try {
            // Use intelligent prompt engine with full context
            val prompt = AIPromptEngine.buildDriftExplanationPrompt(driftResult)
            // Use RunAnywhere SDK for generation
            RunAnywhere.generate(prompt)
        } catch (e: Exception) {
            Timber.e(e, "Error generating AI drift explanation")
            generateFallbackDriftExplanation(driftResult)
        }
    }

    /**
     * Generate natural language explanation for drift detection (streaming)
     */
    fun explainDriftStream(driftResult: DriftResult): Flow<String> = flow {
        if (!isInitialized || !isAvailable()) {
            emit(generateFallbackDriftExplanation(driftResult))
            return@flow
        }

        try {
            // Use intelligent prompt engine
            val prompt = AIPromptEngine.buildDriftExplanationPrompt(driftResult)
            // Use RunAnywhere SDK streaming
            RunAnywhere.generateStream(prompt).collect { token ->
                emit(token)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error generating AI drift explanation stream")
            emit(generateFallbackDriftExplanation(driftResult))
        }
    }

    /**
     * Generate recommendations for addressing detected drift
     */
    suspend fun recommendActions(driftResult: DriftResult): String =
        withContext(Dispatchers.Default) {
            if (!isInitialized || !isAvailable()) {
                return@withContext generateFallbackRecommendations(driftResult)
            }

            try {
                // Use intelligent prompt engine
                val prompt = AIPromptEngine.buildPatchRecommendationPrompt(driftResult)
                // Use RunAnywhere SDK
                RunAnywhere.generate(prompt)
            } catch (e: Exception) {
                Timber.e(e, "Error generating AI recommendations")
                generateFallbackRecommendations(driftResult)
            }
        }

    /**
     * Explain a patch in natural language
     */
    suspend fun explainPatch(patch: Patch): String = withContext(Dispatchers.Default) {
        if (!isInitialized || !isAvailable()) {
            return@withContext generateFallbackPatchExplanation(patch)
        }

        try {
            // Use intelligent prompt engine
            val prompt = AIPromptEngine.buildPatchExplanationPrompt(patch)
            // Use RunAnywhere SDK
            RunAnywhere.generate(prompt)
        } catch (e: Exception) {
            Timber.e(e, "Error generating AI patch explanation")
            generateFallbackPatchExplanation(patch)
        }
    }

    /**
     * Answer questions about drift and model performance - CHAT FUNCTIONALITY
     */
    suspend fun answerQuestion(question: String, context: String = ""): String =
        withContext(Dispatchers.Default) {
            if (!isInitialized || !isAvailable()) {
                return@withContext "AI assistant is not available. Please ensure RunAnywhere SDK is properly configured."
            }

            try {
                // Use intelligent prompt engine that automatically detects question type
                val prompt = AIPromptEngine.buildIntelligentPrompt(question)
                // Use RunAnywhere SDK
                RunAnywhere.generate(prompt)
            } catch (e: Exception) {
                Timber.e(e, "Error generating AI answer")
                "Unable to generate answer. Please try again."
            }
        }

    /**
     * Stream answers for chat - provides real-time response
     */
    fun answerQuestionStream(question: String, context: String = ""): Flow<String> = flow {
        if (!isInitialized || !isAvailable()) {
            emit("AI assistant is not available. Please ensure RunAnywhere SDK is properly configured.")
            return@flow
        }

        try {
            // Use intelligent prompt engine that automatically detects question type
            val prompt = AIPromptEngine.buildIntelligentPrompt(question)

            // Use RunAnywhere SDK streaming
            RunAnywhere.generateStream(prompt).collect { token ->
                emit(token)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error generating AI answer stream")
            emit("Unable to generate answer. Please try again.")
        }
    }

    // ========================================
    // Fallback Explanations (when AI not available)
    // ========================================

    private fun generateFallbackDriftExplanation(driftResult: DriftResult): String {
        val topFeatures = driftResult.featureDrifts
            .sortedByDescending { it.driftScore }
            .take(3)
            .joinToString(", ") { it.featureName }

        return when {
            !driftResult.isDriftDetected -> {
                "No significant drift detected. The model's input data distribution remains consistent with the training data."
            }

            driftResult.driftScore > 0.5 -> {
                "⚠️ High drift detected (score: ${
                    String.format(
                        "%.2f",
                        driftResult.driftScore
                    )
                }). " +
                        "Major changes in data distribution observed in features: $topFeatures. " +
                        "Immediate attention required - model performance may be significantly degraded."
            }

            driftResult.driftScore > 0.2 -> {
                "⚡ Moderate drift detected (score: ${
                    String.format(
                        "%.2f",
                        driftResult.driftScore
                    )
                }). " +
                        "Notable shifts in features: $topFeatures. " +
                        "Monitor closely and consider applying patches or retraining."
            }

            else -> {
                "ℹ️ Minor drift detected (score: ${
                    String.format(
                        "%.2f",
                        driftResult.driftScore
                    )
                }). " +
                        "Small changes in features: $topFeatures. " +
                        "Continue monitoring, but no immediate action required."
            }
        }
    }

    private fun generateFallbackRecommendations(driftResult: DriftResult): String {
        val recommendations = mutableListOf<String>()

        when {
            driftResult.driftScore > 0.5 -> {
                recommendations.add("1. **Urgent**: Apply auto-generated patch immediately to stabilize model performance")
                recommendations.add("2. **Schedule**: Plan model retraining with recent data within the next 1-2 weeks")
                recommendations.add("3. **Monitor**: Increase monitoring frequency to hourly checks")
                recommendations.add("4. **Validate**: Run validation suite to quantify performance degradation")
            }

            driftResult.driftScore > 0.2 -> {
                recommendations.add("1. **Apply Patch**: Review and apply suggested patches to adapt to data changes")
                recommendations.add("2. **Collect Data**: Gather more samples from the new distribution for retraining")
                recommendations.add("3. **Feature Analysis**: Investigate why top features are drifting")
                recommendations.add("4. **Timeline**: Plan retraining within 1-2 months if drift continues")
            }

            else -> {
                recommendations.add("1. **Continue Monitoring**: Maintain current monitoring schedule")
                recommendations.add("2. **Log Pattern**: Document this drift pattern for trend analysis")
                recommendations.add("3. **Optional**: Consider applying light patches if available")
                recommendations.add("4. **Review**: Check again in 1-2 weeks to ensure drift doesn't increase")
            }
        }

        return recommendations.joinToString("\n")
    }

    private fun generateFallbackPatchExplanation(patch: Patch): String {
        return when (patch.patchType.name) {
            "FEATURE_CLIPPING" -> {
                "This patch applies feature clipping to constrain outlier values. " +
                        "It prevents extreme values from disproportionately affecting model predictions. " +
                        "Trade-off: May lose some information from legitimate extreme cases."
            }

            "FEATURE_REWEIGHTING" -> {
                "This patch adjusts feature importance weights to adapt to the new data distribution. " +
                        "It rebalances how much each feature influences predictions. " +
                        "Trade-off: Changes model behavior, requires thorough validation."
            }

            "THRESHOLD_TUNING" -> {
                "This patch adjusts decision thresholds to maintain desired precision/recall balance. " +
                        "It recalibrates classification boundaries based on current data. " +
                        "Trade-off: Only adjusts decision boundaries, doesn't fix underlying distribution shift."
            }

            "NORMALIZATION_UPDATE" -> {
                "This patch updates feature normalization parameters (mean/std) to match new data. " +
                        "It ensures features are properly scaled for the current distribution. " +
                        "Trade-off: Most conservative approach, minimal risk but limited impact."
            }

            else -> {
                "This patch adapts the model to handle the detected drift. " +
                        "Review the configuration details and safety score before applying."
            }
        }
    }

    /**
     * Clean up resources
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            // When SDK is available: RunAnywhere.unloadModel()
            isInitialized = false
            currentModelId = null
            Timber.d("AI Analysis Engine shut down")
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down AI Analysis Engine")
        }
    }
}
