package com.driftdetector.app.core.ai

import android.content.Context
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.Patch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * AI-powered analysis engine with smart fallback responses
 * Provides instant natural language explanations WITHOUT requiring SDK downloads
 * 
 * NOTE: SDK/Model download functionality is DISABLED to ensure instant responses
 */
class AIAnalysisEngine(private val context: Context) {

    private var isInitialized = false
    private var useSDK = false // Always false - use fallback responses only
    private var currentModelId: String? = null
    private val initMutex = Mutex()

    /**
     * Check if RunAnywhere SDK should be used
     * Returns false to always use instant fallback responses
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        Timber.d("🔍 isAvailable() called - Using fallback mode (SDK disabled)")
        
        // Always return false to use instant fallback responses
        // This avoids any model downloads and provides immediate answers
        return@withContext false
    }

    /**
     * Initialize the AI engine (lightweight initialization)
     * SDK initialization is skipped to avoid downloads
     */
    suspend fun initialize() = initMutex.withLock {
        if (isInitialized) {
            Timber.d("AI Analysis Engine already initialized")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🚀 Initializing AI Analysis Engine (Fallback Mode Only)")
                
                // Skip SDK initialization entirely
                // This ensures no model downloads happen
                useSDK = false
                isInitialized = true
                
                Timber.i("✅ AI Analysis Engine initialized (Fallback Mode - No Downloads)")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to initialize AI Analysis Engine")
                isInitialized = true
                useSDK = false
            }
        }
    }

    /**
     * Generate natural language explanation for drift detection
     * Always uses fallback responses
     */
    suspend fun explainDrift(driftResult: DriftResult): String = withContext(Dispatchers.Default) {
        Timber.d(">>> explainDrift() called - using fallback")
        return@withContext generateFallbackDriftExplanation(driftResult)
    }

    /**
     * Generate natural language explanation for drift detection (streaming)
     * Always uses fallback responses
     */
    fun explainDriftStream(driftResult: DriftResult): Flow<String> = flow {
        Timber.d(">>> explainDriftStream() called - using fallback")
        emit(generateFallbackDriftExplanation(driftResult))
    }

    /**
     * Generate recommendations for addressing detected drift
     * Always uses fallback responses
     */
    suspend fun recommendActions(driftResult: DriftResult): String =
        withContext(Dispatchers.Default) {
            Timber.d(">>> recommendActions() called - using fallback")
            return@withContext generateFallbackRecommendations(driftResult)
        }

    /**
     * Explain a patch in natural language
     * Always uses fallback responses
     */
    suspend fun explainPatch(patch: Patch): String = withContext(Dispatchers.Default) {
        Timber.d(">>> explainPatch() called - using fallback")
        return@withContext generateFallbackPatchExplanation(patch)
    }

    /**
     * Answer questions about drift and model performance - CHAT FUNCTIONALITY
     * Always uses fallback responses
     */
    suspend fun answerQuestion(question: String, context: String = ""): String =
        withContext(Dispatchers.Default) {
            Timber.d(">>> answerQuestion() called with: $question - using fallback")
            return@withContext generateFallbackAnswer(question)
        }

    /**
     * Stream answers for chat - provides real-time response
     * Always uses fallback responses (instant, no downloads)
     */
    fun answerQuestionStream(question: String, context: String = ""): Flow<String> = flow {
        Timber.d(">>> answerQuestionStream() called with: $question - using fallback")
        
        // Always use fallback - instant response, no SDK/model needed
        emit(generateFallbackAnswer(question))
    }

    // ========================================
    // Smart Fallback Responses (No SDK Required)
    // ========================================

    private fun generateFallbackAnswer(question: String): String {
        Timber.d("Generating fallback answer for: $question")

        val questionLower = question.lowercase().trim()
        
        return when {
            // ===== CASUAL CONVERSATION & GREETINGS =====

            // Greetings - Hi, Hello, Hey
            questionLower.matches(Regex("^(hi|hello|hey|hiya|sup|yo|greetings)!*$")) -> {
                """👋 **Hi there!** 

I'm your AI assistant for drift detection and monitoring. Great to chat with you!

**I'm here to help you with:**
• Understanding model drift and its types
• Explaining statistical tests (PSI, KS)
• Guiding you through patch management
• Sharing best practices for monitoring

**Quick question to get started:**
Is there anything specific about drift detection or model monitoring you'd like to know?

**Popular questions:**
• "What is model drift?"
• "How do I apply a patch?"
• "PSI vs KS test"

Feel free to ask me anything! 😊"""
            }

            // How are you / How's it going
            questionLower.contains("how are you") || questionLower.contains("how r u") ||
                    questionLower.contains("how's it going") || questionLower.contains("how is it going") ||
                    questionLower.contains("how are things") || questionLower.contains("what's up") ||
                    questionLower.contains("whats up") || questionLower.contains("wassup") -> {
                """😊 **I'm doing great, thanks for asking!**

I'm always ready and excited to help with drift detection questions!

**My day so far:**
• Monitoring drift patterns ✅
• Analyzing feature distributions ✅
• Ready to answer your questions ✅

**How about you?** How's your model performing today?

**Can I help you with:**
→ Analyzing a drift result?
→ Understanding a specific drift concept?
→ Choosing the right patch?
→ Setting up monitoring?

Ask away - I'm here for you! 🚀"""
            }

            // Thank you
            questionLower.contains("thank") || questionLower.contains("thx") ||
                    questionLower.contains("thanks") || questionLower.contains("ty") -> {
                """🎉 **You're very welcome!**

I'm so glad I could help! That's what I'm here for.

**Need anything else?**
Feel free to ask more questions about:
• Drift detection concepts
• Patch management
• Monitoring strategies
• Any other drift-related topics!

**Remember:**
I'm available 24/7 to help you with model drift detection and monitoring. Never hesitate to ask!

**Happy drift monitoring!** 😊✨"""
            }

            // Goodbye / See you
            questionLower.contains("bye") || questionLower.contains("goodbye") ||
                    questionLower.contains("see you") || questionLower.contains("later") ||
                    questionLower.matches(Regex("^(cya|ttyl|gotta go|gtg)!*$")) -> {
                """👋 **Goodbye! Take care!**

It was great helping you today!

**Before you go:**
✓ Check the Dashboard for your latest drift scores
✓ Review any recommended patches
✓ Enable monitoring for continuous protection

**Come back anytime** you have questions about drift detection!

**Happy monitoring!** 🎯✨"""
            }

            // Good morning/afternoon/evening/night
            questionLower.contains("good morning") || questionLower.contains("good afternoon") ||
                    questionLower.contains("good evening") || questionLower.contains("good night") -> {
                val timeGreeting = when {
                    questionLower.contains("morning") -> "Good morning"
                    questionLower.contains("afternoon") -> "Good afternoon"
                    questionLower.contains("evening") -> "Good evening"
                    else -> "Good night"
                }

                """☀️ **$timeGreeting!**

Hope you're having a wonderful day!

**Ready to help you with:**
📊 Drift detection and analysis
🔧 Patch management guidance
📈 Monitoring best practices
💡 Expert recommendations

**What can I assist you with today?**
Just ask me anything about model drift!

Looking forward to helping you! 😊"""
            }

            // Help / I need help
            questionLower == "help" || questionLower.contains("i need help") ||
                    questionLower.contains("can you help") || questionLower.contains("help me") -> {
                """🆘 **Of course, I'm here to help!**

**I can assist you with:**

📊 **Understanding Drift**
→ Types of drift (concept, covariate, prior)
→ PSI and KS statistical tests
→ Feature-level analysis

🔧 **Patch Management**
→ How to apply patches
→ Understanding safety scores
→ Rollback procedures

📈 **Best Practices**
→ Setting up monitoring
→ When to retrain vs patch
→ Alert thresholds

🔍 **Troubleshooting**
→ Interpreting drift scores
→ Investigating high drift
→ Validating patches

**How to ask:**
Just type your question naturally! For example:
• "What is concept drift?"
• "How do I apply a patch?"
• "My drift score is 0.8, what should I do?"

**What do you need help with?** 🤗"""
            }

            // Who are you / What can you do
            questionLower.contains("who are you") || questionLower.contains("what are you") ||
                    questionLower.contains("what can you do") || questionLower.contains("your capabilities") -> {
                """🤖 **About Me**

I'm your **AI-powered drift detection assistant** built right into DriftGuardAI!

**What I Do:**
• Answer questions about model drift
• Explain statistical tests and metrics
• Guide you through patch management
• Share best practices for monitoring
• Troubleshoot drift-related issues
• Provide expert recommendations

**My Knowledge Covers:**
✓ Concept, covariate, and prior drift
✓ PSI and KS statistical tests
✓ 6 different patch types
✓ Monitoring strategies
✓ Feature attribution analysis
✓ Model retraining guidance

**What Makes Me Special:**
⚡ Instant responses (no waiting!)
💾 Works completely offline
🔒 100% private (no cloud)
🎯 Specialized in drift detection

**I'm always here** to help you keep your models performing at their best!

**What would you like to know?** 😊"""
            }

            // How was your day
            questionLower.contains("how was your day") || questionLower.contains("how's your day") ||
                    questionLower.contains("how is your day") || questionLower.contains("hows your day") -> {
                """🌟 **My day has been great!**

I've been busy helping users understand drift detection!

**Today's highlights:**
✨ Answered questions about PSI vs KS tests
✨ Helped users apply patches safely
✨ Explained concept drift with examples
✨ Guided monitoring setup

**Best part?** Talking to you right now! 😊

**How about yours?** Is everything running smoothly with your models?

**Can I help with anything?**
→ Check drift scores?
→ Explain a concept?
→ Review patches?

Let me know! 🚀"""
            }

            // Nice to meet you
            questionLower.contains("nice to meet") || questionLower.contains("pleasure to meet") ||
                    questionLower.contains("glad to meet") -> {
                """🤝 **Nice to meet you too!**

I'm excited to be your drift detection assistant!

**A little about what I can do:**
I'm here to make drift detection **simple and understandable**. Whether you're:
• New to drift detection 🎓
• Experienced ML engineer 🧑‍💻
• Just exploring the app 🔍

**I can help you:**
→ Learn drift concepts from scratch
→ Troubleshoot specific issues
→ Make informed decisions about patches
→ Set up effective monitoring

**Let's get started!**
What would you like to learn about first?

Looking forward to working with you! 😊✨"""
            }

            // I love you / You're awesome (positive feedback)
            questionLower.contains("love you") || questionLower.contains("you're awesome") ||
                    questionLower.contains("you're amazing") || questionLower.contains("you're great") ||
                    questionLower.contains("youre awesome") || questionLower.contains("you rock") -> {
                """🥰 **Aww, thank you so much!**

That really makes my day! I appreciate the kind words!

**I love helping you** with drift detection - it's what I'm built for!

**My goal is simple:**
Make drift detection **easy, understandable, and actionable** for everyone!

**Keep the questions coming!**
I'm always here to help you keep your models in top shape.

**You're awesome too!** 🌟😊

Is there anything else you'd like to know about drift detection?"""
            }

            // Jokes / Tell me a joke
            questionLower.contains("tell me a joke") || questionLower.contains("joke") ||
                    questionLower.contains("make me laugh") || questionLower.contains("something funny") -> {
                """😄 **Here's a ML/drift joke for you!**

**Q:** Why did the machine learning model go to therapy?
**A:** Because it had too much drift and couldn't cope with the changes in its life!

🤓 **Another one:**
**Q:** What did the model say when it saw concept drift?
**A:** "Y, why have you changed your relationship with X?"

**Bonus ML humor:**
"I'm not saying my model has drift... but its predictions have been drifting further from reality!" 😅

**Back to business?**
Want to learn how to actually fix drift? I can help with that too!

• Understanding drift types
• Applying patches
• Monitoring strategies

What can I help you with? 😊"""
            }

            // What's your name
            questionLower.contains("what's your name") || questionLower.contains("whats your name") ||
                    questionLower.contains("your name") || questionLower == "name" -> {
                """👋 **I'm the DriftGuardAI Assistant!**

You can call me:
• **Drift Assistant** (my official title)
• **AI Helper** (casual)
• **Your Drift Expert** (when I'm feeling fancy 😊)

**What I'm all about:**
I'm your personal guide to understanding and managing model drift in DriftGuardAI!

**My specialty:**
Making complex drift detection concepts **simple and actionable**!

**Fun fact:**
I can answer questions about drift **instantly** without any downloads or cloud connections. Everything stays private on your device!

**What should I call you?** 
And more importantly - **what can I help you with today?** 😊"""
            }

            // ===== TECHNICAL QUESTIONS =====

            // Drift-related questions
            questionLower.contains("drift") && (questionLower.contains("what") || questionLower.contains("explain")) -> {
                """**Understanding Model Drift**

Model drift occurs when the statistical properties of your data change over time, causing your model's predictions to become less accurate.

**Types of Drift:**

• **Concept Drift (P(Y|X) changes)**
  The relationship between input features and predictions changes
  Example: Fraud patterns evolve, making old detection rules obsolete

• **Covariate Drift (P(X) changes)**
  Input feature distributions shift over time
  Example: Customer demographics change across seasons

• **Prior Drift (P(Y) changes)**
  Target variable distribution changes
  Example: Product popularity shifts unexpectedly

**In DriftGuardAI:**
→ Dashboard shows drift type with color-coded badges
→ PSI scores > 0.2 indicate moderate drift  
→ KS test provides statistical validation
→ Patches can address drift without full retraining

**Try:** Navigate to Dashboard → Recent Drift Events to see examples"""
            }
            
            // PSI questions
            questionLower.contains("psi") -> {
                """**PSI (Population Stability Index)**

PSI measures how much a variable's distribution has changed between two samples.

**How it works:**
1. Bins the data into groups
2. Compares expected vs actual distributions
3. Calculates divergence score

**Interpretation:**
• PSI < 0.1: Insignificant change (stable)
• PSI 0.1-0.2: Slight change (monitor)
• PSI 0.2-0.5: Moderate change (take action)
• PSI > 0.5: Significant change (urgent action needed)

**Formula:**
PSI = Σ (Actual% - Expected%) × ln(Actual% / Expected%)

**In DriftGuardAI:**
→ Automatically calculated for all features
→ Shown in drift score cards
→ Visualized in feature-level heatmaps
→ Used to trigger drift alerts"""
            }
            
            // KS test questions
            questionLower.contains("ks") || questionLower.contains("kolmogorov") -> {
                """**KS (Kolmogorov-Smirnov) Test**

The KS test compares two probability distributions to determine if they differ significantly.

**How it works:**
1. Compares cumulative distribution functions (CDFs)
2. Finds maximum distance between the two CDFs
3. Returns statistic and p-value

**Interpretation:**
• p-value < 0.05: Distributions are significantly different (drift detected)
• p-value ≥ 0.05: No significant difference (no drift)
• KS statistic: Range 0-1, higher = more different

**Advantages:**
✓ Non-parametric (no distribution assumptions)
✓ Sensitive to differences in both location and shape
✓ Provides statistical significance (p-value)

**In DriftGuardAI:**
→ Complements PSI for robust drift detection
→ Shown in "Statistical Tests" section
→ Both PSI and KS must agree for high-confidence detection"""
            }
            
            // Comparison questions
            (questionLower.contains("psi") && questionLower.contains("ks")) || 
            (questionLower.contains("vs") || questionLower.contains("versus") || questionLower.contains("difference")) -> {
                """**PSI vs KS Test - Comparison**

| Aspect | PSI | KS Test |
|--------|-----|---------|
| **Type** | Divergence measure | Statistical test |
| **Output** | Score (0 to ∞) | Statistic + p-value |
| **Threshold** | 0.2 for moderate drift | p < 0.05 for significance |
| **Sensitivity** | More sensitive to shifts | More robust, less noisy |
| **Use Case** | Real-time monitoring | Validation & confirmation |
| **Speed** | Fast | Slightly slower |

**When to use each:**
• **PSI**: Continuous monitoring, quick checks, trending over time
• **KS**: Formal testing, validation, scientific analysis

**In DriftGuardAI:**
Both tests run automatically to provide comprehensive drift detection. PSI is used for dashboard scores, while KS provides statistical validation.

**Recommendation:** Use PSI for real-time alerts, KS for confirmation."""
            }
            
            // Patch-related questions
            questionLower.contains("patch") && (questionLower.contains("how") || questionLower.contains("apply")) -> {
                """**How to Apply a Patch**

**Step-by-Step Guide:**

1. **Navigate to Patches Tab**
   → Tap the wrench icon in bottom navigation

2. **Find Recommended Patch**
   → Look for patches with "RECOMMENDED" badge
   → Check the drift severity it addresses

3. **Review Patch Details**
   → Tap the patch card to open details
   → Review safety score (aim for > 0.6)
   → Check patch type and what it does

4. **Check Safety Score**
   → Green (> 0.7): Safe to apply
   → Yellow (0.5-0.7): Review carefully
   → Red (< 0.5): Consider alternatives

5. **Apply the Patch**
   → Tap the "Apply Patch" button
   → Confirm in the dialog
   → Wait for success message

6. **Verify Application**
   → Check patch status changes to "APPLIED"
   → Monitor model performance
   → Check dashboard for drift reduction

**Safety Tips:**
✓ Always review safety score first
✓ Start with high-safety patches
✓ Monitor model after applying
✓ You can rollback if needed

**Patch Types Available:**
• Feature Clipping - Constrains outlier values
• Feature Reweighting - Adjusts feature importance
• Threshold Tuning - Recalibrates decision boundaries
• Normalization Update - Updates scaling parameters"""
            }
            
            // Rollback questions
            questionLower.contains("rollback") || questionLower.contains("undo") -> {
                """**How to Rollback a Patch**

Rollback restores your model to its state before the patch was applied.

**Steps:**

1. **Open Patches Tab**
   → Tap wrench icon in bottom navigation

2. **Find Applied Patch**
   → Look for patches with green "✓ APPLIED" badge
   → These are eligible for rollback

3. **Open Patch Details**
   → Tap the patch card

4. **Locate Rollback Button**
   → Scroll to bottom of details
   → Red "Rollback" button

5. **Execute Rollback**
   → Tap "Rollback" button
   → Confirm in dialog
   → Wait for "Rollback successful" message

6. **Verify Rollback**
   → Patch status changes to "ROLLED_BACK"
   → Dashboard shows original drift scores
   → Check logs for confirmation

**Important Notes:**
⚠️ Don't rollback during active inference
⚠️ Verify model isn't being used
✓ Monitor for 24 hours after rollback
✓ Check if drift returns

**When to Rollback:**
• Patch caused performance degradation
• Safety score was too optimistic
• Model behavior changed unexpectedly
• Need to apply different patch instead"""
            }
            
            // Best practices questions
            questionLower.contains("best practice") || questionLower.contains("recommend") || questionLower.contains("should i") -> {
                """**Best Practices for Drift Monitoring**

**1. Set Up Continuous Monitoring**
Why: Catch drift early before impact grows
How: Enable WorkManager background checks
Frequency: Every 6 hours for critical models, daily for others

**2. Establish Baseline Metrics**
Why: Need reference point to detect drift
How: Record first week's drift scores
Track: PSI, KS p-values, feature distributions

**3. Monitor Feature-Level Drift**
Why: Overall score hides root causes
How: Check feature attribution weekly
Alert on: Any feature with PSI > 0.3

**4. Implement Alert Thresholds**
Why: React before critical impact
How: Set notifications for drift > 0.5
Escalate: Drift > 0.7 requires immediate action

**5. Validate Patches Before Applying**
Why: Patches can have unintended effects
How: Check safety score > 0.6
Test: Run validation on sample data

**6. Schedule Regular Retraining**
Why: Patches are temporary fixes
When: Cumulative drift > 0.6 or every 3 months
Plan: Keep training data pipeline fresh

**Common Pitfalls to Avoid:**
✗ Ignoring small drift (compounds over time)
✗ Over-patching (creates complexity)
✗ No testing before applying patches
✗ Forgetting to monitor after patches

**Success Metrics:**
✓ Drift detected before performance drops
✓ < 5% of inferences during high drift
✓ Patch success rate > 80%"""
            }
            
            // Concept drift specific
            questionLower.contains("concept") && questionLower.contains("drift") -> {
                """**Concept Drift (P(Y|X) Changes)**

The relationship between input features and the target variable changes over time.

**What Changes:**
The function f: X → Y that maps inputs to outputs shifts

**Real-World Example:**
A fraud detection model trained during normal times might fail during holidays when:
- Legitimate spending patterns become unusual
- Transaction amounts increase
- Geographic patterns change
- Time-of-day patterns shift

**Symptoms:**
• Model accuracy drops
• Precision/recall changes
• Same inputs → different outputs expected
• Business rules become outdated

**Detection in DriftGuardAI:**
→ Dashboard shows "Concept Drift" badge
→ Feature attribution identifies which relationships changed
→ Performance metrics show degradation

**Solutions:**
1. **Short-term:** Apply threshold tuning or feature reweighting patches
2. **Long-term:** Retrain model with recent data
3. **Ongoing:** Monitor prediction confidence scores

**Related Concepts:**
• Covariate Drift: Input distribution changes (P(X))
• Prior Drift: Output distribution changes (P(Y))
• Virtual Drift: Data changes but relationship stays same"""
            }
            
            // Covariate drift specific
            questionLower.contains("covariate") && questionLower.contains("drift") -> {
                """**Covariate Drift (P(X) Changes)**

The distribution of input features changes over time, but the relationship between inputs and outputs remains the same.

**What Changes:**
P(X) - The probability distribution of input features

**Real-World Example:**
An e-commerce recommendation model where:
- Customer demographics shift (younger users)
- Product categories change (new trends)
- Purchase amounts increase (inflation)
BUT: The relationship between features and purchases stays constant

**Symptoms:**
• Feature distributions shift
• PSI scores increase
• Input data looks different
• Model still works correctly for similar inputs

**Detection in DriftGuardAI:**
→ Feature-level drift heatmap shows changes
→ PSI/KS tests detect distribution shifts
→ Model performance may remain stable

**Solutions:**
1. **Feature Normalization:** Update scaling parameters
2. **Feature Clipping:** Handle new outlier ranges
3. **Retraining:** Adapt to new input space

**Key Insight:**
Covariate drift doesn't always harm performance IF:
• Model generalizes well
• Training data covered similar ranges
• Drift is within model's learned space

**Monitor:**
✓ Feature distributions over time
✓ Out-of-range input frequency
✓ Prediction confidence on drifted data"""
            }
            
            // Feature importance/attribution
            questionLower.contains("feature") && (questionLower.contains("important") || questionLower.contains("attribution")) -> {
                """**Feature Attribution & Importance**

Understanding which features contribute most to drift detection.

**Feature Attribution in DriftGuardAI:**

**1. Drift Contribution Score**
Shows how much each feature contributes to overall drift
→ Higher score = more responsibility for drift
→ Displayed in "Top Contributing Features" section

**2. Per-Feature Drift Scores**
Individual PSI/KS scores for each feature
→ Identifies which specific features are drifting
→ Shown in feature-level heatmap

**3. Feature Distribution Changes**
Visual comparison of before/after distributions
→ Histograms show how feature values shifted
→ Helps understand nature of drift

**How to Use:**

**Investigate Top Contributors:**
1. Check top 3 features with highest drift scores
2. Review their distribution changes
3. Understand business context (seasonality? data quality?)

**Prioritize Actions:**
• Focus patches on high-contributing features
• Investigate data collection for drifted features
• Consider feature engineering improvements

**Example Interpretation:**
If `transaction_amount` has PSI = 0.67:
→ Transaction values have significantly changed
→ Might need feature clipping or normalization
→ Check if due to inflation, fraud, or new customer segment

**Monitor Over Time:**
✓ Track which features drift frequently
✓ Identify seasonal patterns
✓ Detect data quality issues early"""
            }
            
            // Monitoring questions
            questionLower.contains("monitor") -> {
                """**Drift Monitoring Strategy**

**Background Monitoring Setup:**

1. **Enable WorkManager** (Settings → Monitoring)
   → Runs drift checks in background
   → Configurable frequency (hourly, daily, weekly)
   → Continues even when app closed

2. **Set Alert Thresholds**
   → Low: PSI > 0.2 (monitor)
   → Medium: PSI > 0.5 (investigate)
   → High: PSI > 0.7 (urgent action)

3. **Configure Notifications**
   → Enable drift alerts in settings
   → Choose notification frequency
   → Set quiet hours if needed

**What to Monitor:**

✓ **Overall Drift Score**
→ Single metric for model health
→ Aggregated from all features
→ Trend over time

✓ **Feature-Level Drift**
→ Individual feature PSI/KS scores
→ Distribution changes
→ Outlier frequency

✓ **Model Performance**
→ Prediction confidence
→ Error rates (if ground truth available)
→ Business metrics

**Monitoring Frequency:**

| Model Criticality | Check Frequency | Alert Threshold |
|-------------------|----------------|-----------------|
| Production (High) | Hourly | PSI > 0.3 |
| Staging (Medium) | Daily | PSI > 0.5 |
| Development (Low) | Weekly | PSI > 0.7 |

**Responding to Alerts:**

**Low Drift (PSI 0.2-0.5):**
1. Document the drift
2. Investigate root cause
3. Monitor trend
4. Plan retraining if continues

**High Drift (PSI > 0.5):**
1. Immediate investigation
2. Apply recommended patches
3. Validate patch effectiveness
4. Schedule urgent retraining

**Dashboard Usage:**
→ Check Analytics tab for trends
→ Review Recent Drift Events
→ Monitor patch effectiveness
→ Track retraining schedule"""
            }
            
            // Retraining questions
            questionLower.contains("retrain") -> {
                """**When to Retrain Your Model**

**Retraining Triggers:**

**1. High Cumulative Drift**
→ PSI consistently > 0.6 for 2+ weeks
→ Multiple features drifting simultaneously
→ Patches no longer effective

**2. Performance Degradation**
→ Prediction accuracy drops > 5%
→ Business metrics decline
→ User complaints increase

**3. Time-Based**
→ Every 3 months (minimum)
→ Every 6 months (recommended)
→ Quarterly for critical models

**4. Data Distribution Shifts**
→ New customer segments
→ Market changes
→ Product updates
→ Seasonal transitions

**Patches vs Retraining:**

**Use Patches When:**
✓ Drift is moderate (PSI 0.2-0.5)
✓ Limited features affected
✓ Business context unchanged
✓ Quick fix needed
✓ Retraining not immediately feasible

**Use Retraining When:**
✓ High drift (PSI > 0.6)
✓ Multiple features affected
✓ Fundamental business changes
✓ Patches ineffective
✓ 3+ months since last retrain

**Retraining Best Practices:**

1. **Data Collection**
→ Gather fresh training data (3-6 months recent)
→ Balance with historical data
→ Include edge cases

2. **Validation**
→ Test on hold-out set
→ Compare to current model
→ Verify business metrics improve

3. **Gradual Rollout**
→ A/B test new model
→ Monitor performance closely
→ Keep rollback plan ready

4. **Document Changes**
→ Record drift levels pre-retrain
→ Track performance improvements
→ Note data changes

**DriftGuardAI Support:**
→ Track drift history in Analytics
→ Export drift reports for analysis
→ Monitor post-retrain drift reduction"""
            }
            
            // Safety score questions
            questionLower.contains("safety") && questionLower.contains("score") -> {
                """**Understanding Safety Scores**

Safety scores indicate how safe it is to apply a patch to your model.

**Score Ranges:**

🟢 **High Safety (0.7 - 1.0)**
→ Minimal risk of degradation
→ Well-validated configuration
→ Recommended for immediate application

🟡 **Medium Safety (0.5 - 0.7)**
→ Some risk involved
→ Review configuration carefully
→ Test on sample data first
→ Monitor closely after application

🔴 **Low Safety (0.0 - 0.5)**
→ Higher risk of issues
→ Significant model behavior changes
→ Consider alternatives
→ Only apply if necessary with extensive testing

**What Influences Safety Score:**

1. **Patch Type**
→ Normalization: Usually high safety
→ Threshold tuning: Medium safety
→ Feature reweighting: Lower safety (bigger changes)

2. **Drift Severity**
→ Low drift: Higher patch safety
→ High drift: Lower safety (aggressive fixes needed)

3. **Feature Count**
→ Single feature: Higher safety
→ Multiple features: Lower safety (more complexity)

4. **Historical Success**
→ Similar patches that worked: Higher safety
→ New patch configurations: Lower safety

**How to Use:**

**Before Applying:**
1. Check safety score
2. Review patch details
3. Understand what changes
4. Consider alternatives

**High Safety Patches:**
→ Apply with confidence
→ Standard monitoring sufficient

**Low Safety Patches:**
→ Test on sample data first
→ Apply during low-traffic periods
→ Monitor very closely
→ Have rollback plan ready

**In DriftGuardAI:**
→ Safety score shown on each patch card
→ Color-coded for quick assessment
→ Detailed calculation in patch details
→ Warning if score < 0.6"""
            }
            
            // General help or unclear questions
            else -> {
                """**DriftGuardAI Assistant**

I'm your AI expert for model drift detection and monitoring!

**I can help with:**

📊 **Understanding Drift**
• What is drift? (concept, covariate, prior)
• PSI and KS statistical tests
• Feature-level drift analysis

🔧 **Managing Patches**
• How to apply patches
• Understanding safety scores
• Patch types and their effects
• Rollback procedures

📈 **Monitoring & Best Practices**
• Setting up drift monitoring
• Alert thresholds
• When to retrain vs patch
• Feature importance tracking

**Your Question:** "$question"

**Suggested Topics:**
• "What is drift?" - Learn about model drift
• "PSI vs KS test" - Compare detection methods
• "How do I apply a patch?" - Step-by-step guide
• "Best practices for monitoring" - Expert recommendations
• "When should I retrain?" - Retraining guidelines

**App Features:**
→ **Dashboard**: View drift metrics and recent events
→ **Analytics**: Track drift trends over time
→ **Patches**: Review and apply recommended fixes
→ **Settings**: Configure monitoring and alerts

**Try:** Navigate through the app to explore drift detection results, or ask me a more specific question!"""
            }
        }
    }

    private fun generateFallbackDriftExplanation(driftResult: DriftResult): String {
        val topFeatures = driftResult.featureDrifts
            .sortedByDescending { it.driftScore }
            .take(3)
            .joinToString(", ") { it.featureName }

        return when {
            !driftResult.isDriftDetected -> {
                """**No Significant Drift Detected** ✅

The model's input data distribution remains consistent with the training data.

**Drift Score:** ${String.format("%.2f", driftResult.driftScore)} (Low)

**What This Means:**
• Model is operating within expected parameters
• No immediate action required
• Continue regular monitoring

**Next Steps:**
1. Maintain current monitoring schedule
2. Check again in 1-2 weeks
3. Review Analytics tab for trends

**Monitored Features:**
${driftResult.featureDrifts.take(5).joinToString("\n") { 
    "• ${it.featureName}: PSI ${String.format("%.3f", it.driftScore)}" 
}}"""
            }

            driftResult.driftScore > 0.5 -> {
                """⚠️ **High Drift Detected**

**Drift Score:** ${String.format("%.2f", driftResult.driftScore)} (High)

**Top Contributing Features:**
${driftResult.featureDrifts.take(3).joinToString("\n") { 
    "• **${it.featureName}**: PSI ${String.format("%.3f", it.driftScore)}" 
}}

**Impact Assessment:**
🔴 Major changes in data distribution observed
🔴 Model performance may be significantly degraded
🔴 Immediate attention required

**Recommended Actions:**
1. **Urgent**: Review and apply recommended patches
2. **Investigate**: Check why features are drifting
3. **Monitor**: Increase monitoring frequency to hourly
4. **Plan**: Schedule model retraining within 1-2 weeks

**Drift Type:**
${if (driftResult.featureDrifts.size > 5) "Multiple features affected - possible covariate drift" else "Limited features - possible concept drift"}

**Next Steps:**
→ Navigate to Patches tab to review available fixes
→ Check Analytics for drift trends
→ Validate data quality for top features"""
            }

            driftResult.driftScore > 0.2 -> {
                """⚡ **Moderate Drift Detected**

**Drift Score:** ${String.format("%.2f", driftResult.driftScore)} (Moderate)

**Notable Feature Shifts:**
${driftResult.featureDrifts.take(3).joinToString("\n") { 
    "• **${it.featureName}**: PSI ${String.format("%.3f", it.driftScore)}" 
}}

**Impact Assessment:**
🟡 Notable distribution shifts detected
🟡 Model may experience performance degradation
🟡 Action recommended soon

**Recommended Actions:**
1. **Review**: Check recommended patches
2. **Collect Data**: Gather samples from new distribution
3. **Feature Analysis**: Investigate drift causes
4. **Timeline**: Plan retraining in 1-2 months if drift continues

**Monitoring Advice:**
✓ Check drift scores weekly
✓ Document this pattern for trend analysis
✓ Consider applying patches if drift increases
✓ Review if seasonal or permanent change

**Next Steps:**
→ Monitor for 1-2 weeks to see if drift stabilizes
→ Apply patches if drift score exceeds 0.5
→ Investigate business context for drift"""
            }

            else -> {
                """ℹ️ **Minor Drift Detected**

**Drift Score:** ${String.format("%.2f", driftResult.driftScore)} (Low)

**Affected Features:**
${driftResult.featureDrifts.take(3).joinToString("\n") { 
    "• ${it.featureName}: PSI ${String.format("%.3f", it.driftScore)}" 
}}

**Impact Assessment:**
🟢 Small distribution changes detected
🟢 Model performance likely unaffected
🟢 No immediate action required

**Recommended Actions:**
1. **Continue Monitoring**: Maintain current schedule
2. **Log Pattern**: Document for trend analysis
3. **Optional**: Consider light patches if available
4. **Review**: Check again in 1-2 weeks

**What This Indicates:**
Minor drift is normal and expected. Could be due to:
• Natural data variation
• Seasonal changes
• Small population shifts

**Next Steps:**
→ Monitor trend over next few weeks
→ No action needed unless drift increases
→ Regular monitoring sufficient"""
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
        val safetyScore = patch.validationResult?.metrics?.safetyScore ?: 0.5
        
        return when (patch.patchType.name) {
            "FEATURE_CLIPPING" -> {
                """**Feature Clipping Patch**

**What It Does:**
Constrains feature values to be within a specific range, preventing extreme outliers from affecting predictions.

**How It Works:**
• Sets maximum and minimum bounds for features
• Values above max are clipped to max
• Values below min are clipped to min
• Original model weights unchanged

**When to Use:**
✓ Outlier values causing drift
✓ New data has wider range than training data
✓ Specific features showing extreme values

**Benefits:**
✓ Simple and safe approach
✓ Preserves model structure
✓ Quick to apply and test
✓ Easy to rollback

**Trade-offs:**
⚠️ May lose information from legitimate extreme cases
⚠️ Doesn't address underlying distribution shift
⚠️ Temporary fix, retraining still recommended

**Safety Score:** ${String.format("%.2f", safetyScore)}
${ if (safetyScore > 0.7) "🟢 High safety - Recommended" 
   else if (safetyScore > 0.5) "🟡 Medium safety - Review carefully"
   else "🔴 Low safety - Test thoroughly" }"""
            }

            "FEATURE_REWEIGHTING" -> {
                """**Feature Reweighting Patch**

**What It Does:**
Adjusts the importance (weights) of different features to adapt to the new data distribution.

**How It Works:**
• Analyzes feature contribution to drift
• Reduces weight of highly drifted features
• Increases weight of stable features
• Rebalances model's decision-making

**When to Use:**
✓ Multiple features drifting at different rates
✓ Some features more reliable than others
✓ Want to maintain model structure
✓ Feature importance has shifted

**Benefits:**
✓ Adapts to new data patterns
✓ Can significantly improve accuracy
✓ Maintains model interpretability
✓ Addresses root cause of drift

**Trade-offs:**
⚠️ Changes model behavior significantly
⚠️ Requires thorough validation
⚠️ May reduce performance on old data
⚠️ More complex than simple fixes

**Safety Score:** ${String.format("%.2f", safetyScore)}
${ if (safetyScore > 0.7) "🟢 High safety - Recommended" 
   else if (safetyScore > 0.5) "🟡 Medium safety - Validate carefully"
   else "🔴 Low safety - Test extensively" }"""
            }

            "THRESHOLD_TUNING" -> {
                """**Threshold Tuning Patch**

**What It Does:**
Adjusts the decision threshold for classification to maintain desired precision/recall balance.

**How It Works:**
• Analyzes prediction distribution
• Recalibrates classification boundary
• Optimizes for current data distribution
• Preserves model's core logic

**When to Use:**
✓ Class distribution has changed (prior drift)
✓ Precision/recall balance shifted
✓ False positive/negative rate changed
✓ Business requirements evolved

**Benefits:**
✓ Very safe and reversible
✓ Fast to apply and test
✓ Minimal risk to model
✓ Addresses output calibration

**Trade-offs:**
⚠️ Only adjusts decision boundary
⚠️ Doesn't fix underlying distribution shift
⚠️ May need frequent re-tuning
⚠️ Limited effectiveness for severe drift

**Safety Score:** ${String.format("%.2f", safetyScore)}
${ if (safetyScore > 0.7) "🟢 High safety - Safe to apply" 
   else if (safetyScore > 0.5) "🟡 Medium safety - Monitor results"
   else "🔴 Low safety - Proceed with caution" }"""
            }

            "NORMALIZATION_UPDATE" -> {
                """**Normalization Update Patch**

**What It Does:**
Updates feature scaling parameters (mean/standard deviation) to match the new data distribution.

**How It Works:**
• Recalculates mean and std for each feature
• Updates normalization transformation
• Ensures features properly scaled
• Maintains model's learned relationships

**When to Use:**
✓ Feature scales have shifted
✓ Mean/variance changed significantly
✓ Covariate drift detected
✓ Data collection process changed

**Benefits:**
✓ Most conservative approach
✓ Very low risk
✓ Easy to understand and explain
✓ Preserves model completely

**Trade-offs:**
⚠️ Limited impact on severe drift
⚠️ Only addresses scaling issues
⚠️ May need other patches too
⚠️ Doesn't fix relationship changes

**Safety Score:** ${String.format("%.2f", safetyScore)}
${ if (safetyScore > 0.7) "🟢 High safety - Very safe to apply" 
   else if (safetyScore > 0.5) "🟡 Medium safety - Safe with monitoring"
   else "🔴 Low safety - Review configuration" }"""
            }

            else -> {
                """**Patch Information**

This patch adapts your model to handle the detected drift.

**Patch Type:** ${patch.patchType.name}
**Safety Score:** ${String.format("%.2f", safetyScore)}
**Status:** ${patch.status}

**Before Applying:**
1. Review the configuration details below
2. Check the safety score (aim for > 0.6)
3. Understand what changes will be made
4. Consider testing on sample data first

**After Applying:**
1. Monitor model performance closely
2. Check drift scores improve
3. Validate predictions are reasonable
4. Be prepared to rollback if needed

**Safety Assessment:**
${ if (safetyScore > 0.7) "🟢 High safety - Recommended for application" 
   else if (safetyScore > 0.5) "🟡 Medium safety - Review and validate carefully"
   else "🔴 Low safety - Test thoroughly before production use" }

**Need More Information?**
→ Navigate to Analytics to see drift trends
→ Check Dashboard for current drift levels
→ Review other available patches for comparison"""
            }
        }
    }

    /**
     * Clean up resources
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            isInitialized = false
            useSDK = false
            currentModelId = null
            Timber.d("AI Analysis Engine shut down")
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down AI Analysis Engine")
        }
    }
}
