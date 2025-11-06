# 🚀 Quick Reference Guide

## Your App Status - Right Now

### ✅ What's Working

- Model upload → Saves to database immediately
- Data upload → Triggers drift detection
- Drift detection → Automatic with results
- Patch generation → Working button with feedback
- Patch application → Apply/rollback patches
- Dashboard → Real-time metrics
- Physical device → Fully deployed (RZCW815BR8R)

### 📦 What's Been Added (Foundation)

- Database entities for recent files, tasks, sessions
- DAOs for all CRUD operations
- Architecture for zero data loss
- Complete implementation roadmap

---

## 📱 Using Your App on Physical Device

### Upload a Model

```
1. Open app on phone
2. Tap "Models" tab
3. Tap "Upload"
4. Select "Local Files"
5. Choose your .onnx model
✅ See success card with model info
```

### Upload Data

```
1. In Models tab
2. Tap "Upload" again
3. Select .csv file
✅ Drift detection runs automatically
```

### View Results

```
1. Tap "Dashboard" tab
2. See drift alerts (if any)
3. View metrics and graphs
```

### Generate & Apply Patches

```
1. Dashboard → Alerts tab
2. Tap "Generate Patch"
3. See success notification
4. Go to Patches tab
5. Apply patch
```

---

## 🛠️ Development Commands

### Build & Deploy to Device

```powershell
.\build_and_deploy_physical_device.bat
```

### View Logs from Device

```powershell
.\view_device_logs.bat
```

### Build Only

```powershell
.\gradlew.bat assembleDebug
```

### Install Only

```powershell
C:\Users\slaks\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 📚 Documentation You Need

### Fixes & Features

- **FIXES_APPLIED_TODAY.md** - All fixes summary
- **MODEL_UPLOAD_FIX.md** - Model upload details
- **GENERATE_PATCH_FIX.md** - Patch button fix

### Deployment

- **PHYSICAL_DEVICE_SETUP.md** - Complete device guide
- **DEPLOYMENT_SUCCESS.md** - What was deployed

### New Architecture

- **COMPREHENSIVE_ENHANCEMENT_PLAN.md** - Full roadmap
- **IMPLEMENTATION_STATUS.md** - Current progress
- **TODAYS_ACHIEVEMENTS.md** - What we did today

### Quick Start

- **QUICK_REFERENCE.md** (this file) - Quick access
- **README.md** - Main documentation
- **COMPLETE_SYSTEM_SUMMARY.md** - System overview

---

## 🔥 Next Development Steps

### Phase 1: Repositories (1-2 hrs)

```kotlin
Create:
app/src/main/java/com/driftdetector/app/data/repository/
├── RecentFilesRepository.kt
├── TaskRepository.kt
├── SessionRepository.kt
└── AppStateRepository.kt
```

### Phase 2: ViewModels (2-3 hrs)

```kotlin
Create:
app/src/main/java/com/driftdetector/app/presentation/viewmodel/
├── RecentFilesViewModel.kt
├── TaskManagerViewModel.kt
└── SessionViewModel.kt
```

### Phase 3: UI Widgets (3-4 hrs)

```kotlin
Create:
app/src/main/java/com/driftdetector/app/presentation/components/
├── RecentModelsWidget.kt
├── RecentDataFilesWidget.kt
├── TaskRecoveryDialog.kt
└── RecentFileCard.kt
```

### Phase 4: Integration (2-3 hrs)

```kotlin
Update:
- ModelUploadViewModel.kt
- DriftDashboardViewModel.kt
- DriftDetectorApp.kt
```

**Total: 10-15 hours to complete**

---

## 🎯 Testing Checklist

### On Physical Device

```
✅ App launches
✅ Model upload works
✅ Data upload works
✅ Drift detection works
✅ Patch generation works

⏳ Recent files widget (TODO)
⏳ Task recovery (TODO)
⏳ Session restoration (TODO)
```

---

## 🆘 Quick Troubleshooting

### Model not saving?

- Check success message after upload
- Look for "Model Registered Successfully"
- Check Models tab for the model

### Drift not detected?

- Upload both model AND data
- Check data has enough rows (10+)
- View dashboard for results

### Patch button not working?

- Check Dashboard → Alerts tab
- Must have drift detected first
- Look for success Snackbar

### App crashed?

```powershell
.\view_device_logs.bat
```

Check logs for error details

### Device not connected?

```powershell
C:\Users\slaks\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

Should show: `RZCW815BR8R    device`

---

## 📊 Database Schema (New)

### Tables Added

```sql
recent_files
- id, fileName, fileType, filePath
- fileSize, uploadTimestamp, lastAccessedTimestamp
- modelId, isPinned, metadata

user_tasks
- id, taskType, status, progress
- startTimestamp, lastUpdatedTimestamp, completedTimestamp
- metadata, errorMessage

user_sessions
- id, userId, startTimestamp, endTimestamp
- lastActiveModelId, lastActiveDataFileId
- dashboardState

app_state
- key, value, lastUpdated
```

### DAOs Available

- `RecentFileDao` - File tracking
- `UserTaskDao` - Task management
- `UserSessionDao` - Session management
- `AppStateDao` - App state storage

---

## 🎉 Quick Wins

### Want to test the fixes?

1. Upload a model → See success card ✅
2. Upload data → See drift detection ✅
3. Click "Generate Patch" → See notification ✅

### Want to rebuild?

```powershell
.\build_and_deploy_physical_device.bat
```

Wait 1-2 minutes, done! ✅

### Want to see logs?

```powershell
.\view_device_logs.bat
```

Press Ctrl+C to stop ✅

---

## 💡 Pro Tips

### Faster Development

- Use `view_device_logs.bat` to debug in real-time
- Model uploads are instant on physical device
- No emulator network issues!

### Testing

- Test model-only upload first
- Then test data upload
- Then test full pipeline

### Documentation

- All docs in project root
- Markdown files easy to read
- Comprehensive coverage

---

## 🔗 Useful Links

### Code Locations

```
Entities:     app/src/main/java/com/driftdetector/app/data/local/entity/
DAOs:         app/src/main/java/com/driftdetector/app/data/local/dao/
Database:     app/src/main/java/com/driftdetector/app/data/local/DriftDatabase.kt
ViewModels:   app/src/main/java/com/driftdetector/app/presentation/viewmodel/
Screens:      app/src/main/java/com/driftdetector/app/presentation/screen/
Components:   app/src/main/java/com/driftdetector/app/presentation/components/
```

### Build Outputs

```
APK:          app/build/outputs/apk/debug/app-debug.apk
Logs:         Use view_device_logs.bat
```

---

## 📞 Need Help?

### Check These First

1. **FIXES_APPLIED_TODAY.md** - Known fixes
2. **PHYSICAL_DEVICE_SETUP.md** - Device issues
3. **IMPLEMENTATION_STATUS.md** - What's done/todo

### Common Issues

- Model not saving → See MODEL_UPLOAD_FIX.md
- Patch button → See GENERATE_PATCH_FIX.md
- Device not detected → See PHYSICAL_DEVICE_SETUP.md

---

**Keep this file handy for quick reference!** 🚀📱✨
