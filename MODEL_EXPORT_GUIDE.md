# How to Export Model Outputs After Processing

This guide explains how to export the outputs and results of your ML models after the drift
detection and patching process is complete.

## 📊 What Can Be Exported?

Your DriftGuard AI app can export:

1. **Model Predictions** - All predictions made by your model with metadata
2. **Drift Reports** - Comprehensive drift detection results over time
3. **Patch Comparisons** - Before/after analysis of applied patches
4. **Performance Metrics** - Model accuracy and drift scores

## 🎯 Export Formats

- **CSV** - Easy to open in Excel, Google Sheets, or data analysis tools
- **JSON** - Machine-readable format for programmatic processing

## 🚀 Quick Start: Export from the UI

### Method 1: Using the Settings Screen

1. **Open the app** on your Android device
2. **Navigate to Settings** (bottom navigation bar)
3. **Scroll down to "Data Management"** section
4. **Tap "Export Data"**
5. **Choose what to export:**
    - All drift reports
    - Patch history
    - Model predictions
6. **Select format** (CSV or JSON)
7. **Tap "Export"**
8. **Share or save** the exported file

### Method 2: Programmatically

Add the export functionality to your code:

```kotlin
// Inject the ModelExportManager
val exportManager: ModelExportManager by inject()

// Export predictions to CSV
val predictions = listOf(
    PredictionResult(
        timestamp = Instant.now(),
        input = floatArrayOf(1.0f, 2.0f, 3.0f),
        prediction = floatArrayOf(0.8f, 0.2f),
        confidence = 0.85f,
        modelVersion = "1.0.0",
        patchId = "patch-123",
        driftScore = 0.15
    )
)

val result = exportManager.exportPredictionsToCsv(
    modelName = "FraudDetectionModel",
    predictions = predictions,
    includeMetadata = true
)

result.onSuccess { exportResult ->
    println("✅ Exported to: ${exportResult.fileName}")
    println("📊 Records: ${exportResult.recordCount}")
    println("💾 Size: ${exportResult.fileSizeMB} MB")
    
    // Share the file
    val shareIntent = exportManager.shareExportedFile(exportResult)
    startActivity(Intent.createChooser(shareIntent, "Share Export"))
}
```

## 📁 Export Examples

### 1. Export Model Predictions

```kotlin
suspend fun exportMyModelPredictions() {
    val exportManager: ModelExportManager by inject()
    val repository: DriftRepository by inject()
    
    // Get recent predictions
    val predictions = repository.getRecentPredictions(
        modelId = "my-model-id",
        startTime = Instant.now().minus(7, ChronoUnit.DAYS)
    ).first()
    
    // Convert to PredictionResult format
    val predictionResults = predictions.map { pred ->
        PredictionResult(
            timestamp = pred.timestamp,
            input = pred.input,
            prediction = pred.prediction,
            confidence = pred.confidence,
            modelVersion = pred.modelVersion,
            patchId = pred.patchId,
            driftScore = pred.driftScore
        )
    }
    
    // Export to CSV
    val result = exportManager.exportPredictionsToCsv(
        modelName = "MyModel",
        predictions = predictionResults
    )
    
    result.onSuccess { println("✅ Export successful!") }
}
```

### 2. Export Drift Report

```kotlin
suspend fun exportDriftReport() {
    val exportManager: ModelExportManager by inject()
    val repository: DriftRepository by inject()
    
    // Get model and drift data
    val model = repository.getModelById("my-model-id")!!
    val driftResults = repository.getDriftResultsByModel("my-model-id").first()
    val patches = repository.getPatchesByModel("my-model-id").first()
    
    // Export comprehensive report
    val result = exportManager.exportDriftReport(
        model = model,
        driftResults = driftResults,
        patches = patches
    )
    
    result.onSuccess { exportResult ->
        println("✅ Drift report exported: ${exportResult.fileName}")
    }
}
```

### 3. Export Patch Comparison

