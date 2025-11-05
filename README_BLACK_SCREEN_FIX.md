# 🖥️ Black Screen Fix - Quick Start

## Problem

The app shows a **black screen** and crashes immediately upon launch.

## 🚀 FASTEST FIX - Run Diagnostic Script

### Windows PowerShell

```powershell
# Connect your device/emulator first!
# Then run:
.\diagnose_crash.ps1
```

**What it does:**

- ✅ Automatically captures all logs
- ✅ Builds and installs the app
- ✅ Analyzes the crash
- ✅ Shows you exactly what's wrong
- ✅ Opens the logs folder

**Time:** ~5 minutes

---

## 🔧 MANUAL FIX - If Script Doesn't Work

### Step 1: Complete Clean (Required!)

```bash
# In PowerShell or Terminal
cd C:/drift_X

# Stop Gradle
./gradlew --stop

# Clean project
./gradlew clean

# Uninstall app
adb uninstall com.driftdetector.app
```

**In Android Studio:**

- File → Invalidate Caches...
- Select "Invalidate and Restart"
- Wait for indexing to complete

### Step 2: Rebuild

```bash
./gradlew assembleDebug
```

### Step 3: Install and Monitor

```bash
# Clear logcat
adb logcat -c

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Start monitoring (in another terminal)
adb logcat -v time -s APP_INIT:* KOIN:* ACTIVITY:* AndroidRuntime:E

# Launch the app from your device
```

---

## 🔍 What to Look For in Logs

### Success Indicators

Look for these in logcat:

```
✓ super.onCreate() completed
✓ Timber initialized
✓ Koin initialized successfully
✓ Database created successfully
✓ DriftDetectorApp base initialization complete
✓ MainActivity onCreate called
✓ setContent completed
```

### Failure Indicators

If you see any of these, you found the problem:

```
✗ FAILED
FATAL EXCEPTION
java.lang.RuntimeException
NoBeanDefFoundException
SQLiteException
Resources$NotFoundException
```

---

## 📋 Common Fixes

### Fix 1: Database Issue

```bash
# Delete corrupt database
adb shell
cd /data/data/com.driftdetector.app/
rm -rf databases/
exit

# Reinstall
./gradlew installDebug
```

### Fix 2: Gradle Cache Issue

```bash
# Clean everything
./gradlew --stop
./gradlew clean

# Rebuild
./gradlew assembleDebug
```

### Fix 3: Dependency Conflict

**Temporarily disable RunAnywhere SDK:**

Edit `app/build.gradle.kts` and comment out:

```kotlin
// TEMPORARY - Comment out these lines
/*
fileTree("libs") {
    include("RunAnywhereKotlinSDK-release.aar")
    include("runanywhere-llm-llamacpp-release.aar")
}.forEach { file ->
    implementation(files(file))
}
*/
```

Then: `./gradlew clean && ./gradlew assembleDebug`

---

## 📱 Expected Result After Fix

1. ✅ App launches (no black screen)
2. ✅ Light/white screen appears
3. ✅ "DriftGuardAI" text in top bar
4. ✅ Bottom navigation shows:
    - Dashboard
    - Models
    - Patches
    - Settings
5. ✅ Floating AI button visible

---

## 🆘 Still Not Working?

### Get Detailed Logs

```bash
# Run these commands and send the results:

# 1. Full logcat
adb logcat -d > full_log.txt

# 2. App logs
adb pull /data/data/com.driftdetector.app/files/ ./app_logs/

# 3. Device info
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model

# 4. Dependency tree
./gradlew app:dependencies > dependencies.txt
```

### Files to Share

1. `full_log.txt` - System logs
2. `app_logs/app_init.log` - Initialization log
3. `app_logs/crash_*.log` - Crash details
4. Device model and Android version

---

## 📚 Detailed Documentation

For comprehensive troubleshooting:

- **BLACK_SCREEN_FIX.md** - Complete guide with all possible fixes
- **CRASH_DIAGNOSIS_AND_FIX.md** - General crash troubleshooting
- **diagnose_crash.ps1** - Automated diagnostic script

---

## ⚡ Quick Commands Cheat Sheet

```bash
# Clean build
./gradlew --stop && ./gradlew clean

# Build
./gradlew assembleDebug

# Uninstall
adb uninstall com.driftdetector.app

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Clear app data
adb shell pm clear com.driftdetector.app

# Monitor logs
adb logcat -v time -s APP_INIT:* KOIN:* ACTIVITY:* AndroidRuntime:E

# Pull logs
adb pull /data/data/com.driftdetector.app/files/ ./logs/
```

---

## ✅ Verification Checklist

Before reporting issues, verify:

- [ ] Gradle sync completes without errors
- [ ] Build completes successfully
- [ ] Device/emulator is connected (`adb devices`)
- [ ] App installs without errors
- [ ] Logcat is running before app launch
- [ ] You waited for logs after crash
- [ ] You checked `app_init.log`

---

## 💡 Tips

1. **Always run clean build first** - Most issues are cache-related
2. **Check logcat BEFORE launching** - You need to see initialization
3. **Look for FIRST error** - Subsequent errors are often caused by the first one
4. **Use the diagnostic script** - It does everything automatically
5. **Read app_init.log** - It has detailed step-by-step initialization logs

---

**The logs will tell you EXACTLY what's wrong!** 🎯
