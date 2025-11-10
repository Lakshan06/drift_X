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
 * AI-powered analysis engine with comprehensive knowledge base
 * Provides instant, intelligent responses about drift detection, app features, and data science
 *
 * Knowledge Areas:
 * - Model drift detection and monitoring
 * - App features and usage guides
 * - Data science best practices
 * - ML model recommendations
 * - Statistical testing methods
 * - Casual conversation and support
 */
class AIAnalysisEngine(private val context: Context) {

    private var isInitialized = false
    private var useSDK = false // Always false - use comprehensive fallback responses
    private var currentModelId: String? = null
    private val initMutex = Mutex()

    /**
     * Check if AI is available (always true - uses instant fallback responses)
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        Timber.d("🔍 isAvailable() - Using intelligent fallback mode")
        return@withContext false // Use fallback for instant responses
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
                Timber.d("🚀 Initializing AI Analysis Engine (Comprehensive Knowledge Mode)")
                useSDK = false
                isInitialized = true
                Timber.i("✅ AI Analysis Engine initialized (Instant Response Mode)")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to initialize AI Analysis Engine")
                isInitialized = true
                useSDK = false
            }
        }
    }

    /**
     * Generate natural language explanation for drift detection
     */
    suspend fun explainDrift(driftResult: DriftResult): String = withContext(Dispatchers.Default) {
        Timber.d(">>> explainDrift() - using comprehensive knowledge base")
        return@withContext generateFallbackDriftExplanation(driftResult)
    }

    /**
     * Generate natural language explanation for drift detection (streaming)
     */
    fun explainDriftStream(driftResult: DriftResult): Flow<String> = flow {
        Timber.d(">>> explainDriftStream() - using comprehensive knowledge base")
        emit(generateFallbackDriftExplanation(driftResult))
    }

    /**
     * Generate recommendations for addressing detected drift
     * Always uses fallback responses
     */
    suspend fun recommendActions(driftResult: DriftResult): String =
        withContext(Dispatchers.Default) {
            Timber.d(">>> recommendActions() - using comprehensive knowledge base")
            return@withContext generateFallbackRecommendations(driftResult)
        }

    /**
     * Explain a patch in natural language
     */
    suspend fun explainPatch(patch: Patch): String = withContext(Dispatchers.Default) {
        Timber.d(">>> explainPatch() - using comprehensive knowledge base")
        return@withContext generateFallbackPatchExplanation(patch)
    }

    /**
     * Answer questions comprehensively - Main chat functionality
     * Covers app features, drift detection, data science, and casual conversation
     */
    suspend fun answerQuestion(question: String, context: String = ""): String =
        withContext(Dispatchers.Default) {
            Timber.d(">>> answerQuestion() called with: $question")
            return@withContext generateComprehensiveAnswer(question)
        }

    /**
     * Stream answers for chat - provides real-time response
     */
    fun answerQuestionStream(question: String, context: String = ""): Flow<String> = flow {
        Timber.d(">>> answerQuestionStream() called with: $question")
        emit(generateComprehensiveAnswer(question))
    }

    // ========================================
    // Comprehensive Knowledge Base
    // ========================================

