# 🚀 Quick Debug Reference

## Run Debug Session (One Command)

```powershell
.\debug_crash.ps1
```

This rebuilds, installs, launches, and captures everything!

---

## Capture Logs After Crash

```powershell
.\capture_logs.ps1
```

This grabs all logs from the device and shows crash info.

---

## Manual Commands

### Get Logs

```powershell
# Capture current logcat
adb logcat -d > logs/manual_log.txt

# Live monitoring (filtered)
adb logcat -s APP_INIT:D ACTIVITY:D KOIN:D AndroidRuntime:E
```

### View Device Logs

```powershell
# View crash logs
adb shell cat /data/data/com.driftdetector.app/files/crash_*.log

# View init log
adb shell cat /data/data/com.driftdetector.app/files/app_init.log
```

### Pull Device Logs

```powershell
adb pull /data/data/com.driftdetector.app/files/ ./device_logs/
```

---

## What to Look For

### ✅ Success Pattern

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] ✓ Timber initialized
[APP_INIT] ✓ Koin initialized successfully
[APP_INIT] === APP STARTUP COMPLETE ===
[ACTIVITY] === MainActivity.onCreate() START ===
[ACTIVITY] ✓ setContent completed
[ACTIVITY] === MainActivity.onCreate() COMPLETE ===
```

### ❌ Crash Pattern

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] Starting Koin initialization...
[KOIN] Loading databaseModule...
[KOIN] ✗ Database creation FAILED    ← PROBLEM HERE!
FATAL EXCEPTION: main
```

---

## Rebuild & Test

```powershell
# Clean build
.\build.ps1 clean assembleDebug

# Uninstall old
adb uninstall com.driftdetector.app

# Install new
.\build.ps1 installDebug

# Launch
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
```

---

## Device Info

```powershell
# Quick device check
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.build.version.sdk      # API level
adb shell getprop ro.product.cpu.abi        # Architecture
```

---

## Common Fixes

### Fix 1: Clean State

```powershell
adb uninstall com.driftdetector.app
.\build.ps1 clean assembleDebug installDebug
```

### Fix 2: Clear App Data

```powershell
adb shell pm clear com.driftdetector.app
```

### Fix 3: Check Manifest

```powershell
Get-Content app/src/main/AndroidManifest.xml | Select-String "android:name"
# Should show: android:name=".DriftDetectorApp"
```

---

## Share for Help

If asking for help, run this:

```powershell
.\capture_logs.ps1

# Then share the file:
# logs/logcat_[timestamp].log
```

Look for the section with ✗ or FATAL EXCEPTION

---

Made with ⚡ for quick debugging
