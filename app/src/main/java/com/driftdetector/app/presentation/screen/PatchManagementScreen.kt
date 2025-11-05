package com.driftdetector.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.PatchStatus
import com.driftdetector.app.presentation.viewmodel.PatchManagementState
import com.driftdetector.app.presentation.viewmodel.PatchManagementViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PatchManagementScreen(
    viewModel: PatchManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Mock modelId for demonstration
    LaunchedEffect(Unit) {
        viewModel.loadPatches("model-1")
    }

    when (val state = uiState) {
        is PatchManagementState.Loading -> LoadingScreen()
        is PatchManagementState.Success -> PatchListContent(
            state = state,
            onApplyPatch = { viewModel.applyPatch(it) },
            onRollbackPatch = { viewModel.rollbackPatch(it) }
        )

        is PatchManagementState.Error -> ErrorScreen(state.message)
    }
}

@Composable
fun PatchListContent(
    state: PatchManagementState.Success,
    onApplyPatch: (String) -> Unit,
    onRollbackPatch: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Patches (${state.appliedCount} applied)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.patches) { patch ->
                PatchCard(
                    patch = patch,
                    onApply = { onApplyPatch(patch.id) },
                    onRollback = { onRollbackPatch(patch.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchCard(
    patch: Patch,
    onApply: () -> Unit,
    onRollback: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth()
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Created: ${
                            patch.createdAt.atZone(ZoneId.systemDefault()).format(dateFormatter)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(patch.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Validation metrics if available
            patch.validationResult?.let { validation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricChip("Accuracy", String.format("%.2f", validation.metrics.accuracy))
                    MetricChip("Safety", String.format("%.2f", validation.metrics.safetyScore))
                    MetricChip("F1", String.format("%.2f", validation.metrics.f1Score))
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
                            Text("Apply")
                        }
                    }

                    PatchStatus.APPLIED -> {
                        OutlinedButton(onClick = onRollback) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rollback")
                        }
                    }

                    else -> {}
                }
            }
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
