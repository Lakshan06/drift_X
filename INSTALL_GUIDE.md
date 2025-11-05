# 📱 Quick Installation Guide

## 🔴 CRITICAL: You MUST Uninstall First!

The new version uses a different database format. **You MUST completely uninstall the old version**
before installing the new one, or the app will crash with database corruption errors.

---

## ⚡ Quick Install (ADB)

```bash
# 1. Uninstall old version (REQUIRED!)
adb uninstall com.driftdetector.app

# 2. Install new version
adb install C:\drift_X\app\build\outputs\apk\debug\app-debug.apk

# 3. Launch app
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
```

---

## 📋 Manual Install (No ADB)

### Step 1: Uninstall Old Version ⚠️ CRITICAL

1. Open **Settings** on your Android device
2. Go to **Apps** (or **Applications**)
3. Find and tap **DriftGuardAI**
4. Tap **Uninstall**
5. When prompted, **make sure to delete app data**
6. Confirm uninstall

### Step 2: Install New Version

1. Copy `C:\drift_X\app\build\outputs\apk\debug\app-debug.apk` to your phone
2. Open **File Manager** on your phone
3. Navigate to where you copied the APK
4. Tap the **app-debug.apk** file
5. If prompted, enable **"Install from unknown sources"**
6. Tap **Install**
7. Wait for installation to complete

### Step 3: Launch

1. Find **DriftGuardAI** in your app drawer
2. Tap to open
3. You should see the Dashboard screen
4. ✅ Done!

---

## ✅ Quick Test

After installation, verify everything works:

1. **Launch test:** App opens without crashing ✓
2. **Navigation test:** All 4 tabs work (Dashboard, Models, Patches, AI) ✓
3. **Upload test:** Tap upload icon in Models tab ✓
4. **AI test:** Type "Hi" in AI Assistant tab ✓

---

## 🐛 Troubleshooting

### App crashes on launch?

You probably didn't uninstall the old version first. Fix:

```bash
# Complete uninstall
adb uninstall com.driftdetector.app

# Clear any remaining data
adb shell rm -rf /data/data/com.driftdetector.app

# Reinstall
adb install C:\drift_X\app\build\outputs\apk\debug\app-debug.apk
```

### Database corruption error?

Same issue - old database file is still there. Follow the steps above.

### Can't find the APK file?

The APK is located at:

```
C:\drift_X\app\build\outputs\apk\debug\app-debug.apk
```

---

## 📝 What Changed?

- ✅ Fixed all crashes
- ✅ Removed deprecated encryption library
- ✅ Added automatic database migration
- ✅ Modern Android APIs
- ⚠️ Your old data will be reset (necessary for migration)

---

## 📞 Need Help?

If you're still having issues:

1. Check the logs:
   ```bash
   adb logcat | grep -E "driftdetector|CRASH|Error"
   ```

2. See the full fix documentation: `FINAL_CRASH_FIX.md`

---

**Build Date:** 2025-11-05  
**APK Location:** `C:\drift_X\app\build\outputs\apk\debug\app-debug.apk`  
**Status:** ✅ Ready to Install
