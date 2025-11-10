package com.driftdetector.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern UI Components for DriftDetector App
 * Enhances existing UI with animations and effects
 */

// ==================== Animated Gradient Background ====================

@Composable
fun AnimatedGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    animationDurationMillis: Int = 3000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = colors,
                startY = offset * 1000f,
                endY = (offset + 1f) * 1000f
            )
        )
    )
}

// ==================== Glassmorphic Card ====================

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ),
    elevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            pressedElevation = elevation + 2.dp,
            focusedElevation = elevation + 2.dp,
            hoveredElevation = elevation + 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

// ==================== Spring Button ====================

@Composable
fun SpringButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(scale),
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

// ==================== Animated Counter ====================

@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var oldCount by remember { mutableStateOf(count) }
    val countAnimatable = remember { Animatable(oldCount.toFloat()) }

    LaunchedEffect(count) {
        countAnimatable.animateTo(
            targetValue = count.toFloat(),
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        oldCount = count
    }

    Text(
        text = countAnimatable.value.toInt().toString(),
        style = style,
        modifier = modifier
    )
}

// ==================== Shimmer Loading Effect ====================

@Composable
fun ShimmerLoading(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
            val shimmerOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_offset"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startX = shimmerOffset - 500f,
                            endX = shimmerOffset + 500f
                        )
                    )
            )
        }
    }
}

// ==================== Lottie Loading Animation ====================

@Composable
fun LottieLoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    // Using a simple circular progress animation
    // You can replace this with actual Lottie files later
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * 0.8f),
            strokeWidth = 4.dp
        )
    }
}

// ==================== Fade In Card Animation ====================

@Composable
fun FadeInCard(
    visible: Boolean = true,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(500, delayMillis = delayMillis)
        ) + expandVertically(
            animationSpec = tween(500, delayMillis = delayMillis)
        ),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(modifier = modifier) {
            content()
        }
    }
}

// ==================== Slide In From Side Animation ====================

@Composable
fun SlideInFromSide(
    visible: Boolean = true,
    delayMillis: Int = 0,
    fromLeft: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { if (fromLeft) -it else it },
            animationSpec = tween(500, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(500, delayMillis = delayMillis)),
        exit = slideOutHorizontally(
            targetOffsetX = { if (fromLeft) -it else it }
        ) + fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

// ==================== Pulsing Effect ====================

@Composable
fun PulsingEffect(
    modifier: Modifier = Modifier,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

// ==================== Enhanced Card with Elevation Animation ====================

@Composable
fun EnhancedCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    baseElevation: Dp = 4.dp,
    hoveredElevation: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (isHovered) hoveredElevation else baseElevation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_elevation"
    )

    Card(
        onClick = { onClick?.invoke(); isHovered = true },
        modifier = modifier,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            kotlinx.coroutines.delay(100)
            isHovered = false
        }
    }
}

// ==================== Success Animation ====================

@Composable
fun SuccessAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        // Show success checkmark or celebration
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
    }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(1500)
            onAnimationComplete()
        }
    }
}

// ==================== Number Roll Animation ====================

@Composable
fun AnimatedNumberRoll(
    number: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 3,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedNumber = remember { Animatable(number) }

    LaunchedEffect(number) {
        animatedNumber.animateTo(
            targetValue = number,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    Text(
        text = String.format("%.${decimals}f", animatedNumber.value),
        style = style,
        color = color,
        modifier = modifier
    )
}

// ==================== Gradient Border Card ====================

@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color(0xFFFC6B3C),
        Color(0xFF8F44AD)
    ),
    borderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(borderWidth),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        content()
    }
}
