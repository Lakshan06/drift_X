# Model Drift Detector with Reversible Auto-Patches

A privacy-first, on-device Android app for continuous ML model drift monitoring with automatic patch
synthesis and reversible application.

## ✅ **CRASH FIXED!**

**The app crash has been identified and fixed!**

The issue was WorkManager being initialized twice. **[See the fix details →](FIX_APPLIED.md)**

**To install the fixed version:**

```powershell
adb uninstall com.driftdetector.app
.\build.ps1 installDebug
```

## 🐛 **DEBUGGING & TROUBLESHOOTING**

### 🚨 **APP CRASHING? → [START HERE!](START_HERE.md)** 🚨

**If the app crashes on startup, we have comprehensive debugging tools!**

### Quick Debug (One Command)

```powershell
.\debug_crash.ps1
```

This will:

- ✓ Rebuild and install the app
- ✓ Launch with full logging
- ✓ Capture detailed crash information
- ✓ Save logs to `logs/crash_debug_[timestamp].log`

### Or Capture Logs After Crash

```powershell
.\capture_logs.ps1
```

### Documentation

- **🎉 [FIX_APPLIED.md](FIX_APPLIED.md)** - **WorkManager crash fix explained**
- **🚀 [START_HERE.md](START_HERE.md)** - **Start with this!** Quick diagnosis guide
- **📋 [QUICK_DEBUG.md](QUICK_DEBUG.md)** - Quick reference card
- **📖 [DEBUG_GUIDE.md](DEBUG_GUIDE.md)** - Comprehensive debugging guide
- **🔍 [DEBUGGING_SUMMARY.md](DEBUGGING_SUMMARY.md)** - What we've built
- **🔧 [CRASH_FIX.md](CRASH_FIX.md)** - Common crash fixes

**The app now has extensive logging that will tell us exactly where it crashes!**

## 🎯 Features

### Core Capabilities

- **Real-time Drift Detection**: PSI (Population Stability Index) and Kolmogorov-Smirnov statistical
  tests optimized for on-device inference
- **Explainable Attribution**: SHAP/LIME-like techniques adapted for mobile to identify drift
  drivers
- **Auto-Patch Synthesis**: Automatically generate reversible patches including:
    - Feature clipping
    - Feature reweighting
    - Threshold tuning
    - Normalization updates
- **Patch Validation**: Safety checks before applying patches with rollback capability
- **Privacy-First**: All processing on-device with encrypted storage
- **Differential Privacy**: Optional encrypted metadata sync with DP guarantees

### Technical Highlights

- **Kotlin + Jetpack Compose**: Modern Android UI development
- **TensorFlow Lite**: Optimized ML inference on device
- **Room + SQLCipher**: Encrypted database for secure storage
- **Android Keystore**: Hardware-backed encryption
- **WorkManager**: Efficient background drift monitoring
- **Koin**: Lightweight dependency injection
- **Material 3**: Beautiful, modern UI design

## 🏗️ Architecture

### Modular Architecture

```
app/
├── core/
│   ├── drift/          # Drift detection algorithms (PSI, KS test)
│   ├── patch/          # Patch synthesis, engine, and validation
│   ├── ml/             # TensorFlow Lite inference
│   └── security/       # Encryption & differential privacy
├── data/
│   ├── local/          # Room database (encrypted)
│   ├── remote/         # Optional API sync
│   ├── repository/     # Data management layer
│   └── mapper/         # Entity-Domain mappings
├── domain/
│   └── model/          # Domain models (DriftResult, Patch, MLModel)
├── presentation/
│   ├── screen/         # Jetpack Compose screens
│   ├── viewmodel/      # ViewModels with state management
│   └── theme/          # Material 3 theming
├── worker/             # WorkManager background tasks
└── di/                 # Koin dependency injection modules
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 26+ (Android 8.0)
- Kotlin 1.9.20+
- Gradle 8.4+
- **Java 17** (required - see troubleshooting below)

### Installation

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/drift-detector.git
cd drift-detector
```

2. **Set up Java 17** (if needed)

If you encounter "Incompatible Gradle JVM" errors, run the setup script:

```powershell
# PowerShell (Windows)
.\setup-java17.ps1

# This will download and configure Java 17 for the project
```

3. **Open in Android Studio**
    - Open Android Studio
    - Select "Open an Existing Project"
    - Navigate to the cloned directory

4. **Sync Gradle**
    - Android Studio will automatically sync Gradle
    - Wait for all dependencies to download

5. **Build and Run**

Use the provided build scripts to ensure Java 17 is used:

```powershell
# PowerShell
.\build.ps1 assembleDebug

# Or for full build
.\build.ps1 clean build
```

Alternatively, connect an Android device or start an emulator and click "Run" in Android Studio.

### Troubleshooting

#### Incompatible Gradle JVM Error

**Problem**: You see an error like "Incompatible Gradle JVM" or "Unsupported class file major
version 69"

