package com.driftdetector.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driftdetector.app.core.offline.NetworkMonitor
import com.driftdetector.app.core.offline.NetworkStatus
import com.driftdetector.app.core.offline.OfflineManager
import com.driftdetector.app.core.offline.SyncState
import org.koin.compose.koinInject

/**
 * Shows offline indicator banner at the top of the screen
 */
@Composable
fun OfflineIndicator(
    networkMonitor: NetworkMonitor = koinInject(),
    offlineManager: OfflineManager = koinInject()
) {
    val networkStatus by networkMonitor.networkStatus.collectAsState()
    val isOnline by networkMonitor.isOnline.collectAsState()
    val pendingCount by offlineManager.pendingCount.collectAsState()
    val syncState by offlineManager.syncState.collectAsState()

    Column {
        // Offline banner
        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Working offline - Changes will sync when online",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (pendingCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge {
                            Text("$pendingCount")
                        }
                    }
                }
            }
        }

        // Sync progress banner
        AnimatedVisibility(
            visible = syncState is SyncState.Syncing,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (val state = syncState) {
                                    is SyncState.Syncing -> "Syncing ${state.current}/${state.total}..."
                                    else -> "Syncing..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (syncState is SyncState.Syncing) {
                        val progress = (syncState as SyncState.Syncing).progress
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Sync success banner
        AnimatedVisibility(
            visible = syncState is SyncState.Success,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (val state = syncState) {
                            is SyncState.Success -> "Synced ${state.result.completedOperations} items"
                            else -> "Synced"
                        },
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Network type indicator (subtle)
        if (isOnline) {
            NetworkTypeIndicator(networkStatus)
        }
    }
}

@Composable
private fun NetworkTypeIndicator(networkStatus: NetworkStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (networkStatus) {
                is NetworkStatus.WiFi -> Icons.Default.Wifi
                is NetworkStatus.Cellular -> Icons.Default.SignalCellularAlt
                is NetworkStatus.Ethernet -> Icons.Default.Cable
                else -> Icons.Default.Cloud
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            networkStatus.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact sync status chip for showing in UI
 */
@Composable
fun SyncStatusChip(
    offlineManager: OfflineManager = koinInject()
) {
    val pendingCount by offlineManager.pendingCount.collectAsState()
    val syncState by offlineManager.syncState.collectAsState()

    if (pendingCount > 0 || syncState !is SyncState.Idle) {
        AssistChip(
            onClick = { /* Open sync details */ },
            label = {
                Text(
                    when (syncState) {
                        is SyncState.Syncing -> "Syncing..."
                        is SyncState.Success -> "✓ Synced"
                        is SyncState.Error -> "⚠ Sync failed"
                        else -> "$pendingCount pending"
                    }
                )
            },
            leadingIcon = {
                when (syncState) {
                    is SyncState.Syncing -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )

                    is SyncState.Success -> Icon(Icons.Default.CheckCircle, null)
                    is SyncState.Error -> Icon(Icons.Default.Error, null)
                    else -> Icon(Icons.Default.CloudQueue, null)
                }
            }
        )
    }
}
