package com.driftdetector.app.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.driftdetector.app.R
import com.driftdetector.app.presentation.MainActivity
import timber.log.Timber

/**
 * Manages push notifications for drift alerts and monitoring updates
 */
class DriftNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_DRIFT_ALERTS = "drift_alerts"
        const val CHANNEL_MONITORING = "monitoring"
        const val CHANNEL_PATCHES = "patches"

        const val NOTIFICATION_ID_DRIFT_ALERT = 1001
        const val NOTIFICATION_ID_MONITORING = 1002
        const val NOTIFICATION_ID_PATCH = 1003
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val driftAlertsChannel = NotificationChannel(
                CHANNEL_DRIFT_ALERTS,
                "Drift Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for model drift detection"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFFF9800.toInt() // Orange light
            }

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Monitoring Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time monitoring status updates"
            }

            val patchesChannel = NotificationChannel(
                CHANNEL_PATCHES,
                "Patch Notifications",
                NotificationManager.IMPORTANCE_HIGH // Changed from DEFAULT to HIGH for better visibility
            ).apply {
                description = "Notifications about patch synthesis and deployment"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt() // Green light
                setShowBadge(true) // Show badge on app icon
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(driftAlertsChannel)
            manager.createNotificationChannel(monitoringChannel)
            manager.createNotificationChannel(patchesChannel)

            Timber.d("✅ Notification channels created")
        }
    }

    /**
     * Check if we have permission to post notifications
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed pre-Android 13
        }
    }

    /**
     * Show drift alert notification
     */
    @SuppressLint("MissingPermission")
    fun showDriftAlert(
        modelId: String,
        modelName: String,
        driftScore: Double,
        severity: String
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "dashboard")
                putExtra("model_id", modelId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = when (severity.lowercase()) {
                "critical" -> "🚨 Critical Drift Detected"
                "high" -> "⚠️ High Drift Alert"
                "medium" -> "⚠️ Drift Detected"
                else -> "ℹ️ Drift Notice"
            }

            // Color based on severity
            val notificationColor = when (severity.lowercase()) {
                "critical" -> 0xFFF44336.toInt() // Red
                "high" -> 0xFFFF9800.toInt() // Orange
                "medium" -> 0xFFFFC107.toInt() // Amber
                else -> 0xFF2196F3.toInt() // Blue
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_DRIFT_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setColor(notificationColor) // Add color to notification
                .setColorized(true) // Color the entire notification header
                .setContentTitle(title)
                .setContentText("Model: $modelName (Score: ${String.format("%.3f", driftScore)})")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Model '$modelName' has detected drift.\n\nDrift Score: ${
                                String.format(
                                    "%.3f",
                                    driftScore
                                )
                            }\nSeverity: $severity\n\nTap to view details and recommendations."
                        )
                )
                .setPriority(
                    when (severity.lowercase()) {
                        "critical", "high" -> NotificationCompat.PRIORITY_HIGH
                        else -> NotificationCompat.PRIORITY_DEFAULT
                    }
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setLights(notificationColor, 1000, 1000) // LED lights matching severity
                .setVibrate(longArrayOf(0, 200, 100, 200)) // Noticeable vibration pattern
                .build()

            notificationManager.notify(NOTIFICATION_ID_DRIFT_ALERT, notification)

            Timber.i("📬 Drift alert notification sent for model: $modelName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show drift alert notification")
        }
    }

    /**
     * Show monitoring status notification
     */
    @SuppressLint("MissingPermission")
    fun showMonitoringStatus(
        activeModels: Int,
        driftsDetected: Int,
        patchesSynthesized: Int
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "dashboard")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_MONITORING)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setColor(0xFF2196F3.toInt()) // Blue color
                .setColorized(true) // Color the entire notification header
                .setContentTitle("Monitoring Status")
                .setContentText("$activeModels models active, $driftsDetected drifts detected")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Active Models: $activeModels\nDrifts Detected: $driftsDetected\nPatches Synthesized: $patchesSynthesized")
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

            notificationManager.notify(NOTIFICATION_ID_MONITORING, notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show monitoring status notification")
        }
    }

    /**
     * Clear monitoring status notification
     */
    fun clearMonitoringStatus() {
        notificationManager.cancel(NOTIFICATION_ID_MONITORING)
    }

    /**
     * Show patch synthesis notification
     */
    @SuppressLint("MissingPermission")
    fun showPatchSynthesized(
        modelName: String,
        patchType: String,
        safetyScore: Double
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "patches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setColor(0xFF4CAF50.toInt()) // Green color
                .setColorized(true) // Color the entire notification header
                .setContentTitle("✅ Patch Ready")
                .setContentText(
                    "$patchType patch for $modelName (Safety: ${
                        String.format(
                            "%.2f",
                            safetyScore
                        )
                    })"
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "A $patchType patch has been synthesized for model '$modelName'.\n\nSafety Score: ${
                                String.format(
                                    "%.2f",
                                    safetyScore
                                )
                            }\n\nTap to review and deploy."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID_PATCH, notification)

            Timber.i("📬 Patch notification sent for model: $modelName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show patch notification")
        }
    }

    /**
     * Show SUMMARY notification for multiple patches synthesized (prevents notification spam)
     */
    @SuppressLint("MissingPermission")
    fun showPatchesSynthesizedSummary(
        modelName: String,
        totalPatches: Int,
        appliedPatches: Int
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "patches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (appliedPatches > 0) {
                "✅ $appliedPatches/$totalPatches Patches Applied"
            } else {
                "🔧 $totalPatches Drift Patches Ready"
            }

            val text = if (appliedPatches > 0) {
                "$appliedPatches patches automatically applied • Tap to see full details"
            } else {
                "$totalPatches intelligent patches generated • Ready to reduce drift by up to 100%"
            }

            val bigText = buildString {
                appendLine("Model: $modelName")
                appendLine()
                if (appliedPatches > 0) {
                    appendLine("✅ Auto-Applied: $appliedPatches patches")
                    appendLine("📋 Total Generated: $totalPatches patches")
                    appendLine()
                    appendLine("Your model now has enhanced drift protection.")
                } else {
                    appendLine("🔧 Generated: $totalPatches intelligent patches")
                    appendLine("🎯 Expected Impact: 95-100% drift reduction")
                    appendLine()
                    appendLine("Patch Types May Include:")
                    appendLine("• Feature Clipping (outlier control)")
                    appendLine("• Normalization (distribution alignment)")
                    appendLine("• Feature Reweighting (importance adjustment)")
                    appendLine("• Threshold Tuning (decision optimization)")
                }
                appendLine()
                appendLine("👆 Tap to view detailed patch information and manage all patches")
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setColor(0xFF4CAF50.toInt()) // Green color for the notification icon and header
                .setColorized(true) // Color the entire notification header
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Make it more visible
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVibrate(longArrayOf(0, 100, 100, 100)) // Gentle vibration pattern
                .setLights(0xFF4CAF50.toInt(), 1000, 1000)
                .build()

            // Use unique ID to avoid flickering
            val notificationId = NOTIFICATION_ID_PATCH + modelName.hashCode()
            notificationManager.notify(notificationId, notification)

            Timber.i("📬 Enhanced patch summary notification sent: $appliedPatches/$totalPatches patches for $modelName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show patch summary notification")
        }
    }

    /**
     * Show patch deployment notification
     */
    @SuppressLint("MissingPermission")
    fun showPatchDeployed(
        modelName: String,
        success: Boolean,
        message: String? = null
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "patches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (success) "✅ Patch Deployed" else "❌ Deployment Failed"
            val text = message ?: if (success) {
                "Patch successfully deployed to $modelName"
            } else {
                "Failed to deploy patch to $modelName"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setColor(if (success) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()) // Green for success, red for failure
                .setColorized(true) // Color the entire notification header
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setLights(if (success) 0xFF4CAF50.toInt() else 0xFFF44336.toInt(), 1000, 1000)
                .build()

            notificationManager.notify(NOTIFICATION_ID_PATCH, notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show patch deployment notification")
        }
    }

    /**
     * Show connection status notification
     */
    @SuppressLint("MissingPermission")
    fun showConnectionStatus(connected: Boolean, serverUrl: String) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val title = if (connected) "🟢 Connected" else "⚫ Disconnected"
            val text = if (connected) {
                "Real-time monitoring active"
            } else {
                "Lost connection to server"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_MONITORING)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setColor(0xFF2196F3.toInt()) // Blue color
                .setColorized(true) // Color the entire notification header
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(connected)
                .build()

            notificationManager.notify(NOTIFICATION_ID_MONITORING, notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to show connection status notification")
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Show generic error notification
     */
    @SuppressLint("MissingPermission")
    fun showError(title: String, message: String) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_DRIFT_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setColor(0xFFF44336.toInt()) // Red
                .setColorized(true)
                .setContentTitle("❌ $title")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            notificationManager.notify(NOTIFICATION_ID_DRIFT_ALERT + 100, notification)
            Timber.e("📬 Error notification sent: $title")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show error notification")
        }
    }

    /**
     * Show patch validation warning
     */
    @SuppressLint("MissingPermission")
    fun showPatchValidationWarning(rejectedCount: Int, totalCount: Int) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "patches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val validatedCount = totalCount - rejectedCount

            val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setColor(0xFFFF9800.toInt()) // Orange
                .setColorized(true)
                .setContentTitle("⚠️ Patch Validation Warning")
                .setContentText("$rejectedCount of $totalCount patches rejected due to safety concerns")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Patch Validation Results:\n\n" +
                                    "✅ Validated: $validatedCount patches\n" +
                                    "❌ Rejected: $rejectedCount patches\n\n" +
                                    "Only validated patches meeting safety criteria (safety > 0.7, drift reduction > 0.6) will be applied.\n\n" +
                                    "Tap to review details."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID_PATCH + 200, notification)
            Timber.w("📬 Patch validation warning sent: $rejectedCount/$totalCount rejected")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show patch validation warning")
        }
    }

    /**
     * Show patch success notification
     */
    @SuppressLint("MissingPermission")
    fun showPatchSuccess(appliedCount: Int, driftReduction: Int) {
        if (!hasNotificationPermission()) {
            Timber.w("No notification permission granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "patches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setColor(0xFF4CAF50.toInt()) // Green
                .setColorized(true)
                .setContentTitle("✅ Patches Applied Successfully")
                .setContentText("$appliedCount patches applied • ${driftReduction}% drift reduction")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Patch Application Success!\n\n" +
                                    "Applied Patches: $appliedCount\n" +
                                    "Drift Reduction: ${driftReduction}%\n\n" +
                                    "Your model's drift has been significantly reduced. " +
                                    "Download the patched files to use in production.\n\n" +
                                    "Tap to download patched files."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 100, 50, 100))
                .setLights(0xFF4CAF50.toInt(), 1000, 1000)
                .build()

            notificationManager.notify(NOTIFICATION_ID_PATCH + 300, notification)
            Timber.i("📬 Patch success notification sent: $appliedCount patches, ${driftReduction}% reduction")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show patch success notification")
        }
    }
}
