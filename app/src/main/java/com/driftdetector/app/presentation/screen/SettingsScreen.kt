package com.driftdetector.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme Settings
        item {
            SettingsSection(title = "Appearance") {
                ThemeSettingItem(
                    selectedTheme = uiState.themeMode,
                    onThemeChange = { viewModel.updateTheme(it) }
                )
            }
        }

        // Privacy & Security
        item {
            SettingsSection(title = "Privacy & Security") {
                SwitchSettingItem(
                    icon = Icons.Default.Lock,
                    title = "Database Encryption",
                    subtitle = "Encrypt all stored data (enabled by default)",
                    checked = uiState.encryptionEnabled,
                    onCheckedChange = { viewModel.toggleEncryption(it) },
                    enabled = false // Always enabled
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Security,
                    title = "Differential Privacy",
                    subtitle = "Add noise to prevent data leakage",
                    checked = uiState.differentialPrivacyEnabled,
                    onCheckedChange = { viewModel.toggleDifferentialPrivacy(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Cloud,
                    title = "Cloud Sync",
                    subtitle = "Sync metadata to cloud (data stays on device)",
                    checked = uiState.cloudSyncEnabled,
                    onCheckedChange = { viewModel.toggleCloudSync(it) }
                )
            }
        }

        // Monitoring Settings
        item {
            SettingsSection(title = "Model Monitoring") {
                SliderSettingItem(
                    icon = Icons.Default.Timer,
                    title = "Monitoring Interval",
                    subtitle = "${uiState.monitoringIntervalMinutes} minutes",
                    value = uiState.monitoringIntervalMinutes.toFloat(),
                    valueRange = 5f..120f,
                    onValueChange = { viewModel.updateMonitoringInterval(it.toInt()) }
                )
                
                SliderSettingItem(
                    icon = Icons.Default.TrendingUp,
                    title = "Drift Threshold",
                    subtitle = String.format("%.2f - Alert when drift exceeds this value", uiState.driftThreshold),
                    value = uiState.driftThreshold,
                    valueRange = 0.1f..0.9f,
                    onValueChange = { viewModel.updateDriftThreshold(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Auto-Apply Patches",
                    subtitle = "Automatically apply validated patches",
                    checked = uiState.autoApplyPatches,
                    onCheckedChange = { viewModel.toggleAutoApplyPatches(it) }
                )
            }
        }

        // Notifications
        item {
            SettingsSection(title = "Notifications") {
                SwitchSettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Drift Alerts",
                    subtitle = "Notify when drift is detected",
                    checked = uiState.driftAlertsEnabled,
                    onCheckedChange = { viewModel.toggleDriftAlerts(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.BuildCircle,
                    title = "Patch Notifications",
                    subtitle = "Notify when patches are generated",
                    checked = uiState.patchNotificationsEnabled,
                    onCheckedChange = { viewModel.togglePatchNotifications(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Warning,
                    title = "Critical Alerts Only",
                    subtitle = "Only notify for high-severity drift",
                    checked = uiState.criticalAlertsOnly,
                    onCheckedChange = { viewModel.toggleCriticalAlertsOnly(it) }
                )
            }
        }

        // AI Settings
        item {
            SettingsSection(title = "AI Assistant") {
                SwitchSettingItem(
                    icon = Icons.Default.SmartToy,
                    title = "AI Explanations",
                    subtitle = "Use AI to explain drift and patches",
                    checked = uiState.aiExplanationsEnabled,
                    onCheckedChange = { viewModel.toggleAIExplanations(it) }
                )
                
                ClickableSettingItem(
                    icon = Icons.Default.Download,
                    title = "AI Model",
                    subtitle = uiState.aiModelStatus,
                    onClick = { viewModel.openAIModelDownload() }
                )
            }
        }

        // Data Management
        item {
            SettingsSection(title = "Data Management") {
                SliderSettingItem(
                    icon = Icons.Default.DataUsage,
                    title = "Data Retention",
                    subtitle = "${uiState.dataRetentionDays} days",
                    value = uiState.dataRetentionDays.toFloat(),
                    valueRange = 7f..90f,
                    onValueChange = { viewModel.updateDataRetention(it.toInt()) }
                )
                
                ClickableSettingItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear Old Data",
                    subtitle = "Remove data older than retention period",
                    onClick = { viewModel.clearOldData() }
                )
                
                ClickableSettingItem(
                    icon = Icons.Default.FileDownload,
                    title = "Export Data",
                    subtitle = "Export drift reports and patch history",
                    onClick = { viewModel.exportData() }
                )
            }
        }

        // About
        item {
            SettingsSection(title = "About") {
                ClickableSettingItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = uiState.appVersion,
                    onClick = { }
                )
                
                ClickableSettingItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Used",
                    subtitle = uiState.storageUsed,
                    onClick = { }
                )
                
                ClickableSettingItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "View privacy policy",
                    onClick = { viewModel.openPrivacyPolicy() }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun ColumnScope.SwitchSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun ColumnScope.SliderSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 8.dp)
        )
    }
}

@Composable
fun ColumnScope.ClickableSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ColumnScope.ThemeSettingItem(
    selectedTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedTheme == mode,
                    onClick = { onThemeChange(mode) },
                    label = { Text(mode.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.AUTO -> Icons.Default.Brightness4
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    AUTO("Auto")
}
