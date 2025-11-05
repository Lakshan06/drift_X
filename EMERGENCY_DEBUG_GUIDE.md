# 🚨 Emergency Debug Guide - App Keeps Stopping

## 🎯 Let's Find the Real Problem

Your app is still crashing. Let's capture the actual error to fix it.

---

## 📱 **Quick Debug Steps**

### Option 1: Use Android Studio Logcat (BEST METHOD)

This will show us the EXACT error:

1. **Open Android Studio**
2. **Connect your device** or start emulator
3. **Open Logcat** (bottom bar, or View → Tool Windows → Logcat)
4. **Filter by your app**:
    - Select "com.driftdetector.app" from dropdown
    - Or type: `package:com.driftdetector.app`
5. **Click the crash icon** (red skull) in Logcat toolbar
6. **Run your app** and watch Logcat
7. **When it crashes**, you'll see red error lines

**Look for these patterns**:

```
FATAL EXCEPTION: main
Process: com.driftdetector.app, PID: xxxxx
java.lang.RuntimeException: ...
Caused by: ...
```

**Copy the entire stack trace and share it!**

---

### Option 2: Run with ADB (Command Line)

```powershell
# Clear old logs
adb logcat -c

# Start monitoring
adb logcat | Select-String "AndroidRuntime|FATAL|Exception|driftdetector" > crash_log.txt

# In another PowerShell window, install and run
.\build.ps1 installDebug

# Launch the app from your device
# When it crashes, stop the logcat (Ctrl+C)
# Check crash_log.txt file
```

---

### Option 3: Add Crash Handler to App

Let me add a crash handler that writes to a file:

---

## 🔧 **Let's Add Emergency Logging**

I'll modify the app to log everything so we can see what's failing:

---

### Step 1: Enhanced Application Class

Add detailed startup logging:

```kotlin
// DriftDetectorApp.kt
override fun onCreate() {
    super.onCreate()
    
    android.util.Log.e("DriftDebug", "========== APP STARTING ==========")
    
    try {
        android.util.Log.e("DriftDebug", "Step 1: Initializing Timber")
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        android.util.Log.e("DriftDebug", "Step 1: ✅ Timber OK")
        
        android.util.Log.e("DriftDebug", "Step 2: Starting Koin")
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DriftDetectorApp)
            workManagerFactory()
            modules(appModules)
        }
        android.util.Log.e("DriftDebug", "Step 2: ✅ Koin OK")
        
        android.util.Log.e("DriftDebug", "Step 3: App initialized successfully!")
        
    } catch (e: Exception) {
        android.util.Log.e("DriftDebug", "❌ CRASH IN ONCREATE: ${e.message}", e)
        throw e
    }
}
```

---

## 🔍 **Common Crash Causes & Quick Fixes**

### Issue 1: Koin Dependency Injection Failure

**Symptom**: "No definition found for..."

**Check**: Are all modules in `appModules` list?

**Fix**: Verify `AppModule.kt`:

```kotlin
val appModules = listOf(
    databaseModule,      // ✅ Check exists
    networkModule,       // ✅ Check exists
    securityModule,      // ✅ Check exists
    coreModule,          // ✅ Check exists
    repositoryModule,    // ✅ Check exists
    viewModelModule,     // ✅ Check exists
    workManagerModule    // ✅ Check exists
)
```

---

### Issue 2: Database Initialization Failure

**Symptom**: SQLiteException, IllegalStateException

**Quick Fix**: Add this to `AppModule.kt`:

```kotlin
single {
    try {
        val passphrase = "DriftDetectorSecureKey2024".toByteArray()
        val factory = SupportFactory(passphrase)

        Room.databaseBuilder(
            androidContext(),
            DriftDatabase::class.java,
            "drift_detector.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    } catch (e: Exception) {
        android.util.Log.e("DriftDebug", "Database creation failed!", e)
        throw e
    }
}
```

---

### Issue 3: Missing Native Libraries

**Symptom**: "UnsatisfiedLinkError" or "couldn't find libXXX.so"

