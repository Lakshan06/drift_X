# 🔧 AI Assistant Troubleshooting Guide

## ⚠️ "Unable to Generate Answers" - SOLUTION

If your AI Assistant isn't responding, follow this comprehensive troubleshooting guide:

---

## ✅ Quick Fix Checklist

### 1. **Verify SDK Files Are Present**

```powershell
# Check if AAR files exist
ls app/libs/

# You should see:
# RunAnywhereKotlinSDK-release.aar (4.0 MB)
# runanywhere-llm-llamacpp-release.aar (2.1 MB)
```

**✓ Both files present?** → Continue to step 2  
**✗ Files missing?** → Download them:

```powershell
cd app/libs

# Download SDK files
Invoke-WebRequest -Uri "https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar" -OutFile "RunAnywhereKotlinSDK-release.aar"

Invoke-WebRequest -Uri "https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar" -OutFile "runanywhere-llm-llamacpp-release.aar"
```

### 2. **Clean and Rebuild**

```powershell
# Clean build
.\gradlew clean

# Rebuild
.\build.ps1 assembleDebug

# Reinstall
adb uninstall com.driftdetector.app
.\build.ps1 installDebug
```

### 3. **Check Logs**

```powershell
# Filter AI-related logs
adb logcat | findstr "AI\|RunAnywhere\|AIAnalysisEngine\|AIPromptEngine"
```

**Look for:**

- ✅ "RunAnywhere SDK initialized successfully"
- ✅ "AI Analysis Engine initialized"
- ✗ "RunAnywhere SDK not found"
- ✗ "Failed to initialize"

### 4. **Download AI Model** (First Use)

The AI needs a model file downloaded on first use:

**When you send first message:**

1. App should show download progress
2. Download happens automatically
3. Choose SmolLM2 360M (119 MB) or Qwen 2.5 (374 MB)
4. **Ensure WiFi connection** for download

**Check if model downloaded:**

```powershell
adb shell ls /data/data/com.driftdetector.app/files/models/
```

---

## 🔍 Detailed Diagnostics

### Issue 1: "AI assistant is not available"

**Symptoms:**

- Welcome message says "AI Assistant is currently initializing"
- Status shows "○ Offline" (red)
- Can't send messages

**Causes & Solutions:**

#### Cause A: SDK Not Initialized

```
Logcat shows: "RunAnywhere SDK not available"
```

**Solution:**

1. Verify AAR files in `app/libs/`
2. Clean and rebuild project
3. Check `app/build.gradle.kts` has:
   ```kotlin
   fileTree("libs") {
       include("RunAnywhereKotlinSDK-release.aar")
       include("runanywhere-llm-llamacpp-release.aar")
   }.forEach { file ->
       implementation(files(file))
   }
   ```

#### Cause B: Model Not Downloaded

```
Logcat shows: "Model not ready"
```

**Solution:**

1. Send a message (triggers download)
2. Wait for download to complete
3. Ensure stable WiFi connection
4. Check storage space (need 200-500 MB)

#### Cause C: Initialization Error

```
Logcat shows: "Failed to initialize AI Analysis Engine"
```

**Solution:**

1. Check device has 2GB+ RAM
2. Close other apps
3. Restart device
4. Reinstall app

### Issue 2: Messages Send But No Response

**Symptoms:**

- Typing indicator shows briefly
- Then error message appears
- Or nothing happens

**Causes & Solutions:**

#### Cause A: Model Loading Failed

```
Logcat shows: "Error generating AI answer"
```

**Solution:**

1. Check logcat for specific error
2. Verify model file integrity:
   ```powershell
   adb shell ls -lh /data/data/com.driftdetector.app/files/models/
   ```
3. Delete corrupted model and re-download:
   ```powershell
   adb shell rm -rf /data/data/com.driftdetector.app/files/models/*
   ```
4. Restart app and try again

#### Cause B: Out of Memory

