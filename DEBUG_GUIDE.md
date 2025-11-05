# 🐛 Comprehensive Debugging Guide

## Quick Start - Capture Crash Logs

### Option 1: Automated Debug Script (Recommended)

This script will rebuild, install, and capture detailed logs:

```powershell
.\debug_crash.ps1
```

**What it does:**

1. ✓ Uninstalls old app version
2. ✓ Builds fresh debug APK
3. ✓ Installs app on device
4. ✓ Launches app with log monitoring
5. ✓ Captures all logs to `logs/crash_debug_[timestamp].log`
6. ✓ Shows real-time filtered logs in console

**Press Ctrl+C when done to stop monitoring**

---

### Option 2: Simple Log Capture (If app already installed)

If the app is already installed and you just want to capture logs after a crash:

```powershell
.\capture_logs.ps1
```

**What it does:**

1. ✓ Captures all logcat history
2. ✓ Automatically extracts crash information
3. ✓ Shows initialization logs
4. ✓ Shows dependency injection logs
5. ✓ Pulls crash logs saved on device
6. ✓ Saves everything to `logs/logcat_[timestamp].log`

---

## What to Look For in the Logs

### 1. Fatal Crashes (RED FLAG 🚨)

Look for these patterns:

```
FATAL EXCEPTION
AndroidRuntime: FATAL EXCEPTION: main
Process: com.driftdetector.app
```

**What to check:**

- Exception type (e.g., `NullPointerException`, `ClassNotFoundException`)
- Stack trace showing which file/line crashed
- "Caused by:" section showing the root cause

---

### 2. App Initialization Logs

Look for `APP_INIT` tag logs in sequence:

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] ✓ Timber initialized
[APP_INIT] Starting Koin initialization...
[APP_INIT] ✓ Koin initialized successfully
[APP_INIT] ✓ DriftDetectorApp base initialization complete
[APP_INIT] === APP STARTUP COMPLETE ===
```

**If any step shows ✗ (red X), that's where the crash happened!**

---

### 3. Dependency Injection (Koin) Logs

Look for `KOIN` tag logs:

```
[KOIN] Loading databaseModule...
[KOIN] Creating encrypted database...
[KOIN] ✓ Database created successfully
[KOIN] Loading networkModule...
[KOIN] ✓ OkHttpClient created
[KOIN] Loading viewModelModule...
```

**Common issues:**

- Database creation failure → Check storage permissions or SQLCipher compatibility
- ViewModel creation failure → Check repository dependencies
- Missing dependencies → Check if all DAOs are created

---

### 4. Activity Lifecycle Logs

Look for `ACTIVITY` tag logs:

```
[ACTIVITY] === MainActivity.onCreate() START ===
[ACTIVITY] ✓ super.onCreate() completed
[ACTIVITY] ✓ setContent block entered
[ACTIVITY] ✓ DriftDetectorTheme applied
[ACTIVITY] === MainActivity.onCreate() COMPLETE ===
[ACTIVITY] MainActivity.onStart()
[ACTIVITY] MainActivity.onResume()
```

**If crash happens here, it's likely a Compose UI issue**

---

## Common Crash Scenarios & Solutions

### Scenario 1: Crash Before Any Logs Appear

**Symptoms:** App closes immediately, no `APP_INIT` logs

**Likely cause:** Application class not registered or manifest issue

**Solution:**

```powershell
# Check manifest
Get-Content app/src/main/AndroidManifest.xml | Select-String "android:name"

# Should show: android:name=".DriftDetectorApp"
```

---

### Scenario 2: Crash During Koin Initialization

**Symptoms:** Logs show `Starting Koin initialization...` but then crash

**Common errors:**

#### Error: `No definition found for...`

```
org.koin.core.error.NoBeanDefFoundException: 
No definition found for class 'X'
```

**Cause:** Missing dependency in Koin modules

**Solution:** Check `AppModule.kt` - ensure all dependencies are defined

#### Error: Database creation failed

```
[KOIN] ✗ Database creation FAILED
net.sqlcipher.database.SQLiteException
```

**Cause:** SQLCipher not compatible with device or architecture issue

**Solution:** Try without encryption temporarily:

```kotlin
// In AppModule.kt, comment out encryption temporarily
Room.databaseBuilder(...)
    // .openHelperFactory(factory)  // Comment this out
    .build()
```

---

### Scenario 3: Crash During ViewModel Creation

**Symptoms:** Koin succeeds, but crash when screen loads

**Error pattern:**

```
[KOIN] ✗ DriftDashboardViewModel creation FAILED
```

**Cause:** ViewModel constructor requires dependency that failed to initialize

**Solution:** Check which dependency is missing in the logs above the error

---

### Scenario 4: Crash in Compose UI

**Symptoms:** Activity starts, but crash during composition

**Error pattern:**

```
[ACTIVITY] ✓ setContent block entered
[CRASH] FATAL EXCEPTION: main
java.lang.IllegalStateException: ...
```

**Common causes:**

- Missing ViewModel
- Null state access
- Invalid Compose structure

**Solution:** Check the screen being loaded (Dashboard/Models/Patches)

---

## Advanced Debugging

### View Crash Logs Saved on Device

The app now saves crash logs directly on the device:

```powershell
# List crash logs
adb shell ls /data/data/com.driftdetector.app/files/

