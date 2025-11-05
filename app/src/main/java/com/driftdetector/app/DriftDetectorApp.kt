package com.driftdetector.app

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.driftdetector.app.core.ai.AIAnalysisEngine
import com.driftdetector.app.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Application class for Model Drift Detector
 * Initializes Koin DI and other app-wide components
 * Enhanced with comprehensive crash prevention and performance monitoring
 */
class DriftDetectorApp : Application(), LifecycleObserver {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appStartTime: Long = 0

    // Lazy inject to avoid crash if Koin isn't ready
    private val aiEngine: AIAnalysisEngine by lazy {
        try {
            inject<AIAnalysisEngine>().value
        } catch (e: Exception) {
            Log.e("APP_INIT", "Failed to inject AIAnalysisEngine", e)
            null
        } ?: AIAnalysisEngine(this)
    }

    override fun onCreate() {
        appStartTime = System.currentTimeMillis()
        
        // Install crash handler FIRST before anything else
        installCrashHandler()
        
        // Setup StrictMode for development (helps catch bugs early)
        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }

        logStep("=== APP STARTUP BEGIN ===")
        logStep("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        logStep("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        logStep("Available Memory: ${getAvailableMemoryMB()} MB")

        try {
            super.onCreate()
            logStep("✓ super.onCreate() completed")
        } catch (e: Exception) {
            logError("✗ super.onCreate() FAILED", e)
            throw e
        }

        // Register lifecycle observer to detect app background/foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Initialize Timber for logging
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
                Timber.plant(FileLoggingTree(this))
            } else {
                Timber.plant(CrashReportingTree())
            }
            logStep("✓ Timber initialized")
        } catch (e: Exception) {
            logError("✗ Timber initialization FAILED", e)
            // Continue anyway
        }

        // Initialize Koin with error recovery
        try {
            logStep("Starting Koin initialization...")
            startKoin {
                androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
                androidContext(this@DriftDetectorApp)
                workManagerFactory()
                modules(appModules)
            }
            logStep("✓ Koin initialized successfully")
        } catch (e: Exception) {
            logError("✗ Koin initialization FAILED", e)
            // Don't throw - try to continue with limited functionality
            Log.e("APP_INIT", "App will run with limited functionality", e)
        }

        Timber.d("DriftDetectorApp initialized")
        logStep("✓ DriftDetectorApp base initialization complete")

        // Initialize AI Analysis Engine (RunAnywhere SDK)
        // This runs asynchronously and won't block app startup
        applicationScope.launch {
            try {
                logStep("Starting AI Analysis Engine initialization...")
                Timber.d("Starting AI Analysis Engine initialization...")

                // Add delay to ensure Koin is fully ready
                delay(1000)

                aiEngine.initialize()
                Timber.i("✅ AI Analysis Engine initialized successfully")
                logStep("✓ AI Analysis Engine initialized")
            } catch (e: NoClassDefFoundError) {
                val msg = "⚠️ RunAnywhere SDK not available (AAR files not present)"
                Timber.w(msg)
                logStep(msg)
            } catch (e: ClassNotFoundException) {
                val msg = "⚠️ RunAnywhere SDK classes not found"
                Timber.w(msg)
                logStep(msg)
            } catch (e: Exception) {
                logError("❌ AI Analysis Engine initialization failed", e)
                Timber.e(
                    e,
                    "❌ AI Analysis Engine initialization failed - app will continue with fallback explanations"
                )
            }
        }

        val startupTime = System.currentTimeMillis() - appStartTime
        logStep("=== APP STARTUP COMPLETE (${startupTime}ms) ===")
        Timber.i("App startup completed in ${startupTime}ms")
    }

    override fun onTerminate() {
        logStep("=== APP TERMINATING ===")
        super.onTerminate()
        Timber.d("DriftDetectorApp terminated")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        logStep("⚠️ LOW MEMORY WARNING - System requesting memory release")
        Timber.w("Low memory detected - clearing caches")
        
        // Clear caches to free memory
        try {
            // Force garbage collection
            System.gc()
            Runtime.getRuntime().gc()
            
            logStep("✓ Memory cleanup attempted")
        } catch (e: Exception) {
            logError("✗ Memory cleanup failed", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelStr = when (level) {
            TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            TRIM_MEMORY_MODERATE -> "MODERATE"
            TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        
        logStep("Memory trim requested: $levelStr")
        Timber.d("onTrimMemory: $levelStr")
        
        // Aggressive cleanup for critical memory situations
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            System.gc()
            Runtime.getRuntime().gc()
            logStep("✓ Aggressive memory cleanup performed")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        logStep("🟢 App moved to FOREGROUND")
        Timber.d("App foregrounded")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        logStep("⚫ App moved to BACKGROUND")
        Timber.d("App backgrounded")
    }

    private fun setupStrictMode() {
        try {
            // Thread policy - detect violations on main thread
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            // VM policy - detect memory leaks and other issues
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            
            logStep("✓ StrictMode enabled (debug mode)")
        } catch (e: Exception) {
            logError("✗ StrictMode setup failed", e)
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logError("!!!!! FATAL CRASH !!!!!", throwable)
                Log.e("CRASH", "===== FATAL CRASH DETECTED =====")
                Log.e("CRASH", "Thread: ${thread.name}")
                Log.e("CRASH", "Exception: ${throwable.javaClass.name}")
                Log.e("CRASH", "Message: ${throwable.message}")

                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                Log.e("CRASH", "Stack trace:\n$sw")

                // Save crash log to file
                saveCrashLog(throwable, thread)
                
                // Attempt graceful shutdown
                try {
                    applicationScope.launch {
                        delay(100) // Give time for log writing
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            } catch (e: Exception) {
                Log.e("CRASH", "Error in crash handler: ${e.message}")
            }

            // Call default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable, thread: Thread) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val crashFile = File(filesDir, "crash_$timestamp.log")

            crashFile.writeText(buildString {
                appendLine("===== CRASH LOG =====")
                appendLine("Time: $timestamp")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Available Memory: ${getAvailableMemoryMB()} MB")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message}")
                appendLine("\nStack Trace:")

                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append(sw.toString())

                appendLine("\n\nCaused by:")
                var cause = throwable.cause
                while (cause != null) {
                    appendLine("${cause.javaClass.name}: ${cause.message}")
                    cause.printStackTrace(PrintWriter(StringWriter().also { append(it.toString()) }))
                    cause = cause.cause
                }
            })

            Log.e("CRASH", "Crash log saved to: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CRASH", "Failed to save crash log: ${e.message}")
        }
    }

    private fun getAvailableMemoryMB(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            maxMemory - usedMemory
        } catch (e: Exception) {
            -1
        }
    }

    private fun logStep(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val logMessage = "[$timestamp] $message"
        Log.d("APP_INIT", logMessage)

        // Also log to file
        try {
            val logFile = File(filesDir, "app_init.log")
            logFile.appendText("$logMessage\n")
        } catch (e: Exception) {
            // Ignore file logging errors
        }
    }

    private fun logError(message: String, throwable: Throwable) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        Log.e("APP_INIT", "[$timestamp] $message", throwable)

        // Also log to file
        try {
            val logFile = File(filesDir, "app_init.log")
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            logFile.appendText("[$timestamp] $message\n$sw\n")
        } catch (e: Exception) {
            // Ignore file logging errors
        }
    }
}

/**
 * Custom Timber tree for production crash reporting
 */
private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
            // In production, send to crash reporting service (e.g., Firebase Crashlytics)
            // For now, just log to system
            android.util.Log.println(priority, tag ?: "DriftDetector", message)
            t?.let {
                android.util.Log.println(
                    priority,
                    tag ?: "DriftDetector",
                    it.stackTraceToString()
                )
            }
        }
    }
}

/**
 * File logging tree for debugging
 */
private class FileLoggingTree(private val context: Application) : Timber.Tree() {
    private val logFile = File(context.filesDir, "timber.log")

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val priorityStr = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                else -> "?"
            }

            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            val logMessage = "[$timestamp] $priorityStr/$tag: $message\n"

            logFile.appendText(logMessage)

            t?.let {
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                logFile.appendText(sw.toString() + "\n")
            }
        } catch (e: Exception) {
            // Ignore file logging errors
        }
    }
}
