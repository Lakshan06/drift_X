# 🖥️ Black Screen Crash - Complete Fix Guide

## 🔍 Problem Analysis

The black screen indicates the app is crashing **during startup** or **in MainActivity** before the
UI renders.

### Most Likely Causes:

1. ❌ **Koin DI initialization failure** - Dependencies not resolving
2. ❌ **Database creation failure** - Room database setup issue
3. ❌ **Compose initialization failure** - UI framework not starting
4. ❌ **Missing AAR dependency resolution** - RunAnywhere SDK files causing issues
5. ❌ **Theme/Resource issue** - Missing required resources
6. ❌ **Permission issue** - Missing required permissions

---

## ✅ CRITICAL FIXES APPLIED

### Fix 1: Dependency Conflict Resolution

**Issue:** Some dependencies may have version conflicts

**Files to check:**

- `app/build.gradle.kts` - All dependency versions

### Fix 2: Compose BOM Compatibility

**Potential Issue:** Compose BOM version might be incompatible with other libraries

**Current version:** `2024.01.00`

---

## 🚨 IMMEDIATE FIXES TO APPLY

### Step 1: Clean Everything

```bash
# Stop all Gradle daemons
./gradlew --stop

# Clean the project
./gradlew clean

# Clear Gradle cache (if needed)
Remove-Item -Recurse -Force ~/.gradle/caches/ -ErrorAction SilentlyContinue

# Clear build directories
Remove-Item -Recurse -Force app/build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
```

### Step 2: Invalidate Caches in Android Studio

1. **File → Invalidate Caches...**
2. Select **"Invalidate and Restart"**
3. Wait for indexing to complete

### Step 3: Sync Gradle

```bash
./gradlew --refresh-dependencies
```

### Step 4: Clear App Data on Device

```bash
# Uninstall completely
adb uninstall com.driftdetector.app

# Or clear data
adb shell pm clear com.driftdetector.app
```

### Step 5: Rebuild

```bash
./gradlew assembleDebug
```

---

## 🔧 GRADLE DEPENDENCY FIXES

### Issue: Potential Version Conflicts

Current dependencies that could cause issues:

1. **Ktor vs OkHttp/Retrofit** - Both are HTTP clients
2. **kotlinx-serialization-json vs Gson** - Both are JSON serializers
3. **Compose BOM** - Might need update

### Recommended Dependency Updates

Edit `app/build.gradle.kts`:

```kotlin
dependencies {
    // Update Compose BOM to latest stable
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    
    // Ensure consistent Kotlin version
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    
    // Use consistent coroutines version
    val coroutinesVersion = "1.8.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
}
```

---

## 📱 DIAGNOSTIC STEPS

### Step 1: Capture Detailed Logs

**BEFORE launching the app:**

```bash
# Clear logcat
adb logcat -c

# Start capturing (in PowerShell)
adb logcat -v time *:V > full_app_log.txt

# In another terminal, install and launch
./gradlew installDebug
adb shell am start -n com.driftdetector.app/.presentation.MainActivity

# Let it crash, then stop logcat (Ctrl+C)
```

### Step 2: Check Specific Log Tags

```bash
# Check app initialization
adb logcat -v time -s APP_INIT:* KOIN:* ACTIVITY:*

# Check crashes
adb logcat -v time -s AndroidRuntime:E

# Check Compose
adb logcat -v time -s Compose:*
```

### Step 3: Pull App Logs

```bash
adb pull /data/data/com.driftdetector.app/files/app_init.log
adb pull /data/data/com.driftdetector.app/files/crash_*.log
```

---

## 🐛 COMMON BLACK SCREEN CAUSES

### Cause 1: Koin Not Started

**Error in logs:**

```
org.koin.core.error.NoBeanDefFoundException: No definition found
```

**Fix:**

- Check that `DriftDetectorApp` extends `Application`
- Verify `android:name=".DriftDetectorApp"` in manifest
- Ensure `startKoin {}` is called in `onCreate()`

**Test:**

```bash
adb logcat -v time -s KOIN:*
```

Look for: `✓ Koin initialized successfully`

---

### Cause 2: Database Creation Failed

**Error in logs:**

```
android.database.sqlite.SQLiteException
```

**Fix:**

```bash
# Delete database completely
adb shell
cd /data/data/com.driftdetector.app/
rm -rf databases/
exit

# Reinstall
./gradlew installDebug
```

---

### Cause 3: Compose Not Initializing

**Error in logs:**

```
java.lang.IllegalStateException: Compose not initialized
```

**Fix:**
Check `MainActivity.kt` - ensure `setContent {}` is called properly

---

### Cause 4: Missing Resources

**Error in logs:**

```
android.content.res.Resources$NotFoundException
```

**Fix:**

- Verify all resources in `res/` are valid
- Check `strings.xml` has no errors
- Ensure themes.xml is correct

---

### Cause 5: RunAnywhere AAR Conflict

**Error in logs:**

```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/runanywhere/
```

**Fix - Temporarily Disable:**