```
Logcat shows: "OutOfMemoryError" or app crashes
```

**Solution:**

1. Use smaller model (SmolLM2 360M instead of Qwen 2.5)
2. Close background apps
3. Restart device
4. Ensure device has 2GB+ RAM

#### Cause C: Prompt Engine Error

```
Logcat shows: "Error in AIPromptEngine"
```

**Solution:**

1. Update to latest build
2. Verify `AIPromptEngine.kt` file exists
3. Check imports are correct
4. Rebuild project

### Issue 3: Responses Are Generic/Not Intelligent

**Symptoms:**

- AI responds but answers are vague
- No drift-specific information
- Doesn't detect question types

**Causes & Solutions:**

#### Cause: Using Fallback Mode

```
AI gives short, generic responses
```

**Solution:**

1. Verify SDK is actually initialized (check logs)
2. Ensure `AIPromptEngine.buildIntelligentPrompt()` is being called
3. Check that `AIAnalysisEngine.answerQuestionStream()` uses AIPromptEngine
4. Rebuild with latest code

---

## 🧪 Testing the AI Assistant

### Test 1: Basic Functionality

```
User: "Hello"
Expected: AI responds with greeting and capabilities list
```

### Test 2: Educational Question

```
User: "What is concept drift?"
Expected: Detailed explanation with examples from the app
```

### Test 3: Comparison

```
User: "PSI vs KS test"
Expected: Side-by-side comparison table
```

### Test 4: How-To

```
User: "How do I rollback a patch?"
Expected: Step-by-step instructions with UI elements
```

### Test 5: Troubleshooting

```
User: "My drift score is 0.8"
Expected: Diagnostic guide with causes and solutions
```

**If ANY test fails:** See diagnostic sections above

---

## 📊 Expected Behavior

### On App Launch

```
Logs should show:
1. "Initializing AI Analysis Engine with RunAnywhere SDK"
2. "Initializing RunAnywhere SDK..."
3. "Registered SmolLM2 360M model"
4. "Registered Qwen 2.5 0.5B model"
5. "✅ RunAnywhere SDK initialized successfully"
6. "AI Analysis Engine initialized"
```

### On Opening AI Assistant

```
UI should show:
1. "● Online" in green (top bar)
2. Welcome message with capabilities list
3. Active input field
4. Blue send button (enabled)
```

### On Sending Message

```
Expected flow:
1. Message appears in chat (right side, blue)
2. Typing indicator appears (3 animated dots)
3. AI message placeholder appears (left side, gray)
4. Tokens stream in real-time
5. Complete message displayed
6. Typing indicator disappears
```

---

## 🔧 Advanced Debugging

### Enable Verbose Logging

Add to `DriftDetectorApp.kt`:

```kotlin
override fun onCreate() {
    super.onCreate()
    
    // Enable verbose logging
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
    
    // ... rest of initialization
}
```

### Check SDK Initialization

Add temporary logging to `RunAnywhereInitializer.kt`:

```kotlin
suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
    Timber.d("🔵 INIT: Starting initialization")
    
    if (isInitialized) {
        Timber.d("🟢 INIT: Already initialized")
        return@withContext
    }

    try {
        Timber.d("🔵 INIT: Calling RunAnywhere.initialize()")
        RunAnywhere.initialize(
            context = context,
            apiKey = "dev",
            environment = SDKEnvironment.DEVELOPMENT
        )
        Timber.d("🟢 INIT: RunAnywhere.initialize() success")

        Timber.d("🔵 INIT: Registering LlamaCpp provider")
        LlamaCppServiceProvider.register()
        Timber.d("🟢 INIT: Provider registered")

        Timber.d("🔵 INIT: Registering models")
        registerModels()
        Timber.d("🟢 INIT: Models registered")

        Timber.d("🔵 INIT: Scanning for downloaded models")
        RunAnywhere.scanForDownloadedModels()
        Timber.d("🟢 INIT: Scan complete")

        isInitialized = true
        Timber.i("✅ RunAnywhere SDK initialized successfully")

    } catch (e: Exception) {
        Timber.e(e, "❌ INIT FAILED: ${e.message}")
        throw e
    }
}
```

