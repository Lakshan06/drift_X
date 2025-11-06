package com.driftdetector.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.driftdetector.app.domain.model.DriftResult
import com.driftdetector.app.domain.model.DriftType
import com.driftdetector.app.domain.model.FeatureDrift
import com.driftdetector.app.presentation.components.*
import com.driftdetector.app.presentation.viewmodel.DriftDashboardState
import com.driftdetector.app.presentation.viewmodel.DriftDashboardViewModel
import com.driftdetector.app.presentation.viewmodel.PatchGenerationState
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriftDashboardScreen(
    viewModel: DriftDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val patchGenerationState by viewModel.patchGenerationState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAIExplanation by remember { mutableStateOf(false) }
    var selectedDriftResult by remember { mutableStateOf<DriftResult?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle patch generation feedback
    LaunchedEffect(patchGenerationState) {
        when (patchGenerationState) {
            is PatchGenerationState.Loading -> {
                snackbarHostState.showSnackbar(
                    message = "🔧 Generating intelligent patches...",
                    duration = SnackbarDuration.Short
                )
            }
            is PatchGenerationState.Success -> {
                val state = patchGenerationState as PatchGenerationState.Success
                val message = buildString {
                    append("✅ Generated ${state.totalGenerated} patch")
                    if (state.totalGenerated != 1) append("es")
                    if (state.autoApplied > 0) {
                        append(" • ${state.autoApplied} auto-applied")
                    }
                    if (state.failed > 0) {
                        append(" • ${state.failed} failed")
                    }
                }
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
            is PatchGenerationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "❌ Failed to generate patch: ${(patchGenerationState as PatchGenerationState.Error).message}",
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drift Monitor Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is DriftDashboardState.Loading -> LoadingScreen()
            is DriftDashboardState.Empty -> EmptyDashboardScreen()
            is DriftDashboardState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Tab navigation
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Overview") },
                            icon = { Icon(Icons.Default.Dashboard, null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Analytics") },
                            icon = { Icon(Icons.Default.Analytics, null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Alerts") },
                            icon = { Icon(Icons.Default.Notifications, null) }
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> OverviewTab(
                            state = state,
                            onDriftClick = {
                                selectedDriftResult = it
                                showAIExplanation = true
                            }
                        )

                        1 -> AnalyticsTab(state = state)
                        2 -> AlertsTab(state = state)
                    }
                }
            }

            is DriftDashboardState.Error -> ErrorScreen(state.message)
        }
    }

    // AI Explanation Bottom Sheet
    if (showAIExplanation && selectedDriftResult != null) {
        AIExplanationSheet(
            driftResult = selectedDriftResult!!,
            onDismiss = {
                showAIExplanation = false
                selectedDriftResult = null
            }
        )
    }
}

@Composable
fun OverviewTab(
    state: DriftDashboardState.Success,
    onDriftClick: (DriftResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Metrics Summary
        item {
            MetricsSummaryCard(state)
        }

        // Drift Gauge - Current Status
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null) {
                DriftStatusGaugeCard(latestDrift)
            }
        }

        // Drift Timeline Chart
        item {
            DriftTimelineCard(state.driftResults)
        }

        // Feature Drift Heatmap
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.featureDrifts.isNotEmpty()) {
                DriftHeatmapCard(latestDrift)
            }
        }

        // Recent Drift Events
        item {
            Text(
                "Recent Drift Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(state.driftResults.take(10)) { drift ->
            InteractiveDriftCard(
                driftResult = drift,
                onClick = { onDriftClick(drift) }
            )
        }
    }
}

@Composable
fun AnalyticsTab(state: DriftDashboardState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Attribution Analysis
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.featureDrifts.isNotEmpty()) {
                FeatureAttributionCard(latestDrift)
            }
        }

        // Drift Distribution by Type
        item {
            DriftTypeDistributionCard(state.driftResults)
        }

        // Statistical Tests Results
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.statisticalTests.isNotEmpty()) {
                StatisticalTestsCard(latestDrift)
            }
        }

        // Drift Trends
        item {
            DriftTrendsCard(state.driftResults)
        }
    }
}

