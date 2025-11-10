package com.driftdetector.app.core.error

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.sql.SQLException

/**
 * Global error handler that converts technical errors into user-friendly messages
 * and provides actionable solutions
 */
class ErrorHandler(private val context: Context) {

    private val _currentError = MutableStateFlow<UserFriendlyError?>(null)
    val currentError: StateFlow<UserFriendlyError?> = _currentError.asStateFlow()

    /**
     * Handle an error and convert it to a user-friendly format
     */
    fun handleError(error: Throwable): UserFriendlyError {
        Timber.e(error, "Error handled by ErrorHandler")

        // Log to Crashlytics for production monitoring
        logToCrashlytics(error)

        val userFriendlyError = when (error) {
            is IOException -> handleNetworkError(error)
            is HttpException -> handleHttpError(error)
            is SQLException -> handleDatabaseError(error)
            is SecurityException -> handleSecurityError(error)
            is OutOfMemoryError -> handleMemoryError(error)
            else -> handleGenericError(error)
        }

        _currentError.value = userFriendlyError
        return userFriendlyError
    }

    /**
     * Clear the current error
     */
    fun clearError() {
        _currentError.value = null
    }

    /**
     * Handle network-related errors
     */
    private fun handleNetworkError(error: IOException): UserFriendlyError {
        return when {
            error.message?.contains("Unable to resolve host", ignoreCase = true) == true -> {
                UserFriendlyError(
                    title = "Connection Issue",
                    message = "Unable to reach the server. Please check your internet connection and try again.",
                    technicalMessage = error.message,
                    action = ErrorAction.AutoRetry(maxRetries = 3),
                    severity = ErrorSeverity.HIGH
                )
            }

            error.message?.contains("timeout", ignoreCase = true) == true -> {
                UserFriendlyError(
                    title = "Connection Timeout",
                    message = "The server is taking too long to respond. Please try again.",
                    technicalMessage = error.message,
                    action = ErrorAction.AutoRetry(maxRetries = 2),
                    severity = ErrorSeverity.MEDIUM
                )
            }

            else -> {
                UserFriendlyError(
                    title = "Network Error",
                    message = "A network error occurred. Please check your connection and try again.",
                    technicalMessage = error.message,
                    action = ErrorAction.AutoRetry(maxRetries = 2),
                    severity = ErrorSeverity.MEDIUM
                )
            }
        }
    }

    /**
     * Handle HTTP errors
     */
    private fun handleHttpError(error: HttpException): UserFriendlyError {
        return when (error.code()) {
            400 -> UserFriendlyError(
                title = "Invalid Request",
                message = "The request was not valid. Please check your input and try again.",
                technicalMessage = "HTTP 400: ${error.message()}",
                action = ErrorAction.Dismiss,
                severity = ErrorSeverity.MEDIUM
            )

            401 -> UserFriendlyError(
                title = "Authentication Required",
                message = "Your session has expired. Please sign in again.",
                technicalMessage = "HTTP 401: ${error.message()}",
                action = ErrorAction.Navigate("login"),
                severity = ErrorSeverity.HIGH
            )

            403 -> UserFriendlyError(
                title = "Access Denied",
                message = "You don't have permission to perform this action.",
                technicalMessage = "HTTP 403: ${error.message()}",
                action = ErrorAction.Dismiss,
                severity = ErrorSeverity.HIGH
            )

            404 -> UserFriendlyError(
                title = "Not Found",
                message = "The requested resource was not found.",
                technicalMessage = "HTTP 404: ${error.message()}",
                action = ErrorAction.Dismiss,
                severity = ErrorSeverity.MEDIUM
            )

            408 -> UserFriendlyError(
                title = "Request Timeout",
                message = "The request timed out. Please try again.",
                technicalMessage = "HTTP 408: ${error.message()}",
                action = ErrorAction.AutoRetry(maxRetries = 2),
                severity = ErrorSeverity.MEDIUM
            )

            500, 502, 503, 504 -> UserFriendlyError(
                title = "Server Error",
                message = "Our servers are experiencing issues. We'll retry automatically.",
                technicalMessage = "HTTP ${error.code()}: ${error.message()}",
                action = ErrorAction.AutoRetry(maxRetries = 3),
                severity = ErrorSeverity.HIGH
            )

            else -> UserFriendlyError(
                title = "Server Error",
                message = "An unexpected server error occurred. Please try again later.",
                technicalMessage = "HTTP ${error.code()}: ${error.message()}",
                action = ErrorAction.Contact(),
                severity = ErrorSeverity.MEDIUM
            )
        }
    }