### Monitor Real-Time Logs

```powershell
# Terminal 1: Filtered logs
adb logcat | findstr "🔵\|🟢\|❌\|✅"

# Terminal 2: AI-specific
adb logcat | findstr "AIAnalysisEngine\|AIPromptEngine\|AIAssistantViewModel"

# Terminal 3: Errors only
adb logcat *:E
```

---

## 🐛 Known Issues & Workarounds

### Issue: First Response Very Slow (30+ seconds)

**Cause:** Model loading for first time  
**Workaround:** Expected behavior, subsequent responses will be fast  
**Solution:** Use SmolLM2 360M for faster first load

### Issue: App Crashes When Sending Message

**Cause:** Out of memory during model loading  
**Solution:**

1. Close all background apps
2. Restart device
3. Use SmolLM2 360M (smaller)
4. Check device has 2GB+ RAM

### Issue: Responses Cut Off Mid-Sentence

**Cause:** Streaming interrupted  
**Solution:**

1. Check network stability (if model downloading)
2. Ensure app stays in foreground
3. Check logs for specific error
4. Try clearing chat and asking again

---

## ✅ Verification Steps

### Step 1: Verify Files

```powershell
# Check files exist and have correct size
ls -l app/libs/

# Should show:
# RunAnywhereKotlinSDK-release.aar ~4MB
# runanywhere-llm-llamacpp-release.aar ~2MB
```

### Step 2: Verify Build

```powershell
# Build should succeed
.\build.ps1 assembleDebug

# Look for: BUILD SUCCESSFUL
```

### Step 3: Verify Installation

```powershell
# Install app
.\build.ps1 installDebug

# Check installed
adb shell pm list packages | findstr driftdetector
```

### Step 4: Verify Runtime

```powershell
# Launch app and check logs
adb logcat -c  # Clear logs
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
adb logcat | findstr "AI\|RunAnywhere"
```

**Expected:**

- ✅ "AI Analysis Engine initialized"
- ✅ "RunAnywhere SDK initialized successfully"

---

## 📞 Still Having Issues?

### Collect Diagnostic Information

```powershell
# Capture full logs
adb logcat > logs/ai_debug_$(Get-Date -Format 'yyyyMMdd_HHmmss').log

# Device info
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.product.model          # Device model
adb shell dumpsys meminfo com.driftdetector.app  # Memory usage
```

### Create Issue Report

Include:

1. **Device**: Model, Android version, RAM
2. **Build logs**: `.\gradlew assembleDebug` output
3. **Runtime logs**: Filtered AI/RunAnywhere logs
4. **Steps to reproduce**: Exact steps you took
5. **Expected vs Actual**: What should happen vs what happens
6. **Screenshots**: Show the issue in UI

---

## 🎯 Quick Reference

| Symptom | Most Likely Cause | Quick Fix |
|---------|-------------------|-----------|
| "AI offline" | Model not downloaded | Send message, wait for download |
| No response | Out of memory | Close apps, restart device |
| Generic responses | Fallback mode | Verify SDK initialized |
| Slow first response | Model loading | Wait 30s, subsequent will be fast |
| App crashes | Memory issue | Use SmolLM2 360M, restart device |

---

## ✨ Expected Performance

| Metric | SmolLM2 360M | Qwen 2.5 0.5B |
|--------|--------------|---------------|
| Download size | 119 MB | 374 MB |
| First load | 5-10 seconds | 15-30 seconds |
| Response time | 2-5 seconds | 5-10 seconds |
| Quality | Good | Excellent |
| RAM usage | 300-500 MB | 600-900 MB |

---

**Still need help? Check the logs and follow the diagnostic steps above!** 🔍