@Composable
fun AlertsTab(state: DriftDashboardState.Success) {
    val criticalDrifts = state.driftResults.filter { it.isDriftDetected && it.driftScore > 0.5 }
    val warningDrifts =
        state.driftResults.filter { it.isDriftDetected && it.driftScore in 0.2..0.5 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Alert Summary
        item {
            AlertSummaryCard(
                criticalCount = criticalDrifts.size,
                warningCount = warningDrifts.size,
                totalCount = state.driftResults.count { it.isDriftDetected }
            )
        }

        // Critical Alerts
        if (criticalDrifts.isNotEmpty()) {
            item {
                Text(
                    "🚨 Critical Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            items(criticalDrifts) { drift ->
                AlertCard(drift, AlertLevel.CRITICAL)
            }
        }

        // Warning Alerts
        if (warningDrifts.isNotEmpty()) {
            item {
                Text(
                    "⚠️ Warning Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(warningDrifts) { drift ->
                AlertCard(drift, AlertLevel.WARNING)
            }
        }
    }
}

// ========== Card Components ==========

@Composable
fun MetricsSummaryCard(state: DriftDashboardState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.model.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Model Performance Overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "v${state.model.version}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricBox(
                    label = "Total Drifts",
                    value = state.totalDrifts.toString(),
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.primary,
                    tooltip = "Total number of drift events detected across all monitoring sessions"
                )

                val criticalCount = state.driftResults.count { it.driftScore > 0.5 }
                MetricBox(
                    label = "Critical",
                    value = criticalCount.toString(),
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.error,
                    tooltip = "Number of high-severity drift events requiring immediate attention"
                )

                val avgDrift = state.driftResults.takeIf { it.isNotEmpty() }
                    ?.map { it.driftScore }
                    ?.average()
                    ?: 0.0
                MetricBox(
                    label = "Avg Score",
                    value = String.format("%.3f", avgDrift),
                    icon = Icons.Default.ShowChart,
                    color = MaterialTheme.colorScheme.tertiary,
                    tooltip = "Average drift score across all monitoring periods"
                )
            }
        }
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    tooltip: String? = null
) {
    var showTooltip by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable { if (tooltip != null) showTooltip = !showTooltip }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            if (tooltip != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }

        // Tooltip overlay
        if (showTooltip && tooltip != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = MaterialTheme.shapes.small,
                tonalElevation = 8.dp
            ) {
                Text(
                    text = tooltip,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}

@Composable
fun DriftStatusGaugeCard(driftResult: DriftResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Drift Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Drift Level Indicator with clear labels
            val driftLevel = when {
                driftResult.driftScore > 0.7 -> "HIGH"
                driftResult.driftScore > 0.4 -> "MODERATE"
                driftResult.driftScore > 0.15 -> "LOW"
                else -> "MINIMAL"
            }

            val driftColor = when (driftLevel) {
                "HIGH" -> Color(0xFFF44336)
                "MODERATE" -> Color(0xFFFF9800)
                "LOW" -> Color(0xFFFFC107)
                else -> Color(0xFF4CAF50)
            }

            // Visual Drift Level Badge
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = driftColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Drift Level: $driftLevel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = driftColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Score: ${String.format("%.3f", driftResult.driftScore)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = driftColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DriftGauge(
                value = driftResult.driftScore.toFloat(),
                maxValue = 1f,
                label = "${driftResult.driftType.name.replace("_", " ")}"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Business Impact Explanation
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "What does this mean?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getDriftExplanation(driftLevel, driftResult.driftType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action recommendation
            Text(
                text = getActionRecommendation(driftLevel),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

fun getDriftExplanation(level: String, type: DriftType): String {
    val baseExplanation = when (type) {
        DriftType.CONCEPT_DRIFT -> "The relationship between inputs and outputs has changed. Your model's predictions may become less accurate."
        DriftType.COVARIATE_DRIFT -> "The distribution of input features has shifted. New data looks different from training data."
        DriftType.PRIOR_DRIFT -> "The distribution of target outcomes has changed. The real-world problem may be evolving."
        DriftType.NO_DRIFT -> "No significant drift detected. The model is performing as expected."
    }

    val impactExplanation = when (level) {
        "HIGH" -> "\n\n📉 Business Impact: Model accuracy may be significantly degraded. Immediate attention required."
        "MODERATE" -> "\n\n⚠️ Business Impact: Model performance is declining. Action recommended soon."
        "LOW" -> "\n\nℹ️ Business Impact: Minor drift detected. Monitor closely but no immediate action needed."
        else -> "\n\n✅ Business Impact: Minimal to no impact on model performance."
    }

    return baseExplanation + impactExplanation
}

fun getActionRecommendation(level: String): String {
    return when (level) {
        "HIGH" -> "⚡ Recommended Action: Apply available patches or retrain the model immediately"
        "MODERATE" -> "💡 Recommended Action: Review and apply patches, or schedule model retraining"
        "LOW" -> "👀 Recommended Action: Continue monitoring. Consider patches if available"
        else -> "✅ Recommended Action: No action needed. Continue regular monitoring"
    }
}

@Composable
fun DriftTimelineCard(driftResults: List<DriftResult>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val chartData = driftResults.takeLast(20).reversed().map {
                ChartDataPoint(
                    label = it.timestamp.toString(),
                    value = it.driftScore.toFloat(),
                    timestamp = it.timestamp.toEpochMilli()
                )
            }

            DriftLineChart(
                data = chartData,
                title = "Drift Score Over Time",
                thresholdLine = 0.3f,
                showGrid = true
            )
        }
    }
}

@Composable
fun DriftHeatmapCard(driftResult: DriftResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val featureNames = driftResult.featureDrifts.map { it.featureName }
            val driftScores = driftResult.featureDrifts.map { it.driftScore.toFloat() }

            DriftHeatmap(
                featureNames = featureNames,
                driftScores = driftScores,
                title = "Feature-Level Drift Distribution"
            )
        }
    }
}

@Composable
fun FeatureAttributionCard(driftResult: DriftResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val chartData = driftResult.featureDrifts
                .sortedByDescending { it.attribution }
                .take(10)
                .map {
                    ChartDataPoint(
                        label = it.featureName,
                        value = it.attribution.toFloat()
                    )
                }

            FeatureAttributionBarChart(
                data = chartData,
                title = "Top Contributing Features (Attribution Scores)"
            )
        }
    }
}

