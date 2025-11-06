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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about patch synthesis and deployment"
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

            val notification = NotificationCompat.Builder(context, CHANNEL_DRIFT_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText("Model: $modelName (Score: ${String.format("%.3f", driftScore)})")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Model '$modelName' has detected drift.\nDrift Score: ${
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
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
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
}
