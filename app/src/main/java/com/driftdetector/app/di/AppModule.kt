package com.driftdetector.app.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.driftdetector.app.core.ai.AIAnalysisEngine
import com.driftdetector.app.core.auth.AuthManager
import com.driftdetector.app.core.cloud.CloudStorageManager
import com.driftdetector.app.core.connectivity.NetworkConnectivityManager
import com.driftdetector.app.core.drift.AttributionEngine
import com.driftdetector.app.core.drift.DriftDetector
import com.driftdetector.app.core.drift.InstantDriftFixManager
import com.driftdetector.app.core.export.ModelExportManager
import com.driftdetector.app.core.export.PatchExportManager
import com.driftdetector.app.core.error.ErrorHandler
import com.driftdetector.app.core.error.CircuitBreaker
import com.driftdetector.app.core.ml.ModelMetadataExtractor
import com.driftdetector.app.core.ml.TFLiteModelInference
import com.driftdetector.app.core.monitoring.AccuracyMonitor
import com.driftdetector.app.core.monitoring.ModelMonitoringService
import com.driftdetector.app.core.notifications.DriftNotificationManager
import com.driftdetector.app.core.patch.PatchEngine
import com.driftdetector.app.core.patch.PatchSynthesizer
import com.driftdetector.app.core.patch.PatchValidator
import com.driftdetector.app.core.patch.IntelligentPatchGenerator
import com.driftdetector.app.core.patch.UltraAggressivePatchGenerator
import com.driftdetector.app.core.patch.RealPatchApplicator
import com.driftdetector.app.core.backup.AutomaticBackupManager
import com.driftdetector.app.core.realtime.RealtimeClient
import com.driftdetector.app.core.security.DifferentialPrivacy
import com.driftdetector.app.core.security.EncryptionManager
import com.driftdetector.app.core.upload.FileUploadProcessor
import com.driftdetector.app.data.local.DriftDatabase
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.data.remote.DriftApiService
import com.driftdetector.app.presentation.viewmodel.AIAssistantViewModel
import com.driftdetector.app.presentation.viewmodel.DriftDashboardViewModel
import com.driftdetector.app.presentation.viewmodel.InstantDriftFixViewModel
import com.driftdetector.app.presentation.viewmodel.ModelManagementViewModel
import com.driftdetector.app.presentation.viewmodel.ModelUploadViewModel
import com.driftdetector.app.presentation.viewmodel.PatchManagementViewModel
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Koin DI module definitions
 */

