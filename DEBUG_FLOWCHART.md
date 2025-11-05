# 🔀 Debug Flowchart

## Start Here → Find Your Path → Get Solution

```
                    ┌──────────────────────────┐
                    │  App Crashes on Startup  │
                    └────────────┬─────────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │  Have you run debug      │
                    │  scripts yet?            │
                    └────────┬────────┬────────┘
                             │        │
                         NO  │        │  YES
                             │        │
                    ┌────────▼────┐   │
                    │             │   │
                    │  Run:       │   │
                    │  .\debug_   │   │
                    │  crash.ps1  │   │
                    │             │   │
                    └──────┬──────┘   │
                           │          │
                           └──────┬───┘
                                  │
                                  ▼
                    ┌──────────────────────────┐
                    │  Check Console Output    │
                    │  Look for ✗ markers      │
                    └────────┬────────┬────────┘
                             │        │
                        ✓ Only│        │ ✗ Found
                             │        │
          ┌──────────────────┘        └─────────────────┐
          │                                              │
          ▼                                              ▼
┌─────────────────────┐                   ┌──────────────────────────┐
│  APP WORKS!         │                   │  Which component failed? │
│  ✅ Success         │                   └───────────┬──────────────┘
└─────────────────────┘                               │
                                         ┌─────────────┼─────────────┐
                                         │             │             │
                                         ▼             ▼             ▼
                              ┌──────────────┐ ┌─────────────┐ ┌────────────┐
                              │  [APP_INIT]  │ │   [KOIN]    │ │ [ACTIVITY] │
                              │     ✗        │ │     ✗       │ │     ✗      │
                              └──────┬───────┘ └──────┬──────┘ └─────┬──────┘
                                     │                │               │
                                     ▼                ▼               ▼
                              ┌──────────────┐ ┌─────────────┐ ┌────────────┐
                              │ Manifest or  │ │  Database,  │ │  ViewModel │
                              │ App class    │ │  Network,   │ │  or Compose│
                              │ issue        │ │  or Module  │ │  UI issue  │
                              └──────┬───────┘ └──────┬──────┘ └─────┬──────┘
                                     │                │               │
                                     ▼                ▼               ▼
                              ┌──────────────┐ ┌─────────────┐ ┌────────────┐
                              │ See Scenario │ │  See        │ │  See       │
                              │ 1 in         │ │  Scenario 2 │ │  Scenario  │
                              │ DEBUG_GUIDE  │ │  in DEBUG_  │ │  3 or 4 in │
                              │              │ │  GUIDE      │ │  DEBUG_    │
                              │              │ │             │ │  GUIDE     │
                              └──────────────┘ └─────────────┘ └────────────┘
```

---

## Decision Tree

### 1️⃣ Do you see any logs?

**YES** → Go to step 2

**NO** → Likely manifest issue

