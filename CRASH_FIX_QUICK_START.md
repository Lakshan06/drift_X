# 🚨 App Crash - Quick Fix Guide

## ✅ FIXED: Missing SciChart License

**I've already added the missing resource for you!**

The app was crashing because it was trying to access `R.string.sciChart_license` which didn't exist.

## What I Did

Added this to `app/src/main/res/values/strings.xml`:

```xml
<!-- SciChart License Key -->
<string name="sciChart_license"></string>
```

**Note:** Empty string = Trial mode with watermark. This is fine for development.

---

## 🔧 Steps to Run the App Now

### 1. Clean and Rebuild

```bash
# In Android Studio Terminal
./gradlew clean
./gradlew build
```

Or in Android Studio: **Build → Clean Project**, then **Build → Rebuild Project**

### 2. Install on Device

```bash
./gradlew installDebug
```

Or click the **Run** button (green play icon) in Android Studio.

### 3. Launch the App

The app should now launch successfully!

---

## 📊 If Still Crashing

### Quick Check: View Logcat

In Android Studio:

1. Click **Logcat** tab at bottom
2. Select your device
3. Filter: `package:com.driftdetector.app`
4. Look for red error messages

### Get Crash Details

The app saves crash logs automatically:

```bash
# Pull crash logs from device
adb pull /data/data/com.driftdetector.app/files/crash_*.log

# View initialization log
adb pull /data/data/com.driftdetector.app/files/app_init.log
```

These files will tell you EXACTLY what went wrong.

---

## 🔍 Common Issues After This Fix

### Issue 1: Database Error

**Error:** "SQLite error" or "database is locked"

**Fix:**

```bash
# Clear app data
adb shell pm clear com.driftdetector.app

# Reinstall
./gradlew installDebug
```

### Issue 2: Koin/DI Error

**Error:** "No definition found" or "Koin not started"

**Fix:** Check `app_init.log` to see which dependency failed

```bash
adb pull /data/data/com.driftdetector.app/files/app_init.log
cat app_init.log
```

Look for `✗` marks showing what failed.

### Issue 3: Out of Memory

**Error:** "OutOfMemoryError" or "Failed to allocate"

**Fix:** Already handled in manifest with `largeHeap="true"`. If still occurring:

- Use a device with more RAM
- Reduce model sizes
- Close other apps

---

## 🎯 Next Steps

### If App Launches Successfully

1. ✅ Dashboard should load
2. ✅ Try uploading a model (Settings → Model Management → Upload)
3. ✅ Check the Dashboard for drift monitoring
4. ✅ Explore other features

### If App Still Crashes

1. **Capture the crash:**
   ```bash
   adb logcat -v time > crash_log.txt
   # (Launch app and let it crash)
   # Then Ctrl+C to stop logging
   ```

2. **Pull app logs:**
   ```bash
   adb pull /data/data/com.driftdetector.app/files/ ./app_logs/
   ```

3. **Share these files:**
    - `crash_log.txt` (from logcat)
    - `app_init.log` (initialization steps)
    - `crash_*.log` (detailed crash info)

---

## 📚 More Information

See `CRASH_DIAGNOSIS_AND_FIX.md` for:

- Detailed crash investigation steps
- All possible crash causes
- Emergency fallback options
- How to get help

---

## ⚡ Quick Commands Reference

```bash
# Clean build
./gradlew clean

# Rebuild
./gradlew build

# Install
./gradlew installDebug

# Clear app data
adb shell pm clear com.driftdetector.app

# View errors in logcat
adb logcat -v time *:E

# Pull crash logs
adb pull /data/data/com.driftdetector.app/files/ ./logs/
```

---

## ✅ Summary

- **Critical fix applied:** Added missing `sciChart_license` resource
- **Action needed:** Clean and rebuild the project
- **Expected result:** App should launch successfully
- **If still crashing:** Check the logs and follow diagnosis guide

Good luck! 🚀