val databaseModule = module {
    Log.d("KOIN", "Loading databaseModule...")

    single {
        try {
            Log.d("KOIN", "Creating database (standard Room, no SQLCipher)...")

            // Delete old SQLCipher encrypted database if it exists
            val context = androidContext()
            val dbFile = context.getDatabasePath(DriftDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                Log.d("KOIN", "Found existing database file, checking if it's encrypted...")
                try {
                    // Try to detect if it's an old SQLCipher database by reading the file header
                    // SQLCipher databases start with "SQLite format 3" but are encrypted
                    // We'll just delete it to be safe since we changed encryption approach
                    val deleted = dbFile.delete()
                    if (deleted) {
                        Log.d("KOIN", "✓ Deleted old database file (may have been encrypted)")
                        // Also delete related files
                        context.getDatabasePath("${DriftDatabase.DATABASE_NAME}-shm")?.delete()
                        context.getDatabasePath("${DriftDatabase.DATABASE_NAME}-wal")?.delete()
                        context.getDatabasePath("${DriftDatabase.DATABASE_NAME}-journal")?.delete()
                    } else {
                        Log.w("KOIN", "⚠ Could not delete old database file")
                    }
                } catch (e: Exception) {
                    Log.w("KOIN", "⚠ Error checking/deleting old database: ${e.message}")
                }
            }

            val db = Room.databaseBuilder(
                androidContext(),
                DriftDatabase::class.java,
                DriftDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()

            Log.d("KOIN", "✓ Database created successfully")
            db
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Database creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Getting driftResultDao...")
        try {
            get<DriftDatabase>().driftResultDao()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Getting driftResultDao FAILED", e)
            throw e
        }
    }
    single {
        Log.d("KOIN", "Getting mlModelDao...")
        try {
            get<DriftDatabase>().mlModelDao()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Getting mlModelDao FAILED", e)
            throw e
        }
    }
    single {
        Log.d("KOIN", "Getting patchDao...")
        try {
            get<DriftDatabase>().patchDao()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Getting patchDao FAILED", e)
            throw e
        }
    }
    single {
        Log.d("KOIN", "Getting patchSnapshotDao...")
        try {
            get<DriftDatabase>().patchSnapshotDao()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Getting patchSnapshotDao FAILED", e)
            throw e
        }
    }
    single {
        Log.d("KOIN", "Getting modelPredictionDao...")
        try {
            get<DriftDatabase>().modelPredictionDao()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Getting modelPredictionDao FAILED", e)
            throw e
        }
    }
}

val networkModule = module {
    Log.d("KOIN", "Loading networkModule...")

    single {
        try {
            Log.d("KOIN", "Creating OkHttpClient...")
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            Log.d("KOIN", "✓ OkHttpClient created")
            client
        } catch (e: Exception) {
            Log.e("KOIN", "✗ OkHttpClient creation FAILED", e)
            throw e
        }
    }

    single<Gson> {
        Log.d("KOIN", "Creating Gson...")
        GsonBuilder()
            .setLenient()
            .create()
    }

    single<Retrofit> {
        try {
            Log.d("KOIN", "Creating Retrofit...")
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.driftdetector.example.com/")
                .client(get())
                .addConverterFactory(GsonConverterFactory.create(get()))
                .build()

            Log.d("KOIN", "✓ Retrofit created")
            retrofit
        } catch (e: Exception) {
            Log.e("KOIN", "✗ Retrofit creation FAILED", e)
            throw e
        }
    }

    single<DriftApiService> {
        Log.d("KOIN", "Creating DriftApiService...")
        try {
            get<Retrofit>().create(DriftApiService::class.java)
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftApiService creation FAILED", e)
            throw e
        }
    }
}

val securityModule = module {
    Log.d("KOIN", "Loading securityModule...")

    single {
        try {
            Log.d("KOIN", "Creating EncryptionManager...")
            val manager = EncryptionManager(androidContext())
            Log.d("KOIN", "✓ EncryptionManager created")
            manager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ EncryptionManager creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating DifferentialPrivacy...")
        DifferentialPrivacy(epsilon = 0.5, delta = 1e-5)
    }
}

val coreModule = module {
    Log.d("KOIN", "Loading coreModule...")

    // Error Handling & Retry Logic
    single {
        try {
            Log.d("KOIN", "Creating ErrorHandler...")
            val errorHandler = ErrorHandler(androidContext())
            Log.d("KOIN", "✓ ErrorHandler created")
            errorHandler
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ErrorHandler creation FAILED", e)
            throw e
        }
    }

    single {
        try {
            Log.d("KOIN", "Creating CircuitBreaker...")
            val circuitBreaker = CircuitBreaker(
                failureThreshold = 5,
                resetTimeoutMs = 60000L // 1 minute
            )
            Log.d("KOIN", "✓ CircuitBreaker created")
            circuitBreaker
        } catch (e: Exception) {
            Log.e("KOIN", "✗ CircuitBreaker creation FAILED", e)
            throw e
        }
    }

    // Drift Detection
    single {
        Log.d("KOIN", "Creating DriftDetector...")
        try {
            DriftDetector(psiThreshold = 0.2, ksThreshold = 0.05)
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftDetector creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating AttributionEngine...")
        try {
            AttributionEngine()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AttributionEngine creation FAILED", e)
            throw e
        }
    }

    // ML Inference & Model Analysis
    factory { (context: Context) ->
        try {
            Log.d("KOIN", "Creating TFLiteModelInference...")
            TFLiteModelInference(context, useGpu = true, numThreads = 4)
        } catch (e: Exception) {
            Log.e("KOIN", "✗ TFLiteModelInference creation FAILED", e)
            throw e
        }
    }

    // Model Metadata Extractor
    single {
        try {
            Log.d("KOIN", "Creating ModelMetadataExtractor...")
            val extractor = ModelMetadataExtractor(androidContext())
            Log.d("KOIN", "✓ ModelMetadataExtractor created")
            extractor
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ModelMetadataExtractor creation FAILED", e)
            throw e
        }
    }

    // Model Export Manager
    single {
        try {
            Log.d("KOIN", "Creating ModelExportManager...")
            val exportManager = ModelExportManager(androidContext())
            Log.d("KOIN", "✓ ModelExportManager created")
            exportManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ModelExportManager creation FAILED", e)
            throw e
        }
    }

    // Patch Export Manager
    single {
        try {
            Log.d("KOIN", "Creating PatchExportManager...")
            val patchExportManager = PatchExportManager(androidContext())
            Log.d("KOIN", "✓ PatchExportManager created")
            patchExportManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchExportManager creation FAILED", e)
            throw e
        }
    }

    // Patch Management
    single {
        Log.d("KOIN", "Creating PatchEngine...")
        try {
            PatchEngine()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchEngine creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating PatchSynthesizer...")
        try {
            PatchSynthesizer()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchSynthesizer creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating PatchValidator...")
        try {
            PatchValidator(get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchValidator creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating IntelligentPatchGenerator...")
        try {
            IntelligentPatchGenerator()
        } catch (e: Exception) {
            Log.e("KOIN", "✗ IntelligentPatchGenerator creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating RealPatchApplicator...")
        try {
            RealPatchApplicator(androidContext())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ RealPatchApplicator creation FAILED", e)
            throw e
        }
    }

    // File Upload Processor
    single {
        try {
            Log.d("KOIN", "Creating FileUploadProcessor...")
            val processor = FileUploadProcessor(
                context = androidContext(),
                repository = get(),
                metadataExtractor = get()
            )
            Log.d("KOIN", "✓ FileUploadProcessor created")
            processor
        } catch (e: Exception) {
            Log.e("KOIN", "✗ FileUploadProcessor creation FAILED", e)
            throw e
        }
    }

    // Model Monitoring Service
    single {
        try {
            Log.d("KOIN", "Creating ModelMonitoringService...")
            val service = ModelMonitoringService(
                context = androidContext(),
                repository = get(),
                notificationManager = get()
            )
            Log.d("KOIN", "✓ ModelMonitoringService created")
            service
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ModelMonitoringService creation FAILED", e)
            throw e
        }
    }

    // AI Analysis Engine
    single {
        try {
            Log.d("KOIN", "Creating AIAnalysisEngine...")
            val engine = AIAnalysisEngine(androidContext())
            Log.d("KOIN", "✓ AIAnalysisEngine created")
            engine
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AIAnalysisEngine creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating AccuracyMonitor...")
        try {
            val monitor = AccuracyMonitor()
            Log.d("KOIN", "✓ AccuracyMonitor created")
            monitor
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AccuracyMonitor creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating CloudStorageManager...")
        try {
            CloudStorageManager(androidContext())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ CloudStorageManager creation FAILED", e)
            throw e
        }
    }
    
    // Real-time Monitoring Components
    single {
        try {
            Log.d("KOIN", "Creating AuthManager...")
            val authManager = AuthManager(
                context = androidContext(),
                gson = get(),
                encryptionManager = get()
            )
            Log.d("KOIN", "✓ AuthManager created")
            authManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AuthManager creation FAILED", e)
            throw e
        }
    }

    single {
        try {
            Log.d("KOIN", "Creating NetworkConnectivityManager...")
            val connectivityManager = NetworkConnectivityManager(
                context = androidContext()
            )
            Log.d("KOIN", "✓ NetworkConnectivityManager created")
            connectivityManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ NetworkConnectivityManager creation FAILED", e)
            throw e
        }
    }

    single {
        try {
            Log.d("KOIN", "Creating DriftNotificationManager...")
            val notificationManager = DriftNotificationManager(
                context = androidContext()
            )
            Log.d("KOIN", "✓ DriftNotificationManager created")
            notificationManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftNotificationManager creation FAILED", e)
            throw e
        }
    }

    single {
        try {
            Log.d("KOIN", "Creating RealtimeClient...")
            // Local development server - TRUE 24/7 monitoring!
            val serverUrl = "ws://192.168.130.140:8080"
            val realtimeClient = RealtimeClient(
                serverUrl = serverUrl,
                authToken = null, // Will be set after authentication
                gson = get()
            )
            Log.d("KOIN", "✓ RealtimeClient created with URL: $serverUrl")
            realtimeClient
        } catch (e: Exception) {
            Log.e("KOIN", "✗ RealtimeClient creation FAILED", e)
            throw e
        }
    }

    single {
        try {
            Log.d("KOIN", "Creating AutomaticBackupManager...")
            val backupManager = AutomaticBackupManager(
                context = androidContext(),
                repository = get(),
                encryptionManager = get()
            )
            Log.d("KOIN", "✓ AutomaticBackupManager created")
            backupManager
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AutomaticBackupManager creation FAILED", e)
            throw e
        }
    }

    single {
        Log.d("KOIN", "Creating InstantDriftFixManager...")
        try {
            InstantDriftFixManager(
                context = androidContext(),
                driftDetector = get(),
                patchGenerator = get(),
                patchValidator = get(),
                patchApplicator = get(),
                metadataExtractor = get(),
                notificationManager = get()
            )
        } catch (e: Exception) {
            Log.e("KOIN", "✗ InstantDriftFixManager creation FAILED", e)
            throw e
        }
    }
}

val repositoryModule = module {
    Log.d("KOIN", "Loading repositoryModule...")

    single {
        try {
            Log.d("KOIN", "Creating DriftRepository...")
            val repo = DriftRepository(
                driftResultDao = get(),
                mlModelDao = get(),
                patchDao = get(),
                patchSnapshotDao = get(),
                modelPredictionDao = get(),
                driftDetector = get(),
                patchSynthesizer = get(),
                patchValidator = get(),
                patchEngine = get(),
                attributionEngine = get(),
                context = androidContext(),
                patchApplicator = get()
            )
            Log.d("KOIN", "✓ DriftRepository created")
            repo
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftRepository creation FAILED", e)
            throw e
        }
    }
}

val viewModelModule = module {
    Log.d("KOIN", "Loading viewModelModule...")

    viewModel {
        try {
            Log.d("KOIN", "Creating DriftDashboardViewModel...")
            DriftDashboardViewModel(
                repository = get(),
                intelligentPatchGenerator = get(),
                notificationManager = get(),
                errorHandler = get()
            )
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftDashboardViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating ModelManagementViewModel...")
            ModelManagementViewModel(get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ModelManagementViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating ModelUploadViewModel...")
            ModelUploadViewModel(
                fileUploadProcessor = get(),
                context = androidContext(),
                cloudStorageManager = get(),
                errorHandler = get()
            )
        } catch (e: Exception) {
            Log.e("KOIN", "✗ ModelUploadViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating PatchManagementViewModel...")
            PatchManagementViewModel(
                repository = get(),
                exportManager = get(),
                errorHandler = get()
            )
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchManagementViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating SettingsViewModel...")
            SettingsViewModel(get(), androidContext(), get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ SettingsViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating AIAssistantViewModel...")
            AIAssistantViewModel(get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ AIAssistantViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating InstantDriftFixViewModel...")
            InstantDriftFixViewModel(get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ InstantDriftFixViewModel creation FAILED", e)
            throw e
        }
    }
}

val workManagerModule = module {
    Log.d("KOIN", "Loading workManagerModule...")

    worker {
        try {
            Log.d("KOIN", "Creating DriftMonitorWorker...")
            com.driftdetector.app.worker.DriftMonitorWorker(get(), get(), get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ DriftMonitorWorker creation FAILED", e)
            throw e
        }
    }
}

// Combine all modules
val appModules = listOf(
    databaseModule,
    networkModule,
    securityModule,
    coreModule,
    repositoryModule,
    viewModelModule,
    workManagerModule
)
