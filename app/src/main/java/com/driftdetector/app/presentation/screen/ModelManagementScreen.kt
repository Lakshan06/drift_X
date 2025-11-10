package com.driftdetector.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.presentation.viewmodel.ModelManagementState
import com.driftdetector.app.presentation.viewmodel.ModelManagementViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ModelManagementScreen(
    viewModel: ModelManagementViewModel = koinViewModel(),
    onNavigateToUpload: () -> Unit = {},
    onNavigateToInstantDriftFix: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is ModelManagementState.Loading -> LoadingScreen()
        is ModelManagementState.Empty -> EmptyModelsScreen(
            onAddClick = { showAddDialog = true },
            onUploadClick = onNavigateToUpload,
            onInstantDriftFixClick = onNavigateToInstantDriftFix
        )
        is ModelManagementState.Success -> ModelListContent(
            models = state.models,
            onDeactivate = { viewModel.deactivateModel(it) },
            onAddClick = { showAddDialog = true },
            onUploadClick = onNavigateToUpload,
            onInstantDriftFixClick = onNavigateToInstantDriftFix
        )
        is ModelManagementState.Error -> ErrorScreen(state.message)
    }

    // Model Registration Dialog
    if (showAddDialog) {
        ModelRegistrationDialog(
            onDismiss = { showAddDialog = false },
            onRegister = { name, version, modelPath, features, labels ->
                viewModel.registerModel(name, version, modelPath, features, labels)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ModelListContent(
    models: List<MLModel>,
    onDeactivate: (String) -> Unit,
    onAddClick: () -> Unit,
    onUploadClick: () -> Unit,
    onInstantDriftFixClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Registered Models (${models.size})",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upload button
                FloatingActionButton(
                    onClick = onUploadClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload Model")
                }

                // Instant Drift Fix button
                FloatingActionButton(
                    onClick = onInstantDriftFixClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Instant Drift Fix")
                }

                // Register button
                FloatingActionButton(
                    onClick = onAddClick
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Register Model")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(models) { model ->
                ModelCard(
                    model = model,
                    onDeactivate = { onDeactivate(model.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelRegistrationDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, List<String>, List<String>) -> Unit
) {
    var modelName by remember { mutableStateOf("") }
    var modelVersion by remember { mutableStateOf("1.0.0") }
    var modelPath by remember { mutableStateOf("") }
    var features by remember { mutableStateOf("") }
    var labels by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Model") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { 
                            modelName = it
                            showError = false
                        },
                        label = { Text("Model Name") },
                        placeholder = { Text("e.g., FraudDetectionModel") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && modelName.isBlank(),
                        supportingText = if (showError && modelName.isBlank()) {
                            { Text("Model name is required") }
                        } else null
                    )
                }

                item {
                    OutlinedTextField(
                        value = modelVersion,
                        onValueChange = { modelVersion = it },
                        label = { Text("Version") },
                        placeholder = { Text("e.g., 1.0.0") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = modelPath,
                        onValueChange = { 
                            modelPath = it
                            showError = false
                        },
                        label = { Text("Model Path") },
                        placeholder = { Text("e.g., fraud_model.tflite") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && modelPath.isBlank(),
                        supportingText = if (showError && modelPath.isBlank()) {
                            { Text("Model path is required") }
                        } else {
                            { Text("Path to .tflite file in assets") }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = features,
                        onValueChange = { 
                            features = it
                            showError = false
                        },
                        label = { Text("Input Features") },
                        placeholder = { Text("amount,merchant,time,category") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && features.isBlank(),
                        supportingText = if (showError && features.isBlank()) {
                            { Text("Features are required") }
                        } else {
                            { Text("Comma-separated feature names") }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = labels,
                        onValueChange = { 
                            labels = it
                            showError = false
                        },
                        label = { Text("Output Labels") },
                        placeholder = { Text("legitimate,fraud") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && labels.isBlank(),
                        supportingText = if (showError && labels.isBlank()) {
                            { Text("Labels are required") }
                        } else {
                            { Text("Comma-separated class labels") }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelName.isBlank() || modelPath.isBlank() || 
                        features.isBlank() || labels.isBlank()) {
                        showError = true
                    } else {
                        val featureList = features.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val labelList = labels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onRegister(modelName, modelVersion, modelPath, featureList, labelList)
                    }
                }
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCard(
    model: MLModel,
    onDeactivate: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

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
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Version: ${model.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (model.isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Model is active",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow("Features", "${model.inputFeatures.size} inputs")
                InfoRow("Outputs", "${model.outputLabels.size} classes")
                InfoRow(
                    "Created",
                    model.createdAt.atZone(ZoneId.systemDefault()).format(dateFormatter)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button
            if (model.isActive) {
                OutlinedButton(
                    onClick = onDeactivate,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Deactivate")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EmptyModelsScreen(
    onAddClick: () -> Unit,
    onUploadClick: () -> Unit,
    onInstantDriftFixClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "No models registered",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No models registered",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Upload a model file or register an existing one to start monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Upload button (primary action)
            Button(
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Upload Model")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Model & Data")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instant Drift Fix button
            Button(
                onClick = onInstantDriftFixClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Instant Drift Fix")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Instant Drift Fix")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Register button (secondary action)
            OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register Model")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Existing Model")
            }
        }
    }
}

