package com.driftdetector.app.core.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.driftdetector.app.R
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.presentation.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Smart Notification and Alert System
 *
 * Features:
 * - Intelligent notification grouping
 * - Priority-based scheduling
 * - Rate limiting to prevent spam
 * - User preference management
 * - Smart Do Not Disturb mode
 * - Notification analytics
 * - Action buttons for quick responses
 */
class SmartNotificationManager(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val systemNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Notification history for analytics
    private val notificationHistory = mutableListOf<NotificationEvent>()

    // Rate limiting
    private val lastNotificationTime = mutableMapOf<String, Long>()
    private val MIN_NOTIFICATION_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5)

    // Notification counters for grouping
    private var driftAlertCount = 0
    private var patchAlertCount = 0

    // State flow for notification stats
    private val _notificationStats = MutableStateFlow(NotificationStats())
    val notificationStats: StateFlow<NotificationStats> = _notificationStats

    companion object {
        // Channel Groups
        const val GROUP_ALERTS = "alerts_group"
        const val GROUP_UPDATES = "updates_group"
        const val GROUP_SYSTEM = "system_group"

        // Channels
        const val CHANNEL_CRITICAL = "critical_alerts"
        const val CHANNEL_HIGH = "high_priority_alerts"
        const val CHANNEL_MEDIUM = "medium_priority_alerts"
        const val CHANNEL_LOW = "low_priority_alerts"
        const val CHANNEL_PATCHES = "patch_notifications"
        const val CHANNEL_MONITORING = "monitoring_updates"
        const val CHANNEL_DIGEST = "daily_digest"
        const val CHANNEL_SYSTEM = "system_notifications"

        // Notification IDs
        const val ID_CRITICAL_BASE = 1000
        const val ID_HIGH_BASE = 2000
        const val ID_MEDIUM_BASE = 3000
        const val ID_LOW_BASE = 4000
        const val ID_PATCH_BASE = 5000
        const val ID_MONITORING = 6000
        const val ID_DIGEST = 7000
        const val ID_SYSTEM = 8000

        // Summary IDs
        const val ID_DRIFT_SUMMARY = 9000
        const val ID_PATCH_SUMMARY = 9001

        // Actions
        const val ACTION_VIEW_DETAILS = "action_view_details"
        const val ACTION_APPLY_PATCH = "action_apply_patch"
        const val ACTION_DISMISS = "action_dismiss"
        const val ACTION_SNOOZE = "action_snooze"
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels with proper grouping
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel groups
            val alertsGroup = NotificationChannelGroup(GROUP_ALERTS, "Drift & Patch Alerts")
            val updatesGroup = NotificationChannelGroup(GROUP_UPDATES, "Monitoring Updates")
            val systemGroup = NotificationChannelGroup(GROUP_SYSTEM, "System Notifications")

            systemNotificationManager.createNotificationChannelGroup(alertsGroup)
            systemNotificationManager.createNotificationChannelGroup(updatesGroup)
            systemNotificationManager.createNotificationChannelGroup(systemGroup)

            // Critical alerts
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                "Critical Drift Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical drift events requiring immediate attention"
                group = GROUP_ALERTS
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFF44336.toInt() // Red
                setBypassDnd(true) // Bypass Do Not Disturb
                setShowBadge(true)
            }

            // High priority alerts
            val highChannel = NotificationChannel(
                CHANNEL_HIGH,
                "High Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority drift events"
                group = GROUP_ALERTS
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFFF9800.toInt() // Orange
                setShowBadge(true)
            }

            // Medium priority alerts
            val mediumChannel = NotificationChannel(
                CHANNEL_MEDIUM,
                "Medium Priority Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Medium priority drift events"
                group = GROUP_ALERTS
                enableVibration(false)
                setShowBadge(true)
            }

            // Low priority alerts
            val lowChannel = NotificationChannel(
                CHANNEL_LOW,
                "Low Priority Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low priority drift events and info"
                group = GROUP_ALERTS
                setShowBadge(false)
            }

            // Patch notifications
            val patchChannel = NotificationChannel(
                CHANNEL_PATCHES,
                "Patch Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about patch synthesis and deployment"
                group = GROUP_ALERTS
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt() // Green
                setShowBadge(true)
            }

            // Monitoring updates
            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Monitoring Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time monitoring status"
                group = GROUP_UPDATES
                setShowBadge(false)
            }

            // Daily digest
            val digestChannel = NotificationChannel(
                CHANNEL_DIGEST,
                "Daily Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of drift events and patches"
                group = GROUP_UPDATES
                setShowBadge(true)
            }

            // System notifications
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "App updates and system messages"
                group = GROUP_SYSTEM
                setShowBadge(false)
            }

            // Create all channels
            systemNotificationManager.createNotificationChannels(
                listOf(
                    criticalChannel, highChannel, mediumChannel, lowChannel,
                    patchChannel, monitoringChannel, digestChannel, systemChannel
                )
            )

            Timber.d("✅ Smart notification channels created")
        }
    }

    /**
     * Show smart drift alert with priority-based handling
     */
    @SuppressLint("MissingPermission")
    fun showDriftAlert(
        model: MLModel,
        driftResult: DriftResult,
        priority: AlertPriority = determinePriority(driftResult.driftScore)
    ) {
        // Rate limiting check
        if (!shouldShowNotification("drift_${model.id}", priority)) {
            Timber.d("⏳ Rate limiting: Skipping drift notification for ${model.name}")
            return
        }

        val channel = getChannelForPriority(priority)
        val notificationId = getNotificationId(priority, model.id.hashCode())

        // Create intent with deep link
        val intent = createDeepLinkIntent(
            "drift_details", mapOf(
                "model_id" to model.id,
                "drift_id" to driftResult.id
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action buttons
        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "View Details",
            pendingIntent
        ).build()

        val snoozeAction = createSnoozeAction(notificationId)

        // Build notification
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(getPriorityIcon(priority))
            .setColor(getPriorityColor(priority))
            .setColorized(true)
            .setContentTitle(getAlertTitle(priority, "Drift"))
            .setContentText(
                "${model.name}: Drift score ${
                    String.format(
                        "%.3f",
                        driftResult.driftScore
                    )
                }"
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildDriftMessage(model, driftResult, priority)
                )
            )
            .setPriority(getNotificationPriority(priority))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(viewAction)
            .setGroup("DRIFT_ALERTS")
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setWhen(System.currentTimeMillis())
            .build()

        if (priority == AlertPriority.CRITICAL || priority == AlertPriority.HIGH) {
            notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT
        }

        // Show notification
        notificationManager.notify(notificationId, notification)

        // Update counters
        driftAlertCount++

        // Show summary if needed
        if (driftAlertCount >= 3) {
            showDriftSummaryNotification()
        }

        // Track notification
        trackNotification(
            NotificationEvent(
                type = NotificationType.DRIFT_ALERT,
                priority = priority,
                modelId = model.id,
                timestamp = System.currentTimeMillis()
            )
        )

        Timber.i("📬 Smart drift alert sent: ${model.name} (Priority: $priority)")
    }

    /**
     * Show smart patch notification
     */
    @SuppressLint("MissingPermission")
    fun showPatchNotification(
        model: MLModel,
        patch: Patch,
        autoApplied: Boolean = false
    ) {
        // Rate limiting
        if (!shouldShowNotification("patch_${model.id}", AlertPriority.HIGH)) {
            Timber.d("⏳ Rate limiting: Skipping patch notification for ${model.name}")
            return
        }

        val notificationId = ID_PATCH_BASE + patch.id.hashCode()

        val intent = createDeepLinkIntent(
            "patch_details", mapOf(
                "model_id" to model.id,
                "patch_id" to patch.id
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action buttons
        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "View Patch",
            pendingIntent
        ).build()

        val applyAction = if (!autoApplied) {
            createApplyPatchAction(patch.id, notificationId)
        } else null

        val title = if (autoApplied) {
            "✅ Patch Auto-Applied"
        } else {
            "🔧 New Patch Ready"
        }

        val text = "${model.name}: ${patch.patchType} patch" +
                " (Safety: ${
                    String.format(
                        "%.0f%%",
                        patch.validationResult?.metrics?.safetyScore ?: 0.0
                    )
                })"

        val builder = NotificationCompat.Builder(context, CHANNEL_PATCHES)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setColor(0xFF4CAF50.toInt())
            .setColorized(true)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildPatchMessage(model, patch, autoApplied)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(viewAction)
            .setGroup("PATCH_ALERTS")
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setWhen(System.currentTimeMillis())

        applyAction?.let { builder.addAction(it) }

        notificationManager.notify(notificationId, builder.build())

        // Update counters
        patchAlertCount++

        // Show summary if needed
        if (patchAlertCount >= 3) {
            showPatchSummaryNotification()
        }

        // Track notification
        trackNotification(
            NotificationEvent(
                type = NotificationType.PATCH_NOTIFICATION,
                priority = AlertPriority.HIGH,
                modelId = model.id,
                timestamp = System.currentTimeMillis()
            )
        )

        Timber.i("📬 Smart patch notification sent: ${model.name}")
    }

    /**
     * Show daily digest notification
     */
    @SuppressLint("MissingPermission")
    fun showDailyDigest(
        driftEvents: Int,
        patchesSynthesized: Int,
        patchesApplied: Int,
        modelsMonitored: Int,
        topDriftedModel: String?
    ) {
        val intent = createDeepLinkIntent("dashboard", emptyMap())
        val pendingIntent = PendingIntent.getActivity(
            context,
            ID_DIGEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = buildString {
            appendLine("📊 Daily Summary")
            appendLine()
            appendLine("Models Monitored: $modelsMonitored")
            appendLine("Drift Events: $driftEvents")
            appendLine("Patches Synthesized: $patchesSynthesized")
            appendLine("Patches Applied: $patchesApplied")
            if (topDriftedModel != null) {
                appendLine()
                appendLine("⚠️ Most Drifted: $topDriftedModel")
            }
            appendLine()
            appendLine("Tap for detailed analytics")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setColor(0xFF2196F3.toInt())
            .setContentTitle("📊 DriftGuardAI Daily Digest")
            .setContentText("$driftEvents drifts • $patchesApplied patches applied")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(ID_DIGEST, notification)

        Timber.i("📬 Daily digest notification sent")
    }

    /**
     * Show drift summary notification (grouped)
     */
    @SuppressLint("MissingPermission")
    private fun showDriftSummaryNotification() {
        val intent = createDeepLinkIntent("dashboard", emptyMap())
        val pendingIntent = PendingIntent.getActivity(
            context,
            ID_DRIFT_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HIGH)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(0xFFFF9800.toInt())
            .setContentTitle("⚠️ Multiple Drift Alerts")
            .setContentText("$driftAlertCount models detected drift")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$driftAlertCount models have detected drift.\n\nTap to view all drift events and recommended actions."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("DRIFT_ALERTS")
            .setGroupSummary(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(ID_DRIFT_SUMMARY, notification)
    }

    /**
     * Show patch summary notification (grouped)
     */
    @SuppressLint("MissingPermission")
    private fun showPatchSummaryNotification() {
        val intent = createDeepLinkIntent("patches", emptyMap())
        val pendingIntent = PendingIntent.getActivity(
            context,
            ID_PATCH_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PATCHES)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setColor(0xFF4CAF50.toInt())
            .setContentTitle("🔧 Multiple Patches Ready")
            .setContentText("$patchAlertCount patches synthesized")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$patchAlertCount patches have been synthesized and are ready for review.\n\nTap to manage all patches."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("PATCH_ALERTS")
            .setGroupSummary(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(ID_PATCH_SUMMARY, notification)
    }

    /**
     * Rate limiting check
     */
    private fun shouldShowNotification(key: String, priority: AlertPriority): Boolean {
        // Critical alerts always show
        if (priority == AlertPriority.CRITICAL) return true

        val lastTime = lastNotificationTime[key] ?: 0L
        val currentTime = System.currentTimeMillis()

        return if (currentTime - lastTime >= MIN_NOTIFICATION_INTERVAL_MS) {
            lastNotificationTime[key] = currentTime
            true
        } else {
            false
        }
    }

    /**
     * Determine priority based on drift score
     */
    private fun determinePriority(driftScore: Double): AlertPriority {
        return when {
            driftScore >= 0.8 -> AlertPriority.CRITICAL
            driftScore >= 0.5 -> AlertPriority.HIGH
            driftScore >= 0.3 -> AlertPriority.MEDIUM
            else -> AlertPriority.LOW
        }
    }

    /**
     * Get notification channel for priority
     */
    private fun getChannelForPriority(priority: AlertPriority): String {
        return when (priority) {
            AlertPriority.CRITICAL -> CHANNEL_CRITICAL
            AlertPriority.HIGH -> CHANNEL_HIGH
            AlertPriority.MEDIUM -> CHANNEL_MEDIUM
            AlertPriority.LOW -> CHANNEL_LOW
        }
    }

    /**
     * Get notification ID based on priority
     */
    private fun getNotificationId(priority: AlertPriority, hash: Int): Int {
        val base = when (priority) {
            AlertPriority.CRITICAL -> ID_CRITICAL_BASE
            AlertPriority.HIGH -> ID_HIGH_BASE
            AlertPriority.MEDIUM -> ID_MEDIUM_BASE
            AlertPriority.LOW -> ID_LOW_BASE
        }
        return base + (hash % 1000)
    }

    // Helper functions
    private fun getPriorityIcon(priority: AlertPriority) = when (priority) {
        AlertPriority.CRITICAL -> android.R.drawable.stat_notify_error
        else -> android.R.drawable.ic_dialog_alert
    }

    private fun getPriorityColor(priority: AlertPriority) = when (priority) {
        AlertPriority.CRITICAL -> 0xFFF44336.toInt() // Red
        AlertPriority.HIGH -> 0xFFFF9800.toInt() // Orange
        AlertPriority.MEDIUM -> 0xFFFFC107.toInt() // Amber
        AlertPriority.LOW -> 0xFF2196F3.toInt() // Blue
    }

    private fun getNotificationPriority(priority: AlertPriority) = when (priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
        AlertPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
        AlertPriority.LOW -> NotificationCompat.PRIORITY_LOW
    }

    private fun getAlertTitle(priority: AlertPriority, type: String) = when (priority) {
        AlertPriority.CRITICAL -> "🚨 Critical $type Detected"
        AlertPriority.HIGH -> "⚠️ High Priority $type"
        AlertPriority.MEDIUM -> "⚠️ $type Detected"
        AlertPriority.LOW -> "ℹ️ $type Notice"
    }

    private fun buildDriftMessage(
        model: MLModel,
        drift: DriftResult,
        priority: AlertPriority
    ): String {
        return buildString {
            appendLine("Model: ${model.name}")
            appendLine("Drift Score: ${String.format("%.3f", drift.driftScore)}")
            appendLine("Type: ${drift.driftType}")
            appendLine("Priority: ${priority.name}")
            appendLine()
            if (drift.featureDrifts.isNotEmpty()) {
                appendLine("Affected Features:")
                drift.featureDrifts.filter { it.isDrifted }.take(3).forEach { featureDrift ->
                    appendLine("  • ${featureDrift.featureName}")
                }
                val driftedCount = drift.featureDrifts.count { it.isDrifted }
                if (driftedCount > 3) {
                    appendLine("  • ... and ${driftedCount - 3} more")
                }
                appendLine()
            }
            appendLine("👆 Tap to view detailed analysis and recommendations")
        }
    }

    private fun buildPatchMessage(model: MLModel, patch: Patch, autoApplied: Boolean): String {
        return buildString {
            appendLine("Model: ${model.name}")
            appendLine("Patch Type: ${patch.patchType}")
            patch.validationResult?.let { validation ->
                appendLine(
                    "Safety Score: ${
                        String.format(
                            "%.0f%%",
                            validation.metrics.safetyScore
                        )
                    }"
                )
                appendLine(
                    "Drift Reduction: ${
                        String.format(
                            "%.1f%%",
                            validation.metrics.driftReduction * 100
                        )
                    }"
                )
            }
            appendLine()
            if (autoApplied) {
                appendLine("✅ This patch was automatically applied")
                appendLine("Your model now has enhanced drift protection")
            } else {
                appendLine("🔧 Ready to apply")
                appendLine("Tap to review and deploy this patch")
            }
        }
    }

    private fun createDeepLinkIntent(destination: String, extras: Map<String, String>): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", destination)
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }

    private fun createSnoozeAction(notificationId: Int): NotificationCompat.Action {
        // TODO: Implement snooze functionality
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_recent_history,
            "Snooze 1h",
            pendingIntent
        ).build()
    }

    private fun createApplyPatchAction(
        patchId: String,
        notificationId: Int
    ): NotificationCompat.Action {
        // TODO: Implement direct patch application from notification
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "apply_patch")
            putExtra("patch_id", patchId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 20000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_upload,
            "Apply Now",
            pendingIntent
        ).build()
    }

    /**
     * Track notification for analytics
     */
    private fun trackNotification(event: NotificationEvent) {
        notificationHistory.add(event)

        // Update stats
        val stats = _notificationStats.value
        _notificationStats.value = stats.copy(
            totalSent = stats.totalSent + 1,
            byType = stats.byType + (event.type to (stats.byType[event.type] ?: 0) + 1),
            byPriority = stats.byPriority + (event.priority to (stats.byPriority[event.priority]
                ?: 0) + 1)
        )

        // Keep only last 1000 events
        if (notificationHistory.size > 1000) {
            notificationHistory.removeAt(0)
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        notificationManager.cancelAll()
        driftAlertCount = 0
        patchAlertCount = 0
    }

    /**
     * Clear specific notification
     */
    fun clear(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Get notification history
     */
    fun getNotificationHistory(): List<NotificationEvent> {
        return notificationHistory.toList()
    }
}

/**
 * Alert priority levels
 */
enum class AlertPriority {
    CRITICAL,  // Drift score >= 0.8
    HIGH,      // Drift score >= 0.5
    MEDIUM,    // Drift score >= 0.3
    LOW        // Drift score < 0.3
}

/**
 * Notification types
 */
enum class NotificationType {
    DRIFT_ALERT,
    PATCH_NOTIFICATION,
    MONITORING_UPDATE,
    DAILY_DIGEST,
    SYSTEM_MESSAGE
}

/**
 * Notification event for tracking
 */
data class NotificationEvent(
    val type: NotificationType,
    val priority: AlertPriority,
    val modelId: String,
    val timestamp: Long
)

/**
 * Notification statistics
 */
data class NotificationStats(
    val totalSent: Int = 0,
    val byType: Map<NotificationType, Int> = emptyMap(),
    val byPriority: Map<AlertPriority, Int> = emptyMap()
)
