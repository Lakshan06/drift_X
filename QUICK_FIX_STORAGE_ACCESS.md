# 🚀 Quick Fix: Emulator Can't Load Google Drive Files

## ✅ The Problem is Fixed!

Your app couldn't access files because it was missing **storage permissions**. This has been fixed!

## 📦 What Changed

1. ✅ **Added storage permissions** to `AndroidManifest.xml`
2. ✅ **Created PermissionHelper** utility for automatic permission requests
3. ✅ **Updated MainActivity** to request permissions on launch

## 🎯 Quick Install (Option 1)

**Easiest way - Using the install script:**

1. **Run the install script:**
   ```bash
   install_with_permissions.bat
   ```

This will:

- Install the updated app
- Grant all storage permissions automatically
- Verify everything works

## 🔨 Manual Install (Option 2)

**Using Android Studio:**

1. Click **Run** ▶️ button
2. When app launches, **grant storage permissions** when prompted
3. Done!

## 🧪 Testing

After installing:

1. **Open DriftGuardAI** on emulator
2. **Grant permissions** when asked (important!)
3. Go to **Model Upload** screen
4. Click **Local Files**
5. You should now see multiple folders! ✅

### What You Should See

✅ **Downloads** folder visible  
✅ **Documents** folder visible  
✅ **Google Drive** files (if synced)  
✅ File picker shows all accessible locations

## 📁 Accessing Google Drive Files

### Option A: Through Google Drive App

1. Open **Google Drive** app on emulator
2. Find your files
3. Tap **⋮** (three dots) → **Make available offline**
4. Files will be in: `/storage/emulated/0/Documents/`

### Option B: Direct Upload to Emulator

```bash
# Push files directly to emulator
adb push your-model.tflite /sdcard/Download/model.tflite
adb push your-data.csv /sdcard/Download/data.csv
```

Then browse to **Downloads** in the app!

### Option C: Android Studio Device File Explorer

1. **View → Tool Windows → Device File Explorer**
2. Navigate to `/sdcard/Download/`
3. Right-click → **Upload** your files
4. Access them in the app

## ⚠️ If Permissions Aren't Working

### Quick Permission Grant (via ADB)

Run these commands to grant permissions manually:

```bash
# Navigate to Android SDK platform-tools, or add to PATH
cd %LOCALAPPDATA%\Android\Sdk\platform-tools

# Grant all permissions at once
adb shell pm grant com.driftdetector.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_IMAGES
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_AUDIO
adb shell pm grant com.driftdetector.app android.permission.POST_NOTIFICATIONS
```

### Enable "All Files Access" (Android 11+)

1. Open **Settings** on emulator
2. Go to **Apps** → **DriftGuardAI**
3. Tap **Permissions**
4. Tap **Files and media**
5. Select **"Allow management of all files"**

## 🔍 Verify Permissions

### Check in Logcat

```bash
adb logcat | findstr "PermissionHelper"
```

Should see:

```
✓ All storage permissions granted
✓ Has MANAGE_EXTERNAL_STORAGE permission
```

### Check in Settings

Emulator → Settings → Apps → DriftGuardAI → Permissions  
Should show **"Allowed"** for Storage/Files

## 🎉 You're Done!

The app should now work perfectly with file access!

**Test it:**

1. Open app
2. Go to **Model Upload**
3. Click **Local Files**
4. Browse and select files
5. Upload works! ✅

---

## 📖 Full Documentation

For detailed information, see:

- **`STORAGE_PERMISSIONS_GUIDE.md`** - Complete guide
- **`MODEL_UPLOAD_FEATURE_SUMMARY.md`** - How to upload files
- **`UPLOAD_ONNX_MODELS_GUIDE.md`** - ONNX model uploading

## 🆘 Still Having Issues?

1. **Uninstall and reinstall:**
   ```bash
   adb uninstall com.driftdetector.app
   install_with_permissions.bat
   ```

2. **Check emulator is running:**
   ```bash
   adb devices
   ```

3. **View permission logs:**
   ```bash
   adb logcat | findstr "Permission"
   ```

4. **Reset permissions:**
   ```bash
   adb shell pm reset-permissions com.driftdetector.app
   ```

Need more help? Check the full guide: `STORAGE_PERMISSIONS_GUIDE.md`
