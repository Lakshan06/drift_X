# RunAnywhere SDK Integration Guide

## 🎯 Overview

The Model Drift Detector app now includes **AI-powered explanations** using the **RunAnywhere SDK**
for on-device LLM inference. This integration provides:

- **Natural language drift explanations** - Understand what drift means in plain English
- **Intelligent patch recommendations** - Get AI-powered suggestions for addressing drift
- **Interactive Q&A** - Ask questions about your model's performance
- **Privacy-first** - All AI processing happens on-device

## 📦 Step 1: Download RunAnywhere SDK AARs

Download both required AAR files:

### Option A: Direct Download (Fastest)

1. **RunAnywhereKotlinSDK** (Core SDK - 4.0MB)
   ```
   https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar
   ```

2. **runanywhere-llm-llamacpp** (LLM Module - 2.1MB)
   ```
   https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar
   ```

### Option B: Command Line (PowerShell)

```powershell
# Navigate to app/libs directory
cd C:\drift_X\app\libs

# Download Core SDK
Invoke-WebRequest -Uri "https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar" -OutFile "RunAnywhereKotlinSDK-release.aar"

# Download LLM Module
Invoke-WebRequest -Uri "https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar" -OutFile "runanywhere-llm-llamacpp-release.aar"
```

### Option C: Using curl

```bash
cd app/libs

curl -L -o RunAnywhereKotlinSDK-release.aar \
  https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar

curl -L -o runanywhere-llm-llamacpp-release.aar \
  https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar
```

## 📁 Step 2: Place AAR Files

Place both downloaded AAR files in the `app/libs/` directory:

```
drift_X/
└── app/
    └── libs/
        ├── RunAnywhereKotlinSDK-release.aar  ← Place here
        └── runanywhere-llm-llamacpp-release.aar  ← Place here
```

**Note**: The `app/libs/` directory should already exist. If not, create it manually.

## 🔧 Step 3: Sync Gradle

The dependencies are **already configured** in `app/build.gradle.kts`. Just sync:

1. Open Android Studio
2. Click **File → Sync Project with Gradle Files**
3. Wait for sync to complete (may take a few minutes)

## ✅ Step 4: Verify Integration

Build the project to verify everything is working:

```powershell
# PowerShell
.\build.ps1 assembleDebug
```

or in Android Studio: **Build → Make Project**

## 🤖 Step 5: Enable Full AI Features (Optional)

The app works **without** RunAnywhere SDK (uses fallback explanations). To enable full AI features:

### 5.1 Create RunAnywhere Initialization Module

Create `app/src/main/java/com/driftdetector/app/core/ai/RunAnywhereInitializer.kt`:

```kotlin
package com.driftdetector.app.core.ai

import android.content.Context
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Initialize RunAnywhere SDK for on-device AI
 */
object RunAnywhereInitializer {

    private var isInitialized = false

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Timber.d("RunAnywhere SDK already initialized")
            return@withContext
        }

        try {
            Timber.d("Initializing RunAnywhere SDK...")

            // Step 1: Initialize SDK
            RunAnywhere.initialize(
                context = context,
                apiKey = "dev",  // Any string in DEVELOPMENT mode
                environment = SDKEnvironment.DEVELOPMENT
            )

            // Step 2: Register LLM Service Provider
            LlamaCppServiceProvider.register()

            // Step 3: Register lightweight models (optimized for drift analysis)
            registerModels()

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()

            isInitialized = true
            Timber.i("✅ RunAnywhere SDK initialized successfully")

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize RunAnywhere SDK")
            throw e
        }
    }

    private suspend fun registerModels() {
        // SmolLM2 360M - Fastest, smallest (119 MB) - Best for quick explanations
        addModelFromURL(
            url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
            name = "SmolLM2 360M Q8_0",
            type = "LLM"
        )

        // Qwen 2.5 0.5B - Better quality (374 MB) - For detailed analysis
        addModelFromURL(
            url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
            name = "Qwen 2.5 0.5B Instruct Q6_K",
            type = "LLM"
        )
    }

    fun isInitialized(): Boolean = isInitialized
}
```

### 5.2 Update AIAnalysisEngine

Modify `app/src/main/java/com/driftdetector/app/core/ai/AIAnalysisEngine.kt`:

```kotlin
// In the initialize() method, add:
suspend fun initialize() = withContext(Dispatchers.IO) {
    try {
        Timber.d("Initializing AI Analysis Engine with RunAnywhere SDK")
        
        // Initialize RunAnywhere SDK
        RunAnywhereInitializer.initialize(context)
        
        isInitialized = true
        Timber.i("AI Analysis Engine initialized successfully")
    } catch (e: Exception) {
        Timber.e(e, "Failed to initialize AI Analysis Engine")
        isInitialized = false
    }
}

// In explainDrift(), replace the TODO with actual SDK call:
suspend fun explainDrift(driftResult: DriftResult): String = withContext(Dispatchers.Default) {
    if (!isInitialized || !isAvailable()) {
        return@withContext generateFallbackDriftExplanation(driftResult)
    }

    try {
        val prompt = buildDriftExplanationPrompt(driftResult)
        RunAnywhere.generate(prompt) // ✅ Actual AI call
    } catch (e: Exception) {
        Timber.e(e, "Error generating AI drift explanation")
        generateFallbackDriftExplanation(driftResult)
    }
}
```

