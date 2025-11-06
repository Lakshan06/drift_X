package com.driftdetector.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Data point for charts
 */
data class ChartDataPoint(
    val label: String,
    val value: Float,
    val timestamp: Long? = null
)

/**
 * Line chart for drift metrics over time
 */
@Composable
fun DriftLineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    title: String = "",
    thresholdLine: Float? = null,
    showGrid: Boolean = true
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val padding = 40f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // Find min/max values
            val minValue = data.minOfOrNull { it.value } ?: 0f
            val maxValue = data.maxOfOrNull { it.value } ?: 1f
            val valueRange = maxValue - minValue

            // Draw grid
            if (showGrid) {
                for (i in 0..4) {
                    val y = padding + (chartHeight / 4f) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }

            // Draw threshold line if provided
            thresholdLine?.let { threshold ->
                if (threshold in minValue..maxValue) {
                    val thresholdY = padding + chartHeight * (1 - (threshold - minValue) / valueRange)
                    drawLine(
                        color = errorColor,
                        start = Offset(padding, thresholdY),
                        end = Offset(width - padding, thresholdY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                    )
                }
            }

            // Draw line chart
            val path = Path()
            val progressData = data.take((data.size * animatedProgress.value).toInt().coerceAtLeast(1))
            
            progressData.forEachIndexed { index, point ->
                val x = padding + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
                val normalizedValue = (point.value - minValue) / valueRange
                val y = padding + chartHeight * (1 - normalizedValue)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw points
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Draw the line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Fill area under line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(padding + chartWidth, padding + chartHeight)
                lineTo(padding, padding + chartHeight)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.0f)
                    )
                )
            )

            // Draw axes
            drawLine(
                color = onSurfaceColor,
                start = Offset(padding, padding),
                end = Offset(padding, height - padding),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = onSurfaceColor,
                start = Offset(padding, height - padding),
                end = Offset(width - padding, height - padding),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

/**
 * Bar chart for feature attribution
 */
@Composable
fun FeatureAttributionBarChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    title: String = "Feature Attribution"
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (data.isEmpty()) {
            Text(
                text = "No feature attribution data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        val maxValue = data.maxOfOrNull { abs(it.value) } ?: 1f

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { point ->
                FeatureAttributionBar(
                    featureName = point.label,
                    value = point.value,
                    maxValue = maxValue,
                    progress = animatedProgress.value
                )
            }
        }
    }
}

@Composable
private fun FeatureAttributionBar(
    featureName: String,
    value: Float,
    maxValue: Float,
    progress: Float
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val normalizedValue = (abs(value) / maxValue).coerceIn(0f, 1f)
    val barColor = if (value >= 0) primaryColor else errorColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Feature name
        Text(
            text = featureName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp),
            maxLines = 1
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedValue * progress)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Value
        Text(
            text = String.format("%.3f", value),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp),
            color = barColor
        )
    }
}

/**
 * Heatmap for feature drift visualization
 */
@Composable
fun DriftHeatmap(
    featureNames: List<String>,
    driftScores: List<Float>,
    modifier: Modifier = Modifier,
    title: String = "Drift Heatmap"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (featureNames.isEmpty()) {
            Text(
                text = "No drift data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        // Grid layout for heatmap
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            featureNames.forEachIndexed { index, featureName ->
                val score = driftScores.getOrNull(index) ?: 0f
                
                // Color gradient from green (low drift) to red (high drift)
                val cellColor = when {
                    score < 0.2f -> Color(0xFF4CAF50) // Green
                    score < 0.4f -> Color(0xFFFFC107) // Yellow
                    score < 0.6f -> Color(0xFFFF9800) // Orange
                    else -> Color(0xFFF44336) // Red
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            color = cellColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (featureName.length > 20)
                            featureName.take(18) + ".."
                        else featureName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Text(
                        text = String.format("%.3f", score),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Circular gauge for drift severity
 */
@Composable
fun DriftGauge(
    value: Float,
    maxValue: Float = 1f,
    modifier: Modifier = Modifier,
    label: String = "Drift Score"
) {
    val animatedValue = remember { Animatable(0f) }
    
    LaunchedEffect(value) {
        animatedValue.animateTo(
            targetValue = value,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val normalizedValue = (animatedValue.value / maxValue).coerceIn(0f, 1f)
    
    // Color based on severity
    val gaugeColor = when {
        normalizedValue < 0.2f -> Color(0xFF4CAF50) // Green - Safe
        normalizedValue < 0.5f -> Color(0xFFFFC107) // Yellow - Warning
        normalizedValue < 0.7f -> Color(0xFFFF9800) // Orange - Alert
        else -> Color(0xFFF44336) // Red - Critical
    }

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 2 - 20f
                val strokeWidth = 20f

                // Background arc
                drawArc(
                    color = backgroundColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Value arc
                drawArc(
                    color = gaugeColor,
                    startAngle = 135f,
                    sweepAngle = 270f * normalizedValue,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Value text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.3f", animatedValue.value),
                    style = MaterialTheme.typography.headlineMedium,
                    color = gaugeColor
                )
                Text(
                    text = when {
                        normalizedValue < 0.2f -> "Safe"
                        normalizedValue < 0.5f -> "Warning"
                        normalizedValue < 0.7f -> "Alert"
                        else -> "Critical"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = textColor
        )
    }
}

/**
 * Mini sparkline chart for compact views
 */
@Composable
fun Sparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.height(40.dp)) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val minValue = data.minOrNull() ?: 0f
        val maxValue = data.maxOrNull() ?: 1f
        val valueRange = (maxValue - minValue).coerceAtLeast(0.01f)

        val path = Path()
        
        data.forEachIndexed { index, value ->
            val x = (width / (data.size - 1).coerceAtLeast(1)) * index
            val normalizedValue = (value - minValue) / valueRange
            val y = height * (1 - normalizedValue)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
