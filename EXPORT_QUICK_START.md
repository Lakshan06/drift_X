# Export Quick Start Guide

## 🚀 How to Export Model Outputs After Processing

### From the App (UI Method)

**Step 1:** Open the app on your Android device

**Step 2:** Navigate to the **Settings** tab (bottom navigation)

**Step 3:** Scroll to **"Data Management"** section

**Step 4:** Tap **"Export Data"**

**Step 5:** Wait for the export to complete (you'll see a progress dialog)

**Step 6:** When complete, tap **"Share"** to:

- Send via email
- Upload to Google Drive/Dropbox
- Share to any installed app

---

## 📁 Where Are Files Exported?

Exported files are saved to:

```
/storage/emulated/0/Android/data/com.driftdetector.app/files/
```

### File Names Format

- **Predictions:** `predictions_ModelName_2024-11-05_14-30-00.csv`
- **Drift Reports:** `drift_report_ModelName_2024-11-05_14-30-00.json`
- **Patch Comparisons:** `patch_comparison_patch-id_2024-11-05_14-30-00.json`

---

## 📥 Pull Files to Your Computer

Using ADB (Android Debug Bridge):

```bash
# List all exported files
adb shell ls /storage/emulated/0/Android/data/com.driftdetector.app/files/

# Download all exports
adb pull /storage/emulated/0/Android/data/com.driftdetector.app/files/ ./exports/

# Download specific file
adb pull /storage/emulated/0/Android/data/com.driftdetector.app/files/drift_report_MyModel_2024-11-05_14-30-00.json
```

---

## 📊 What Gets Exported?

### 1. **Drift Reports** (JSON format)

- Complete drift detection history
- Feature-level drift scores
- Statistical test results
- Summary statistics
- Applied patches

### 2. **Predictions** (CSV format)

- Model inputs
- Predictions/outputs
- Confidence scores
- Timestamps
- Model version
- Patch information

### 3. **Patch Comparisons** (JSON format)

- Before/after predictions
- Performance metrics
- Accuracy improvements
- Drift reduction

---

## 💡 Common Use Cases

### Export After Drift Detection

```
1. Run drift detection on your model
2. Apply patches if recommended
3. Go to Settings → Export Data
4. Share the drift report for analysis
```

### Export Predictions for Analysis

```
1. Let your model make predictions
2. Wait 24-48 hours for data collection
3. Go to Settings → Export Data
4. Open CSV in Excel/Python for analysis
```

### Compare Before/After Patch

```
1. Note predictions before patch
2. Apply recommended patch
3. Wait for new predictions
4. Export data to compare performance
```

---

## 🔍 Analyzing Exported Data

### In Excel/Google Sheets

1. Open the CSV file
2. Create pivot tables for analysis
3. Visualize confidence trends
4. Identify low-confidence predictions

### In Python

```python
import pandas as pd
import json

# Load predictions CSV
df = pd.read_csv('predictions_MyModel_2024-11-05_14-30-00.csv')

# Analyze confidence distribution
print(df['Confidence'].describe())
print(f"Low confidence predictions: {(df['Confidence'] < 0.7).sum()}")

# Load drift report JSON
with open('drift_report_MyModel_2024-11-05_14-30-00.json') as f:
    report = json.load(f)

print(f"Total drift events: {report['summary']['totalDriftEvents']}")
print(f"Average drift score: {report['summary']['averageDriftScore']}")
```

---

## ⚠️ Troubleshooting

### "No models found" Error

**Solution:** Upload a model first in the Models tab

### "No data available to export" Error

**Solution:** Your model needs to have:

- Made predictions, OR
- Had drift detection run

Wait 24 hours after model upload for data to accumulate.

### Can't Find Exported Files

**Solution:** Use the Share button in the success dialog instead of manually searching for files.

### Share Button Not Working

**Solution:**

1. Grant storage permissions to the app
2. Ensure you have apps installed that can receive files (email, Drive, etc.)

---

## 🎯 Best Practices

✅ **Export regularly** - Don't wait until you need the data  
✅ **Use Share feature** - Easier than manual file transfer  
✅ **Analyze in Python/Excel** - More powerful than viewing on device  
✅ **Keep exports organized** - Rename files after downloading  
✅ **Clean old exports** - Use "Clear Old Data" to free space

---

## 📱 Need More Help?

- **Full Guide:** [MODEL_EXPORT_GUIDE.md](MODEL_EXPORT_GUIDE.md)
- **Troubleshooting:** [AI_TROUBLESHOOTING.md](AI_TROUBLESHOOTING.md)
- **Installation:** [INSTALL_GUIDE.md](INSTALL_GUIDE.md)

---

**Questions?** Check the comprehensive export guide or open an issue on GitHub.
