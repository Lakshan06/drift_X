package com.driftdetector.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pull-to-Refresh Component and Micro-interactions
 * Provides intuitive gesture-based UI interactions
 */

// ==================== Pull to Refresh ====================

/**
 * State for pull-to-refresh functionality
 */
class PullToRefreshState {
    var isRefreshing by mutableStateOf(false)
        private set

    var pullProgress by mutableStateOf(0f)
        private set

    var isPulling by mutableStateOf(false)
        private set

    fun startRefresh() {
        isRefreshing = true
    }

    fun endRefresh() {
        isRefreshing = false
        pullProgress = 0f
        isPulling = false
    }

    fun updatePullProgress(progress: Float) {
        pullProgress = progress.coerceIn(0f, 1f)
    }

    fun setIsPulling(pulling: Boolean) {
        isPulling = pulling
    }
}

@Composable
fun rememberPullToRefreshState(): PullToRefreshState {
    return remember { PullToRefreshState() }
}

/**
 * Pull-to-Refresh Container
 * Wraps content and adds pull-to-refresh functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshContainer(
    state: PullToRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Dp = 80.dp,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.toPx() }

    var pullOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            onRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        if (!state.isRefreshing) {
                            state.setIsPulling(true)
                        }
                    },
                    onDragEnd = {
                        if (pullOffset >= refreshThresholdPx && !state.isRefreshing) {
                            state.startRefresh()
                            coroutineScope.launch {
                                // Animate back to refresh position
                                pullOffset = refreshThresholdPx
                            }
                        } else {
                            // Reset
                            pullOffset = 0f
                            state.setIsPulling(false)
                            state.updatePullProgress(0f)
                        }
                    },
                    onDragCancel = {
                        pullOffset = 0f
                        state.setIsPulling(false)
                        state.updatePullProgress(0f)
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (!state.isRefreshing && dragAmount > 0) {
                            // Apply resistance to pull
                            pullOffset = (pullOffset + dragAmount * 0.5f).coerceAtLeast(0f)
                            val progress = (pullOffset / refreshThresholdPx).coerceIn(0f, 1f)
                            state.updatePullProgress(progress)
                        }
                    }
                )
            }
    ) {
        // Content with offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = if (state.isRefreshing) refreshThresholdPx else pullOffset
                }
        ) {
            content()
        }

        // Refresh indicator
        PullToRefreshIndicator(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-refreshThreshold))
                .graphicsLayer {
                    translationY = if (state.isRefreshing) refreshThresholdPx else pullOffset
                }
        )
    }
}

/**
 * Visual indicator for pull-to-refresh
 */
@Composable
private fun PullToRefreshIndicator(
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (state.isRefreshing) 360f else state.pullProgress * 180f,
        animationSpec = if (state.isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            spring()
        },
        label = "refresh_rotation"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                alpha = state.pullProgress
                scaleX = state.pullProgress
                scaleY = state.pullProgress
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.pullProgress >= 1f) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                    contentDescription = "Pull to refresh",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

// ==================== Micro-interactions ====================

/**
 * Bounce animation on press
 */
@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = colors,
        content = content
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Card with press feedback animation
 */
@Composable
fun InteractiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = spring(),
        label = "card_press_elevation"
    )

    Card(
        onClick = {
            if (enabled) {
                isPressed = true
                onClick()
            }
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Icon button with ripple and scale effect
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "icon_button_scale"
    )

    IconButton(
        onClick = {
            if (enabled) {
                isPressed = true
                onClick()
            }
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled
    ) {
        content()
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(80)
            isPressed = false
        }
    }
}

/**
 * Switch with smooth animation
 */
@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val transition = updateTransition(targetState = checked, label = "switch")

    val thumbPosition by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "thumb_position"
    ) { isChecked ->
        if (isChecked) 1f else 0f
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer {
            // Add subtle bounce effect
            scaleX = 1f + (abs(thumbPosition - 0.5f) * 0.1f)
            scaleY = 1f + (abs(thumbPosition - 0.5f) * 0.1f)
        },
        enabled = enabled
    )
}

/**
 * Floating Action Button with reveal animation
 */
@Composable
fun AnimatedFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "fab_rotation"
    )

    FloatingActionButton(
        onClick = {
            isExpanded = !isExpanded
            onClick()
        },
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.graphicsLayer { rotationZ = -rotation }) {
                icon()
            }

            AnimatedVisibility(
                visible = isExpanded && text != null,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                text?.invoke()
            }
        }
    }
}

/**
 * Checkbox with smooth check animation
 */
@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val transition = updateTransition(targetState = checked, label = "checkbox")

    val scale by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        },
        label = "checkbox_scale"
    ) { isChecked ->
        if (isChecked) 1.1f else 1f
    }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled
    )
}

/**
 * Progress indicator with bounce animation
 */
@Composable
fun BouncingProgressIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce_progress")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

/**
 * Pulsing dot indicator
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Box(
        modifier = modifier
            .size(12.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color,
            modifier = Modifier.fillMaxSize()
        ) {}
    }
}
