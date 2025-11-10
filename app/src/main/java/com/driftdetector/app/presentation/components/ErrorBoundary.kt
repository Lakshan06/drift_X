package com.driftdetector.app.presentation.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.core.error.*
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * Error boundary composable that catches and displays errors
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    errorHandler: ErrorHandler = koinInject(),
    content: @Composable () -> Unit
) {
    val currentError by errorHandler.currentError.collectAsState()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        content()

        // Show error overlay when error occurs
        AnimatedVisibility(
            visible = currentError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            currentError?.let { error ->
                ErrorScreen(
                    error = error,
                    onDismiss = { errorHandler.clearError() },
                    onAction = { action ->
                        handleErrorAction(context, action)
                        errorHandler.clearError()
                    }
                )
            }
        }
    }
}

/**
 * Full-screen error display
 */
@Composable
fun ErrorScreen(
    error: UserFriendlyError,
    onDismiss: () -> Unit,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error icon
            Icon(
                imageVector = when (error.severity) {
                    ErrorSeverity.CRITICAL -> Icons.Default.Error
                    ErrorSeverity.HIGH -> Icons.Default.Warning
                    ErrorSeverity.MEDIUM -> Icons.Default.Info
                    ErrorSeverity.LOW -> Icons.Default.Info
                },
                contentDescription = "Error icon",
                modifier = Modifier.size(64.dp),
                tint = when (error.severity) {
                    ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                    ErrorSeverity.HIGH -> MaterialTheme.colorScheme.error
                    ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.primary
                    ErrorSeverity.LOW -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(Modifier.height(24.dp))

            // Error title
            Text(
                text = error.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Error message
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Action buttons
            ErrorActionButtons(
                action = error.action,
                onAction = onAction,
                onDismiss = onDismiss
            )

            // Technical details (expandable)
            error.technicalMessage?.let { technical ->
                Spacer(Modifier.height(24.dp))
                TechnicalDetailsCard(technicalMessage = technical)
            }
        }
    }
}

/**
 * Error action buttons
 */
@Composable
private fun ErrorActionButtons(
    action: ErrorAction,
    onAction: (ErrorAction) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (action) {
            is ErrorAction.Dismiss -> {
                Button(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }

            is ErrorAction.Retry -> {
                Button(
                    onClick = {
                        onAction(action)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(Modifier.width(8.dp))
                    Text("Retry")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

            is ErrorAction.AutoRetry -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text("Retrying automatically...")
            }

            is ErrorAction.Navigate -> {
                Button(onClick = { onAction(action) }) {
                    Text("Continue")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

            is ErrorAction.ClearCache -> {
                Button(onClick = { onAction(action) }) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear cache")
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Cache")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

            is ErrorAction.OpenSettings -> {
                Button(onClick = { onAction(action) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Open settings")
                    Spacer(Modifier.width(8.dp))
                    Text("Open Settings")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

            is ErrorAction.Contact -> {
                Button(onClick = { onAction(action) }) {
                    Icon(Icons.Default.Email, contentDescription = "Contact support")
                    Spacer(Modifier.width(8.dp))
                    Text("Contact Support")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Technical details card (expandable)
 */
@Composable
private fun TechnicalDetailsCard(technicalMessage: String) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Text("Technical Details")
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details"
            )
        }

        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = technicalMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact error display (for inline errors)
 */
@Composable
fun CompactErrorDisplay(
    error: UserFriendlyError,
    onDismiss: () -> Unit,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (error.severity) {
                ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                ErrorSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (error.severity) {
                    ErrorSeverity.CRITICAL, ErrorSeverity.HIGH -> Icons.Default.Error
                    else -> Icons.Default.Info
                },
                contentDescription = "Error icon",
                tint = when (error.severity) {
                    ErrorSeverity.CRITICAL, ErrorSeverity.HIGH -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

/**
 * Handle error actions
 */
private fun handleErrorAction(context: android.content.Context, action: ErrorAction) {
    when (action) {
        is ErrorAction.Navigate -> {
            // Handle navigation
            Timber.d("Navigate to: ${action.route}")
        }

        is ErrorAction.OpenSettings -> {
            val intent = when (action.settingsType) {
                SettingsType.NETWORK -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                SettingsType.STORAGE -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                SettingsType.PERMISSIONS -> Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )

                SettingsType.APP_INFO -> Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            }
            context.startActivity(intent)
        }

        is ErrorAction.Contact -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${action.supportEmail}")
                putExtra(Intent.EXTRA_SUBJECT, "DriftGuardAI Support Request")
            }
            context.startActivity(Intent.createChooser(intent, "Contact Support"))
        }

        else -> {
            // Other actions handled by calling code
        }
    }
}

/**
 * Retry indicator for auto-retry operations
 */
@Composable
fun RetryIndicator(
    currentAttempt: Int,
    maxAttempts: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Retrying... ($currentAttempt/$maxAttempts)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