**Solution**: This project requires Java 17. If you have a different Java version installed:

1. **Run the setup script**: `.\setup-java17.ps1`
2. **Use the build scripts**: Always use `.\build.ps1` (PowerShell) or `build.bat` (Command Prompt)
   instead of `gradlew` directly
3. **Set JAVA_HOME manually** (if needed):
   ```powershell
   $env:JAVA_HOME = "C:\drift_X\.java\jdk-17"
   .\gradlew build
   ```

**Why this happens**: The project is configured for Java 17, but your system may have a different
Java version (e.g., Java 25) in your PATH. The build scripts automatically set JAVA_HOME to use the
correct version.

#### Android Studio JDK Configuration

If Android Studio shows JDK-related errors:

1. Go to **File → Project Structure → SDK Location**
2. Set **Gradle JDK** to: `C:\drift_X\.java\jdk-17`
3. Click **Apply** and **OK**
4. Sync Gradle again

## 📱 Usage

### 1. Register a Model

```kotlin
val model = MLModel(
    id = "model-1",
    name = "Fraud Detection Model",
    version = "1.0.0",
    modelPath = "fraud_model.tflite",
    inputFeatures = listOf("amount", "merchant_type", "hour_of_day"),
    outputLabels = listOf("legitimate", "fraud"),
    createdAt = Instant.now(),
    lastUpdated = Instant.now(),
    isActive = true
)

// Register via ViewModel
modelManagementViewModel.registerModel(model)
```

### 2. Monitor for Drift

```kotlin
// Schedule background monitoring
DriftMonitorWorker.schedule(context, modelId = "model-1")

// Or perform one-time check
DriftCheckWorker.enqueue(context, modelId = "model-1")
```

### 3. Review and Apply Patches

The app automatically synthesizes patches when drift is detected. Review patches in the Patches tab
and:

- **Apply**: Test the patch on your model
- **Rollback**: Revert to original behavior if needed

## 🔒 Privacy & Security

### On-Device Processing

All drift detection and patch synthesis happens entirely on-device. No model data leaves your
device.

### Encrypted Storage

- **Database**: SQLCipher encryption for all stored data
- **Shared Preferences**: Android EncryptedSharedPreferences
- **Keystore**: Hardware-backed key storage

### Differential Privacy

Optional metadata sync applies differential privacy:

```kotlin
val dp = DifferentialPrivacy(epsilon = 0.5, delta = 1e-5)
val privatizedScore = dp.addLaplaceNoise(driftScore, sensitivity = 1.0)
```

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Example Test

```kotlin
@Test
fun testDriftDetection() {
    val detector = DriftDetector()
    val referenceData = generateSampleData(1000)
    val driftedData = generateDriftedData(1000)
    
    val result = runBlocking {
        detector.detectDrift(
            modelId = "test-model",
            referenceData = referenceData,
            currentData = driftedData,
            featureNames = listOf("feature1", "feature2", "feature3")
        )
    }
    
    assertTrue(result.isDriftDetected)
    assertTrue(result.driftScore > 0.2)
}
```

## 📊 Drift Detection Algorithms

### Population Stability Index (PSI)

Measures distribution shift between reference and current data:

```
PSI = Σ (% current - % reference) × ln(% current / % reference)
```

### Kolmogorov-Smirnov Test

Statistical test for distribution equality:

```
D = max |F_reference(x) - F_current(x)|
```

## 🔧 Configuration

### Drift Thresholds

Configure in `AppModule.kt`:

```kotlin
single { DriftDetector(psiThreshold = 0.2, ksThreshold = 0.05) }
```

### Background Monitoring

Adjust frequency in `DriftMonitorWorker`:

```kotlin
val workRequest = PeriodicWorkRequestBuilder<DriftMonitorWorker>(
    repeatInterval = 6,  // Hours
    repeatIntervalTimeUnit = TimeUnit.HOURS
)
```

## 📦 Dependencies

### Core

- `androidx.core:core-ktx:1.12.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0`
- `androidx.activity:activity-compose:1.8.2`

### Compose

- `androidx.compose:compose-bom:2024.01.00`
- `androidx.compose.material3:material3:1.2.0`

### Dependency Injection

- `io.insert-koin:koin-android:3.5.3`
- `io.insert-koin:koin-androidx-compose:3.5.3`

### Database

- `androidx.room:room-runtime:2.6.1`
- `net.zetetic:android-database-sqlcipher:4.5.4`

### ML

- `org.tensorflow:tensorflow-lite:2.14.0`
- `org.tensorflow:tensorflow-lite-gpu:2.14.0`

### Security

- `androidx.security:security-crypto:1.1.0-alpha06`

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- TensorFlow Lite team for on-device ML
- Android team for Jetpack libraries
- Koin team for lightweight DI

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Note**: This is a demonstration app showcasing privacy-first ML drift monitoring. For production
use, ensure thorough testing and security audits.
