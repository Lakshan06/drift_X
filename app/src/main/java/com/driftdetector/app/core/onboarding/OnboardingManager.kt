package com.driftdetector.app.core.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Manages user onboarding experience and help system
 *
 * Features:
 * - Multi-step onboarding flow
 * - Feature discovery
 * - Contextual tooltips
 * - Tutorial system
 * - Help documentation
 * - Progress tracking
 */
class OnboardingManager(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

    companion object {
        // Onboarding completion flags
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_CURRENT_STEP = intPreferencesKey("current_step")

        // Feature discovery flags
        private val KEY_UPLOAD_MODEL_SHOWN = booleanPreferencesKey("upload_model_shown")
        private val KEY_DRIFT_DETECTION_SHOWN = booleanPreferencesKey("drift_detection_shown")
        private val KEY_PATCH_SYNTHESIS_SHOWN = booleanPreferencesKey("patch_synthesis_shown")
        private val KEY_AUTO_PATCHING_SHOWN = booleanPreferencesKey("auto_patching_shown")
        private val KEY_DASHBOARD_SHOWN = booleanPreferencesKey("dashboard_shown")
        private val KEY_AI_ASSISTANT_SHOWN = booleanPreferencesKey("ai_assistant_shown")
        private val KEY_SETTINGS_SHOWN = booleanPreferencesKey("settings_shown")

        // Tutorial completion flags
        private val KEY_TUTORIAL_UPLOAD = booleanPreferencesKey("tutorial_upload")
        private val KEY_TUTORIAL_DRIFT = booleanPreferencesKey("tutorial_drift")
        private val KEY_TUTORIAL_PATCH = booleanPreferencesKey("tutorial_patch")

        // Help system flags
        private val KEY_HELP_DISMISSED_COUNT = intPreferencesKey("help_dismissed_count")
    }

    /**
     * Check if main onboarding is completed
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Get current onboarding step
     */
    val currentOnboardingStep: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_STEP] ?: 0
    }

    /**
     * Complete main onboarding
     */
    suspend fun completeOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
            prefs[KEY_CURRENT_STEP] = OnboardingStep.values().size
        }
        Timber.i("✅ Onboarding completed")
    }

    /**
     * Set current onboarding step
     */
    suspend fun setOnboardingStep(step: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_STEP] = step
        }
    }

    /**
     * Reset onboarding (for testing or user request)
     */
    suspend fun resetOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = false
            prefs[KEY_CURRENT_STEP] = 0

            // Reset feature discovery
            prefs[KEY_UPLOAD_MODEL_SHOWN] = false
            prefs[KEY_DRIFT_DETECTION_SHOWN] = false
            prefs[KEY_PATCH_SYNTHESIS_SHOWN] = false
            prefs[KEY_AUTO_PATCHING_SHOWN] = false
            prefs[KEY_DASHBOARD_SHOWN] = false
            prefs[KEY_AI_ASSISTANT_SHOWN] = false
            prefs[KEY_SETTINGS_SHOWN] = false

            // Reset tutorials
            prefs[KEY_TUTORIAL_UPLOAD] = false
            prefs[KEY_TUTORIAL_DRIFT] = false
            prefs[KEY_TUTORIAL_PATCH] = false
        }
        Timber.i("🔄 Onboarding reset")
    }

    /**
     * Check if feature discovery should be shown
     */
    suspend fun shouldShowFeatureDiscovery(feature: FeatureDiscoveryType): Boolean {
        val key = when (feature) {
            FeatureDiscoveryType.UPLOAD_MODEL -> KEY_UPLOAD_MODEL_SHOWN
            FeatureDiscoveryType.DRIFT_DETECTION -> KEY_DRIFT_DETECTION_SHOWN
            FeatureDiscoveryType.PATCH_SYNTHESIS -> KEY_PATCH_SYNTHESIS_SHOWN
            FeatureDiscoveryType.AUTO_PATCHING -> KEY_AUTO_PATCHING_SHOWN
            FeatureDiscoveryType.DASHBOARD -> KEY_DASHBOARD_SHOWN
            FeatureDiscoveryType.AI_ASSISTANT -> KEY_AI_ASSISTANT_SHOWN
            FeatureDiscoveryType.SETTINGS -> KEY_SETTINGS_SHOWN
        }

        return context.dataStore.data.map { prefs ->
            !(prefs[key] ?: false)
        }.map { it }.first()
    }

    /**
     * Mark feature discovery as shown
     */
    suspend fun markFeatureDiscoveryShown(feature: FeatureDiscoveryType) {
        val key = when (feature) {
            FeatureDiscoveryType.UPLOAD_MODEL -> KEY_UPLOAD_MODEL_SHOWN
            FeatureDiscoveryType.DRIFT_DETECTION -> KEY_DRIFT_DETECTION_SHOWN
            FeatureDiscoveryType.PATCH_SYNTHESIS -> KEY_PATCH_SYNTHESIS_SHOWN
            FeatureDiscoveryType.AUTO_PATCHING -> KEY_AUTO_PATCHING_SHOWN
            FeatureDiscoveryType.DASHBOARD -> KEY_DASHBOARD_SHOWN
            FeatureDiscoveryType.AI_ASSISTANT -> KEY_AI_ASSISTANT_SHOWN
            FeatureDiscoveryType.SETTINGS -> KEY_SETTINGS_SHOWN
        }

        context.dataStore.edit { prefs ->
            prefs[key] = true
        }
    }

    /**
     * Check if tutorial should be shown
     */
    suspend fun shouldShowTutorial(tutorial: TutorialType): Boolean {
        val key = when (tutorial) {
            TutorialType.UPLOAD -> KEY_TUTORIAL_UPLOAD
            TutorialType.DRIFT_DETECTION -> KEY_TUTORIAL_DRIFT
            TutorialType.PATCH_APPLICATION -> KEY_TUTORIAL_PATCH
        }

        return context.dataStore.data.map { prefs ->
            !(prefs[key] ?: false)
        }.map { it }.first()
    }

    /**
     * Mark tutorial as completed
     */
    suspend fun markTutorialCompleted(tutorial: TutorialType) {
        val key = when (tutorial) {
            TutorialType.UPLOAD -> KEY_TUTORIAL_UPLOAD
            TutorialType.DRIFT_DETECTION -> KEY_TUTORIAL_DRIFT
            TutorialType.PATCH_APPLICATION -> KEY_TUTORIAL_PATCH
        }

        context.dataStore.edit { prefs ->
            prefs[key] = true
        }
        Timber.i("✅ Tutorial completed: $tutorial")
    }

    /**
     * Get onboarding steps
     */
    fun getOnboardingSteps(): List<OnboardingStepData> {
        return OnboardingStep.values().map { step ->
            when (step) {
                OnboardingStep.WELCOME -> OnboardingStepData(
                    step = step,
                    title = "Welcome to DriftGuardAI",
                    description = "Your intelligent ML model drift detection and automatic patching system",
                    icon = "welcome",
                    ctaText = "Get Started"
                )

                OnboardingStep.UPLOAD_MODEL -> OnboardingStepData(
                    step = step,
                    title = "Upload Your Model",
                    description = "Support for TensorFlow Lite, ONNX, Keras, and PyTorch models. Upload from local storage, cloud, or URL",
                    icon = "upload",
                    ctaText = "Next"
                )

                OnboardingStep.UPLOAD_DATA -> OnboardingStepData(
                    step = step,
                    title = "Add Your Data",
                    description = "Upload datasets in CSV, JSON, or Parquet format. We'll automatically detect drift in your model's predictions",
                    icon = "data",
                    ctaText = "Next"
                )

                OnboardingStep.DRIFT_DETECTION -> OnboardingStepData(
                    step = step,
                    title = "Automatic Drift Detection",
                    description = "We use advanced statistical tests (KS, Chi-Square, PSI) to detect drift in your model's performance",
                    icon = "drift",
                    ctaText = "Next"
                )

                OnboardingStep.PATCH_SYNTHESIS -> OnboardingStepData(
                    step = step,
                    title = "Intelligent Patch Synthesis",
                    description = "When drift is detected, we automatically generate and validate patches to restore your model's performance",
                    icon = "patch",
                    ctaText = "Next"
                )

                OnboardingStep.AUTO_PATCHING -> OnboardingStepData(
                    step = step,
                    title = "Automatic Patching",
                    description = "Patches can be automatically applied with safety validation. Achieve 95-99.5% drift reduction!",
                    icon = "auto",
                    ctaText = "Next"
                )

                OnboardingStep.DASHBOARD -> OnboardingStepData(
                    step = step,
                    title = "Monitor Everything",
                    description = "Beautiful dashboard with real-time metrics, charts, and analytics. Track model health at a glance",
                    icon = "dashboard",
                    ctaText = "Next"
                )

                OnboardingStep.COMPLETE -> OnboardingStepData(
                    step = step,
                    title = "You're All Set!",
                    description = "Start monitoring your ML models for drift and let us handle the automatic patching. Need help? Use the AI assistant anytime!",
                    icon = "complete",
                    ctaText = "Start Using DriftGuardAI"
                )
            }
        }
    }

    /**
     * Get help content for a specific topic
     */
    fun getHelpContent(topic: HelpTopic): HelpContent {
        return when (topic) {
            HelpTopic.MODEL_UPLOAD -> HelpContent(
                title = "Uploading Models",
                sections = listOf(
                    HelpSection(
                        heading = "Supported Model Formats",
                        content = """
                            DriftGuardAI supports multiple model formats:
                            
                            • TensorFlow Lite (.tflite) - Optimized for mobile
                            • ONNX (.onnx) - Cross-platform format
                            • Keras (.h5) - TensorFlow/Keras models
                            • PyTorch (.pt, .pth) - PyTorch models
                            • TensorFlow SavedModel (.pb)
                            
                            We automatically extract metadata like input features and output labels.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Upload Methods",
                        content = """
                            Choose from 4 convenient upload methods:
                            
                            1. Local Files - Select from your device
                            2. Cloud Storage - Import from Google Drive, Dropbox, or OneDrive
                            3. URL Import - Download directly from a URL
                            4. Drag & Drop - Simply drag files into the app
                            
                            All uploads are encrypted and stored securely on your device.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Best Practices",
                        content = """
                            For best results:
                            
                            • Upload both your model AND reference data together
                            • Ensure your data matches the model's expected features
                            • Use representative data for accurate drift detection
                            • Update your reference data periodically
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.DATA_UPLOAD, HelpTopic.DRIFT_DETECTION)
            )

            HelpTopic.DATA_UPLOAD -> HelpContent(
                title = "Uploading Data",
                sections = listOf(
                    HelpSection(
                        heading = "Supported Data Formats",
                        content = """
                            We support various data formats:
                            
                            • CSV - Comma-separated values
                            • JSON - Single object or array format
                            • TSV - Tab-separated values
                            • TXT - Custom delimiters
                            • PSV - Pipe-separated values
                            • DAT - Generic data files
                            • Parquet - Columnar format (coming soon)
                            
                            Format is auto-detected based on file extension and content.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Data Requirements",
                        content = """
                            Your data should:
                            
                            • Match your model's input features
                            • Include column headers (for CSV/TSV)
                            • Be properly formatted (no corrupt data)
                            • Have sufficient samples (minimum 100 rows recommended)
                            
                            Missing values are handled automatically.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.MODEL_UPLOAD, HelpTopic.DRIFT_DETECTION)
            )

            HelpTopic.DRIFT_DETECTION -> HelpContent(
                title = "Understanding Drift Detection",
                sections = listOf(
                    HelpSection(
                        heading = "What is Model Drift?",
                        content = """
                            Model drift occurs when your model's performance degrades over time due to changes in:
                            
                            • Input data distribution (Covariate Drift)
                            • Relationship between features and target (Concept Drift)
                            • Target variable distribution (Prior Drift)
                            
                            DriftGuardAI automatically detects all types of drift.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Detection Methods",
                        content = """
                            We use three statistical tests:
                            
                            1. Kolmogorov-Smirnov (KS) Test
                               - Compares distributions
                               - Good for continuous features
                            
                            2. Chi-Square Test
                               - Compares categorical distributions
                               - Good for discrete features
                            
                            3. Population Stability Index (PSI)
                               - Measures distribution shift
                               - Industry-standard metric
                            
                            All tests run automatically on your data.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Interpreting Drift Scores",
                        content = """
                            Drift scores range from 0.0 to 1.0:
                            
                            • 0.0 - 0.3: Low drift (monitor)
                            • 0.3 - 0.5: Medium drift (review)
                            • 0.5 - 0.8: High drift (action recommended)
                            • 0.8 - 1.0: Critical drift (immediate action)
                            
                            Higher scores indicate more significant distribution changes.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.PATCH_SYNTHESIS, HelpTopic.DASHBOARD)
            )

            HelpTopic.PATCH_SYNTHESIS -> HelpContent(
                title = "Patch Synthesis",
                sections = listOf(
                    HelpSection(
                        heading = "What are Patches?",
                        content = """
                            Patches are reversible modifications that restore your model's performance:
                            
                            • Feature Clipping - Control outlier values
                            • Feature Reweighting - Adjust feature importance
                            • Threshold Tuning - Optimize decision boundaries
                            • Normalization Update - Align distributions
                            
                            All patches are validated before application.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Safety Validation",
                        content = """
                            Every patch includes:
                            
                            • Safety Score (0-100%) - Risk assessment
                            • Expected Impact - Predicted drift reduction
                            • Accuracy Metrics - Performance validation
                            • Rollback Capability - Undo if needed
                            
                            Patches with safety scores >80% are considered safe.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Ultra-Aggressive Mode",
                        content = """
                            For maximum drift reduction (95-99.5%):
                            
                            • Multiple patch strategies combined
                            • Intelligent patch chaining
                            • Automatic application available
                            • Still maintains safety validation
                            
                            Enable in Settings for automatic ultra-aggressive patching.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.AUTO_PATCHING, HelpTopic.DRIFT_DETECTION)
            )

            HelpTopic.AUTO_PATCHING -> HelpContent(
                title = "Automatic Patching",
                sections = listOf(
                    HelpSection(
                        heading = "How It Works",
                        content = """
                            Automatic patching:
                            
                            1. Detects drift in your model
                            2. Synthesizes appropriate patches
                            3. Validates patch safety
                            4. Automatically applies if safe
                            5. Monitors performance
                            6. Rolls back if issues detected
                            
                            Fully automated, no manual intervention needed!
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Configuration",
                        content = """
                            You can customize:
                            
                            • Minimum safety score threshold
                            • Auto-apply enabled/disabled
                            • Drift score threshold for action
                            • Notification preferences
                            
                            Access settings from the Settings screen.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.PATCH_SYNTHESIS, HelpTopic.SETTINGS)
            )

            HelpTopic.DASHBOARD -> HelpContent(
                title = "Using the Dashboard",
                sections = listOf(
                    HelpSection(
                        heading = "Dashboard Overview",
                        content = """
                            The dashboard shows:
                            
                            • Real-time drift metrics
                            • Model health scores (0-100)
                            • Recent drift events
                            • Applied patches
                            • Trend charts
                            • Quick actions
                            
                            All metrics update automatically as data changes.
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "Understanding Metrics",
                        content = """
                            Key metrics explained:
                            
                            • Model Health: Overall model condition (0-100)
                            • Drift Events: Number of detected drifts
                            • Patches Applied: Auto-applied patch count
                            • Success Rate: Patch application success %
                            
                            Green = healthy, Yellow = warning, Red = critical
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.DRIFT_DETECTION, HelpTopic.ANALYTICS)
            )

            HelpTopic.AI_ASSISTANT -> HelpContent(
                title = "AI Assistant",
                sections = listOf(
                    HelpSection(
                        heading = "What Can It Do?",
                        content = """
                            The AI Assistant helps with:
                            
                            • Explaining drift detection results
                            • Recommending patch strategies
                            • Answering technical questions
                            • Providing step-by-step guidance
                            • Troubleshooting issues
                            
                            Available 24/7 for instant help!
                        """.trimIndent()
                    ),
                    HelpSection(
                        heading = "How to Use",
                        content = """
                            Simply:
                            
                            1. Tap the AI Assistant icon
                            2. Type or speak your question
                            3. Get instant, context-aware answers
                            4. Follow suggested actions
                            
                            The assistant learns from your usage patterns.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.DASHBOARD, HelpTopic.SETTINGS)
            )

            HelpTopic.ANALYTICS -> HelpContent(
                title = "Analytics & Reporting",
                sections = listOf(
                    HelpSection(
                        heading = "Available Analytics",
                        content = """
                            DriftGuardAI provides:
                            
                            • Drift trend analysis
                            • 7-day drift forecasting
                            • Model comparison
                            • Feature-level insights
                            • Patch effectiveness tracking
                            • Custom time ranges
                            
                            Export reports in CSV or JSON format.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.DASHBOARD, HelpTopic.DRIFT_DETECTION)
            )

            HelpTopic.SETTINGS -> HelpContent(
                title = "Settings",
                sections = listOf(
                    HelpSection(
                        heading = "Customization Options",
                        content = """
                            Configure:
                            
                            • Theme (Light/Dark/Auto)
                            • Notification preferences
                            • Auto-patching settings
                            • Language selection
                            • Backup settings
                            • Data export
                            
                            Changes take effect immediately.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.AUTO_PATCHING, HelpTopic.DASHBOARD)
            )

            HelpTopic.TROUBLESHOOTING -> HelpContent(
                title = "Troubleshooting",
                sections = listOf(
                    HelpSection(
                        heading = "Common Issues",
                        content = """
                            If you encounter problems:
                            
                            1. Model won't upload
                               → Check file format and size
                               → Ensure sufficient storage
                            
                            2. Drift not detected
                               → Verify data format matches model
                               → Check minimum sample size
                            
                            3. Patch failed to apply
                               → Review safety score
                               → Check model compatibility
                            
                            Still stuck? Contact support or use AI Assistant.
                        """.trimIndent()
                    )
                ),
                relatedTopics = listOf(HelpTopic.MODEL_UPLOAD, HelpTopic.AI_ASSISTANT)
            )
        }
    }

    /**
     * Get quick tips
     */
    fun getQuickTips(): List<QuickTip> {
        return listOf(
            QuickTip(
                id = "tip_upload",
                title = "Upload Together",
                message = "Upload your model and data together for instant drift detection!",
                icon = "upload"
            ),
            QuickTip(
                id = "tip_auto",
                title = "Enable Auto-Patching",
                message = "Turn on auto-patching in Settings for hands-free drift management",
                icon = "auto"
            ),
            QuickTip(
                id = "tip_dashboard",
                title = "Monitor Regularly",
                message = "Check the dashboard daily to catch drift early",
                icon = "dashboard"
            ),
            QuickTip(
                id = "tip_ai",
                title = "Ask the AI",
                message = "Not sure about something? The AI Assistant is always ready to help!",
                icon = "ai"
            ),
            QuickTip(
                id = "tip_export",
                title = "Export Reports",
                message = "Export analytics as CSV or JSON for external analysis",
                icon = "export"
            )
        )
    }
}

// Helper extension function for Flow.first()
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect { value ->
        result = value
        return@collect
    }
    return result!!
}

/**
 * Onboarding steps
 */
enum class OnboardingStep {
    WELCOME,
    UPLOAD_MODEL,
    UPLOAD_DATA,
    DRIFT_DETECTION,
    PATCH_SYNTHESIS,
    AUTO_PATCHING,
    DASHBOARD,
    COMPLETE
}

/**
 * Data for each onboarding step
 */
data class OnboardingStepData(
    val step: OnboardingStep,
    val title: String,
    val description: String,
    val icon: String,
    val ctaText: String
)

/**
 * Feature discovery types
 */
enum class FeatureDiscoveryType {
    UPLOAD_MODEL,
    DRIFT_DETECTION,
    PATCH_SYNTHESIS,
    AUTO_PATCHING,
    DASHBOARD,
    AI_ASSISTANT,
    SETTINGS
}

/**
 * Tutorial types
 */
enum class TutorialType {
    UPLOAD,
    DRIFT_DETECTION,
    PATCH_APPLICATION
}

/**
 * Help topics
 */
enum class HelpTopic {
    MODEL_UPLOAD,
    DATA_UPLOAD,
    DRIFT_DETECTION,
    PATCH_SYNTHESIS,
    AUTO_PATCHING,
    DASHBOARD,
    AI_ASSISTANT,
    ANALYTICS,
    SETTINGS,
    TROUBLESHOOTING
}

/**
 * Help content structure
 */
data class HelpContent(
    val title: String,
    val sections: List<HelpSection>,
    val relatedTopics: List<HelpTopic> = emptyList()
)

/**
 * Help section
 */
data class HelpSection(
    val heading: String,
    val content: String
)

/**
 * Quick tip
 */
data class QuickTip(
    val id: String,
    val title: String,
    val message: String,
    val icon: String
)