**Fix**: Check if AAR files are correct:

```powershell
dir app\libs\*.aar
```

Expected:

- RunAnywhereKotlinSDK-release.aar (should be present)
- runanywhere-llm-llamacpp-release.aar (should be present)

If corrupted, re-download:

```powershell
# Delete old files
Remove-Item app\libs\*.aar

# Re-download (provided earlier)
```

---

### Issue 4: AndroidManifest Issues

**Quick Check**:

```powershell
# Verify manifest is valid
.\gradlew :app:processDebugManifest
```

If fails, check:

- Application name: `.DriftDetectorApp` exists?
- Activity name: `.presentation.MainActivity` exists?

---

## 🚀 **Emergency Quick Fix: Minimal App**

Let's create a minimal version to test if basic app works:

### Create `MinimalMainActivity.kt`:

```kotlin
package com.driftdetector.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MinimalMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.e("DriftDebug", "MainActivity.onCreate() called")
        
        try {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Model Drift Detector", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("App is running!", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            android.util.Log.e("DriftDebug", "MainActivity.onCreate() completed")
        } catch (e: Exception) {
            android.util.Log.e("DriftDebug", "MainActivity crash!", e)
            throw e
        }
    }
}
```

---

## 🔧 **What I Can Do Right Now**

I can help you by:

1. **Adding extensive logging** to pinpoint the crash
2. **Creating a minimal test version** to isolate the issue
3. **Checking specific files** if you tell me the error
4. **Fixing the actual issue** once we know what it is

---

## 📝 **What You Should Do**

### BEST OPTION: Share the crash log

1. Open Android Studio
2. Run the app
3. Open Logcat (bottom panel)
4. Filter by "FATAL" or "Exception"
5. Copy the red error text
6. Share it with me

**The error will look like**:

```
java.lang.RuntimeException: Unable to start activity...
Caused by: java.lang.IllegalStateException: ...
    at com.driftdetector.app.something.Something.method(Something.kt:123)
```

### ALTERNATIVE: Try minimal build

1. Tell me if you want me to create a minimal test version
2. This strips everything down to basics
3. We add features back one by one
4. Identify what causes the crash

---

## 🎯 **Most Likely Issues (Ranked)**

Based on your setup, most likely causes:

### 1. 🔴 Koin Dependency Issue (80% likely)

**Symptom**: Crashes immediately on startup
**Cause**: Missing dependency in Koin modules
**Solution**: I can add defensive checks

### 2. 🟡 Database Initialization (15% likely)

**Symptom**: Crashes when accessing database
**Cause**: SQLCipher or Room issue
**Solution**: Already fixed exportSchema, but might be passphrase

### 3. 🟢 Activity/Compose Issue (5% likely)

**Symptom**: App starts but crashes on UI
**Cause**: Compose or ViewModel issue
**Solution**: Use minimal activity

---

## 🚨 **EMERGENCY: Can't Get Logs?**

If you can't access logs, let me create a version that:

1. Shows a Toast message with the error
2. Writes error to a text file
3. Shows error in the UI instead of crashing

---

## 📞 **Tell Me**

1. **When does it crash?**
    - Immediately on launch? (Koin/Database issue)
    - After splash screen? (Activity/Compose issue)
    - When clicking something? (ViewModel/Repository issue)

2. **What do you see?**
    - White screen then crash?
    - Black screen then crash?
    - App icon then crash?
    - Nothing at all?

3. **Can you access Android Studio?**
    - YES: Let me guide you to Logcat
    - NO: Let me add file logging

---

## 🔧 **Quick Actions I Can Take**

Tell me what to do:

**A**: Add extensive logging to find the crash point
**B**: Create minimal test version with just "Hello World"
**C**: Remove all features except core (no DB, no AI, no Workers)
**D**: Check specific file if you have error message
**E**: Add Toast notifications to show where it crashes

**Just say A, B, C, D, or E and I'll do it immediately!**

---

Made with 🚨 for emergency debugging