```kotlin
suspend fun exportPatchAnalysis(patchId: String) {
    val exportManager: ModelExportManager by inject()
    
    // Get patch and predictions before/after
    val patch = getPatchById(patchId)
    val beforePredictions = getPredictionsBeforePatch(patchId)
    val afterPredictions = getPredictionsAfterPatch(patchId)
    
    // Calculate performance metrics
    val metrics = PatchPerformanceMetrics(
        accuracyBefore = 0.85,
        accuracyAfter = 0.92,
        accuracyImprovement = 0.07,
        driftScoreBefore = 0.45,
        driftScoreAfter = 0.12,
        driftReduction = 0.33
    )
    
    // Export comparison
    val result = exportManager.exportPatchComparison(
        patch = patch,
        beforePredictions = beforePredictions,
        afterPredictions = afterPredictions,
        performanceMetrics = metrics
    )
    
    result.onSuccess { println("✅ Patch comparison exported!") }
}
```

## 📤 Accessing Exported Files

### From Android Device

Exported files are saved to:

```
/storage/emulated/0/Android/data/com.driftdetector.app/files/
```

### Using ADB (Android Debug Bridge)

Pull exported files to your computer:

```bash
# List exported files
adb shell ls /storage/emulated/0/Android/data/com.driftdetector.app/files/

# Pull a specific file
adb pull /storage/emulated/0/Android/data/com.driftdetector.app/files/predictions_MyModel_2024-11-05_14-30-00.csv

# Pull all exports
adb pull /storage/emulated/0/Android/data/com.driftdetector.app/files/
```

### Share via Android

The app provides a built-in share functionality:

```kotlin
val shareIntent = exportManager.shareExportedFile(exportResult)
context.startActivity(Intent.createChooser(shareIntent, "Share Export"))
```

This allows you to share via:

- Email
- Google Drive
- Dropbox
- Any installed share target

## 🔧 Complete Implementation Example

Here's a complete ViewModel implementation for the Settings screen:

```kotlin
// In SettingsViewModel.kt
fun exportData() {
    viewModelScope.launch {
        try {
            _uiState.update { it.copy(isExporting = true) }
            
            val exportManager: ModelExportManager by inject()
            val repository: DriftRepository by inject()
            
            // Get all active models
            val models = repository.getActiveModels().first()
            
            models.forEach { model ->
                // Export drift report for each model
                val driftResults = repository.getDriftResultsByModel(model.id).first()
                val patches = repository.getPatchesByModel(model.id).first()
                
                val result = exportManager.exportDriftReport(
                    model = model,
                    driftResults = driftResults,
                    patches = patches
                )
                
                result.onSuccess { exportResult ->
                    Timber.i("✅ Exported ${model.name}: ${exportResult.fileName}")
                    
                    // Show share dialog
                    val shareIntent = exportManager.shareExportedFile(exportResult)
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Share ${model.name} Report")
                    )
                }
                
                result.onFailure { error ->
                    Timber.e(error, "❌ Failed to export ${model.name}")
                }
            }
            
            _uiState.update { it.copy(isExporting = false, exportSuccess = true) }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Export failed")
            _uiState.update { it.copy(isExporting = false, exportError = e.message) }
        }
    }
}
```

## 📋 CSV Export Format

### Predictions CSV

```csv
Timestamp,Input,Prediction,Confidence,Model Version,Patch Applied,Drift Score
2024-11-05T14:30:00Z,"[1.0, 2.0, 3.0]","[0.8, 0.2]",0.85,1.0.0,Yes,0.15
2024-11-05T14:31:00Z,"[1.5, 2.5, 3.5]","[0.9, 0.1]",0.92,1.0.0,Yes,0.12
```

## 📄 JSON Export Format

### Drift Report JSON

