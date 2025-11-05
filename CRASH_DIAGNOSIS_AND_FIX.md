# App Crash Diagnosis and Fixes

## Critical Issues Found

### 1. ⚠️ MISSING: SciChart License Key

**Issue:** The app references `R.string.sciChart_license` but it's not defined in `strings.xml`

**Location:** This is used in chart screens that may be loaded

**Fix:** Add the SciChart license to `strings.xml`:

```xml
<!-- In app/src/main/res/values/strings.xml -->
<string name="sciChart_license">YOUR_SCICHART_LICENSE_KEY_HERE</string>
```

**Get a license:** https://www.scichart.com/getting-started/

For development, you can use a trial key or leave it empty (will show trial watermark).

---

## How to Diagnose the Crash

### Step 1: Check Logcat Output

Run the app and immediately check logcat for errors:

```bash
# In Android Studio Terminal
adb logcat -v time *:E

# Or filter by your app package
adb logcat -v time | findstr "driftdetector"
```

Look for:

- `FATAL EXCEPTION`
- `CRASH`
- `java.lang.RuntimeException`
- `android.content.res.Resources$NotFoundException`

### Step 2: Check Crash Logs Saved by App

The app saves detailed crash logs to:

```
/data/data/com.driftdetector.app/files/crash_*.log
```

Pull them using:

```bash
adb pull /data/data/com.driftdetector.app/files/ ./crash_logs/
```

### Step 3: Check App Init Log

```bash
adb pull /data/data/com.driftdetector.app/files/app_init.log
```

This will show you exactly where initialization fails.

---

## Common Crash Causes & Fixes

### Crash 1: Resources$NotFoundException

**Symptom:** App crashes immediately on launch

**Cause:** Missing resource (likely `sciChart_license`)

**Fix:** Add missing resources as shown above

---

### Crash 2: Koin DI Initialization Failure

**Symptom:** Crash with "No definition found" or "Koin not started"

**Cause:** Dependency injection not properly initialized

**Fix:** Already handled in code - check logs for specific missing dependency

**Debug:** Look for `APP_INIT` logs showing where Koin fails

---

### Crash 3: Database Creation Failure

**Symptom:** Crash when trying to access database

**Cause:** Database migration or corruption issue

**Fix 1 - Clear app data:**

```bash
adb shell pm clear com.driftdetector.app
```

**Fix 2 - Manual delete:**

```bash
adb shell
cd /data/data/com.driftdetector.app/databases/
rm drift_database.db
rm drift_database.db-shm
rm drift_database.db-wal
exit
```

---

### Crash 4: RunAnywhere SDK Missing

**Symptom:** `NoClassDefFoundError` or `ClassNotFoundException` with "RunAnywhere"

**Cause:** AAR files not present in `app/libs/`

**Fix:** This is EXPECTED and handled gracefully. App should continue with fallback explanations.

**Check:** Look for log: "⚠️ RunAnywhere SDK not available (AAR files not present)"

---

### Crash 5: TensorFlow Lite Issues

**Symptom:** Crash when loading/inferencing models

**Cause:** TFLite model format incompatibility or corrupted file

**Fix:** Ensure models are valid TFLite format (`.tflite` files)

**Test:** Use
Google's [TFLite model validator](https://www.tensorflow.org/lite/guide/model_analyzer)

---

### Crash 6: Out of Memory (OOM)

**Symptom:** Crash with `OutOfMemoryError` or "Failed to allocate"

**Cause:** Large models or too many charts in memory

**Fix:** Already handled with `android:largeHeap="true"` in manifest

**Additional Fix:** Reduce model size or implement pagination for charts

---

## Quick Fixes to Apply Now

### Fix 1: Add SciChart License (Critical)

Edit `app/src/main/res/values/strings.xml` and add:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">DriftGuardAI</string>
    
    <!-- ADD THIS LINE -->
    <string name="sciChart_license"></string>
    <!-- Empty string will use trial mode with watermark -->
    
    <!-- Rest of your strings... -->
</resources>
```

---

### Fix 2: Clean and Rebuild

```bash
# In Android Studio Terminal or PowerShell
cd C:/drift_X

