# 🚀 START HERE - Debugging the Drift Detector App

## Your app is crashing? Let's fix it!

This guide will help you quickly diagnose and fix the issue.

---

## ⚡ Quick Actions

### 🔴 App Crashes Immediately

**Run this command:**

```powershell
.\debug_crash.ps1
```

This will rebuild, install, and capture detailed crash logs automatically.

Then jump to: **[What to Look For](#what-to-look-for-in-output)**

---

### 🟡 App Already Crashed - Need Logs

**Run this command:**

```powershell
.\capture_logs.ps1
```

This will grab all logs and show you what failed.

Then jump to: **[What to Look For](#what-to-look-for-in-output)**

---

### 🟢 Want to Understand the Debug Tools

Read: **[DEBUGGING_SUMMARY.md](DEBUGGING_SUMMARY.md)**

This explains everything we've added and how it works.

---

## 📚 Documentation Guide

### For Quick Reference

→ **[QUICK_DEBUG.md](QUICK_DEBUG.md)**

- One-page cheat sheet
- Essential commands
- Common patterns
- Quick fixes

### For Step-by-Step Help

→ **[DEBUG_GUIDE.md](DEBUG_GUIDE.md)**

- Comprehensive debugging guide
- How to read logs
- Common crash scenarios
- Advanced debugging techniques
- Testing strategies

### For Understanding What We Built

→ **[DEBUGGING_SUMMARY.md](DEBUGGING_SUMMARY.md)**

- What debugging features were added
- How the logging system works
- What the scripts do
- Log file structure

### For Known Issues

→ **[CRASH_FIX.md](CRASH_FIX.md)**

- Common crash causes
- Quick fixes
- Code-level solutions

---

## 🎯 What to Look For in Output

After running `debug_crash.ps1` or `capture_logs.ps1`, you'll see color-coded output:

### ✅ Success (Green)

```
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] ✓ Timber initialized
[KOIN] ✓ Database created successfully
[ACTIVITY] ✓ setContent completed
```

→ **These steps succeeded**

### ❌ Failure (Red)

```
[KOIN] ✗ Database creation FAILED
[APP_INIT] ✗ Koin initialization FAILED
```

→ **This is where it crashed!**

### ⚠️ Warning (Yellow)

```
⚠️ RunAnywhere SDK not available
```

→ **Non-critical warnings**

---

## 🔍 Quick Diagnosis

### Scenario 1: Crash in First Few Seconds

**Symptoms:** App closes right away

**Look for:** `APP_INIT` or `KOIN` logs with ✗ markers

**Most likely:**

- Database initialization failure
- Missing dependency
- Koin configuration error

**Jump to:
** [DEBUG_GUIDE.md - Scenario 2](DEBUG_GUIDE.md#scenario-2-crash-during-koin-initialization)

---

### Scenario 2: App Starts Then Crashes

**Symptoms:** App window appears briefly, then closes

**Look for:** `ACTIVITY` logs showing ✓ then a crash

**Most likely:**

- ViewModel creation failure
- Compose UI error
- Missing screen dependency

**Jump to:
** [DEBUG_GUIDE.md - Scenario 3](DEBUG_GUIDE.md#scenario-3-crash-during-viewmodel-creation)

---

### Scenario 3: No Logs Appear

**Symptoms:** App crashes but no custom logs

**Most likely:**

- App not actually running our code
- Manifest issue
- Build issue

**Jump to:** [DEBUG_GUIDE.md - Scenario 1](DEBUG_GUIDE.md#scenario-1-crash-before-any-logs-appear)

---

## 🛠️ The Two Main Scripts

### `debug_crash.ps1` - Full Debug Workflow

**When to use:** First time debugging, or after making code changes

**What it does:**

1. ✓ Cleans and rebuilds app
2. ✓ Uninstalls old version
3. ✓ Installs fresh build
4. ✓ Launches app
5. ✓ Monitors logs live
6. ✓ Saves everything to file

**Run time:** ~2-3 minutes (includes build)

---

### `capture_logs.ps1` - Quick Log Grab

**When to use:** App already installed and crashed

**What it does:**

1. ✓ Grabs all logcat history
2. ✓ Extracts crash info
3. ✓ Shows relevant logs
4. ✓ Pulls device log files
5. ✓ Analyzes patterns

**Run time:** ~10-30 seconds

---

## 📁 Where Logs Are Saved

### On Your PC

All logs saved to `logs/` directory:

- `crash_debug_[timestamp].log` - From debug_crash.ps1
- `logcat_[timestamp].log` - From capture_logs.ps1
- Device log files pulled by capture_logs.ps1

### On Device

App saves logs to:

- `crash_[timestamp].log` - Crash details
- `app_init.log` - Startup sequence
- `timber.log` - All Timber logs

Access with:

```powershell
adb shell cat /data/data/com.driftdetector.app/files/crash_*.log
```

---

## 💡 Pro Tips

### Tip 1: Use debug_crash.ps1 for Clean Slate

If you're not sure what's wrong, start with a clean build:

```powershell
.\debug_crash.ps1
```

### Tip 2: Use capture_logs.ps1 for Quick Checks

If you just want to see what happened:

```powershell
.\capture_logs.ps1
```

### Tip 3: Look for ✗ Markers

The red ✗ markers pinpoint exactly where the crash occurred.

### Tip 4: Read From Top to Bottom

Logs show the sequence - the first ✗ is usually the root cause.

### Tip 5: Check Device Compatibility

```powershell
adb shell getprop ro.build.version.sdk  # Should be 26 or higher
```

---

## 🎓 Learning Path

### Level 1: Just Fix It

1. Run `.\debug_crash.ps1`
2. Look for ✗ markers
3. Check [QUICK_DEBUG.md](QUICK_DEBUG.md) for quick fixes

### Level 2: Understand It

1. Read [DEBUGGING_SUMMARY.md](DEBUGGING_SUMMARY.md)
2. Learn what each log tag means
3. Understand the initialization sequence

### Level 3: Debug Like a Pro

1. Read [DEBUG_GUIDE.md](DEBUG_GUIDE.md) fully
2. Learn advanced debugging techniques
3. Use manual logcat commands
4. Test individual components

---

## 🆘 Still Stuck?

### Before Asking for Help

1. **Run the log capture:**
   ```powershell
   .\capture_logs.ps1
   ```

2. **Open the log file:**
   ```powershell
   notepad logs\logcat_[latest].log
   ```

3. **Find the error section** (look for ✗ or FATAL EXCEPTION)

4. **Share that section** along with:
    - Device Android version
    - Device architecture
    - What you see happening

### Include This Info

```powershell
# Run these commands and share output
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.build.version.sdk      # API level  
adb shell getprop ro.product.cpu.abi        # Architecture
```

---

## ✅ Success Checklist

When the app works, you should see:

```
[APP_INIT] === APP STARTUP BEGIN ===
[APP_INIT] ✓ super.onCreate() completed
[APP_INIT] ✓ Timber initialized
[APP_INIT] ✓ Koin initialized successfully
[APP_INIT] === APP STARTUP COMPLETE ===
[ACTIVITY] === MainActivity.onCreate() START ===
[ACTIVITY] ✓ setContent completed
[ACTIVITY] === MainActivity.onCreate() COMPLETE ===
[ACTIVITY] MainActivity.onResume()
```

All ✓ markers, no ✗ markers, no FATAL EXCEPTION!

---

## 📞 Next Steps

**Choose your path:**

- 🔴 **App is crashing now** → Run `.\debug_crash.ps1`
- 🟡 **App crashed earlier** → Run `.\capture_logs.ps1`
- 📖 **Want to learn** → Read [DEBUGGING_SUMMARY.md](DEBUGGING_SUMMARY.md)
- ⚡ **Need quick ref** → Check [QUICK_DEBUG.md](QUICK_DEBUG.md)
- 🔍 **Deep dive** → Read [DEBUG_GUIDE.md](DEBUG_GUIDE.md)

---

Made with 💡 to get you debugging quickly!
