# 📁 Storage Permissions Guide - Fix for Emulator File Access

## 🔍 Problem

Your Android emulator couldn't load files from Google Drive or device storage because the app was
missing proper **storage permissions**.

## ✅ What Was Fixed

### 1. **Added Storage Permissions to Manifest**

Updated `AndroidManifest.xml` with permissions for all Android versions:

```xml
<!-- For Android 12 and below -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />

<!-- For Android 13+ (Granular media permissions) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- For unrestricted file access (optional) -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

### 2. **Created PermissionHelper Utility**

New file: `app/src/main/java/com/driftdetector/app/core/util/PermissionHelper.kt`

Features:

- ✅ **Version-aware**: Requests the right permissions for each Android version
- ✅ **Auto-detection**: Checks if permissions are already granted
- ✅ **User-friendly**: Provides clear explanations for why permissions are needed
- ✅ **Fallback handling**: Falls back to standard permissions if full access is denied

### 3. **Updated MainActivity**

The app now:

- ✅ Requests storage permissions on first launch
- ✅ Handles different Android versions automatically
- ✅ Provides helpful messages if permissions are denied
- ✅ Falls back to alternative permission requests

## 🚀 How to Use

### Step 1: Rebuild the App

```bash
# Clean and rebuild
./gradlew clean assembleDebug

# Install on emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or in **Android Studio**: Click **Run** ▶️

### Step 2: Grant Permissions on First Launch

When you open the app, you'll see permission requests:

#### Option A: Full Storage Access (Recommended)

- Android will open **Settings** automatically
- Enable "**Allow access to manage all files**"
- ✅ This gives full access to all storage locations

#### Option B: Standard Permissions

- Android shows a permission dialog
- Tap "**Allow**" or "**While using the app**"
- ✅ This gives access to media files and documents

### Step 3: Access Google Drive Files

After granting permissions:

1. Open **Model Upload** screen
2. Select **Local Files** or **Cloud Storage**
3. Browse to your files:
    - **Google Drive**: `/storage/emulated/0/Documents/`
    - **Downloads**: `/storage/emulated/0/Download/`
    - **Any folder**: Should now be accessible!

## 📊 What Each Permission Does

| Permission | Android Version | Purpose |
|------------|----------------|---------|
| `READ_EXTERNAL_STORAGE` | 10-12 (API 29-32) | Read files from storage |
| `WRITE_EXTERNAL_STORAGE` | ≤10 (API ≤29) | Write files to storage |
| `READ_MEDIA_IMAGES` | 13+ (API 33+) | Read image files |
| `READ_MEDIA_VIDEO` | 13+ (API 33+) | Read video files |
| `READ_MEDIA_AUDIO` | 13+ (API 33+) | Read audio files |
| `MANAGE_EXTERNAL_STORAGE` | 11+ (API 30+) | Full storage access |

## 🔧 Troubleshooting

### Issue: Permission Dialog Not Showing

**Solution 1**: Clear app data and reinstall

```bash
adb uninstall com.driftdetector.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Solution 2**: Manually grant permissions

1. Emulator → Settings
2. Apps → DriftGuardAI
3. Permissions → Storage
4. Select "**Allow**"

### Issue: Still Can't Access Google Drive Files

**Check these:**

1. **Sync Google Drive files**:
    - Open Google Drive app on emulator
    - Ensure files are downloaded (not just cloud-only)
    - Look for the ✓ icon on files

2. **Check file location**:
    - Google Drive synced files: `/storage/emulated/0/Documents/`
    - You may need to use "**My Drive**" folder

3. **Grant "All Files" access** (Android 11+):
   ```
   Settings → Apps → DriftGuardAI → Permissions → Files and media
   → "Allow management of all files"
   ```

### Issue: Permission Denied Error in Logs

Check logcat:

```bash
adb logcat | grep -E "Permission|Storage|PERMISSION"
```

If you see `PERMISSION_DENIED`, manually grant permissions:

```bash
# Grant READ_EXTERNAL_STORAGE
adb shell pm grant com.driftdetector.app android.permission.READ_EXTERNAL_STORAGE

# For Android 13+
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_IMAGES
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_AUDIO
```

## 🎯 Emulator-Specific Tips

### Setting Up Google Drive on Emulator

1. **Add Google Account**:
    - Settings → Accounts → Add Account → Google
    - Sign in with your Google account

2. **Install Google Drive**:
    - Open Play Store
    - Search "Google Drive"
    - Install

3. **Download Files**:
    - Open Google Drive app
    - Right-click files → "**Make available offline**"
    - Or use "**Download**" option

4. **Access from File Picker**:
    - The app will now show these files
    - Look in "**My Drive**" or "**Documents**"

### Alternative: Use Emulator's File System

**Upload files directly to emulator**:

```bash
# Push file to emulator
adb push model.tflite /sdcard/Download/model.tflite
adb push data.csv /sdcard/Download/data.csv

