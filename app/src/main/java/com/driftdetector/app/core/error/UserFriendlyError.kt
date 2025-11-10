package com.driftdetector.app.core.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Represents a user-friendly error with actionable solutions
 */
data class UserFriendlyError(
    val title: String,
    val message: String,
    val technicalMessage: String? = null,
    val action: ErrorAction = ErrorAction.Dismiss,
    val severity: ErrorSeverity = ErrorSeverity.MEDIUM
)

/**
 * Actions that users can take to resolve errors
 */
sealed class ErrorAction {
    object Dismiss : ErrorAction()
    data class Retry(val onRetry: suspend () -> Unit) : ErrorAction()
    data class AutoRetry(val maxRetries: Int = 3) : ErrorAction()
    data class Navigate(val route: String) : ErrorAction()
    data class ClearCache(val onClearCache: suspend () -> Unit) : ErrorAction()
    data class OpenSettings(val settingsType: SettingsType) : ErrorAction()
    data class Contact(val supportEmail: String = "support@driftguardai.com") : ErrorAction()
}

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    LOW,      // Informational, app continues normally
    MEDIUM,   // Warning, some features may not work
    HIGH,     // Error, feature unavailable
    CRITICAL  // Critical, app may crash or lose data
}

/**
 * Types of settings that can be opened
 */
enum class SettingsType {
    NETWORK,
    STORAGE,
    PERMISSIONS,
    APP_INFO
}

/**
 * Extension to convert exceptions to user-friendly errors
 */
fun Throwable.toUserFriendlyError(): UserFriendlyError {
    return when (this) {
        is UnknownHostException -> UserFriendlyError(
            title = "No Internet Connection",
            message = "Unable to connect to the server. Please check your internet connection and try again.",
            technicalMessage = this.message,
            action = ErrorAction.OpenSettings(SettingsType.NETWORK),
            severity = ErrorSeverity.HIGH
        )

        is SocketTimeoutException -> UserFriendlyError(
            title = "Connection Timeout",
            message = "The server is taking too long to respond. Please try again.",
            technicalMessage = this.message,
            action = ErrorAction.AutoRetry(maxRetries = 3),
            severity = ErrorSeverity.MEDIUM
        )

        is IOException -> UserFriendlyError(
            title = "Connection Issue",
            message = "There was a problem connecting to the server. Please check your connection and try again.",
            technicalMessage = this.message,
            action = ErrorAction.AutoRetry(maxRetries = 2),
            severity = ErrorSeverity.MEDIUM
        )

        is OutOfMemoryError -> UserFriendlyError(
            title = "Memory Issue",
            message = "The app is running out of memory. Try closing other apps or clearing the cache.",
            technicalMessage = this.message,
            action = ErrorAction.ClearCache {},
            severity = ErrorSeverity.CRITICAL
        )

        is SecurityException -> UserFriendlyError(
            title = "Permission Required",
            message = "This feature requires additional permissions. Please grant the necessary permissions in settings.",
            technicalMessage = this.message,
            action = ErrorAction.OpenSettings(SettingsType.PERMISSIONS),
            severity = ErrorSeverity.HIGH
        )

        else -> UserFriendlyError(
            title = "Unexpected Error",
            message = "Something went wrong. We've logged this issue and will fix it soon.",
            technicalMessage = this.message ?: "Unknown error",
            action = ErrorAction.Contact(),
            severity = ErrorSeverity.MEDIUM
        )
    }
}