    /**
     * Handle database errors
     */
    private fun handleDatabaseError(error: SQLException): UserFriendlyError {
        return when {
            error.message?.contains("full", ignoreCase = true) == true -> {
                UserFriendlyError(
                    title = "Storage Full",
                    message = "Your device is running out of storage. Please free up some space.",
                    technicalMessage = error.message,
                    action = ErrorAction.OpenSettings(SettingsType.STORAGE),
                    severity = ErrorSeverity.CRITICAL
                )
            }

            error.message?.contains("corrupt", ignoreCase = true) == true -> {
                UserFriendlyError(
                    title = "Database Issue",
                    message = "The app's database is corrupted. We'll try to fix it automatically.",
                    technicalMessage = error.message,
                    action = ErrorAction.ClearCache {},
                    severity = ErrorSeverity.CRITICAL
                )
            }

            else -> {
                UserFriendlyError(
                    title = "Storage Issue",
                    message = "There was a problem saving data. Please try again.",
                    technicalMessage = error.message,
                    action = ErrorAction.AutoRetry(maxRetries = 2),
                    severity = ErrorSeverity.HIGH
                )
            }
        }
    }

    /**
     * Handle security/permission errors
     */
    private fun handleSecurityError(error: SecurityException): UserFriendlyError {
        return UserFriendlyError(
            title = "Permission Required",
            message = "This feature requires additional permissions. Please grant the necessary permissions.",
            technicalMessage = error.message,
            action = ErrorAction.OpenSettings(SettingsType.PERMISSIONS),
            severity = ErrorSeverity.HIGH
        )
    }

    /**
     * Handle memory errors
     */
    private fun handleMemoryError(error: OutOfMemoryError): UserFriendlyError {
        return UserFriendlyError(
            title = "Memory Issue",
            message = "The app is running out of memory. Try closing other apps or clearing the cache.",
            technicalMessage = error.message,
            action = ErrorAction.ClearCache {},
            severity = ErrorSeverity.CRITICAL
        )
    }

    /**
     * Handle generic errors
     */
    private fun handleGenericError(error: Throwable): UserFriendlyError {
        return UserFriendlyError(
            title = "Unexpected Error",
            message = "Something went wrong. We've logged this issue and will fix it soon.",
            technicalMessage = error.message ?: "Unknown error: ${error::class.simpleName}",
            action = ErrorAction.Contact(),
            severity = ErrorSeverity.MEDIUM
        )
    }

    /**
     * Log error to Crashlytics for production monitoring
     * TODO: Enable Firebase Crashlytics integration
     */
    private fun logToCrashlytics(error: Throwable) {
        try {
            // Log to Timber for now
            Timber.e(error, "[Crashlytics] Error: ${error::class.simpleName} - ${error.message}")

            // TODO: Uncomment when Firebase Crashlytics is configured:
            // Firebase.crashlytics.apply {
            //     setCustomKey("error_type", error::class.simpleName ?: "Unknown")
            //     setCustomKey("error_message", error.message ?: "No message")
            //     recordException(error)
            // }
        } catch (e: Exception) {
            Timber.w(e, "Failed to log to Crashlytics")
        }
    }

    /**
     * Determine if an error is recoverable
     */
    fun isRecoverable(error: Throwable): Boolean {
        return when (error) {
            is IOException -> true  // Network errors are recoverable
            is HttpException -> error.code() in 408..599  // Server errors and timeouts
            is SQLException -> false  // Database errors usually require app restart
            is OutOfMemoryError -> false  // Memory errors are critical
            else -> false
        }
    }

    /**
     * Get recommended retry delay based on error type
     */
    fun getRetryDelay(error: Throwable, attemptNumber: Int): Long {
        val baseDelay = when (error) {
            is IOException -> 1000L  // 1 second for network errors
            is HttpException -> when (error.code()) {
                429 -> 5000L  // 5 seconds for rate limiting
                in 500..599 -> 2000L  // 2 seconds for server errors
                else -> 1000L
            }

            else -> 1000L
        }

        // Exponential backoff: baseDelay * 2^attemptNumber
        return (baseDelay * Math.pow(2.0, attemptNumber.toDouble())).toLong()
            .coerceAtMost(30000L)  // Max 30 seconds
    }
}
