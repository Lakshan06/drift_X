package com.driftdetector.app.core.ai

import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchType

/**
 * AI Prompt Engineering System
 * Creates intelligent prompts that make the AI respond like a drift detection expert
 */
object AIPromptEngine {

    /**
     * System context that defines the AI's role and knowledge
     */
    private const val SYSTEM_CONTEXT =
        """You are an expert AI assistant for the DriftGuardAI mobile application, specialized in machine learning model drift detection and monitoring.

YOUR EXPERTISE:
- Model drift detection (concept drift, covariate drift, prior drift)
- Statistical tests (PSI, Kolmogorov-Smirnov, Chi-square)
- Feature attribution and SHAP-like analysis
- Auto-patch synthesis (feature clipping, reweighting, threshold tuning, normalization)
- TensorFlow Lite on-device inference
- Privacy-first, on-device ML monitoring

YOUR PERSONALITY:
- Professional yet friendly and approachable
- Patient and educational - explain complex concepts clearly
- Practical - always provide actionable advice
- Honest about limitations and risks
- Encouraging but realistic about challenges

DriftGuardAI APP CAPABILITIES:
1. Real-time drift detection using PSI and KS statistical tests
2. Feature-level attribution to identify drift drivers
3. Automatic patch synthesis with 4 types:
   - Feature Clipping: Constrains outlier values to reduce noise
   - Feature Reweighting: Adjusts feature importance weights
   - Threshold Tuning: Recalibrates decision boundaries
   - Normalization Update: Updates scaling parameters
4. Patch validation with safety checks before application
5. Reversible patches - can rollback any change
6. Encrypted local storage with SQLCipher
7. Background monitoring with WorkManager
8. Interactive dashboard with visualizations

KEY CONCEPTS:
- PSI (Population Stability Index): Measures distribution shift, threshold typically 0.2
- KS Test: Statistical test comparing two distributions, p-value < 0.05 indicates drift
- Concept Drift: P(Y|X) changes - relationship between features and target changes
- Covariate Drift: P(X) changes - input distribution changes
- Prior Drift: P(Y) changes - target distribution changes

THRESHOLDS IN APP:
- PSI > 0.2: Moderate drift (yellow alert)
- PSI > 0.5: High drift (red alert)
- KS p-value < 0.05: Statistically significant drift
- Drift Score > 0.7: Critical - immediate action needed
- Drift Score 0.5-0.7: High - action recommended
- Drift Score 0.2-0.5: Moderate - monitor closely
- Drift Score < 0.2: Low - continue monitoring

RESPONSE GUIDELINES:
1. Always acknowledge the user's question directly
2. Provide specific, actionable advice
3. Use examples from the app when relevant
4. Explain technical terms in simple language
5. Mention relevant app features that can help
6. Warn about risks when appropriate
7. Suggest next steps or follow-up actions
8. Keep responses concise but complete (2-4 paragraphs)
9. Use bullet points for lists and steps
10. Be encouraging and supportive"""

    /**
     * Generate a comprehensive prompt for general drift questions
     */
    fun buildGeneralQuestionPrompt(
        question: String,
        conversationHistory: List<String> = emptyList()
    ): String {
        val historyContext = if (conversationHistory.isNotEmpty()) {
            "\n\nRECENT CONVERSATION:\n${conversationHistory.takeLast(4).joinToString("\n")}"
        } else ""

        return """$SYSTEM_CONTEXT

$historyContext

USER QUESTION: $question

Provide a helpful, accurate, and actionable response. If the question is about drift detection, model monitoring, or the DriftGuardAI app, answer with your full expertise. If it's unrelated to drift detection, politely redirect to drift-related topics.

RESPONSE:"""
    }

    /**
     * Generate prompt for drift result explanation
     */
    fun buildDriftExplanationPrompt(driftResult: DriftResult): String {
        val severity = when {
            driftResult.driftScore > 0.7 -> "CRITICAL"
            driftResult.driftScore > 0.5 -> "HIGH"
            driftResult.driftScore > 0.2 -> "MODERATE"
            else -> "LOW"
        }

        val topFeatures = driftResult.featureDrifts
            .sortedByDescending { it.driftScore }
            .take(5)
            .joinToString("\n") {
                "  - ${it.featureName}: ${
                    String.format(
                        "%.4f",
                        it.driftScore
                    )
                } (attribution: ${String.format("%.4f", it.attribution)})"
            }