# View crash log
adb shell cat /data/data/com.driftdetector.app/files/crash_*.log

# View initialization log
adb shell cat /data/data/com.driftdetector.app/files/app_init.log

# View Timber log (detailed)
adb shell cat /data/data/com.driftdetector.app/files/timber.log
```

### Pull All Logs to PC

```powershell
# Pull all logs at once
adb pull /data/data/com.driftdetector.app/files/ ./device_logs/
```

### Real-Time Log Monitoring

Monitor specific tags in real-time:

```powershell
# Monitor app initialization
adb logcat -s APP_INIT:D

# Monitor Koin DI
adb logcat -s KOIN:D

# Monitor crashes only
adb logcat -s AndroidRuntime:E

# Monitor everything from our app
adb logcat -s APP_INIT:D ACTIVITY:D KOIN:D CRASH:E AndroidRuntime:E
```

---

## Testing Specific Components

### Test Database Only

```kotlin
// In DriftDetectorApp.kt, comment out everything except database
try {
    val db = get<DriftDatabase>()
    Log.d("TEST", "✓ Database accessible")
} catch (e: Exception) {
    Log.e("TEST", "✗ Database failed", e)
}
```

### Test Without AI Engine

```kotlin
// In DriftDetectorApp.kt, comment out AI initialization
// applicationScope.launch {
//     aiEngine.initialize()
// }
```

### Test Screens Individually

Change MainActivity to start with specific screen:

```kotlin
// In MainActivity.kt
NavHost(
    navController = navController,
    startDestination = Screen.Models.route,  // Test Models screen first
    modifier = Modifier.padding(innerPadding)
) { ... }
```

---

## Minimal Test Build

If the app still crashes, try a minimal version:

### 1. Disable Encryption

In `AppModule.kt`:

```kotlin
Room.databaseBuilder(
    androidContext(),
    DriftDatabase::class.java,
    DriftDatabase.DATABASE_NAME
)
    // .openHelperFactory(factory)  // Comment out
    .fallbackToDestructiveMigration()
    .build()
```

### 2. Disable WorkManager

In `AppModule.kt`, comment out:

```kotlin
// val workManagerModule = module { ... }

// In appModules list:
val appModules = listOf(
    databaseModule,
    networkModule,
    securityModule,
    coreModule,
    repositoryModule,
    viewModelModule,
    // workManagerModule  // Comment out
)
```

### 3. Disable AI Engine

In `DriftDetectorApp.kt`:

```kotlin
// Comment out AI initialization
// applicationScope.launch {
//     aiEngine.initialize()
// }
```

---

## Getting Help

### Sharing Crash Information

If you need help, share:

1. **Full crash log** from `logs/crash_debug_[timestamp].log`
2. **Device info:**
   ```powershell
   adb shell getprop ro.build.version.release  # Android version
   adb shell getprop ro.build.version.sdk       # API level
   adb shell getprop ro.product.cpu.abi         # Architecture
   ```
3. **The specific error message** from the crash log
4. **Which initialization step failed** (check APP_INIT logs)

### Common Info to Include

```powershell
# Run this to get device info
Write-Host "Android Version:" (adb shell getprop ro.build.version.release)
Write-Host "API Level:" (adb shell getprop ro.build.version.sdk)
Write-Host "Architecture:" (adb shell getprop ro.product.cpu.abi)
Write-Host "Device:" (adb shell getprop ro.product.model)
Write-Host "Manufacturer:" (adb shell getprop ro.product.manufacturer)
```

---

## Next Steps

1. **Run the debug script:**
   ```powershell
   .\debug_crash.ps1
   ```

2. **Wait for app to crash (or succeed!)**

3. **Check the log file** in `logs/crash_debug_[timestamp].log`

4. **Look for:**
    - ✗ (failed steps) in APP_INIT logs
    - ✗ (failed steps) in KOIN logs
    - FATAL EXCEPTION in crash logs

5. **Share the relevant section** of the log showing the error

---

## Enhanced Logging Features

The app now has:

- ✅ **Uncaught exception handler** - Catches all crashes
- ✅ **Step-by-step initialization logging** - Shows exactly where startup fails
- ✅ **Dependency injection logging** - Shows which Koin modules load
- ✅ **Activity lifecycle logging** - Tracks activity state
- ✅ **File logging** - Saves logs to device for later retrieval
- ✅ **Crash log files** - Automatically saves crash details with stack traces
- ✅ **Timestamp tracking** - Precise timing of each initialization step

**The app will tell us exactly where it crashes!**

---

Made with 🔍 for comprehensive debugging