## 📱 Usage in Your App

### Example: Get AI Explanation for Drift

```kotlin
// In ViewModel
class DriftDashboardViewModel(
    private val repository: DriftRepository,
    private val aiEngine: AIAnalysisEngine  // Inject via Koin
) : ViewModel() {

    fun explainDrift(driftResult: DriftResult) {
        viewModelScope.launch {
            val explanation = aiEngine.explainDrift(driftResult)
            _aiExplanation.value = explanation
        }
    }

    fun getRecommendations(driftResult: DriftResult) {
        viewModelScope.launch {
            val recommendations = aiEngine.recommendActions(driftResult)
            _aiRecommendations.value = recommendations
        }
    }
}
```

### Example: Streaming Explanations (Real-time)

```kotlin
fun explainDriftStreaming(driftResult: DriftResult) {
    viewModelScope.launch {
        var fullExplanation = ""
        
        aiEngine.explainDriftStream(driftResult).collect { token ->
            fullExplanation += token
            _streamingExplanation.value = fullExplanation
        }
    }
}
```

## 🎨 UI Integration Example

Add AI explanations to your drift dashboard screen:

```kotlin
@Composable
fun DriftResultCard(
    driftResult: DriftResult,
    aiExplanation: String?,
    onExplainClick: () -> Unit
) {
    Card {
        Column {
            // Existing drift result display
            Text("Drift Score: ${driftResult.driftScore}")
            Text("Status: ${if (driftResult.isDriftDetected) "⚠️ Drift Detected" else "✅ No Drift"}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // AI Explanation Section
            if (aiExplanation != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, "AI")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Explanation",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(aiExplanation)
                    }
                }
            } else {
                Button(onClick = onExplainClick) {
                    Icon(Icons.Default.Psychology, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get AI Explanation")
                }
            }
        }
    }
}
```

## 🔍 Features Enabled by RunAnywhere SDK

### 1. **Drift Explanations**

- Converts technical drift scores into plain English
- Identifies root causes
- Explains impact on model performance

### 2. **Intelligent Recommendations**

- Suggests specific actions to address drift
- Prioritizes by urgency
- Provides implementation guidance

### 3. **Patch Explanations**

- Explains what each patch does in simple terms
- Describes how it fixes the drift
- Notes trade-offs and considerations

### 4. **Interactive Q&A** (Coming Soon)

- Ask questions about your model's performance
- Get context-aware answers
- Learn about drift detection concepts

## 📊 Model Selection Guide

| Model | Size | Speed | Quality | Use Case |
|-------|------|-------|---------|----------|
| SmolLM2 360M | 119 MB | ⚡⚡⚡ | ⭐⭐ | Quick summaries, basic explanations |
| Qwen 2.5 0.5B | 374 MB | ⚡⚡ | ⭐⭐⭐⭐ | Detailed analysis, recommendations |

**Recommendation**: Start with **SmolLM2 360M** for testing, upgrade to **Qwen 2.5 0.5B** for
production.

## 🚀 Performance Tips

1. **Download models on WiFi** - They're 100-400MB each
2. **Load model at startup** - First load takes 5-15 seconds
3. **Use streaming for UX** - Shows results as they generate
4. **Cache explanations** - Store frequently accessed explanations
5. **Unload when backgrounded** - Free memory when app not visible

## 🐛 Troubleshooting

### "ClassNotFoundException: com.runanywhere.sdk.public.RunAnywhere"

**Solution**: AAR files not in `app/libs/`. Download and place them as per Step 1-2.

### App works but no AI explanations

**Solution**: This is normal! The app uses fallback explanations. To enable AI:

1. Download AAR files
2. Sync Gradle
3. Implement RunAnywhereInitializer (Step 5)

### Build error: "Duplicate class"

**Solution**: You may have conflicting Ktor versions. The SDK requires Ktor 3.0.3.

### Out of memory during model loading

**Solution**:

1. Ensure `android:largeHeap="true"` in manifest ✅ (already set)
2. Use smaller model (SmolLM2 360M)
3. Close other apps
4. Test on device with 3GB+ RAM

## 📚 Additional Resources

- **RunAnywhere SDK Docs**: See the complete guide provided earlier
- **Model Hub**: [HuggingFace GGUF Models](https://huggingface.co/models?library=gguf)
- **Sample Apps**: [RunAnywhere GitHub](https://github.com/RunanywhereAI/runanywhere-sdks)

## ✨ What's Next?

1. **Download AAR files** and place in `app/libs/`
2. **Build the app** - it works with or without AI
3. **Enable full AI features** - Follow Step 5 when ready
4. **Download an AI model** - Start with SmolLM2 360M (119MB)
5. **Enjoy AI-powered drift analysis!** 🎉

---

**Note**: The app is **fully functional without RunAnywhere SDK** - it uses intelligent fallback
explanations. The SDK enhances the experience with dynamic AI-generated insights.