Edit `app/build.gradle.kts` and comment out:

```kotlin
// TEMPORARY FIX - Comment these out
/*
fileTree("libs") {
    include("RunAnywhereKotlinSDK-release.aar")
    include("runanywhere-llm-llamacpp-release.aar")
}.forEach { file ->
    implementation(files(file))
}
*/
```

Then rebuild:

```bash
./gradlew clean
./gradlew assembleDebug
```

---

### Cause 6: Memory Issue

**Error in logs:**

```
OutOfMemoryError
```

**Fix:**
Already handled with `android:largeHeap="true"` in manifest

If still occurring, add to `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

---

## 🔬 ADVANCED DEBUGGING

### Enable Strict Mode (Debug Only)

Add to `DriftDetectorApp.kt` in `onCreate()`:

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
}
```

### Add Crash Handler Enhancement

Already implemented in `DriftDetectorApp.kt` - check the logs it creates!

---

## 📊 VERIFICATION CHECKLIST

After applying fixes, verify:

- [ ] **Gradle sync** succeeds without errors
- [ ] **Build** completes successfully (`./gradlew assembleDebug`)
- [ ] **Installation** succeeds (`./gradlew installDebug`)
- [ ] **Logcat shows** `APP_INIT` logs
- [ ] **Logcat shows** `✓ Koin initialized successfully`
- [ ] **Logcat shows** `✓ Database created successfully`
- [ ] **Logcat shows** `MainActivity onCreate called`
- [ ] **Logcat shows** `✓ setContent completed`
- [ ] **Screen** is NOT black - UI appears

---

## 🎯 STEP-BY-STEP FIX PROCEDURE

### Phase 1: Clean Everything (5 minutes)

```bash
# 1. Stop Gradle
./gradlew --stop

# 2. Clean project
./gradlew clean

# 3. Uninstall app
adb uninstall com.driftdetector.app

# 4. In Android Studio: File → Invalidate Caches → Invalidate and Restart
```

### Phase 2: Check Dependencies (2 minutes)

```bash
# Check for dependency conflicts
./gradlew app:dependencies > dependencies.txt

# Look for "FAILED" or "conflict" in the output
```

### Phase 3: Rebuild and Test (5 minutes)

```bash
# 1. Rebuild
./gradlew assembleDebug

# 2. Install
adb install app/build/outputs/apk/debug/app-debug.apk

# 3. Start logging BEFORE launching
adb logcat -c
adb logcat -v time *:E > error_log.txt

# 4. Launch app manually from device

# 5. Check logs after crash
```

### Phase 4: Analyze Logs (10 minutes)

```bash
# 1. Pull app logs
adb pull /data/data/com.driftdetector.app/files/ ./app_logs/

# 2. Check files:
cat ./app_logs/app_init.log      # Shows init steps
cat ./app_logs/crash_*.log       # Shows crash details  
cat error_log.txt                # Shows system errors

# 3. Look for the FIRST error - that's usually the root cause
```

### Phase 5: Apply Specific Fix (Varies)

Based on the error found:

- **Koin error** → Check DI module definitions
- **Database error** → Delete database and reinstall
- **Compose error** → Check MainActivity.kt
- **Resource error** → Check res/ files
- **AAR error** → Temporarily disable RunAnywhere libs

---

## 🆘 IF NOTHING WORKS

### Nuclear Option: Minimal Working Version

1. **Comment out** AI Assistant completely
2. **Comment out** Monitoring Service
3. **Comment out** Chart library usage
4. **Use** basic Compose components only

Create a minimal `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DriftDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text("DriftGuardAI - Minimal Version")
                }
            }
        }
    }
}
```

If this works, gradually add back features one by one to find the culprit.

---

## 📋 INFORMATION TO COLLECT

If you need to report the issue, collect:

1. **Full logcat:** `adb logcat -d > full_logcat.txt`
2. **App init log:** `adb pull /data/data/com.driftdetector.app/files/app_init.log`
3. **Crash log:** `adb pull /data/data/com.driftdetector.app/files/crash_*.log`
4. **Dependency tree:** `./gradlew app:dependencies > dependencies.txt`
5. **Device info:**
   ```bash
   adb shell getprop ro.build.version.release  # Android version
   adb shell getprop ro.product.model          # Device model
   ```

---

## ✅ EXPECTED RESULT

After successful fix:

1. ✅ App launches
2. ✅ White/Light colored screen (not black)
3. ✅ "DriftGuardAI" in top bar
4. ✅ Bottom navigation visible
5. ✅ Dashboard loads (may be empty)
6. ✅ No crash logs generated

---

## 🚀 QUICK START COMMANDS

```bash
# Complete clean and rebuild sequence
./gradlew --stop
./gradlew clean
adb uninstall com.driftdetector.app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Monitor launch
adb logcat -c
adb logcat -v time -s APP_INIT:* KOIN:* ACTIVITY:* AndroidRuntime:E
```

Then launch the app from your device.

---

**The logs will tell you EXACTLY what's wrong. Follow the steps above to capture them!**
