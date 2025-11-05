package com.driftdetector.app

import android.app.Application
import android.util.Log
import com.driftdetector.app.core.ai.AIAnalysisEngine
import com.driftdetector.app.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

/**
 * Application class for Model Drift Detector
 * Initializes Koin DI and other app-wide components
 */
class DriftDetectorApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val aiEngine: AIAnalysisEngine by inject()

    override fun onCreate() {
        // Install crash handler FIRST before anything else
        installCrashHandler()

        logStep("=== APP STARTUP BEGIN ===")

        try {
            super.onCreate()
            logStep("✓ super.onCreate() completed")
        } catch (e: Exception) {
            logError("✗ super.onCreate() FAILED", e)
            throw e
        }

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

        // Initialize Koin
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
            throw e // This is critical, can't continue
        }

        Timber.d("DriftDetectorApp initialized")
        logStep("✓ DriftDetectorApp base initialization complete")

        // Initialize AI Analysis Engine (RunAnywhere SDK)
        // This runs asynchronously and won't block app startup
        applicationScope.launch {
            try {
                logStep("Starting AI Analysis Engine initialization...")
                Timber.d("Starting AI Analysis Engine initialization...")
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

        logStep("=== APP STARTUP COMPLETE ===")
    }

    override fun onTerminate() {
        logStep("=== APP TERMINATING ===")
        super.onTerminate()
        Timber.d("DriftDetectorApp terminated")
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
