package com.driftdetector.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driftdetector.app.presentation.util.*

/**
 * Responsive container that centers content on large screens
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val maxWidth = rememberMaxContentWidth()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth)
        ) {
            content()
        }
    }
}

/**
 * Responsive row that becomes column on small screens
 */
@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.Top,
    content: @Composable () -> Unit
) {
    val windowSize = rememberWindowSizeInfo()

    if (windowSize.isCompact && windowSize.isPortrait) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(ResponsiveSpacing.vertical()),
            content = { content() }
        )
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = { content() }
        )
    }
}

/**
 * Responsive grid for cards or items
 */
@Composable
fun ResponsiveGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    val columns = ResponsiveGrid.cardColumns()
    val padding = ResponsivePadding.medium()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(padding),
        horizontalArrangement = Arrangement.spacedBy(padding),
        verticalArrangement = Arrangement.spacedBy(padding),
        content = content
    )
}

/**
 * Responsive two-pane layout for master-detail pattern
 */
@Composable
fun ResponsiveTwoPane(
    modifier: Modifier = Modifier,
    masterPane: @Composable BoxScope.() -> Unit,
    detailPane: @Composable BoxScope.() -> Unit,
    showDetailPane: Boolean = true
) {
    val useTwoPane = shouldUseTwoPane()
    val windowSize = rememberWindowSizeInfo()

    if (useTwoPane && showDetailPane) {
        // Two-pane layout for tablets
        Row(modifier = modifier.fillMaxSize()) {
            // Master pane (list)
            Box(
                modifier = Modifier
                    .weight(if (windowSize.isExpanded) 0.4f else 0.5f)
                    .fillMaxHeight()
            ) {
                masterPane()
            }

            // Detail pane
            Box(
                modifier = Modifier
                    .weight(if (windowSize.isExpanded) 0.6f else 0.5f)
                    .fillMaxHeight()
            ) {
                detailPane()
            }
        }
    } else {
        // Single pane layout for phones
        if (showDetailPane) {
            Box(modifier = modifier.fillMaxSize()) {
                detailPane()
            }
        } else {
            Box(modifier = modifier.fillMaxSize()) {
                masterPane()
            }
        }
    }
}

/**
 * Responsive card with adaptive sizing
 */
@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = ResponsivePadding.medium()

    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(ResponsiveSpacing.vertical()),
            content = content
        )
    }
}

/**
 * Responsive scaffold with adaptive navigation
 */
@Composable
fun ResponsiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    navigationRail: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSize = rememberWindowSizeInfo()

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = if (!windowSize.isExpanded) topBar else {
            {}
        },
        bottomBar = if (windowSize.isCompact) bottomBar else {
            {}
        }
    ) { padding ->
        if (windowSize.isExpanded || (windowSize.isMedium && windowSize.isLandscape)) {
            // Navigation rail for tablets
            Row(modifier = Modifier.fillMaxSize()) {
                navigationRail()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(padding)
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        } else {
            // Standard content for phones
            content(padding)
        }
    }
}

/**
 * Responsive text with scaled font size
 */
@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge
) {
    val scaleFactor = ResponsiveText.scaleFactor()

    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = style.fontSize * scaleFactor)
    )
}

/**
 * Adaptive dialog that becomes full screen on small devices
 */
@Composable
fun ResponsiveDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val windowSize = rememberWindowSizeInfo()

    if (windowSize.isCompact && windowSize.isPortrait) {
        // Full screen dialog for small phones
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ResponsivePadding.medium())
            ) {
                androidx.compose.material3.Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content = content
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    content = actions
                )
            }
        }
    } else {
        // Standard dialog for larger screens
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { androidx.compose.material3.Text(title) },
            text = { Column(content = content) },
            confirmButton = { Row(content = actions) }
        )
    }
}

/**
 * Responsive bottom sheet that adapts to screen size
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResponsiveBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val windowSize = rememberWindowSizeInfo()
    val maxWidth = if (windowSize.isExpanded) 600.dp else windowSize.screenWidth

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = maxWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ResponsivePadding.large()),
            content = content
        )
    }
}