@Composable
fun DriftTypeDistributionCard(driftResults: List<DriftResult>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Drift Type Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val typeCount = driftResults.groupingBy { it.driftType }.eachCount()

            DriftType.values().forEach { type ->
                val count = typeCount[type] ?: 0
                val percentage = if (driftResults.isNotEmpty()) {
                    (count.toFloat() / driftResults.size) * 100
                } else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$count occurrences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format("%.1f%%", percentage),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = when (type) {
                        DriftType.CONCEPT_DRIFT -> Color(0xFFF44336)
                        DriftType.COVARIATE_DRIFT -> Color(0xFFFF9800)
                        DriftType.PRIOR_DRIFT -> Color(0xFF2196F3)
                        DriftType.NO_DRIFT -> Color(0xFF4CAF50)
                    }
                )

                if (type != DriftType.values().last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StatisticalTestsCard(driftResult: DriftResult) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistical Tests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    driftResult.statisticalTests.forEach { test ->
                        StatisticalTestRow(
                            test.testName,
                            test.statistic,
                            test.pValue,
                            test.isPassed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticalTestRow(testName: String, statistic: Double, pValue: Double, isPassed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = testName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row {
                Text(
                    text = "Statistic: ${String.format("%.4f", statistic)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "p-value: ${String.format("%.4f", pValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DriftTrendsCard(driftResults: List<DriftResult>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Drift Trends & Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val recentDrifts = driftResults.take(10)
            val trend = if (recentDrifts.size >= 2) {
                val recent = recentDrifts.take(5).map { it.driftScore }.average()
                val older = recentDrifts.drop(5).take(5).map { it.driftScore }.average()
                when {
                    recent > older + 0.1 -> "Increasing"
                    recent < older - 0.1 -> "Decreasing"
                    else -> "Stable"
                }
            } else "Insufficient Data"

            InsightRow(
                icon = Icons.Default.TrendingUp,
                label = "Trend",
                value = trend,
                color = when (trend) {
                    "Increasing" -> Color(0xFFF44336)
                    "Decreasing" -> Color(0xFF4CAF50)
                    else -> Color(0xFF2196F3)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val avgDrift = recentDrifts.map { it.driftScore }.average()
            InsightRow(
                icon = Icons.Default.Analytics,
                label = "Average Drift (Last 10)",
                value = String.format("%.4f", avgDrift),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            val highDriftCount = recentDrifts.count { it.driftScore > 0.5 }
            InsightRow(
                icon = Icons.Default.Warning,
                label = "High Drift Events",
                value = "$highDriftCount / ${recentDrifts.size}",
                color = if (highDriftCount > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun InsightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun InteractiveDriftCard(
    driftResult: DriftResult,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val formattedDate = driftResult.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (driftResult.isDriftDetected) {
                when {
                    driftResult.driftScore > 0.5 -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (driftResult.isDriftDetected) {
                        Icon(
                            imageVector = if (driftResult.driftScore > 0.5)
                                Icons.Default.Error
                            else Icons.Default.Warning,
                            contentDescription = "Drift detected",
                            tint = if (driftResult.driftScore > 0.5)
                                MaterialTheme.colorScheme.error
                            else Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = driftResult.driftType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mini sparkline
                if (driftResult.featureDrifts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Sparkline(
                        data = driftResult.featureDrifts.map { it.driftScore.toFloat() },
                        modifier = Modifier
                            .width(120.dp)
                            .height(30.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.3f", driftResult.driftScore),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        driftResult.driftScore > 0.5 -> MaterialTheme.colorScheme.error
                        driftResult.driftScore > 0.2 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = "Drift Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========== Alert Components ==========

enum class AlertLevel {
    CRITICAL, WARNING, INFO
}

@Composable
fun AlertSummaryCard(criticalCount: Int, warningCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AlertMetric(
                icon = Icons.Default.Error,
                count = criticalCount,
                label = "Critical",
                color = MaterialTheme.colorScheme.error
            )
            AlertMetric(
                icon = Icons.Default.Warning,
                count = warningCount,
                label = "Warning",
                color = Color(0xFFFF9800)
            )
            AlertMetric(
                icon = Icons.Default.Notifications,
                count = totalCount,
                label = "Total",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AlertMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
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
fun AlertCard(driftResult: DriftResult, level: AlertLevel) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
    val formattedDate = driftResult.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter)
    val viewModel: DriftDashboardViewModel = koinViewModel()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (level) {
                AlertLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                AlertLevel.WARNING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                AlertLevel.INFO -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (level) {
                            AlertLevel.CRITICAL -> Icons.Default.Error
                            AlertLevel.WARNING -> Icons.Default.Warning
                            AlertLevel.INFO -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (level) {
                            AlertLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            AlertLevel.WARNING -> Color(0xFFFF9800)
                            AlertLevel.INFO -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = driftResult.driftType.name.replace("_", " "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (level) {
                        AlertLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        AlertLevel.WARNING -> Color(0xFFFF9800)
                        AlertLevel.INFO -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(
                        text = String.format("%.3f", driftResult.driftScore),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Top affected features:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            driftResult.featureDrifts
                .sortedByDescending { it.driftScore }
                .take(3)
                .forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${feature.featureName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = String.format("%.3f", feature.driftScore),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { /* View Details */ }) {
                    Text("View Details")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.generatePatch(driftResult)
                    },
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Patch")
                }
            }
        }
    }
}

// ========== AI Explanation Sheet ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIExplanationSheet(
    driftResult: DriftResult,
    onDismiss: () -> Unit
) {
    // TODO: Integrate with AIAnalysisEngine
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mock AI explanation - replace with actual AI integration
            Text(
                text = generateMockExplanation(driftResult),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recommended Actions:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val recommendations = generateMockRecommendations(driftResult)
            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

fun generateMockExplanation(driftResult: DriftResult): String {
    val severity = when {
        driftResult.driftScore > 0.7 -> "High"
        driftResult.driftScore > 0.5 -> "Moderate"
        driftResult.driftScore > 0.2 -> "Minor"
        else -> "Low"
    }

    val topFeatures = driftResult.featureDrifts
        .sortedByDescending { it.driftScore }
        .take(3)
        .joinToString(", ") { it.featureName }

    return """
        $severity drift detected in your model. The drift score of ${
        String.format(
            "%.3f",
            driftResult.driftScore
        )
    } indicates ${if (driftResult.driftScore > 0.5) "significant" else "notable"} changes in the input data distribution.
        
        The primary contributing features are: $topFeatures. These features are showing substantial deviation from the reference distribution, which may impact model predictions.
        
        ${if (driftResult.driftScore > 0.5) "Immediate action is recommended to maintain model performance." else "Continue monitoring and consider intervention if drift persists."}
    """.trimIndent()
}

fun generateMockRecommendations(driftResult: DriftResult): List<String> {
    return when {
        driftResult.driftScore > 0.5 -> listOf(
            "Apply auto-generated patch immediately to stabilize performance",
            "Schedule model retraining within the next 1-2 weeks",
            "Increase monitoring frequency to hourly checks",
            "Run validation suite to quantify performance degradation"
        )

        driftResult.driftScore > 0.2 -> listOf(
            "Review and apply suggested patches to adapt to data changes",
            "Collect more samples from the new distribution for retraining",
            "Investigate why top features are drifting",
            "Plan retraining within 1-2 months if drift continues"
        )

        else -> listOf(
            "Continue current monitoring schedule",
            "Document this drift pattern for trend analysis",
            "Optional: Consider applying light patches if available",
            "Review again in 1-2 weeks to ensure drift doesn't increase"
        )
    }
}

// ========== Empty/Loading States ==========

@Composable
fun EmptyDashboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No Active Models",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Register a model to start monitoring drift",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading dashboard...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error: $message",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