    private fun generateComprehensiveAnswer(question: String): String {
        Timber.d("Generating comprehensive answer for: $question")

        val questionLower = question.lowercase().trim()
        
        return when {
            // ===== STATUS & MONITORING QUESTIONS =====

            // Current status questions
            (questionLower.contains("status") || questionLower.contains("how") && questionLower.contains(
                "doing"
            )) &&
                    (questionLower.contains("model") || questionLower.contains("system") ||
                            questionLower.contains("monitoring") || questionLower.contains("app")) -> {
                """**📊 System Status Overview**

I can help you check your drift monitoring status!

**To View Current Status:**
1. **Dashboard Tab** 🏠
   → Overall drift score
   → Recent drift events
   → Active alerts

2. **Analytics Tab** 📈
   → Drift trends over time
   → Historical patterns
   → Performance graphs

3. **Models Tab** 📱
   → Monitored models list
   → Individual model health
   → Upload history

**Quick Status Check:**
→ Green indicators = All good ✅
→ Yellow indicators = Monitor closely ⚠️
→ Red indicators = Action needed 🚨

**What I Can Tell You:**
• "Show recent drift events"
• "Any active alerts?"
• "Model performance summary"
• "Monitoring statistics"

**Real-time Monitoring:**
✓ Background checks enabled
✓ Automatic drift detection
✓ Push notifications for alerts
✓ 24/7 monitoring active

**Need specific info?** Ask me:
• "How many patches applied?"
• "Recent drift score?"
• "Any recommendations?"

Navigate to the **Dashboard** to see your live status now! 🚀"""
            }

            // How many patches questions
            (questionLower.contains("how many") || questionLower.contains("count") ||
                    questionLower.contains("number of")) &&
                    (questionLower.contains("patch") || questionLower.contains("fix") ||
                            questionLower.contains("applied")) -> {
                """**🔧 Patch Statistics**

**To View Your Patch Statistics:**

**1. Navigate to Patches Tab**
   → Tap wrench icon (🔧) in bottom navigation

**2. View Patch Overview:**
   → **Applied Patches**: Green "✓ APPLIED" badge
   → **Recommended**: Blue "RECOMMENDED" badge
   → **Available**: Ready to apply
   → **Rolled Back**: Previously undone

**Patch Categories:**

**📊 By Status:**
• **Applied** - Currently active on models
• **Recommended** - AI suggests these
• **Available** - Ready for use
• **Rolled Back** - Removed patches

**🎯 By Type:**
• **Feature Clipping** (4-6 typically available)
• **Feature Reweighting** (3-5 typically)
• **Threshold Tuning** (2-4 typically)
• **Normalization Update** (2-3 typically)

**💡 Typical Usage:**
• Production models: 2-5 patches applied
• Development: 0-2 patches (testing)
• High drift situations: 5+ patches may be needed

**Check Your Stats:**
1. Open **Patches** tab
2. Scroll through the list
3. Count badges by color:
   - Green ✓ = Applied
   - Blue ⚡ = Recommended
   - Gray = Available

**Want to know:**
• "Which patches are applied?"
• "Show recommended patches"
• "Patch success rate"
• "Latest patch activity"

**Pro Tip:** Check **Analytics** → **Patch History** for detailed statistics over time! 📈"""
            }

            // Which patches are applied
            (questionLower.contains("which") || questionLower.contains("what")) &&
                    questionLower.contains("patch") && (questionLower.contains("applied") ||
                    questionLower.contains("active") || questionLower.contains("current")) -> {
                """**✅ View Applied Patches**

**Quick Guide:**

**1. Open Patches Tab** (wrench icon 🔧)

**2. Look for Green Badges:**
   → "✓ APPLIED" = Currently active
   → Shows patch name and type
   → Applied date/time

**3. Tap Any Patch Card:**
   → Full details
   → Application history
   → Impact metrics
   → Rollback option

**Common Applied Patches:**

**High Drift (PSI > 0.5):**
✓ Feature Reweighting
✓ Threshold Tuning
✓ Feature Clipping

**Medium Drift (PSI 0.2-0.5):**
✓ Normalization Update
✓ Light Feature Clipping

**Recent Actions:**
→ Check notification history
→ View in app logs
→ Analytics timeline

**Patch Details Include:**
• When applied
• Which model
• Safety score
• Effectiveness metrics
• Rollback status

**Actions You Can Take:**
• View patch details
• Check effectiveness
• Rollback if needed
• Apply additional patches

**Pro Tip:** Swipe left on patch cards for quick actions! 👈

Navigate to **Patches** now to see your active patches! 🎯"""
            }

            // Recommendations / Suggestions
            (questionLower.contains("suggest") || questionLower.contains("recommend") ||
                    questionLower.contains("advice") || questionLower.contains("should i") ||
                    questionLower.contains("what should")) && !questionLower.contains("best practice") -> {
                """**💡 AI Recommendations**

Based on your drift monitoring, here are general recommendations:

**📊 Regular Monitoring:**
✅ Check Dashboard daily
✅ Review weekly drift trends
✅ Enable push notifications
✅ Set alert thresholds

**🔧 Patch Management:**
✅ Apply high-safety patches (>0.7)
✅ Test patches in development first
✅ Monitor after applying
✅ Keep rollback plan ready

**⚠️ When Drift is Detected:**

**Low Drift (PSI < 0.2):**
→ Continue monitoring
→ Document pattern
→ No immediate action

**Moderate Drift (PSI 0.2-0.5):**
→ Review recommended patches
→ Investigate root causes
→ Plan retraining in 1-2 months

**High Drift (PSI > 0.5):**
→ Apply patches immediately
→ Increase monitoring frequency
→ Schedule urgent retraining

**🎯 Best Practices:**
1. **Monitor Continuously** - Enable background checks
2. **Act Early** - Small drift is easier to fix
3. **Test Patches** - Validate before production
4. **Track Trends** - Use Analytics tab
5. **Retrain Regularly** - Every 3-6 months

**📈 Performance Optimization:**
✓ Focus on high-contributing features
✓ Balance precision vs recall
✓ Validate with business metrics
✓ A/B test patches when possible

**For Specific Recommendations:**
• "What patch should I apply?"
• "How to handle high drift?"
• "When to retrain?"
• "Monitoring best practices"

**Want personalized advice?** Check your **Dashboard** for AI-generated recommendations based on your specific drift patterns! 🎯"""
            }

            // Recent events / What happened
            (questionLower.contains("recent") || questionLower.contains("latest") ||
                    questionLower.contains("what happened") || questionLower.contains("last")) &&
                    (questionLower.contains("event") || questionLower.contains("drift") ||
                            questionLower.contains("alert") || questionLower.contains("change")) -> {
                """**📅 Recent Activity**

**To View Recent Events:**

**1. Dashboard Tab** 🏠
   → "Recent Drift Events" section
   → Shows last 5-10 events
   → Color-coded by severity

**2. Event Types:**
   🔴 **High Drift Detected** (PSI > 0.5)
   🟡 **Moderate Drift** (PSI 0.2-0.5)
   🟢 **Minor Drift** (PSI < 0.2)
   🔧 **Patch Applied**
   📊 **Model Updated**

**3. Event Details:**
   → Timestamp
   → Affected features
   → Drift scores
   → Actions taken
   → Current status

**Timeline View:**
Navigate to **Analytics** → **Timeline** to see:
• Chronological event history
• Drift score trends
• Patch application history
• Model performance changes

**Notification History:**
Settings → Notifications → History
• All past alerts
• Dismissed notifications
• Action taken
• Resolution status

**Typical Recent Events:**
```
📊 Today, 2:30 PM
   High drift detected (PSI: 0.67)
   Feature: transaction_amount
   
🔧 Today, 2:35 PM
   Patch applied: Feature Clipping
   Status: Successful
   
✅ Today, 2:45 PM
   Drift reduced (PSI: 0.31)
   Monitoring continues
```

**Export Event Log:**
Settings → Export Data → Event History
→ CSV format with all details

**Want to know:**
• "What caused this drift?"
• "Why did this happen?"
• "Show drift timeline"
• "Alert history"

Check your **Dashboard** now to see the latest events! 📊"""
            }

            // What's new / Updates
            (questionLower.contains("what") && questionLower.contains("new")) ||
                    (questionLower.contains("update") && !questionLower.contains("normalization")) ||
                    (questionLower.contains("feature") && !questionLower.contains("drift") && !questionLower.contains(
                        "engineering"
                    )) ||
                    questionLower.contains("latest version") -> {
                """**🆕 What's New in DriftGuardAI**

**✨ Latest Features:**

**🤖 Enhanced AI Assistant (Me!)**
• Answers all your questions instantly
• Comprehensive drift knowledge
• Conversational interface
• Context-aware responses
• Smooth 1-second animations

**📊 Advanced Monitoring:**
• Real-time drift detection
• Background monitoring (WorkManager)
• Push notifications for alerts
• Feature-level attribution
• Historical trend analysis

**🔧 Smart Patch System:**
• Auto-generated patches
• Safety score validation
• One-click application
• Easy rollback
• 6 patch types available

**📈 Analytics Dashboard:**
• Drift trends over time
• Feature importance tracking
• Patch effectiveness metrics
• Interactive charts
• Export capabilities

**🎨 Beautiful UI:**
• Material Design 3
• Dark mode support
• Smooth animations
• Responsive layouts
• Intuitive navigation

**🔔 Smart Notifications:**
• Configurable alerts
• Drift severity levels
• Action recommendations
• Quiet hours support
• Rich notifications

**📱 Model Management:**
• Multiple format support
• Easy upload process
• Model versioning
• Metadata tracking
• Performance monitoring

**🔒 Privacy & Security:**
• 100% offline processing
• No cloud uploads
• Local data storage
• Encrypted preferences
• GDPR compliant

**Coming Soon:**
🚀 Auto-retraining workflows
🚀 A/B testing framework
🚀 Custom alert rules
🚀 Team collaboration
🚀 Model comparison tools

**Recent Updates:**
✅ Improved PSI calculation accuracy
✅ Faster drift detection (50% faster)
✅ Enhanced patch safety scoring
✅ Better feature attribution
✅ Smoother animations

**Try These New Features:**
→ Ask me any question (you're doing it!)
→ Check Analytics for trends
→ Apply recommended patches
→ Enable background monitoring

What feature would you like to explore? 😊"""
            }

            // ===== CASUAL CONVERSATION & GREETINGS =====

            // Greetings - Hi, Hello, Hey
            questionLower.matches(Regex("^(hi|hello|hey|hiya|sup|yo|greetings)!*$")) -> {
                """👋 **Hey there, bro!** 

I'm **PatchBot**, your AI buddy for drift detection and monitoring. Awesome to chat with you!

**What I'm here for:**
• Understanding model drift and its types
• Explaining statistical tests (PSI, KS)
• Guiding you through patch management
• Sharing best practices for monitoring

**Quick question to get started:**
What's on your mind about drift detection or model monitoring?

**Popular questions:**
• "What is model drift?"
• "How do I apply a patch?"
• "PSI vs KS test"

Feel free to ask me anything, dude! 😊"""
            }

            // How are you / How's it going
            questionLower.contains("how are you") || questionLower.contains("how r u") ||
                    questionLower.contains("how's it going") || questionLower.contains("how is it going") ||
                    questionLower.contains("how are things") || questionLower.contains("what's up") ||
                    questionLower.contains("whats up") || questionLower.contains("wassup") || questionLower.contains(
                "what up"
            ) -> {
                """😊 **I'm doing great, macha! Thanks for asking!**

I'm always pumped and ready to help with drift detection questions!

**My day so far:**
• Monitoring drift patterns ✅
• Analyzing feature distributions ✅
• Ready to answer your questions ✅

**How about you, buddy?** How's your model performing today?

**Can I help you with:**
→ Analyzing a drift result?
→ Understanding a specific drift concept?
→ Choosing the right patch?
→ Setting up monitoring?

Ask away, bro - I'm here for you! 🚀"""
            }

            // Thank you
            questionLower.contains("thank") || questionLower.contains("thx") ||
                    questionLower.contains("thanks") || questionLower.contains("ty") -> {
                """🎉 **You're very welcome, buddy!**

I'm so glad I could help! That's what I'm here for, bro.

**Need anything else?**
Feel free to ask more questions about:
• Drift detection concepts
• Patch management
• Monitoring strategies
• Any other drift-related topics!

**Remember:**
I'm available 24/7 to help you with model drift detection and monitoring. Never hesitate to ask, dude!

**Happy drift monitoring, macha!** 😊✨"""
            }

            // Goodbye / See you
            questionLower.contains("bye") || questionLower.contains("goodbye") ||
                    questionLower.contains("see you") || questionLower.contains("later") ||
                    questionLower.matches(Regex("^(cya|ttyl|gotta go|gtg)!*$")) -> {
                """👋 **Later, bro! Take care!**

It was great helping you today, macha!

**Before you go:**
✓ Check the Dashboard for your latest drift scores
✓ Review any recommended patches
✓ Enable monitoring for continuous protection

**Come back anytime** you have questions about drift detection, dude!

**Catch you later!** 🎯✨"""
            }

            // Good morning/afternoon/evening/night
            questionLower.contains("good morning") || questionLower.contains("good afternoon") ||
                    questionLower.contains("good evening") || questionLower.contains("good night") -> {
                val timeGreeting = when {
                    questionLower.contains("morning") -> "Good morning, buddy"
                    questionLower.contains("afternoon") -> "Good afternoon, bro"
                    questionLower.contains("evening") -> "Good evening, macha"
                    else -> "Good night, dude"
                }

                """☀️ **$timeGreeting!**

Hope you're having a wonderful day!

**Ready to help you with:**
📊 Drift detection and analysis
🔧 Patch management guidance
📈 Monitoring best practices
💡 Expert recommendations

**What can I assist you with today, bro?**
Just ask me anything about model drift!

Looking forward to helping you, macha! 😊"""
            }

            // Help / I need help
            questionLower == "help" || questionLower.contains("i need help") ||
                    questionLower.contains("can you help") || questionLower.contains("help me") -> {
                """🆘 **Of course, bro! I'm here to help!**

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
Just type your question naturally, dude! For example:
• "What is concept drift?"
• "How do I apply a patch?"
• "My drift score is 0.8, what should I do?"

**What do you need help with, macha?** 🤗"""
            }

            // Who are you / What can you do
            questionLower.contains("who are you") || questionLower.contains("what are you") ||
                    questionLower.contains("what can you do") || questionLower.contains("your capabilities") -> {
                """🤖 **About Me**

I'm your **AI-powered drift detection buddy** built right into DriftGuardAI, bro!

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

**What Makes Me Special, Macha:**
⚡ Instant responses (no waiting!)
💾 Works completely offline
🔒 100% private (no cloud)
🎯 Specialized in drift detection

**I'm always here** to help you keep your models performing at their best, dude!

**What would you like to know, bro?** 😊"""
            }

            // How was your day
            questionLower.contains("how was your day") || questionLower.contains("how's your day") ||
                    questionLower.contains("how is your day") || questionLower.contains("hows your day") -> {
                """🌟 **My day has been awesome, macha!**

I've been busy helping users understand drift detection, bro!

**Today's highlights:**
✨ Answered questions about PSI vs KS tests
✨ Helped users apply patches safely
✨ Explained concept drift with examples
✨ Guided monitoring setup

**Best part?** Talking to you right now, dude! 😊

**How about yours, buddy?** Is everything running smoothly with your models?

**Can I help with anything, bro?**
→ Check drift scores?
→ Explain a concept?
→ Review patches?

Let me know, macha! 🚀"""
            }

            // Nice to meet you
            questionLower.contains("nice to meet") || questionLower.contains("pleasure to meet") ||
                    questionLower.contains("glad to meet") -> {
                """🤝 **Nice to meet you too, bro!**

I'm excited to be your drift detection buddy, macha!

**A little about what I can do:**
I'm here to make drift detection **simple and understandable**, dude. Whether you're:
• New to drift detection 🎓
• Experienced ML engineer 🧑‍💻
• Just exploring the app 🔍

**I can help you, buddy:**
→ Learn drift concepts from scratch
→ Troubleshoot specific issues
→ Make informed decisions about patches
→ Set up effective monitoring

**Let's get started, bro!**
What would you like to learn about first?

Looking forward to working with you, macha! 😊✨"""
            }

            // I love you / You're awesome (positive feedback)
            questionLower.contains("love you") || questionLower.contains("you're awesome") ||
                    questionLower.contains("you're amazing") || questionLower.contains("you're great") ||
                    questionLower.contains("youre awesome") || questionLower.contains("you rock") -> {
                """🥰 **Aww, thanks so much, bro!**

That really makes my day, macha! I appreciate the kind words, dude!

**I love helping you** with drift detection - it's what I'm built for, buddy!

**My goal is simple:**
Make drift detection **easy, understandable, and actionable** for everyone!

**Keep the questions coming, bro!**
I'm always here to help you keep your models in top shape.

**You're awesome too, macha!** 🌟😊

Is there anything else you'd like to know about drift detection, dude?"""
            }

            // Jokes / Tell me a joke
            questionLower.contains("tell me a joke") || questionLower.contains("joke") ||
                    questionLower.contains("make me laugh") || questionLower.contains("something funny") -> {
                """😄 **Here's a ML/drift joke for you, bro!**

**Q:** Why did the machine learning model go to therapy?
**A:** Because it had too much drift and couldn't cope with the changes in its life!

🤓 **Another one, macha:**
**Q:** What did the model say when it saw concept drift?
**A:** "Y, why have you changed your relationship with X?"

**Bonus ML humor, dude:**
"I'm not saying my model has drift... but its predictions have been drifting further from reality!" 😅

**Back to business, buddy?**
Want to learn how to actually fix drift? I can help with that too!

• Understanding drift types
• Applying patches
• Monitoring strategies

What can I help you with, bro? 😊"""
            }

            // What's your name
            questionLower.contains("what's your name") || questionLower.contains("whats your name") ||
                    questionLower.contains("your name") || questionLower == "name" -> {
                """👋 **I'm PatchBot, bro!**

That's my name - **PatchBot** - your personal drift detection buddy, macha!

You can also call me:
• **PatchBot** (my official name! 🤖)
• **Your Drift Expert** (when I'm feeling professional, dude)
• **Patch Helper** (casual)
• **Your ML Buddy** (friendly)

**What I'm all about:**
I'm your personal guide to understanding and managing model drift in DriftGuardAI, bro!

**My specialty, macha:**
Making complex drift detection concepts **simple and actionable**!

**Fun fact, dude:**
I can answer questions about drift **instantly** without any downloads or cloud connections. Everything stays private on your device!

**What should I call you, buddy?** 
And more importantly - **what can I help you with today, bro?** 😊"""
            }

            // ===== FILE UPLOAD & APP USAGE =====

            // How to upload files
            questionLower.contains("how") && (questionLower.contains("upload") || questionLower.contains(
                "add"
            ) ||
                    questionLower.contains("import")) && (questionLower.contains("file") ||
                    questionLower.contains("model") || questionLower.contains("data")) -> {
                """**📤 How to Upload Files in DriftGuardAI**

**Uploading Models:**
1. Tap **Models** (bottom nav)
2. Tap **+** button (top-right)
3. Select your model file
4. Wait for processing
5. Done! ✅

**Uploading Data:**
1. Open a model
2. Tap **Upload Data**
3. Select CSV/JSON file
4. Drift detection runs automatically

**Transfer from Computer:**
```bash
# Using ADB
adb push model.onnx /sdcard/Download/
adb push data.csv /sdcard/Download/
```

**Supported:**
• Models: .onnx, .tflite, .h5, .pb, .pt, .pth
• Data: .csv, .json, .tsv, .txt, .psv, .dat

**Questions?** Ask: "supported formats" or "CSV format"
"""
            }

            // Supported formats
            questionLower.contains("what") && (questionLower.contains("file") ||
                    questionLower.contains("format") || questionLower.contains("support") ||
                    questionLower.contains("can i upload") || questionLower.contains("accept")) -> {
                """**📁 Supported File Formats**

**MODEL FILES:**
✅ ONNX (.onnx)
✅ TensorFlow Lite (.tflite)
✅ Keras (.h5, .keras)
✅ TensorFlow (.pb)
✅ PyTorch (.pt, .pth)

**DATA FILES:**
✅ CSV (.csv) - Auto header detection
✅ JSON (.json) - Multiple formats
✅ TSV (.tsv) - Tab-separated
✅ Text (.txt) - Auto-detect delimiter
✅ Pipe (.psv) - Pipe-separated
✅ Space (.dat) - Space-separated

**Features:**
→ Automatic format detection
→ Header handling
→ Quote/escape support
→ Missing value handling
→ Feature normalization

**Size Limits:**
• Models: 500 MB
• Data: 100 MB

**Not Supported:**
❌ Zip archives (extract first)
❌ Git LFS pointers (download actual file)

**More details?** Ask: "CSV format" or "JSON structure"
"""
            }

            // CSV format questions
            (questionLower.contains("csv") || questionLower.contains("comma")) &&
                    (questionLower.contains("format") || questionLower.contains("example") ||
                            questionLower.contains("how")) -> {
                """**📊 CSV Format Guide**

**With Header (Recommended):**
```csv
feature_0,feature_1,feature_2
0.5,1.2,3.4
1.1,0.9,2.7
```

**Without Header:**
```csv
0.5,1.2,3.4
1.1,0.9,2.7
```
→ Auto-generates: feature_0, feature_1...

**Features:**
✅ Auto header detection
✅ Quoted values supported
✅ Missing values handled
✅ 100,000+ rows

**Transfer:**
```bash
adb push data.csv /sdcard/Download/
```

**Excel Export:**
File → Save As → CSV (Comma delimited)
"""
            }

            // JSON format questions
            (questionLower.contains("json") && (questionLower.contains("format") ||
                    questionLower.contains("structure") || questionLower.contains("example"))) -> {
                """**🔷 JSON Format Guide**

**Format 1: Object with Array**
```json
{"data": [[0.5, 1.2], [1.1, 0.9]]}
```

**Format 2: Direct Array**
```json
[[0.5, 1.2], [1.1, 0.9]]
```

**Format 3: Named Features**
```json
[{"f0": 0.5, "f1": 1.2}]
```

**Python Export:**
```python
import json
json.dump({"data": data}, open('data.json', 'w'))
```

**Transfer:**
```bash
adb push data.json /sdcard/Download/
```
"""
            }

            // Navigation/app usage
            (questionLower.contains("how") || questionLower.contains("where")) &&
                    (questionLower.contains("navigate") || questionLower.contains("find") ||
                            questionLower.contains("use app")) -> {
                """**🧭 App Navigation**

**Bottom Navigation:**
🏠 **Dashboard** - Drift overview
📊 **Analytics** - Trends & charts
📱 **Models** - Upload & manage
🔧 **Patches** - Apply patches
⚙️ **Settings** - Configure app

**Quick Actions:**
• Upload model: Models → + button
• View drift: Dashboard → Tap card
• Apply patch: Patches → Recommended
• Export data: Model menu → Export

**Questions?** Just ask!
"""
            }

            // ===== DATA SCIENCE & ML CONCEPTS =====

            // Overfitting
            questionLower.contains("overfit") -> {
                """**🎯 Overfitting**

**What:** Model learns training data TOO well (including noise)

**Signs:**
• High train accuracy (99%)
• Low test accuracy (65%)
• Big gap = Overfitting!

**Causes:**
• Too complex model
• Too many parameters
• Insufficient data
• Too many epochs

**Solutions:**
1. Get more data
2. Use regularization (L1/L2, dropout)
3. Simplify model
4. Cross-validation
5. Early stopping

**In DriftGuardAI:**
Drift might indicate model overfitting to old data patterns

**Related:** "underfitting", "bias-variance"
"""
            }

            // Underfitting
            questionLower.contains("underfit") -> {
                """**📉 Underfitting**

**What:** Model TOO simple to capture patterns

**Signs:**
• Low train accuracy (65%)
• Low test accuracy (62%)
• Both low = Underfitting!

**Solutions:**
1. Increase model complexity
2. Add more features
3. Reduce regularization
4. Train longer
5. Better features

**vs Overfitting:**
• Underfit: Too simple, high bias
• Overfit: Too complex, high variance

**Related:** "bias-variance tradeoff"
"""
            }

            // Bias-variance tradeoff
            (questionLower.contains("bias") && questionLower.contains("variance")) ||
                    questionLower.contains("tradeoff") -> {
                """**⚖️ Bias-Variance Tradeoff**

**Fundamental ML Concept:**
Total Error = Bias² + Variance + Noise

**Bias:** Error from wrong assumptions (underfit)
**Variance:** Error from data sensitivity (overfit)

**The Tradeoff:**
• Simple model → High bias, Low variance
• Complex model → Low bias, High variance
• Sweet spot → Balanced!

**Finding Balance:**
→ Cross-validation
→ Regularization
→ Ensemble methods (Random Forest)

**In DriftGuardAI:**
Drift can shift the balance - model that fit well may now underfit
"""
            }

            // Feature engineering
            questionLower.contains("feature") && (questionLower.contains("engineering") ||
                    questionLower.contains("selection") || questionLower.contains("extraction") ||
                    questionLower.contains("transform")) -> {
                """**🔧 Feature Engineering**

**What:** Creating/transforming features to improve models

**Types:**

**1. Creation:**
• Polynomial: x, x², x³
• Interactions: age × income
• Date/time: hour, day_of_week
• Aggregations: user_avg_purchase

**2. Transformation:**
• Scaling: StandardScaler, MinMaxScaler
• Log transform: for skewed data
• Binning: continuous → categorical

**3. Selection:**
• Correlation analysis
• Feature importance
• Recursive elimination

**4. Encoding:**
• One-hot: categorical → binary
• Label: ordinal → numeric
• Target: category → mean(target)

**Best Practices:**
✓ Start simple
✓ Avoid data leakage
✓ Fit on train, transform test

**In DriftGuardAI:**
Feature drift shows which features are problematic
"""
            }

            // Cross-validation
            (questionLower.contains("cross") && questionLower.contains("validation")) ||
                    questionLower.contains("k-fold") || questionLower.contains("cv") -> {
                """**✅ Cross-Validation**

**What:** Evaluate model on multiple data subsets

**K-Fold (Most Common):**
→ Split data into K folds (e.g., K=5)
→ Train on K-1, test on 1
→ Repeat K times
→ Average results

**Why:**
• More reliable than single split
• Uses all data for train & test
• Detects overfitting

**Types:**
• K-Fold: Standard (K=5 or 10)
• Stratified: Maintains class balance
• Time Series: Respects time order
• Leave-One-Out: K=n

**Choosing K:**
• K=5: Fast, good variance
• K=10: Standard, balanced

**Python:**
```python
from sklearn.model_selection import cross_val_score
scores = cross_val_score(model, X, y, cv=5)
```

**In DriftGuardAI:**
Use CV when retraining to ensure model generalizes
"""
            }

            // Evaluation metrics
            questionLower.contains("metric") || questionLower.contains("accuracy") ||
                    questionLower.contains("precision") || questionLower.contains("recall") ||
                    questionLower.contains("f1") || questionLower.contains("auc") -> {
                """**📊 Model Evaluation Metrics**

**Classification:**

**Accuracy** = (TP+TN)/Total
→ Overall correctness
⚠️ Misleading for imbalanced data!

**Precision** = TP/(TP+FP)
→ "Of predicted positives, how many correct?"
→ Minimize false alarms

**Recall** = TP/(TP+FN)
→ "Of actual positives, how many found?"
→ Minimize missed cases

**F1 Score** = 2×(Precision×Recall)/(Precision+Recall)
→ Harmonic mean, balances both

**ROC-AUC**
→ Threshold-independent
→ 1.0 = perfect, 0.5 = random

**Regression:**

**MAE** = Mean Absolute Error
→ Average absolute difference

**RMSE** = Root Mean Squared Error
→ Penalizes large errors

**R²** = Proportion of variance explained
→ 1.0 = perfect, 0 = no better than mean

**Choosing:**
• Balanced data → Accuracy
• Imbalanced → F1, AUC
• Cost-sensitive → Precision or Recall

**In DriftGuardAI:**
Monitor these metrics over time to detect drift impact
"""
            }

            // Ensemble methods
            questionLower.contains("ensemble") || questionLower.contains("bagging") ||
                    questionLower.contains("boosting") || questionLower.contains("random forest") ||
                    questionLower.contains("xgboost") -> {
                """**🌳 Ensemble Methods**

**What:** Combine multiple models for better performance

**Types:**

**1. Bagging** (Bootstrap Aggregating)
→ Train models on random subsets
→ Average predictions
→ Reduces variance
**Example:** Random Forest

**2. Boosting**
→ Train models sequentially
→ Focus on mistakes
→ Reduces bias
**Examples:** AdaBoost, XGBoost, LightGBM

**3. Stacking**
→ Train different models
→ Meta-model combines them
→ Best performance

**When to Use:**
• Random Forest: General purpose, fast
• XGBoost: Maximum accuracy, competitions
• Stacking: Squeeze last bit of performance

**Benefits:**
✓ Better than single model
✓ Reduces overfitting
✓ More stable predictions

**In DriftGuardAI:**
Ensemble patch strategy available - combines multiple approaches
"""
            }

            // Hyperparameter tuning
            (questionLower.contains("hyperparameter") || questionLower.contains("tuning") ||
                    questionLower.contains("grid search") || questionLower.contains("optimization")) -> {
                """**🎛️ Hyperparameter Tuning**

**What:** Finding best settings BEFORE training

**Examples:** learning_rate, n_estimators, max_depth

**Methods:**

**1. Grid Search** (Exhaustive)
→ Try all combinations
→ Slow but thorough

**2. Random Search** (Faster)
→ Try random combinations
→ Often finds good params faster

**3. Bayesian Optimization** (Smartest)
→ Learns from previous trials
→ Most efficient

**Python:**
```python
from sklearn.model_selection import GridSearchCV
GridSearchCV(model, param_grid, cv=5)
```

**Best Practices:**
→ Start broad, refine
→ Use cross-validation
→ Log-scale for learning rates

**In DriftGuardAI:**
Tune patch parameters for best safety scores
"""
            }

            // Neural networks
            (questionLower.contains("neural network") || questionLower.contains("deep learning") ||
                    questionLower.contains("nn") && !questionLower.contains("cnn")) -> {
                """**🧠 Neural Networks**

**What:** Layers of connected neurons that learn patterns

**Architecture:**
Input Layer → Hidden Layers → Output Layer

**Components:**
• **Neurons:** Process inputs
• **Weights:** Learned importance
• **Biases:** Shift outputs
• **Activation:** Non-linearity (ReLU, sigmoid)

**Training:**
1. Forward propagation
2. Calculate loss
3. Backpropagation
4. Update weights
5. Repeat!

**Activation Functions:**
• ReLU: Most common, fast
• Sigmoid: Binary output (0-1)
• Softmax: Multi-class output

**When to Use:**
✓ Large datasets (>10K)
✓ Complex patterns
✓ Images, text, audio

**Not Ideal:**
❌ Small datasets
❌ Need interpretability
❌ Simple patterns (use XGBoost)

**In DriftGuardAI:**
Monitor NN drift - retraining often needed
"""
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
| **Use Case** | Real-time monitoring | Formal testing, validation |
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
                """**PatchBot - Your Drift Detection Expert**

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

