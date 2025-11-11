package com.driftdetector.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.graphicsLayer
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
    viewModel: DriftDashboardViewModel = koinViewModel(),
    onNavigateToInstantDriftFix: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) {
            Snackbar(
                snackbarData = it,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                actionColor = MaterialTheme.colorScheme.primary,
                dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(8.dp),
                shape = MaterialTheme.shapes.medium
            )
        } }
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
                    TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Overview", color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                            icon = { Icon(Icons.Default.Dashboard, "Overview tab", tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Analytics", color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                            icon = { Icon(Icons.Default.Analytics, "Analytics tab", tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Alerts", color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) },
                            icon = { Icon(Icons.Default.Notifications, "Alerts tab", tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> OverviewTab(
                            state = state,
                            onDriftClick = {
                                selectedDriftResult = it
                                showAIExplanation = true
                            },
                            onNavigateToInstantDriftFix = onNavigateToInstantDriftFix
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
    onDriftClick: (DriftResult) -> Unit,
    onNavigateToInstantDriftFix: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Metrics Summary with fade in animation
        item {
            SlideInFromSide(visible = true, delayMillis = 0, fromLeft = true) {
                MetricsSummaryCard(state)
            }
        }

        // Quick action button for Instant Drift Fix
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onNavigateToInstantDriftFix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Instant Drift Fix",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Instant Drift Fix",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Drift Gauge - Current Status with animation
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null) {
                SlideInFromSide(visible = true, delayMillis = 100, fromLeft = false) {
                    DriftStatusGaugeCard(latestDrift)
                }
            }
        }

        // Drift Timeline Chart with animation
        item {
            SlideInFromSide(visible = true, delayMillis = 200, fromLeft = true) {
                DriftTimelineCard(state.driftResults)
            }
        }

        // Feature Drift Heatmap with animation
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.featureDrifts.isNotEmpty()) {
                SlideInFromSide(visible = true, delayMillis = 300, fromLeft = false) {
                    DriftHeatmapCard(latestDrift)
                }
            }
        }

        // Recent Drift Events
        item {
            FadeInCard(visible = true, delayMillis = 400) {
                Text(
                    "Recent Drift Events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        items(state.driftResults.take(10)) { drift ->
            FadeInCard(visible = true, delayMillis = 450) {
                InteractiveDriftCard(
                    driftResult = drift,
                    onClick = { onDriftClick(drift) }
                )
            }
        }
    }
}

// Animation composables for card sliding/fading
@Composable
fun SlideInFromSide(
    visible: Boolean,
    delayMillis: Int = 0,
    fromLeft: Boolean = true,
    content: @Composable () -> Unit
) {
    val enterTransition = if (fromLeft) {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(durationMillis = 500, delayMillis = delayMillis)
        ) + fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = delayMillis))
    } else {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 3 },
            animationSpec = tween(durationMillis = 500, delayMillis = delayMillis)
        ) + fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = delayMillis))
    }
    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun FadeInCard(
    visible: Boolean,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = delayMillis))
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun AnalyticsTab(state: DriftDashboardState.Success) {
    // Add safety check for empty data
    if (state.driftResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "No analytics data",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Analytics Data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Analytics will appear here once drift detection runs",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Attribution Analysis - with null safety
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.featureDrifts.isNotEmpty()) {
                FeatureAttributionCard(latestDrift)
            }
        }

        // Drift Distribution by Type - with null safety
        item {
            if (state.driftResults.isNotEmpty()) {
                DriftTypeDistributionCard(state.driftResults)
            }
        }

        // Statistical Tests Results - with null safety
        item {
            val latestDrift = state.driftResults.firstOrNull()
            if (latestDrift != null && latestDrift.statisticalTests.isNotEmpty()) {
                StatisticalTestsCard(latestDrift)
            }
        }

        // Drift Trends - with null safety
        item {
            if (state.driftResults.isNotEmpty()) {
                DriftTrendsCard(state.driftResults)
            }
        }
    }
}

@Composable
fun AlertsTab(state: DriftDashboardState.Success) {
    val criticalDrifts = state.driftResults.filter { it.isDriftDetected && it.driftScore > 0.5 }
    val warningDrifts =
        state.driftResults.filter { it.isDriftDetected && it.driftScore in 0.2..0.5 }
    var selectedAlertForDetails by remember { mutableStateOf<DriftResult?>(null) }

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
                AlertCard(
                    driftResult = drift,
                    level = AlertLevel.CRITICAL,
                    onViewDetails = { selectedAlertForDetails = drift }
                )
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
                AlertCard(
                    driftResult = drift,
                    level = AlertLevel.WARNING,
                    onViewDetails = { selectedAlertForDetails = drift }
                )
            }
        }
    }

    // Alert Details Dialog
    if (selectedAlertForDetails != null) {
        AlertDetailsDialog(
            driftResult = selectedAlertForDetails!!,
            onDismiss = { selectedAlertForDetails = null }
        )
    }
}

