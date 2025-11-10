package com.driftdetector.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.core.monitoring.AccuracySnapshot
import com.driftdetector.app.core.monitoring.AccuracyTrend
import com.driftdetector.app.core.monitoring.ConfusionMatrix
import com.driftdetector.app.core.monitoring.ModelMetrics
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/**
 * Comprehensive Accuracy Chart with temporal analysis
 */
@Composable
fun AccuracyChart(
    history: List<AccuracySnapshot>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No accuracy data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier) {
        // Chart Title
        Text(
            "Model Accuracy Over Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Main Chart
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Multi-line chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 40f

                    // Calculate bounds
                    val accuracyValues = history.map { it.accuracy }
                    val minValue = (accuracyValues.minOrNull() ?: 0.0) - 0.05
                    val maxValue = (accuracyValues.maxOrNull() ?: 1.0) + 0.05

                    val xStep = (width - 2 * padding) / max(1, history.size - 1)
                    val yScale = (height - 2 * padding) / (maxValue - minValue).toFloat()

                    // Draw grid lines
                    for (i in 0..10) {
                        val y = padding + (height - 2 * padding) * i / 10f
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(padding, y),
                            end = Offset(width - padding, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw accuracy line
                    val accuracyPath = Path()
                    history.forEachIndexed { index, snapshot ->
                        val x = padding + index * xStep
                        val y =
                            height - padding - ((snapshot.accuracy - minValue) * yScale).toFloat()

                        if (index == 0) {
                            accuracyPath.moveTo(x, y)
                        } else {
                            accuracyPath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = accuracyPath,
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Draw F1 Score line
                    val f1Path = Path()
                    history.forEachIndexed { index, snapshot ->
                        val x = padding + index * xStep
                        val y =
                            height - padding - ((snapshot.f1Score - minValue) * yScale).toFloat()

                        if (index == 0) {
                            f1Path.moveTo(x, y)
                        } else {
                            f1Path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = f1Path,
                        color = Color(0xFF2196F3),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Draw precision line
                    val precisionPath = Path()
                    history.forEachIndexed { index, snapshot ->
                        val x = padding + index * xStep
                        val y =
                            height - padding - ((snapshot.precision - minValue) * yScale).toFloat()

                        if (index == 0) {
                            precisionPath.moveTo(x, y)
                        } else {
                            precisionPath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = precisionPath,
                        color = Color(0xFFFF9800),
                        style = Stroke(
                            width = 2f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                        )
                    )

                    // Draw data points
                    history.forEachIndexed { index, snapshot ->
                        val x = padding + index * xStep
                        val y =
                            height - padding - ((snapshot.accuracy - minValue) * yScale).toFloat()

                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }
                }

                if (showLegend) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChartLegendItem("Accuracy", Color(0xFF4CAF50))
                        ChartLegendItem("F1 Score", Color(0xFF2196F3))
                        ChartLegendItem("Precision", Color(0xFFFF9800))
                    }
                }
            }
        }

        // Stats Summary
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricStatCard(
                label = "Current",
                value = String.format("%.2f%%", history.last().accuracy * 100),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TrendingUp
            )
            MetricStatCard(
                label = "Average",
                value = String.format("%.2f%%", history.map { it.accuracy }.average() * 100),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShowChart
            )
            MetricStatCard(
                label = "Data Points",
                value = history.size.toString(),
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DataUsage
            )
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Confusion Matrix Heatmap Visualization
 */
@Composable
fun ConfusionMatrixView(
    confusionMatrix: ConfusionMatrix,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Confusion Matrix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val matrix = confusionMatrix.matrix
                val maxValue = matrix.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 1

                // Header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f))
                    Text(
                        "Predicted →",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(matrix.size.toFloat())
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Matrix grid
                matrix.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row label
                        if (rowIndex == matrix.size / 2) {
                            Text(
                                "Actual ↓",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                textAlign = TextAlign.End
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Matrix cells
                        row.forEachIndexed { colIndex, value ->
                            val intensity = value.toFloat() / maxValue
                            val color = if (rowIndex == colIndex) {
                                // Diagonal (correct predictions) - green gradient
                                Color(0xFF4CAF50).copy(alpha = 0.3f + intensity * 0.7f)
                            } else {
                                // Off-diagonal (incorrect) - red gradient
                                Color(0xFFF44336).copy(alpha = 0.2f + intensity * 0.6f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(color, MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (intensity > 0.5f) Color.White else Color.Black
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Summary stats
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ConfusionStatItem("TP", confusionMatrix.truePositives, Color(0xFF4CAF50))
                    ConfusionStatItem("TN", confusionMatrix.trueNegatives, Color(0xFF2196F3))
                    ConfusionStatItem("FP", confusionMatrix.falsePositives, Color(0xFFFF9800))
                    ConfusionStatItem("FN", confusionMatrix.falseNegatives, Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
private fun ConfusionStatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Comprehensive Metrics Dashboard
 */
@Composable
fun ModelMetricsCard(
    metrics: ModelMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Performance Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Timestamp
                Text(
                    metrics.timestamp.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary metrics with animated progress bars
            MetricRow("Accuracy", metrics.accuracy, Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("Precision", metrics.precision, Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("Recall", metrics.recall, Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("F1 Score", metrics.f1Score, Color(0xFF9C27B0))

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Advanced metrics in grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdvancedMetricCard(
                    label = "ROC-AUC",
                    value = metrics.rocAuc,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Analytics
                )
                AdvancedMetricCard(
                    label = "MCC",
                    value = metrics.matthewsCorrelation,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BarChart
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdvancedMetricCard(
                    label = "PR-AUC",
                    value = metrics.precisionRecallAuc,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp
                )
                AdvancedMetricCard(
                    label = "Cal. Error",
                    value = metrics.calibrationError,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    isError = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prediction stats
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Total Predictions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            metrics.totalPredictions.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            "Correct",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            metrics.correctPredictions.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column {
                        Text(
                            "Incorrect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            (metrics.totalPredictions - metrics.correctPredictions).toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }

            // Accuracy drift warning
            if (metrics.accuracyDrift > 0.05) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Accuracy degradation",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Accuracy Degradation Detected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Text(
                                "Model accuracy has decreased by ${
                                    String.format(
                                        "%.2f%%",
                                        metrics.accuracyDrift * 100
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                String.format("%.2f%%", value * 100),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Animated progress bar
        var animatedProgress by remember { mutableStateOf(0f) }
        LaunchedEffect(value) {
            animatedProgress = value.toFloat()
        }

        val progress by animateFloatAsState(
            targetValue = animatedProgress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "metric_progress"
        )

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun AdvancedMetricCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isError) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%.4f", value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Accuracy Trend Indicator
 */
@Composable
fun AccuracyTrendIndicator(
    trend: AccuracyTrend,
    modifier: Modifier = Modifier
) {
    val (icon, color, text) = when (trend) {
        AccuracyTrend.IMPROVING -> Triple(
            Icons.Default.TrendingUp,
            Color(0xFF4CAF50),
            "Improving"
        )

        AccuracyTrend.STABLE -> Triple(
            Icons.Default.TrendingFlat,
            Color(0xFF2196F3),
            "Stable"
        )

        AccuracyTrend.DEGRADING -> Triple(
            Icons.Default.TrendingDown,
            Color(0xFFF44336),
            "Degrading"
        )

        AccuracyTrend.INSUFFICIENT_DATA -> Triple(
            Icons.Default.Info,
            Color(0xFF9E9E9E),
            "Insufficient Data"
        )
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
