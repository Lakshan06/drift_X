package com.driftdetector.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driftdetector.app.presentation.viewmodel.ExportLocation
import com.driftdetector.app.presentation.viewmodel.ModelExportInfo
import com.driftdetector.app.presentation.viewmodel.SettingsViewModel
import com.driftdetector.app.presentation.viewmodel.BackendConnectionStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Show export model selection dialog
    if (uiState.showExportDialog) {
        ModelExportDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    // Show export status dialog
    if (uiState.isExporting) {
        ExportProgressDialog()
    }

    if (uiState.exportSuccess) {
        ExportSuccessDialog(
            exportedFiles = uiState.lastExportedFiles,
            exportPath = uiState.lastExportPath,
            onDismiss = { viewModel.clearExportStatus() },
            onShare = { viewModel.shareLastExport() },
            onOpenFolder = { viewModel.openExportLocation() }
        )
    }

    if (uiState.exportError != null) {
        ExportErrorDialog(
            error = uiState.exportError!!,
            onDismiss = { viewModel.clearExportStatus() }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Backend Connection
        item {
            SettingsSection(title = "Backend Connection") {
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
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Backend connection status",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Backend Connection",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (uiState.backendConnectionStatus) {
                                    BackendConnectionStatus.CONNECTED -> "Connected"
                                    BackendConnectionStatus.READY -> "Ready"
                                    BackendConnectionStatus.DISCONNECTED -> "Disconnected"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (uiState.backendConnectionStatus) {
                                    BackendConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                                    BackendConnectionStatus.READY -> MaterialTheme.colorScheme.secondary
                                    BackendConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.showBackendConnectionInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Backend connection info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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
                SwitchSettingItem(
                    icon = Icons.Default.Visibility,
                    title = "Enable Drift Monitoring",
                    subtitle = "Continuously monitor models for drift",
                    checked = uiState.driftMonitoringEnabled,
                    onCheckedChange = { viewModel.toggleDriftMonitoring(it) }
                )
                
                SliderSettingItem(
                    icon = Icons.Default.Timer,
                    title = "Monitoring Interval",
                    subtitle = "${uiState.monitoringIntervalMinutes} minutes",
                    value = uiState.monitoringIntervalMinutes.toFloat(),
                    valueRange = 5f..120f,
                    onValueChange = { viewModel.updateMonitoringInterval(it.toInt()) },
                    enabled = uiState.driftMonitoringEnabled
                )
                
                SliderSettingItem(
                    icon = Icons.Default.TrendingUp,
                    title = "Drift Threshold",
                    subtitle = String.format("%.2f - Alert when drift exceeds this value", uiState.driftThreshold),
                    value = uiState.driftThreshold,
                    valueRange = 0.1f..0.9f,
                    onValueChange = { viewModel.updateDriftThreshold(it) },
                    enabled = uiState.driftMonitoringEnabled
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Auto-Apply Patches",
                    subtitle = "Automatically apply validated patches",
                    checked = uiState.autoApplyPatches,
                    onCheckedChange = { viewModel.toggleAutoApplyPatches(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Science,
                    title = "Data Scientist Mode",
                    subtitle = "Show advanced metrics and configuration options",
                    checked = uiState.dataScientistMode,
                    onCheckedChange = { viewModel.toggleDataScientistMode(it) }
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
                
                SwitchSettingItem(
                    icon = Icons.Default.Vibration,
                    title = "Vibrate on Alerts",
                    subtitle = "Vibrate device when critical alerts occur",
                    checked = uiState.vibrateOnAlerts,
                    onCheckedChange = { viewModel.toggleVibrateOnAlerts(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Email,
                    title = "Email Notifications",
                    subtitle = "Send drift reports via email (requires setup)",
                    checked = uiState.emailNotificationsEnabled,
                    onCheckedChange = { viewModel.toggleEmailNotifications(it) }
                )
            }
        }

        // AI Settings
        item {
            SettingsSection(title = "PatchBot") {
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
                    onClick = { viewModel.showExportDialog() }
                )
            }
        }

        // Model Deployment Settings
        item {
            SettingsSection(title = "Model Deployment") {
                SwitchSettingItem(
                    icon = Icons.Default.Upload,
                    title = "Auto-Register on Upload",
                    subtitle = "Automatically register models after upload",
                    checked = uiState.autoRegisterModels,
                    onCheckedChange = { viewModel.toggleAutoRegisterModels(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Sync,
                    title = "Sync Baseline on Deploy",
                    subtitle = "Capture baseline statistics when deploying",
                    checked = uiState.syncBaselineOnDeploy,
                    onCheckedChange = { viewModel.toggleSyncBaselineOnDeploy(it) }
                )
                
                SwitchSettingItem(
                    icon = Icons.Default.Backup,
                    title = "Auto-Backup Models",
                    subtitle = "Automatically backup model files on changes",
                    checked = uiState.autoBackupModels,
                    onCheckedChange = { viewModel.toggleAutoBackupModels(it) }
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
                    onClick = { showPrivacyPolicyDialog = true }
                )
            }
        }
    }

    if (viewModel.showBackendConnectionInfoDialog) {
        BackendConnectionInfoDialog(
            connectionStatus = uiState.backendConnectionStatus,
            serverUrl = uiState.backendServerUrl ?: "Not configured",
            onDismiss = { viewModel.hideBackendConnectionInfoDialog() }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
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
                contentDescription = "$title setting",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
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
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
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
                contentDescription = "$title setting",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
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
                .padding(start = 40.dp, top = 8.dp),
            enabled = enabled
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
            contentDescription = "$title setting",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
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
                contentDescription = "Theme setting",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
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
                            contentDescription = "${mode.displayName} theme"
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

@Composable
fun ModelExportDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Export Models",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Select models to export:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Model list
                items(uiState.availableModels.size) { index ->
                    val model = uiState.availableModels[index]
                    ModelExportItem(
                        model = model,
                        isSelected = uiState.selectedModelIds.contains(model.modelId),
                        onToggle = { viewModel.toggleModelSelection(model.modelId) }
                    )
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Download Location:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Location selection
                item {
                    ExportLocation.values().forEach { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDownloadLocation(location) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.downloadLocation == location,
                                onClick = { viewModel.selectDownloadLocation(location) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = location.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (location == ExportLocation.DOWNLOADS) {
                                    Text(
                                        text = "/storage/emulated/0/Download",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.exportSelectedModels() },
                enabled = uiState.selectedModelIds.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Export models",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export (${uiState.selectedModelIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ModelExportItem(
    model: ModelExportInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.modelName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v${model.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (model.patchedCount > 0) {
                        ExportBadge(
                            "✅ ${model.patchedCount} Patched",
                            MaterialTheme.colorScheme.primary
                        )
                    }
                    if (model.ongoingCount > 0) {
                        ExportBadge(
                            "⏳ ${model.ongoingCount} Ongoing",
                            MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (model.driftCount > 0) {
                        ExportBadge(
                            "📊 ${model.driftCount} Drifts",
                            MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun ExportProgressDialog() {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Exporting Data") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    "Please wait while your data is being exported...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = { }
    )
}

@Composable
fun ExportSuccessDialog(
    exportedFiles: List<String>,
    exportPath: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Export successful",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Export Successful!") },
        text = {
            Column {
                Text(
                    "Successfully exported ${exportedFiles.size} file(s):",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                exportedFiles.forEach { fileName ->
                    Text(
                        "• $fileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Files are saved in $exportPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFolder,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Open folder",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Folder", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share exported files",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExportErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Export error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Export Failed") },
        text = {
            Column {
                Text(
                    "An error occurred while exporting your data:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun BackendConnectionInfoDialog(
    connectionStatus: BackendConnectionStatus,
    serverUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (connectionStatus) {
                    BackendConnectionStatus.CONNECTED -> Icons.Default.CloudDone
                    BackendConnectionStatus.READY -> Icons.Default.Cloud
                    BackendConnectionStatus.DISCONNECTED -> Icons.Default.CloudOff
                },
                contentDescription = "Connection status",
                tint = when (connectionStatus) {
                    BackendConnectionStatus.CONNECTED -> androidx.compose.ui.graphics.Color(
                        0xFF4CAF50
                    ) // Green
                    BackendConnectionStatus.READY -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Yellow/Amber
                    BackendConnectionStatus.DISCONNECTED -> androidx.compose.ui.graphics.Color(
                        0xFFF44336
                    ) // Red
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Backend Server Connection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Current Status
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (connectionStatus) {
                                BackendConnectionStatus.CONNECTED -> androidx.compose.ui.graphics.Color(
                                    0xFF4CAF50
                                ).copy(alpha = 0.1f)

                                BackendConnectionStatus.READY -> androidx.compose.ui.graphics.Color(
                                    0xFFFFC107
                                ).copy(alpha = 0.1f)

                                BackendConnectionStatus.DISCONNECTED -> androidx.compose.ui.graphics.Color(
                                    0xFFF44336
                                ).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "📊 Current Status:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(16.dp),
                                    shape = CircleShape,
                                    color = when (connectionStatus) {
                                        BackendConnectionStatus.CONNECTED -> androidx.compose.ui.graphics.Color(
                                            0xFF4CAF50
                                        )

                                        BackendConnectionStatus.READY -> androidx.compose.ui.graphics.Color(
                                            0xFFFFC107
                                        )

                                        BackendConnectionStatus.DISCONNECTED -> androidx.compose.ui.graphics.Color(
                                            0xFFF44336
                                        )
                                    }
                                ) {}
                                Text(
                                    when (connectionStatus) {
                                        BackendConnectionStatus.CONNECTED -> "🟢 Connected - Backend is online"
                                        BackendConnectionStatus.READY -> "🟡 Ready - Backend configured, awaiting connection"
                                        BackendConnectionStatus.DISCONNECTED -> "🔴 Disconnected - No backend connection"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (serverUrl != "Not configured") {
                                Text(
                                    "Server: $serverUrl",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Text(
                        "📖 How to Connect Backend Server:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    InstructionStep(
                        number = "1",
                        title = "Set Up Backend Server",
                        description = "Deploy the DriftGuard backend server using Docker or cloud platform (AWS, Azure, GCP)."
                    )
                }

                item {
                    InstructionStep(
                        number = "2",
                        title = "Get Server URL",
                        description = "Obtain your backend server URL (e.g., https://api.driftguard.example.com or ws://192.168.1.100:8080)."
                    )
                }

                item {
                    InstructionStep(
                        number = "3",
                        title = "Configure in AppModule.kt",
                        description = "Update the server URL in di/AppModule.kt:\n\nval serverUrl = \"YOUR_SERVER_URL\"\n\nRecompile the app to apply changes."
                    )
                }

                item {
                    InstructionStep(
                        number = "4",
                        title = "Verify Connection",
                        description = "Restart the app. The status indicator will turn yellow (Ready) or green (Connected) when the backend is reachable."
                    )
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Text(
                        "🎨 Status Indicators:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    StatusExplanationItem(
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        status = "🟢 Connected (Green)",
                        description = "Backend server is online and actively connected. Real-time monitoring and cloud sync are active."
                    )
                }

                item {
                    StatusExplanationItem(
                        color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                        status = "🟡 Ready (Yellow)",
                        description = "Backend is configured and ready. App is attempting to connect or connection is in progress."
                    )
                }

                item {
                    StatusExplanationItem(
                        color = androidx.compose.ui.graphics.Color(0xFFF44336),
                        status = "🔴 Disconnected (Red)",
                        description = "No backend connection. App works offline with local monitoring only. Cloud features disabled."
                    )
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "💡 Note:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "The app works fully offline. Backend connection is optional and enables cloud sync, collaborative monitoring, and remote model management.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun InstructionStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusExplanationItem(
    color: androidx.compose.ui.graphics.Color,
    status: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
            color = color
        ) {}

        Column(modifier = Modifier.weight(1f)) {
            Text(
                status,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PrivacyTip,
                contentDescription = "Privacy Policy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Your Privacy is Our Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text(
                        "DriftGuardAI is designed with privacy-first principles:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Lock,
                        title = "Local-First Storage",
                        description = "All your ML models, data, and drift analysis results are stored locally on your device. We never upload your models or sensitive data to our servers."
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Security,
                        title = "End-to-End Encryption",
                        description = "Your data is encrypted at rest using Android's secure encryption APIs. Database encryption is enabled by default and cannot be disabled."
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Cloud,
                        title = "Optional Cloud Sync",
                        description = "Cloud sync is optional and disabled by default. If enabled, only metadata (model names, drift scores) is synced. Your actual models and datasets always stay on your device."
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Visibility,
                        title = "No Analytics or Tracking",
                        description = "We don't collect analytics, crash reports, or track your usage. This app works completely offline and doesn't require an internet connection."
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Storage,
                        title = "Data Ownership",
                        description = "You own all your data. Export it anytime, delete it anytime. No vendor lock-in, no data retention policies."
                    )
                }

                item {
                    PrivacyPolicySection(
                        icon = Icons.Default.Info,
                        title = "Open Source",
                        description = "DriftGuardAI is open source. You can review the code, verify our privacy claims, and contribute improvements."
                    )
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Text(
                        "📧 Contact",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "For privacy questions or concerns, please contact us at: privacy@driftguardai.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Text(
                        "Last Updated: November 2025",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun PrivacyPolicySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
