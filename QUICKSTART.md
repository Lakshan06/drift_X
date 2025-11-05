# Drift Detector - Quick Start Guide

## ⚠️ Important: Java 17 Requirement

This project requires **Java 17**. If you encounter "Incompatible Gradle JVM" errors:

**Quick Fix:**

```powershell
# Use the provided build script instead of gradlew directly
.\build.ps1 build
```

**Or set JAVA_HOME manually:**

```powershell
$env:JAVA_HOME = "C:\drift_X\.java\jdk-17"
.\gradlew build
```

See [`GRADLE_JVM_FIX.md`](GRADLE_JVM_FIX.md) for detailed troubleshooting.

## Project Overview

This is a **privacy-first, on-device Android app** that continuously monitors ML models for drift
and automatically generates reversible patches using:

- **Kotlin** with **Jetpack Compose** for modern UI
- **Koin** for dependency injection (not Hilt)
- **TensorFlow Lite** for on-device ML inference
- **Room + SQLCipher** for encrypted storage
- **WorkManager** for background drift monitoring

## Key Components Created

### 1. **Domain Models** (`app/src/main/java/com/driftdetector/app/domain/model/`)

- `DriftResult.kt` - Drift detection results with PSI, KS test scores
- `Patch.kt` - Reversible patch configurations (clipping, reweighting, threshold tuning)
- Feature drift attribution and validation metrics

### 2. **Core Drift Detection** (`app/src/main/java/com/driftdetector/app/core/drift/`)

- `DriftDetector.kt` - PSI and Kolmogorov-Smirnov test implementations
- `AttributionEngine.kt` - SHAP-like feature attribution adapted for mobile

### 3. **Patch System** (`app/src/main/java/com/driftdetector/app/core/patch/`)

- `PatchSynthesizer.kt` - Auto-generates patches based on drift type
- `PatchEngine.kt` - Applies/rolls back patches
- `PatchValidator.kt` - Safety validation before application

### 4. **Privacy & Security** (`app/src/main/java/com/driftdetector/app/core/security/`)

- `EncryptionManager.kt` - Android Keystore encryption
- `DifferentialPrivacy.kt` - Laplace/Gaussian noise mechanisms

### 5. **TensorFlow Lite** (`app/src/main/java/com/driftdetector/app/core/ml/`)

- `TFLiteModelInference.kt` - Optimized on-device inference with GPU support

### 6. **Data Layer** (`app/src/main/java/com/driftdetector/app/data/`)

- Encrypted Room database with SQLCipher
- Repository pattern with Koin DI
- Optional Retrofit API client for encrypted sync

### 7. **Koin DI Setup** (`app/src/main/java/com/driftdetector/app/di/`)

- `AppModule.kt` - Complete Koin module configuration
- Database, Network, Security, Core, Repository, ViewModel modules

### 8. **Jetpack Compose UI** (`app/src/main/java/com/driftdetector/app/presentation/`)

- `MainActivity.kt` - Navigation with Material 3
- `DriftDashboardScreen.kt` - Real-time drift monitoring
- `PatchManagementScreen.kt` - Apply/rollback patches
- `ModelManagementScreen.kt` - Model registration

### 9. **Background Workers** (`app/src/main/java/com/driftdetector/app/worker/`)

- `DriftMonitorWorker.kt` - Periodic drift checks (every 6 hours)
- Integrated with Koin for dependency injection

## Build & Run

1. **Open in Android Studio**

```bash
# The project is already in your directory
# Open Android Studio -> Open Existing Project -> Select this folder
```

2. **Sync Gradle**
    - Android Studio will auto-sync dependencies
    - If not, click "Sync Now" in the banner

3. **Run**
    - Connect Android device (API 26+) or start emulator
    - Click Run button (▶️) or press `Shift + F10`

## Architecture Highlights

### Clean Architecture Layers

```
Presentation (Compose UI + ViewModels)
    ↓
Domain (Models + Business Logic)
    ↓
Data (Repository + Room + API)
    ↓
Core (ML, Security, Algorithms)
```

### Koin Dependency Injection

All dependencies are managed through Koin modules:

```kotlin
// Usage in ViewModel
class DriftDashboardViewModel(
    private val repository: DriftRepository  // Injected by Koin
) : ViewModel()

// Usage in Composable
@Composable
fun DriftDashboardScreen(
    viewModel: DriftDashboardViewModel = koinViewModel()  // Koin integration
)
```

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

## Key Features Implemented

✅ **Real-time drift detection** with PSI and KS statistical tests  
✅ **Explainable attribution** using SHAP-like techniques  
✅ **Auto-patch synthesis** (clipping, reweighting, threshold tuning)  
✅ **Patch validation** with safety checks  
✅ **Reversible patches** with rollback capability  
✅ **Encrypted storage** using SQLCipher + Android Keystore  
✅ **Differential privacy** for optional metadata sync  
✅ **Background monitoring** with WorkManager  
✅ **Modern Compose UI** with Material 3  
✅ **Koin DI** throughout the app

## File Structure Summary

```
drift_X/
├── app/
│   ├── build.gradle.kts (dependencies configured)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/driftdetector/app/
│       │   │   ├── DriftDetectorApp.kt (Application class with Koin)
│       │   │   ├── core/
│       │   │   │   ├── drift/ (PSI, KS test, attribution)
│       │   │   │   ├── patch/ (synthesis, engine, validator)
│       │   │   │   ├── ml/ (TFLite inference)
│       │   │   │   └── security/ (encryption, DP)
│       │   │   ├── data/
│       │   │   │   ├── local/ (Room DAOs, entities)
│       │   │   │   ├── remote/ (Retrofit API)
│       │   │   │   ├── repository/ (DriftRepository)
│       │   │   │   └── mapper/ (Entity↔Domain)
│       │   │   ├── domain/
│       │   │   │   └── model/ (DriftResult, Patch, MLModel)
│       │   │   ├── presentation/
│       │   │   │   ├── MainActivity.kt
│       │   │   │   ├── screen/ (Compose screens)
│       │   │   │   ├── viewmodel/ (ViewModels)
│       │   │   │   └── theme/ (Material 3 theme)
│       │   │   ├── worker/ (WorkManager background tasks)
│       │   │   └── di/ (Koin modules)
│       │   └── res/ (strings, themes, XML configs)
│       └── test/ (Unit tests)
├── build.gradle.kts (root)
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Next Steps

1. **Add TFLite models**: Place `.tflite` model files in `app/src/main/assets/`
2. **Configure API endpoint**: Update base URL in `AppModule.kt` if using backend sync
3. **Customize thresholds**: Adjust PSI/KS thresholds in Koin configuration
4. **Test drift detection**: Use provided unit tests as examples
5. **Deploy**: Build signed APK/AAB for production

## Privacy Guarantees

- ✅ All ML processing happens **on-device**
- ✅ Database encrypted with **SQLCipher**
- ✅ Keys stored in **Android Keystore** (hardware-backed)
- ✅ Optional sync uses **differential privacy**
- ✅ No sensitive data leaves the device

## Performance Optimizations

- Drift detection runs in background with **WorkManager**
- TFLite uses **GPU acceleration** when available
- Efficient **Kotlin coroutines** for async operations
- **Room database** with optimized queries
- Lightweight **Koin DI** (no reflection overhead)

---

**You now have a complete, production-ready app!** 🎉

For questions, refer to the full README.md or explore the well-documented code.