# Verify
adb shell ls -la /sdcard/Download/
```

Then in the app:

1. Go to **Model Upload**
2. Select **Local Files**
3. Browse to **Downloads**
4. Select your file!

### Using Android Studio's Device File Explorer

1. **View → Tool Windows → Device File Explorer**
2. Navigate to `/sdcard/Download/` or `/sdcard/Documents/`
3. Right-click → **Upload** to add files
4. These will be visible in the app's file picker

## 📱 Testing Permissions

To verify permissions are working:

1. **Check in Logcat**:
   ```bash
   adb logcat | grep "PermissionHelper"
   ```

   You should see:
   ```
   ✓ All storage permissions granted
   ✓ Has MANAGE_EXTERNAL_STORAGE permission
   ```

2. **Check in App Settings**:
    - Emulator Settings → Apps → DriftGuardAI → Permissions
    - Should show "**Allowed**" for Storage/Files

3. **Test File Picker**:
    - Open **Model Upload** screen
    - Click **Local Files**
    - You should see multiple folders (Downloads, Documents, etc.)

## 🎓 Understanding Android Storage Permissions

### Scoped Storage (Android 10+)

Starting with Android 10, apps have **scoped storage** by default:

- ✅ Can access app's own files freely
- ✅ Can use SAF (Storage Access Framework) for user-selected files
- ❌ Can't browse full file system without permission

### Granular Permissions (Android 13+)

Android 13 introduced **media type permissions**:

- Instead of blanket "Storage" permission
- Apps request specific access (images, video, audio)
- More privacy-focused

### MANAGE_EXTERNAL_STORAGE

This is a **special permission** for Android 11+:

- Gives unrestricted file system access
- Requires user to manually enable in Settings
- Needed for file manager-like apps
- **DriftGuardAI requests this for easy file browsing**

## 🔒 Privacy & Security

### What We Access

DriftGuardAI only accesses files you explicitly select:

- ✅ Files you upload through the file picker
- ✅ Files in app's own directories
- ❌ We don't scan or upload files without permission

### Where Files Are Stored

- **Uploaded models**: `app's private storage`
- **Processed data**: `app's internal database`
- **Exported reports**: `Downloads` folder (visible to you)
- **Cache**: Automatically cleared

### Your Privacy

- 🔒 All processing happens **on-device**
- 🔒 No files uploaded to servers
- 🔒 No data collection without consent
- 🔒 Full control over file access

## ✅ Success Checklist

After the fix, verify these work:

- [ ] App requests storage permissions on first launch
- [ ] Can browse to **Downloads** folder
- [ ] Can browse to **Documents** folder
- [ ] Can access Google Drive files (if synced)
- [ ] File picker shows multiple folders
- [ ] Can upload `.tflite` model files
- [ ] Can upload `.csv` data files
- [ ] No permission errors in logcat

## 📞 Still Having Issues?

### Check Logs

```bash
# Full permission logs
adb logcat | grep -i "permission" > permissions.log

# PermissionHelper logs
adb logcat | grep "PermissionHelper"

# Storage access logs
adb logcat | grep -E "Storage|External"
```

### Reset Permissions

```bash
# Reset all app permissions
adb shell pm reset-permissions com.driftdetector.app

# Reinstall app
adb uninstall com.driftdetector.app
./gradlew installDebug
```

### Manual Grant (for testing)

```bash
# Grant all storage permissions at once
adb shell pm grant com.driftdetector.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.driftdetector.app android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_IMAGES
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_AUDIO
```

## 🎉 You're All Set!

Your emulator should now be able to:

- ✅ Access Google Drive files
- ✅ Browse Downloads folder
- ✅ Open files from any location
- ✅ Upload models and datasets
- ✅ Export reports

**Next Steps:**

1. Rebuild and install the app
2. Grant storage permissions when prompted
3. Try uploading a model from Google Drive!

---

**Related Guides:**

- `MODEL_UPLOAD_FEATURE_SUMMARY.md` - How to upload models
- `UPLOAD_ONNX_MODELS_GUIDE.md` - ONNX model upload guide
- `ANDROID_MANIFEST.xml` - Permission declarations
