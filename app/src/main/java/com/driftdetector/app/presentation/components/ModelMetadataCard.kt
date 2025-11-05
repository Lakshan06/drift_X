package com.driftdetector.app.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.driftdetector.app.core.ml.ModelMetadata
import com.driftdetector.app.core.ml.TensorInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelMetadataCard(
    metadata: ModelMetadata,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getModelIcon(metadata),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Model Detected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = metadata.getModelType(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more"
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Basic Info
            when (metadata) {
                is ModelMetadata.TensorFlowLite -> TensorFlowLiteInfo(metadata, expanded)
                is ModelMetadata.Onnx -> OnnxInfo(metadata, expanded)
                is ModelMetadata.TensorFlow -> TensorFlowInfo(metadata, expanded)
                is ModelMetadata.Unknown -> UnknownInfo(metadata)
                is ModelMetadata.Error -> ErrorInfo(metadata)
            }

            // Action buttons
            if (metadata !is ModelMetadata.Error) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss")
                    }

                    Button(
                        onClick = { /* Navigate to model details */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Details")
                    }
                }
            }
        }
    }
}

@Composable
private fun TensorFlowLiteInfo(metadata: ModelMetadata.TensorFlowLite, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(
                icon = Icons.Default.Input,
                label = "Inputs",
                value = metadata.inputTensors.size.toString()
            )
            StatChip(
                icon = Icons.Default.Output,
                label = "Outputs",
                value = metadata.outputTensors.size.toString()
            )
            StatChip(
                icon = Icons.Default.Storage,
                label = "Size",
                value = ModelMetadata.formatFileSize(metadata.modelSizeBytes)
            )
        }

        // Properties
        PropertiesSection(
            listOf(
                "Version" to metadata.version,
                "Quantized" to if (metadata.quantized) "Yes" else "No",
                "Metadata" to if (metadata.hasMetadata) "Yes" else "No"
            )
        )

        // Detailed view
        if (expanded) {
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Input tensors
            TensorSection(
                title = "Input Tensors",
                tensors = metadata.inputTensors,
                icon = Icons.Default.Input
            )

            // Output tensors
            TensorSection(
                title = "Output Tensors",
                tensors = metadata.outputTensors,
                icon = Icons.Default.Output
            )
        }
    }
}

@Composable
private fun OnnxInfo(metadata: ModelMetadata.Onnx, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(
                icon = Icons.Default.Input,
                label = "Inputs",
                value = metadata.inputNodes.size.toString()
            )
            StatChip(
                icon = Icons.Default.Output,
                label = "Outputs",
                value = metadata.outputNodes.size.toString()
            )
            StatChip(
                icon = Icons.Default.Storage,
                label = "Size",
                value = ModelMetadata.formatFileSize(metadata.modelSizeBytes)
            )
        }

        // Properties
        PropertiesSection(
            listOf(
                "Opset Version" to metadata.opsetVersion.toString()
            )
        )

        // Detailed view
        if (expanded) {
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Input nodes
            TensorSection(
                title = "Input Nodes",
                tensors = metadata.inputNodes,
                icon = Icons.Default.Input
            )

            // Output nodes
            TensorSection(
                title = "Output Nodes",
                tensors = metadata.outputNodes,
                icon = Icons.Default.Output
            )
        }
    }
}

@Composable
private fun TensorFlowInfo(metadata: ModelMetadata.TensorFlow, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(
                icon = Icons.Default.Input,
                label = "Inputs",
                value = metadata.inputSignature.size.toString()
            )
            StatChip(
                icon = Icons.Default.Output,
                label = "Outputs",
                value = metadata.outputSignature.size.toString()
            )
            StatChip(
                icon = Icons.Default.Storage,
                label = "Size",
                value = ModelMetadata.formatFileSize(metadata.modelSizeBytes)
            )
        }

        // Properties
        PropertiesSection(
            listOf(
                "Format" to if (metadata.savedModelFormat) "SavedModel" else "H5"
            )
        )

        if (expanded) {
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Signatures
            InfoRow("Input Signature", metadata.inputSignature.joinToString(", "))
            InfoRow("Output Signature", metadata.outputSignature.joinToString(", "))
        }
    }
}

@Composable
private fun UnknownInfo(metadata: ModelMetadata.Unknown) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
        )
        Text(
            text = "Unknown model format",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "File extension: .${metadata.fileExtension}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorInfo(metadata: ModelMetadata.Error) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Failed to extract metadata",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = metadata.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TensorSection(
    title: String,
    tensors: List<TensorInfo>,
    icon: ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        tensors.forEach { tensor ->
            TensorInfoCard(tensor)
        }
    }
}

@Composable
private fun TensorInfoCard(tensor: TensorInfo) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tensor.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Shape: ${tensor.getShapeString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Type: ${tensor.dataType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (tensor.isDynamic()) {
                Text(
                    text = "⚡ Dynamic shape",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PropertiesSection(properties: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        properties.forEach { (key, value) ->
            InfoRow(key, value)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getModelIcon(metadata: ModelMetadata): ImageVector {
    return when (metadata) {
        is ModelMetadata.TensorFlowLite -> Icons.Default.Memory
        is ModelMetadata.Onnx -> Icons.Default.Extension
        is ModelMetadata.TensorFlow -> Icons.Default.Psychology
        is ModelMetadata.Unknown -> Icons.Default.HelpOutline
        is ModelMetadata.Error -> Icons.Default.Error
    }
}