- Check: `app/src/main/AndroidManifest.xml`
- Look for: `android:name=".DriftDetectorApp"`
- See: [DEBUG_GUIDE.md - Scenario 1](DEBUG_GUIDE.md#scenario-1-crash-before-any-logs-appear)

---

### 2️⃣ Where does it fail?

**APP_INIT ✗** → Application class issue

- Problem: App startup itself is failing
- Common: Manifest not configured, app class error
- See: [DEBUG_GUIDE.md - Scenario 1](DEBUG_GUIDE.md#scenario-1-crash-before-any-logs-appear)

**KOIN ✗** → Dependency injection issue

- Problem: Koin module initialization failing
- Common: Database, network, or dependency error
- See: [DEBUG_GUIDE.md - Scenario 2](DEBUG_GUIDE.md#scenario-2-crash-during-koin-initialization)

**ACTIVITY ✗** → UI/ViewModel issue

- Problem: Activity or Compose UI failing
- Common: ViewModel missing, Compose error
- See: [DEBUG_GUIDE.md - Scenario 3/4](DEBUG_GUIDE.md#scenario-3-crash-during-viewmodel-creation)

---

## Quick Symptom Matcher

### 😵 App closes instantly (< 1 second)

```
Likely: APP_INIT failure
Action: Run .\debug_crash.ps1
Look for: First ✗ in APP_INIT logs
```

### 🔄 App shows splash then closes

```
Likely: KOIN initialization failure
Action: Check KOIN logs for database/network errors
Fix: May need to disable encryption or check network setup
```

### 🖼️ App UI appears briefly then crashes

```
Likely: ViewModel or Compose issue
Action: Check ACTIVITY and KOIN logs
Fix: Ensure ViewModels properly created
```

### 🐌 App hangs then crashes

```
Likely: Blocking operation on main thread
Action: Check logs for timeout or ANR
Fix: Move heavy work to background thread
```

---

## Log Pattern Recognition

### Pattern A: Clean Success

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] ✓ Timber initialized
[KOIN] Loading databaseModule...
[KOIN] ✓ Database created successfully
[APP_INIT] ✓ Koin initialized successfully
[ACTIVITY] === MainActivity.onCreate() START ===
[ACTIVITY] ✓ setContent completed
```

**Result:** ✅ App works!

---

### Pattern B: Database Failure

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[KOIN] Loading databaseModule...
[KOIN] Creating encrypted database...
[KOIN] ✗ Database creation FAILED           ← PROBLEM
net.sqlcipher.database.SQLiteException
```

**Solution:** Database encryption issue

- Try without encryption (comment out in AppModule.kt)
- Check device architecture compatibility
- See: DEBUG_GUIDE.md Database section

---

### Pattern C: ViewModel Failure

```
[KOIN] ✓ Database created successfully
[KOIN] Loading viewModelModule...
[KOIN] Creating DriftDashboardViewModel...
[KOIN] ✗ DriftDashboardViewModel creation FAILED   ← PROBLEM
org.koin.core.error.NoBeanDefFoundException
```

**Solution:** Missing dependency

- Check what DriftDashboardViewModel needs
- Ensure repository is created first
- See: DEBUG_GUIDE.md ViewModel section

---

### Pattern D: Compose UI Failure

```
[ACTIVITY] === MainActivity.onCreate() START ===
[ACTIVITY] ✓ super.onCreate() completed
[ACTIVITY] ✓ setContent block entered
[CRASH] FATAL EXCEPTION: main             ← PROBLEM
IllegalStateException: ViewModelProvider...
```

**Solution:** ViewModel injection issue

- Check Compose screen ViewModels
- Ensure Koin properly set up
- See: DEBUG_GUIDE.md Compose section

---

## Quick Command Reference

### Get Crash Logs

```powershell
.\capture_logs.ps1
```

### Full Debug Session

```powershell
.\debug_crash.ps1
```

### Manual Log Check

```powershell
adb logcat -d | Select-String "APP_INIT|KOIN|ACTIVITY|CRASH"
```

### View Device Logs

```powershell
adb shell cat /data/data/com.driftdetector.app/files/crash_*.log
```

---

## Priority Fixes

### Fix 1: Clean Reinstall

```powershell
adb uninstall com.driftdetector.app
.\build.ps1 clean assembleDebug installDebug
```

**When:** Always try this first

---

### Fix 2: Disable Encryption

Edit `app/src/main/java/com/driftdetector/app/di/AppModule.kt`:

```kotlin
Room.databaseBuilder(...)
    // .openHelperFactory(factory)  // Comment this line
    .build()
```

**When:** Database creation fails

---

### Fix 3: Check Device

```powershell
adb shell getprop ro.build.version.sdk
```

**When:** Should be 26 or higher (Android 8.0+)

---

## Documentation Map

```
START_HERE.md (You are here!)
    │
    ├─→ QUICK_DEBUG.md
    │   └─→ Fast commands, no explanation
    │
    ├─→ DEBUG_GUIDE.md
    │   ├─→ Scenario 1: No logs
    │   ├─→ Scenario 2: Koin failure
    │   ├─→ Scenario 3: ViewModel failure
    │   └─→ Scenario 4: Compose failure
    │
    ├─→ DEBUGGING_SUMMARY.md
    │   └─→ What was built, how it works
    │
    └─→ CRASH_FIX.md
        └─→ Known issues and solutions
```

---

## Success Indicators

### ✅ You're on the right track when:

- You see ✓ markers in the logs
- `[APP_INIT] === APP STARTUP COMPLETE ===` appears
- `MainActivity.onResume()` is reached
- App UI is visible and responsive

### ❌ Something's wrong when:

- You see ✗ markers
- `FATAL EXCEPTION` appears
- App closes without reaching MainActivity
- Logs stop mid-initialization

---

## Next Action

**Based on your situation:**

1. **App crashes right now?**
   ```powershell
   .\debug_crash.ps1
   ```

2. **Need to understand logs?**
    - Read: [DEBUG_GUIDE.md](DEBUG_GUIDE.md)

3. **Know the error, need fix?**
    - Check: [CRASH_FIX.md](CRASH_FIX.md)

4. **Want quick commands?**
    - See: [QUICK_DEBUG.md](QUICK_DEBUG.md)

---

Made with 🔀 for easy navigation!