# Clean project
./gradlew clean

# Rebuild
./gradlew build

# Install on device
./gradlew installDebug
```

---

### Fix 3: Clear App Data

```bash
# Clear all app data including database
adb shell pm clear com.driftdetector.app

# Then reinstall
./gradlew installDebug
```

---

## Step-by-Step Crash Investigation

### 1. Capture Full Crash Log

```bash
# Run this BEFORE launching the app
adb logcat -c  # Clear logcat

# In another terminal, start logging to file
adb logcat -v time > crash_log.txt

# Launch the app and let it crash
# Then stop the logcat (Ctrl+C)
```

### 2. Find the Stack Trace

Search for `FATAL EXCEPTION` in `crash_log.txt`

Look for lines like:

```
E/AndroidRuntime: FATAL EXCEPTION: main
E/AndroidRuntime: Process: com.driftdetector.app, PID: xxxxx
E/AndroidRuntime: java.lang.RuntimeException: Unable to start activity
```

### 3. Identify the Root Cause

The stack trace will show:

- Which Activity or Component crashed
- Which line of code caused it
- The exception type

### 4. Check for Specific Errors

Common error patterns:

**Missing Resource:**

```
android.content.res.Resources$NotFoundException: String resource ID #0x...
```

→ **Fix:** Add the missing resource

**Null Pointer:**

```
java.lang.NullPointerException: Attempt to invoke ... on a null object
```

→ **Fix:** Check null safety in the specified line

**Class Not Found:**

```
java.lang.NoClassDefFoundError: Failed resolution of: L...
```

→ **Fix:** Add missing dependency or AAR file

---

## Emergency Fallback: Minimal Working Version

If crashes persist, comment out problematic features:

### Disable AI Assistant

In `MainActivity.kt`, comment out the FAB and AI route:

```kotlin
// Comment out this section:
/*
floatingActionButton = {
    if (currentRoute != Screen.AIAssistant.route) {
        FloatingActionButton(...) { ... }
    }
}
*/

// And this route:
/*
composable(Screen.AIAssistant.route) {
    AIAssistantScreen(onNavigateBack = { navController.popBackStack() })
}
*/
```

### Disable Monitoring Service

In `MainActivity.kt`:

```kotlin
// Comment out:
// monitoringService.startMonitoring()
```

---

## Verification Steps

After applying fixes:

1. ✅ Clean project: `./gradlew clean`
2. ✅ Rebuild: `./gradlew build`
3. ✅ Check for compilation errors
4. ✅ Install: `./gradlew installDebug`
5. ✅ Monitor logcat: `adb logcat -v time *:E`
6. ✅ Launch app
7. ✅ Check crash logs: `adb pull /data/data/com.driftdetector.app/files/`

---

## Getting Help

If crashes persist after trying all fixes:

1. **Get the full logcat:**
   ```bash
   adb logcat -d > full_crash_log.txt
   ```

2. **Get app logs:**
   ```bash
   adb pull /data/data/com.driftdetector.app/files/ ./app_logs/
   ```

3. **Check these logs:**
    - `app_init.log` - Shows initialization steps
    - `crash_*.log` - Shows crash details
    - `timber.log` - Shows app runtime logs

4. **Provide:**
    - Full stack trace from logcat
    - Content of `app_init.log`
    - Content of latest `crash_*.log`
    - Android version and device model

---

## Next Steps

1. **Immediate:** Add the SciChart license string (even empty)
2. **Then:** Clean and rebuild
3. **Test:** Launch app and check logcat
4. **If still crashing:** Follow the diagnosis steps above
5. **Report:** Share the crash logs for specific help

---

## Prevention: Enable Detailed Logging

The app already has comprehensive crash logging. To access it:

```bash
# Get all log files
adb pull /data/data/com.driftdetector.app/files/ ./logs/

# View app initialization
cat ./logs/app_init.log

# View runtime logs
cat ./logs/timber.log

# View crashes
cat ./logs/crash_*.log
```

These logs will tell you EXACTLY where and why the crash occurred.
