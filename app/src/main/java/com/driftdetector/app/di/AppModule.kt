package com.driftdetector.app.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.driftdetector.app.core.ai.AIAnalysisEngine
import com.driftdetector.app.core.drift.AttributionEngine
import com.driftdetector.app.core.drift.DriftDetector
import com.driftdetector.app.core.ml.TFLiteModelInference
import com.driftdetector.app.core.patch.PatchEngine
import com.driftdetector.app.core.patch.PatchSynthesizer
import com.driftdetector.app.core.patch.PatchValidator
import com.driftdetector.app.core.security.DifferentialPrivacy
import com.driftdetector.app.core.security.EncryptionManager
import com.driftdetector.app.data.local.DriftDatabase
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.data.remote.DriftApiService
import com.driftdetector.app.presentation.viewmodel.AIAssistantViewModel
import com.driftdetector.app.presentation.viewmodel.DriftDashboardViewModel
import com.driftdetector.app.presentation.viewmodel.ModelManagementViewModel
import com.driftdetector.app.presentation.viewmodel.PatchManagementViewModel
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.sqlcipher.database.SupportFactory
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
            Log.d("KOIN", "Creating encrypted database...")
            val passphrase = "DriftDetectorSecureKey2024".toByteArray()
            val factory = SupportFactory(passphrase)

            val db = Room.databaseBuilder(
                androidContext(),
                DriftDatabase::class.java,
                DriftDatabase.DATABASE_NAME
            )
                .openHelperFactory(factory)
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

    // ML Inference
    factory { (context: Context) ->
        try {
            Log.d("KOIN", "Creating TFLiteModelInference...")
            TFLiteModelInference(context, useGpu = true, numThreads = 4)
        } catch (e: Exception) {
            Log.e("KOIN", "✗ TFLiteModelInference creation FAILED", e)
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
                attributionEngine = get()
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
            DriftDashboardViewModel(get())
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
            Log.d("KOIN", "Creating PatchManagementViewModel...")
            PatchManagementViewModel(get())
        } catch (e: Exception) {
            Log.e("KOIN", "✗ PatchManagementViewModel creation FAILED", e)
            throw e
        }
    }

    viewModel {
        try {
            Log.d("KOIN", "Creating SettingsViewModel...")
            SettingsViewModel(get(), androidContext())
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