```json
{
  "reportGeneratedAt": "2024-11-05_14-30-00",
  "model": {
    "id": "model-123",
    "name": "FraudDetectionModel",
    "version": "1.0.0",
    "createdAt": "2024-10-01T00:00:00Z"
  },
  "driftHistory": [
    {
      "timestamp": "2024-11-05T14:00:00Z",
      "driftType": "FEATURE_DRIFT",
      "driftScore": 0.45,
      "isDriftDetected": true,
      "threshold": 0.3,
      "featureDrifts": [
        {
          "featureName": "amount",
          "driftScore": 0.52,
          "psiScore": 0.48,
          "ksStatistic": 0.35,
          "pValue": 0.001,
          "isDrifted": true
        }
      ]
    }
  ],
  "patchesApplied": [
    {
      "patchId": "patch-456",
      "patchType": "FEATURE_CLIPPING",
      "status": "APPLIED",
      "createdAt": "2024-11-05T14:05:00Z",
      "appliedAt": "2024-11-05T14:10:00Z"
    }
  ],
  "summary": {
    "totalDriftEvents": 5,
    "driftDetectedCount": 3,
    "averageDriftScore": 0.32,
    "maxDriftScore": 0.45,
    "totalPatchesApplied": 2,
    "totalPatchesRolledBack": 0
  }
}
```

## 🧹 Cleanup Old Exports

To prevent storage bloat, periodically clean up old exports:

```kotlin
suspend fun cleanupOldExports() {
    val exportManager: ModelExportManager by inject()
    
    // Delete exports older than 7 days
    val deletedCount = exportManager.cleanupOldExports(daysOld = 7)
    
    println("🧹 Cleaned up $deletedCount old export files")
}
```

## 🔐 Privacy & Security

- **All exports are stored locally** on your device
- **No data is sent to the cloud** unless you explicitly share it
- **Exports can contain sensitive information** - handle with care
- **Use encrypted channels** (HTTPS, encrypted email) when sharing exports

## 🎨 Custom Export Formats

You can extend the export functionality to support custom formats:

```kotlin
class CustomExportManager(
    private val baseExportManager: ModelExportManager,
    private val context: Context
) {
    // Export to Excel format
    suspend fun exportToExcel(
        model: MLModel,
        data: List<PredictionResult>
    ): Result<File> {
        // Implement Excel export using Apache POI or similar
        // ...
    }
    
    // Export to TensorBoard format
    suspend fun exportToTensorBoard(
        metrics: List<DriftResult>
    ): Result<File> {
        // Implement TensorBoard event file export
        // ...
    }
}
```

## 📊 Analyzing Exported Data

### Python Analysis Example

```python
import pandas as pd
import json

# Load CSV predictions
df = pd.read_csv('predictions_FraudDetectionModel_2024-11-05_14-30-00.csv')

# Analyze confidence distribution
print(df['Confidence'].describe())

# Load JSON drift report
with open('drift_report_FraudDetectionModel_2024-11-05_14-30-00.json') as f:
    report = json.load(f)

# Analyze drift trends
drift_scores = [d['driftScore'] for d in report['driftHistory']]
print(f"Average drift score: {sum(drift_scores) / len(drift_scores)}")
```

## 🆘 Troubleshooting

### Export Failed Error

**Problem:** Export fails with "Failed to export"

**Solutions:**

1. Check storage permissions
2. Ensure sufficient storage space
3. Verify model has data to export

### Cannot Find Exported Files

**Problem:** Exported files not visible

**Solutions:**

1. Use ADB to list files:
   `adb shell ls /storage/emulated/0/Android/data/com.driftdetector.app/files/`
2. Check app-specific storage location
3. Use the share functionality instead

### Share Intent Not Working

**Problem:** Share sheet doesn't appear

**Solutions:**

1. Ensure FileProvider is configured in AndroidManifest.xml
2. Verify URI permissions are granted
3. Check that the file exists before sharing

## 📱 FileProvider Configuration

Make sure your `AndroidManifest.xml` includes:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

And `res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="exports" path="." />
</paths>
```

## 🎯 Best Practices

1. **Export Regularly** - Don't wait until storage is full
2. **Use Meaningful Names** - The system auto-generates names with timestamps
3. **Cleanup Old Exports** - Use the auto-cleanup feature
4. **Verify Exports** - Always check file size and record count
5. **Secure Sharing** - Use encrypted channels for sensitive data
6. **Backup Important Exports** - Store critical exports off-device

## 📚 Next Steps

- [Understanding Drift Detection Results](DRIFT_DETECTION_GUIDE.md)
- [Patch Management Guide](PATCH_MANAGEMENT_GUIDE.md)
- [API Reference](API_REFERENCE.md)

---

**Need Help?** Open an issue on GitHub or check the troubleshooting guide.
