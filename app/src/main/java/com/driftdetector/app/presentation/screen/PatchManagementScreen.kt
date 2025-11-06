package com.driftdetector.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import com.driftdetector.app.presentation.viewmodel.PatchManagementState
import com.driftdetector.app.presentation.viewmodel.PatchManagementViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchManagementScreen(
    viewModel: PatchManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Mock modelId for demonstration - TODO: Get from navigation
    LaunchedEffect(Unit) {
        viewModel.loadPatches("model-1")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Patches Applied",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState is PatchManagementState.Success) {
                            Text(
                                "${(uiState as PatchManagementState.Success).appliedCount} active patches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPatches("model-1") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is PatchManagementState.Loading -> LoadingScreen()
            is PatchManagementState.Success -> {
                if (state.patches.isEmpty()) {
                    EmptyPatchesScreen()
                } else {
                    PatchListContent(
                        state = state,
                        onApplyPatch = {
                            viewModel.applyPatch(it)
                            // Show feedback
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Applying patch...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onRollbackPatch = {
                            viewModel.rollbackPatch(it)
                            // Show feedback
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Rolling back patch...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is PatchManagementState.Error -> ErrorScreen(state.message)
        }
    }
}

@Composable
fun EmptyPatchesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No patches applied yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Patches will appear here once drift is detected and patches are generated or applied.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 How patches work:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. System detects drift in your model\n" +
                                "2. Auto-patch is generated to mitigate drift\n" +
                                "3. Patch is validated for safety\n" +
                                "4. Apply or rollback patches as needed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PatchListContent(
    state: PatchManagementState.Success,
    onApplyPatch: (String) -> Unit,
    onRollbackPatch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PatchSummaryItem(
                    label = "Applied",
                    value = state.patches.count { it.status == PatchStatus.APPLIED }.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
                PatchSummaryItem(
                    label = "Validated",
                    value = state.patches.count { it.status == PatchStatus.VALIDATED }.toString(),
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.secondary
                )
                PatchSummaryItem(
                    label = "Failed",
                    value = state.patches.count { it.status == PatchStatus.FAILED }.toString(),
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "All Patches (${state.patches.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.patches) { patch ->
                EnhancedPatchCard(
                    patch = patch,
                    onApply = { onApplyPatch(patch.id) },
                    onRollback = { onRollbackPatch(patch.id) }
                )
            }
        }
    }
}

@Composable
fun PatchSummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun EnhancedPatchCard(
    patch: Patch,
    onApply: () -> Unit,
    onRollback: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patch.patchType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Created: ${
                            patch.createdAt.atZone(ZoneId.systemDefault()).format(dateFormatter)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show applied/rollback time
                    patch.appliedAt?.let {
                        Text(
                            text = "Applied: ${it.atZone(ZoneId.systemDefault()).format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    patch.rolledBackAt?.let {
                        Text(
                            text = "Rolled back: ${it.atZone(ZoneId.systemDefault()).format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                StatusBadge(patch.status)
            }

            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Validation metrics if available
                patch.validationResult?.let { validation ->
                    Text(
                        text = "📊 Validation Metrics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricChip("Accuracy", String.format("%.2f%%", validation.metrics.accuracy * 100))
                        MetricChip("Safety", String.format("%.2f", validation.metrics.safetyScore))
                        MetricChip("F1 Score", String.format("%.2f", validation.metrics.f1Score))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricChip("Drift Reduction", String.format("%.1f%%", validation.metrics.driftReduction * 100))
                        MetricChip("Perf Delta", String.format("%.3f", validation.metrics.performanceDelta))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status indicator
                    if (validation.isValid) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "✅ Patch validated and safe to apply",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (validation.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "⚠️ Validation Issues:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                validation.errors.forEach { error ->
                                    Text(
                                        "• $error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (patch.status) {
                    PatchStatus.CREATED, PatchStatus.VALIDATED -> {
                        Button(onClick = onApply) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply Patch")
                        }
                    }

                    PatchStatus.APPLIED -> {
                        OutlinedButton(onClick = onRollback) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rollback")
                        }
                    }

                    else -> {
                        Text(
                            text = "No actions available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun StatusBadge(status: PatchStatus) {
    val color = when (status) {
        PatchStatus.APPLIED -> MaterialTheme.colorScheme.primary
        PatchStatus.VALIDATED -> MaterialTheme.colorScheme.secondary
        PatchStatus.FAILED -> MaterialTheme.colorScheme.error
        PatchStatus.ROLLED_BACK -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
