# 🎯 START HERE: Storage Access Fixed!

## ✅ Your Problem is Solved!

The emulator couldn't access Google Drive files because storage permissions were missing.  
**This has been fixed!**

---

## 🚀 Quick Start (2 Steps)

### Step 1: Install the Updated App

**Option A - Using the script (Recommended):**

```bash
install_with_permissions.bat
```

**Option B - Using Android Studio:**

- Click the green **Run** ▶️ button
- Wait for installation

### Step 2: Grant Permissions

When the app opens:

- You'll see a permission request
- Tap **"Allow"** or **"Grant access"**
- Done! ✅

---

## 🧪 Test It Works

1. Open **DriftGuardAI** on emulator
2. Go to **Model Upload** screen
3. Tap **Local Files**
4. You should now see folders like **Downloads**, **Documents**, etc.

**If you see multiple folders → Success! 🎉**

---

## 📁 How to Access Your Files

### From Google Drive

1. Open **Google Drive** app on emulator
2. Find your files
3. Tap **⋮** → **"Make available offline"**
4. They'll appear in the DriftGuardAI file picker!

### Upload Directly to Emulator

```bash
# Navigate to Android SDK
cd %LOCALAPPDATA%\Android\Sdk\platform-tools

# Upload files
adb push your-model.tflite /sdcard/Download/
adb push your-data.csv /sdcard/Download/
```

### Using Android Studio

1. **View** → **Tool Windows** → **Device File Explorer**
2. Navigate to `/sdcard/Download/`
3. Right-click → **Upload** your files
4. Access them in the app!

---

## ⚠️ Troubleshooting

### Permission Dialog Not Showing?

Manually grant permissions:

1. Open **Settings** on emulator
2. Go to **Apps** → **DriftGuardAI**
3. Tap **Permissions**
4. Enable **"Files and media"** or **"All files access"**

### Still Can't See Files?

Run this to grant permissions via command:

```bash
# Navigate to Android SDK
cd %LOCALAPPDATA%\Android\Sdk\platform-tools

# Grant storage permissions
adb shell pm grant com.driftdetector.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_IMAGES
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.driftdetector.app android.permission.READ_MEDIA_AUDIO
```

### Need to Start Fresh?

```bash
# Uninstall and reinstall
adb uninstall com.driftdetector.app
install_with_permissions.bat
```

---

## 📖 More Information

| Document | Description |
|----------|-------------|
| **`QUICK_FIX_STORAGE_ACCESS.md`** | Quick reference guide |
| **`STORAGE_PERMISSIONS_GUIDE.md`** | Complete detailed guide |
| **`STORAGE_FIX_SUMMARY.md`** | Technical summary of changes |

---

## ✅ What Was Fixed

- ✅ Added storage permissions to manifest
- ✅ Created automatic permission request system
- ✅ App now asks for permissions on startup
- ✅ Works on all Android versions
- ✅ Helpful error messages if permissions denied

---

## 🎉 You're Ready!

Your app can now access files from:

- ✅ Google Drive
- ✅ Downloads folder
- ✅ Documents folder
- ✅ Any storage location

**Go ahead and upload your ML models!** 🚀

---

**Quick Commands:**

```bash
# Install app with permissions
install_with_permissions.bat

# Check if it worked
adb logcat | findstr "PermissionHelper"

# Should see: "✓ All storage permissions granted"
```

Need help? See `STORAGE_PERMISSIONS_GUIDE.md` for detailed instructions.