        val hasStatTests = driftResult.statisticalTests.isNotEmpty()
        val statTests = if (hasStatTests) {
            driftResult.statisticalTests.joinToString("\n") {
                "  - ${it.testName}: statistic=${
                    String.format(
                        "%.4f",
                        it.statistic
                    )
                }, p-value=${
                    String.format(
                        "%.4f",
                        it.pValue
                    )
                }, ${if (it.isPassed) "✓ PASS" else "✗ FAIL"}"
            }
        } else ""

        return """$SYSTEM_CONTEXT

DRIFT DETECTION RESULT TO EXPLAIN:

Drift Summary:
- Model ID: ${driftResult.modelId}
- Drift Type: ${driftResult.driftType.name.replace("_", " ")}
- Drift Score: ${String.format("%.4f", driftResult.driftScore)}
- Severity Level: $severity
- Detection Time: ${driftResult.timestamp}
- Drift Detected: ${if (driftResult.isDriftDetected) "YES ⚠️" else "NO ✓"}

Top Contributing Features:
$topFeatures

${if (hasStatTests) "Statistical Test Results:\n$statTests" else ""}

TASK: Provide a comprehensive explanation of this drift result that:
1. Explains what type of drift was detected and what it means
2. Identifies the main contributing features and why they're drifting
3. Assesses the severity and potential impact on model performance
4. Provides specific, actionable recommendations for addressing this drift
5. Explains any statistical tests that were performed
6. Suggests appropriate patches or actions available in the app

Make the explanation clear for both technical and non-technical users. Focus on practical implications and next steps.

EXPLANATION:"""
    }

    /**
     * Generate prompt for patch recommendations
     */
    fun buildPatchRecommendationPrompt(driftResult: DriftResult): String {
        val topFeatures = driftResult.featureDrifts
            .sortedByDescending { it.driftScore }
            .take(3)
            .joinToString(", ") { it.featureName }

        return """$SYSTEM_CONTEXT

DRIFT SITUATION REQUIRING PATCH RECOMMENDATIONS:

Drift Analysis:
- Drift Score: ${String.format("%.4f", driftResult.driftScore)}
- Drift Type: ${driftResult.driftType.name.replace("_", " ")}
- Top Drifting Features: $topFeatures
- Total Features Affected: ${driftResult.featureDrifts.size}

Available Patch Types in DriftGuardAI:
1. Feature Clipping - Constrains outlier values to reduce impact of extreme values
2. Feature Reweighting - Adjusts feature importance to adapt to new distribution
3. Threshold Tuning - Recalibrates decision boundaries for better separation
4. Normalization Update - Updates mean/std to match current data distribution
5. Ensemble Reweighting - Adjusts weights of ensemble model components to adapt to drift
6. Calibration Adjustment - Recalibrates probability outputs to match observed frequencies

TASK: Recommend the BEST patches for this specific drift situation:
1. Analyze which patch types are most appropriate for this drift
2. Explain WHY each recommended patch will help
3. Prioritize patches from most to least critical
4. Warn about any risks or considerations for each patch
5. Suggest a testing strategy before applying patches
6. Recommend monitoring frequency after patch application
7. Indicate when model retraining should be considered instead

Provide practical, specific recommendations the user can act on immediately.

RECOMMENDATIONS:"""
    }

    /**
     * Generate prompt for patch explanation
     */
    fun buildPatchExplanationPrompt(patch: Patch): String {
        val patchTypeDetail = when (patch.patchType) {
            PatchType.FEATURE_CLIPPING -> """
FEATURE CLIPPING PATCH:
- Purpose: Constrains feature values to a specified range to reduce outlier impact
- How it works: Clips values outside [min, max] bounds
- When to use: Data contains outliers or extreme values causing drift
- Risks: May lose information from legitimate extreme cases
- Reversibility: Fully reversible - original values preserved"""

            PatchType.FEATURE_REWEIGHTING -> """
FEATURE REWEIGHTING PATCH:
- Purpose: Adjusts relative importance of features to adapt to new distribution
- How it works: Multiplies feature values by learned weight factors
- When to use: Feature importance has changed in new data
- Risks: Changes model behavior, requires careful validation
- Reversibility: Fully reversible - can restore original weights"""

            PatchType.THRESHOLD_TUNING -> """
THRESHOLD TUNING PATCH:
- Purpose: Recalibrates decision thresholds to maintain precision/recall balance
- How it works: Adjusts classification boundaries based on new data statistics
- When to use: Class distribution has shifted (prior drift)
- Risks: Only adjusts boundaries, doesn't fix underlying distribution shift
- Reversibility: Fully reversible - can restore original thresholds"""

            PatchType.NORMALIZATION_UPDATE -> """
NORMALIZATION UPDATE PATCH:
- Purpose: Updates feature scaling parameters (mean, std) to match new data
- How it works: Recalculates normalization statistics from current distribution
- When to use: Feature scales have changed but relationships remain stable
- Risks: Lowest risk, most conservative approach
- Reversibility: Fully reversible - old parameters saved"""

            PatchType.ENSEMBLE_REWEIGHT -> """
ENSEMBLE REWEIGHT PATCH:
- Purpose: Adjusts weights of ensemble model components to adapt to drift
- How it works: Reweights individual models in ensemble based on current performance
- When to use: Some ensemble components handle new distribution better than others
- Risks: May reduce overall ensemble diversity, requires validation
- Reversibility: Fully reversible - original ensemble weights preserved"""

            PatchType.CALIBRATION_ADJUST -> """
CALIBRATION ADJUST PATCH:
- Purpose: Recalibrates probability outputs to match observed frequencies
- How it works: Adjusts probability mapping using isotonic regression or Platt scaling
- When to use: Predicted probabilities don't match actual outcomes (calibration drift)
- Risks: Only fixes probability estimates, not underlying predictions
- Reversibility: Fully reversible - original calibration restored"""
        }

        val safetyScore = patch.validationResult?.metrics?.safetyScore
        val safetyAssessment = when {
            safetyScore == null -> "Not yet validated - validation required before application"
            safetyScore >= 0.8 -> "HIGH safety - Low risk, recommended for production"
            safetyScore >= 0.6 -> "MODERATE safety - Some risk, test thoroughly"
            safetyScore >= 0.4 -> "LOW safety - Significant risk, careful testing needed"
            else -> "VERY LOW safety - High risk, consider alternatives"
        }

        return """$SYSTEM_CONTEXT

PATCH TO EXPLAIN:

Patch Details:
- Patch ID: ${patch.id}
- Type: ${patch.patchType.name.replace("_", " ")}
- Target Model: ${patch.modelId}
- Status: ${patch.status.name}
- Created: ${patch.createdAt}
${if (patch.appliedAt != null) "- Applied: ${patch.appliedAt}" else ""}

$patchTypeDetail

Safety Assessment:
- Safety Score: ${safetyScore?.let { String.format("%.2f", it) } ?: "Not validated"}
- Status: $safetyAssessment

TASK: Provide a clear, comprehensive explanation of this patch that:
1. Explains what this specific patch does in simple terms
2. Describes how it addresses the drift problem
3. Discusses the safety score and what it means
4. Lists benefits and potential drawbacks
5. Provides recommendations for:
   - Whether to apply this patch
   - Testing strategy before production use
   - Monitoring after application
   - When to consider rollback
6. Explains the reversibility and rollback process

Make the explanation actionable and help the user make an informed decision.

EXPLANATION:"""
    }

    /**
     * Generate prompt for troubleshooting help
     */
    fun buildTroubleshootingPrompt(issue: String): String {
        return """$SYSTEM_CONTEXT

USER IS EXPERIENCING AN ISSUE:
$issue

TASK: Provide comprehensive troubleshooting help:
1. Acknowledge the issue and show understanding
2. Identify the most likely causes based on common DriftGuardAI scenarios
3. Provide step-by-step troubleshooting steps
4. Suggest specific app features or settings to check
5. Recommend preventive measures for the future
6. Indicate when to seek additional support

Be patient, thorough, and helpful. Assume the user may not be deeply technical.

TROUBLESHOOTING GUIDANCE:"""
    }

    /**
     * Generate prompt for learning/educational questions
     */
    fun buildEducationalPrompt(topic: String): String {
        return """$SYSTEM_CONTEXT

USER WANTS TO LEARN ABOUT: $topic

TASK: Provide an educational response that:
1. Explains the concept clearly with real-world examples
2. Relates it specifically to model drift detection and the DriftGuardAI app
3. Provides practical examples of how this applies in the app
4. Suggests hands-on ways to explore this in the app
5. Links to related concepts they should know
6. Provides tips for best practices

Make learning engaging and practical. Use the app's features as teaching examples.

EDUCATIONAL RESPONSE:"""
    }

    /**
     * Generate prompt for best practices and strategies
     */
    fun buildBestPracticesPrompt(area: String): String {
        return """$SYSTEM_CONTEXT

USER ASKING ABOUT BEST PRACTICES FOR: $area

TASK: Provide expert best practices guidance:
1. List 5-7 key best practices for this area
2. Explain WHY each practice is important
3. Provide specific examples from drift detection scenarios
4. Show how to implement each practice in DriftGuardAI
5. Warn about common pitfalls to avoid
6. Suggest a prioritized implementation order
7. Mention how to measure success

Be authoritative but practical. Focus on actionable advice.

BEST PRACTICES:"""
    }

    /**
     * Generate prompt for comparative questions (X vs Y)
     */
    fun buildComparisonPrompt(itemA: String, itemB: String): String {
        return """$SYSTEM_CONTEXT

USER WANTS TO COMPARE: $itemA vs $itemB

TASK: Provide a comprehensive comparison:
1. Define each concept/approach clearly
2. Create a comparison table with key dimensions:
   - Purpose/Use Case
   - How it works
   - Advantages
   - Disadvantages
   - When to use each
   - Performance characteristics
3. Provide specific examples from drift detection
4. Recommend which to choose in different scenarios
5. Explain how DriftGuardAI supports each (if applicable)

Be objective and help the user make an informed choice.

COMPARISON:"""
    }

    /**
     * Generate prompt for "how-to" questions
     */
    fun buildHowToPrompt(task: String): String {
        return """$SYSTEM_CONTEXT

USER WANTS TO KNOW HOW TO: $task

TASK: Provide clear, step-by-step instructions:
1. Briefly explain what this task accomplishes
2. List prerequisites or requirements
3. Provide numbered steps with specific actions
4. Include relevant app screenshots locations or UI elements to look for
5. Mention any settings or configurations needed
6. Warn about common mistakes
7. Explain how to verify success
8. Suggest next steps after completion

Make instructions crystal clear and easy to follow.

HOW-TO GUIDE:"""
    }

    /**
     * Detect question type and route to appropriate prompt builder
     */
    fun buildIntelligentPrompt(question: String, context: Map<String, Any> = emptyMap()): String {
        val lowerQuestion = question.lowercase()

        return when {
            // Comparison questions
            lowerQuestion.contains(" vs ") || lowerQuestion.contains(" versus ") ||
                    lowerQuestion.contains("difference between") || lowerQuestion.contains("compare") -> {
                val parts =
                    lowerQuestion.split(Regex(" vs | versus | and | or |difference between"))
                if (parts.size >= 2) {
                    buildComparisonPrompt(parts[0].trim(), parts[1].trim())
                } else {
                    buildGeneralQuestionPrompt(question)
                }
            }

            // How-to questions
            lowerQuestion.startsWith("how to") || lowerQuestion.startsWith("how do i") ||
                    lowerQuestion.startsWith("how can i") || lowerQuestion.contains("steps to") -> {
                val task =
                    lowerQuestion.replace(Regex("^(how to|how do i|how can i|steps to)"), "").trim()
                buildHowToPrompt(task)
            }

            // Troubleshooting questions
            lowerQuestion.contains("not working") || lowerQuestion.contains("error") ||
                    lowerQuestion.contains("problem") || lowerQuestion.contains("issue") ||
                    lowerQuestion.contains("failed") || lowerQuestion.contains("can't") ||
                    lowerQuestion.contains("unable to") -> {
                buildTroubleshootingPrompt(question)
            }

            // Best practices questions
            lowerQuestion.contains("best practice") || lowerQuestion.contains("best way") ||
                    lowerQuestion.contains("recommended") || lowerQuestion.contains("should i") ||
                    lowerQuestion.contains("tips for") -> {
                buildBestPracticesPrompt(question)
            }

            // Educational questions
            lowerQuestion.startsWith("what is") || lowerQuestion.startsWith("what are") ||
                    lowerQuestion.startsWith("explain") || lowerQuestion.startsWith("tell me about") ||
                    lowerQuestion.contains("learn about") || lowerQuestion.contains("understand") -> {
                val topic = lowerQuestion.replace(
                    Regex("^(what is|what are|explain|tell me about|learn about)"),
                    ""
                ).trim()
                buildEducationalPrompt(topic)
            }

            // Default: general question
            else -> buildGeneralQuestionPrompt(question)
        }
    }
}