// ========== Card Components ==========

// --- Animated Counter Composables & Effects ---

@Composable
fun AnimatedCounter(
    count: Int,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    var animatedCount by remember { mutableStateOf(0) }
    val animatedValue = animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )
    LaunchedEffect(count) {
        animatedCount = count
    }
    Text(
        text = animatedValue.value.toString(),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color ?: MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    )
}

@Composable
fun AnimatedNumberRoll(
    number: Float,
    decimals: Int,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val animated by animateFloatAsState(
        targetValue = number,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )
    Text(
        text = "%.${decimals}f".format(animated),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color ?: MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    )
}

@Composable
fun PulsingEffect(
    modifier: Modifier = Modifier,
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    durationMillis: Int = 2000,
    content: @Composable (Modifier) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "PulseAnim")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAnimVal"
    )
    content(modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    })
}

@Composable
fun MetricsSummaryCard(state: DriftDashboardState.Success) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.95f)
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
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
                    tooltip = "Average drift score: ${String.format("%.4f", avgDrift)}\nCalculated across ${state.driftResults.size} monitoring periods\n\nThis metric helps track overall model stability over time."
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
    val isDark = isSystemInDarkTheme()

    val iconBackgroundAlpha = if (isDark) 0.35f else 0.20f
    val iconTint = if (isDark) color.copy(alpha = 0.80f) else color
    val labelTextColor =
        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val infoIconAlpha = if (isDark) 0.9f else 0.7f
    val tooltipSurfaceColor =
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f) else MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(
                enabled = tooltip != null,
                onClick = { showTooltip = !showTooltip }
            )
    ) {
        // Pulsing icon for critical metrics
        PulsingEffect(
            modifier = Modifier,
            minScale = 0.98f,
            maxScale = 1.02f,
            durationMillis = 2000
        ) { mod ->
            Box(
                modifier = mod
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = iconBackgroundAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$label metric",
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Animated number display
        val numberValue = value.toIntOrNull()
        if (numberValue != null) {
            AnimatedCounter(
                count = numberValue,
                style = MaterialTheme.typography.headlineMedium,
                color = labelTextColor,
                modifier = Modifier
            )
        } else {
            // For decimal numbers like drift scores
            val floatValue = value.toFloatOrNull()
            if (floatValue != null) {
                AnimatedNumberRoll(
                    number = floatValue,
                    decimals = 3,
                    style = MaterialTheme.typography.headlineMedium,
                    color = labelTextColor
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = labelTextColor
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = labelTextColor
            )
            if (tooltip != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Tap for details",
                    modifier = Modifier.size(16.dp),
                    tint = labelTextColor.copy(alpha = infoIconAlpha)
                )
            }
        }

        // Tooltip overlay - Shows detailed information when tapped
        if (showTooltip && tooltip != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = tooltipSurfaceColor,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 280.dp)
                ) {
                    // Parse and display tooltip data
                    val lines = tooltip.split("\n")
                    lines.forEach { line ->
                        when {
                            line.contains("Average drift score:") -> {
                                Text(
                                    text = "📊 Detailed Metrics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                                )
                            }

                            line.contains("Calculated across") -> {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
                                )
                            }

                            line.contains("This metric helps") -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            line.isNotBlank() -> {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                                )
                            }
                        }
                        if (line.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriftStatusGaugeCard(driftResult: DriftResult) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Drift Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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

            // Visual Drift Level Badge with better contrast
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = driftColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Drift Level: $driftLevel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = driftColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score: ${String.format("%.3f", driftResult.driftScore)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
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

            // Business Impact Explanation with better contrast
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Drift explanation",
                            tint = if (isDark)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "What does this mean?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = getDriftExplanation(driftLevel, driftResult.driftType),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action recommendation with better visibility
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (driftLevel) {
                            "HIGH" -> Icons.Default.Warning
                            "MODERATE" -> Icons.Default.Info
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Drift status: $driftLevel",
                        tint = driftColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getActionRecommendation(driftLevel),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
                    )
                }
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Drift Type Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$count occurrences",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
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
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                Text(
                    text = "Statistic: ${String.format("%.4f", statistic)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "p-value: ${String.format("%.4f", pValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (isPassed) "Test passed" else "Test failed",
            tint = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun DriftTrendsCard(driftResults: List<DriftResult>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Drift Trends & Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
            contentDescription = "$label trend",
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                    driftResult.driftScore > 0.5 -> MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.3f
                    )

                    else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
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
                            contentDescription = "Drift severity indicator",
                            tint = if (driftResult.driftScore > 0.5)
                                MaterialTheme.colorScheme.error
                            else Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = driftResult.driftType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        driftResult.driftScore > 0.5 -> MaterialTheme.colorScheme.error
                        driftResult.driftScore > 0.2 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = "Drift Score",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
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
    val isDark = isSystemInDarkTheme()
    val iconBackgroundAlpha = if (isDark) 0.35f else 0.25f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = iconBackgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label alerts",
                tint = color,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun AlertCard(
    driftResult: DriftResult,
    level: AlertLevel,
    onViewDetails: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
    val formattedDate = driftResult.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter)
    val viewModel: DriftDashboardViewModel = koinViewModel()
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (level) {
                AlertLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.40f else 0.3f)
                AlertLevel.WARNING -> Color(0xFFFF9800).copy(alpha = if (isDark) 0.20f else 0.12f)
                AlertLevel.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.60f else 0.5f)
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
                        contentDescription = "Alert severity: ${level.name}",
                        tint = when (level) {
                            AlertLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            AlertLevel.WARNING -> Color(0xFFFF9800)
                            AlertLevel.INFO -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = driftResult.driftType.name.replace("_", " "),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Top affected features:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.3f", feature.driftScore),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onViewDetails) {
                    Text(
                        "View Details",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.generatePatch(driftResult)
                    },
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Generate patch",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Generate Patch",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== Alert Details Dialog ==========

@Composable
fun AlertDetailsDialog(
    driftResult: DriftResult,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")
    val formattedDate = driftResult.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when {
                    driftResult.driftScore > 0.5 -> Icons.Default.Error
                    driftResult.driftScore > 0.2 -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = "Alert details",
                tint = when {
                    driftResult.driftScore > 0.5 -> MaterialTheme.colorScheme.error
                    driftResult.driftScore > 0.2 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Alert Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drift Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailRow("Drift Type", driftResult.driftType.name.replace("_", " "))
                            DetailRow("Drift Score", String.format("%.4f", driftResult.driftScore))
                            DetailRow("Detected At", formattedDate)
                            DetailRow(
                                "Severity",
                                when {
                                    driftResult.driftScore > 0.7 -> "🔴 HIGH"
                                    driftResult.driftScore > 0.4 -> "🟠 MODERATE"
                                    driftResult.driftScore > 0.15 -> "🟡 LOW"
                                    else -> "🟢 MINIMAL"
                                }
                            )
                        }
                    }
                }

                // Feature Drifts Section
                item {
                    HorizontalDivider()
                    Text(
                        "📊 Feature-Level Drift Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(driftResult.featureDrifts.sortedByDescending { it.driftScore }) { feature ->
                    FeatureDriftDetailCard(feature)
                }

                // Statistical Tests Section
                if (driftResult.statisticalTests.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text(
                            "📈 Statistical Tests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(driftResult.statisticalTests) { test ->
                        StatisticalTestDetailCard(test)
                    }
                }

                // Recommendations Section
                item {
                    HorizontalDivider()
                    Text(
                        "💡 Recommended Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val recommendations = when {
                                driftResult.driftScore > 0.5 -> listOf(
                                    "🔧 Apply auto-generated patch immediately",
                                    "📊 Schedule model retraining within 1-2 weeks",
                                    "⏰ Increase monitoring frequency to hourly",
                                    "✅ Run validation suite to quantify degradation"
                                )

                                driftResult.driftScore > 0.2 -> listOf(
                                    "🔍 Review and apply suggested patches",
                                    "📈 Collect more samples for retraining",
                                    "🔎 Investigate why top features are drifting",
                                    "📅 Plan retraining within 1-2 months if drift persists"
                                )

                                else -> listOf(
                                    "👀 Continue current monitoring schedule",
                                    "📝 Document this pattern for trend analysis",
                                    "🔧 Consider applying light patches if available",
                                    "⏱️ Review again in 1-2 weeks"
                                )
                            }

                            recommendations.forEach { recommendation ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            val viewModel: DriftDashboardViewModel = koinViewModel()
            OutlinedButton(onClick = {
                viewModel.generatePatch(driftResult)
                onDismiss()
            }) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "Generate patch",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Patch")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun FeatureDriftDetailCard(feature: FeatureDrift) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feature.featureName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        feature.driftScore > 0.5 -> MaterialTheme.colorScheme.error
                        feature.driftScore > 0.2 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(
                        text = String.format("%.4f", feature.driftScore),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            LinearProgressIndicator(
                progress = (feature.driftScore.toFloat().coerceIn(0f, 1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = when {
                    feature.driftScore > 0.5 -> MaterialTheme.colorScheme.error
                    feature.driftScore > 0.2 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attribution: ${String.format("%.3f", feature.attribution)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PSI: ${feature.psiScore?.let { String.format("%.3f", it) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatisticalTestDetailCard(test: com.driftdetector.app.domain.model.StatisticalTestResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (test.isPassed)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (test.isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = if (test.isPassed) "Test passed" else "Test failed",
                        tint = if (test.isPassed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = test.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Statistic: ${String.format("%.4f", test.statistic)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "p-value: ${String.format("%.4f", test.pValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    contentDescription = "AI assistant",
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
                        contentDescription = "Recommended action",
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
                contentDescription = "No models available",
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




