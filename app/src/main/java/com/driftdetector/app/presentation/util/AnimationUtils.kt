package com.driftdetector.app.presentation.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard animation durations
 */
object AnimationDurations {
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 700
}

/**
 * Standard animation delays
 */
object AnimationDelays {
    const val NONE = 0
    const val SHORT = 50
    const val MEDIUM = 100
    const val LONG = 150
}

/**
 * Standard easing functions
 */
object AnimationEasing {
    val FastOutSlowIn = FastOutSlowInEasing
    val LinearOutSlowIn = LinearOutSlowInEasing
    val FastOutLinearIn = FastOutLinearInEasing
    val EaseInOut: Easing = androidx.compose.animation.core.EaseInOut
    val EaseIn: Easing = androidx.compose.animation.core.EaseIn
    val EaseOut: Easing = androidx.compose.animation.core.EaseOut
    val Linear = LinearEasing

    // Custom easings
    val Bounce = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    val Smooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
}

/**
 * Fade in animation
 */
fun fadeInAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE,
    easing: Easing = AnimationEasing.FastOutSlowIn
): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delay,
            easing = easing
        )
    )
}

/**
 * Fade out animation
 */
fun fadeOutAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE,
    easing: Easing = AnimationEasing.FastOutSlowIn
): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delay,
            easing = easing
        )
    )
}

/**
 * Slide in from top animation
 */
fun slideInFromTopAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): EnterTransition {
    return slideInVertically(
        animationSpec = tween(duration, delay),
        initialOffsetY = { -it }
    ) + fadeInAnimation(duration, delay)
}

/**
 * Slide in from bottom animation
 */
fun slideInFromBottomAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): EnterTransition {
    return slideInVertically(
        animationSpec = tween(duration, delay),
        initialOffsetY = { it }
    ) + fadeInAnimation(duration, delay)
}

/**
 * Slide in from left animation
 */
fun slideInFromLeftAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(duration, delay),
        initialOffsetX = { -it }
    ) + fadeInAnimation(duration, delay)
}

/**
 * Slide in from right animation
 */
fun slideInFromRightAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(duration, delay),
        initialOffsetX = { it }
    ) + fadeInAnimation(duration, delay)
}

/**
 * Slide out to top animation
 */
fun slideOutToTopAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(duration, delay),
        targetOffsetY = { -it }
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Slide out to bottom animation
 */
fun slideOutToBottomAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(duration, delay),
        targetOffsetY = { it }
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Scale in animation
 */
fun scaleInAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE,
    initialScale: Float = 0.8f
): EnterTransition {
    return scaleIn(
        animationSpec = tween(duration, delay),
        initialScale = initialScale
    ) + fadeInAnimation(duration, delay)
}

/**
 * Scale out animation
 */
fun scaleOutAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE,
    targetScale: Float = 0.8f
): ExitTransition {
    return scaleOut(
        animationSpec = tween(duration, delay),
        targetScale = targetScale
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Expand vertically animation
 */
fun expandVerticallyAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): EnterTransition {
    return expandVertically(
        animationSpec = tween(duration, delay),
        expandFrom = Alignment.Top
    ) + fadeInAnimation(duration, delay)
}

/**
 * Shrink vertically animation
 */
fun shrinkVerticallyAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): ExitTransition {
    return shrinkVertically(
        animationSpec = tween(duration, delay),
        shrinkTowards = Alignment.Top
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Animated visibility with staggered children
 */
@Composable
fun StaggeredAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    staggerDelay: Int = AnimationDelays.SHORT,
    itemCount: Int,
    enter: EnterTransition = fadeInAnimation() + slideInFromBottomAnimation(),
    exit: ExitTransition = fadeOutAnimation() + slideOutToBottomAnimation(),
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    repeat(itemCount) { index ->
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = enter.run {
                // Add delay based on index
                fadeInAnimation(delay = staggerDelay * index) +
                        slideInFromBottomAnimation(delay = staggerDelay * index)
            },
            exit = exit
        ) {
            content(index)
        }
    }
}

/**
 * Animated float state with spring
 */
@Composable
fun animateFloatAsStateSpring(
    targetValue: Float,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium
): State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "float_spring"
    )
}

/**
 * Animated Dp state with spring
 */
@Composable
fun animateDpAsStateSpring(
    targetValue: Dp,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium
): State<Dp> {
    return animateDpAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "dp_spring"
    )
}

/**
 * Pulsating animation
 */
@Composable
fun rememberPulsatingAnimation(
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    return scale
}

/**
 * Rotating animation
 */
@Composable
fun rememberRotatingAnimation(
    duration: Int = 2000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotating")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    return rotation
}

/**
 * Shimmer animation
 */
@Composable
fun rememberShimmerAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return shimmer
}

/**
 * Bounce animation modifier
 * Note: This modifier requires composition, so it should be used within a @Composable context
 */
@Composable
fun Modifier.bounceClickEffect(): Modifier {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )

    return this.scale(animatedScale)
}

/**
 * Shake animation
 */
@Composable
fun rememberShakeAnimation(
    trigger: Boolean,
    duration: Int = 500
): Float {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = duration
                    0f at 0
                    -15f at duration / 8
                    15f at duration / 4
                    -10f at duration * 3 / 8
                    10f at duration / 2
                    -5f at duration * 5 / 8
                    5f at duration * 3 / 4
                    0f at duration
                }
            )
        }
    }

    return rotation.value
}

/**
 * Fade and scale transition for content
 */
fun fadeAndScaleTransition(): ContentTransform {
    return (fadeInAnimation() + scaleInAnimation()).togetherWith(
        fadeOutAnimation() + scaleOutAnimation()
    )
}

/**
 * Slide and fade transition for content
 */
fun slideAndFadeTransition(): ContentTransform {
    return (slideInFromRightAnimation() + fadeInAnimation()).togetherWith(
        slideOutToLeftAnimation() + fadeOutAnimation()
    )
}

/**
 * Slide out to left animation
 */
fun slideOutToLeftAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(duration, delay),
        targetOffsetX = { -it }
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Slide out to right animation
 */
fun slideOutToRightAnimation(
    duration: Int = AnimationDurations.NORMAL,
    delay: Int = AnimationDelays.NONE
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(duration, delay),
        targetOffsetX = { it }
    ) + fadeOutAnimation(duration, delay)
}

/**
 * Animated content switcher
 */
@Composable
fun <T> AnimatedContentSwitcher(
    targetState: T,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        fadeAndScaleTransition()
    },
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        label = "content_switcher"
    ) { state ->
        content(state)
    }
}

/**
 * Loading pulse animation
 */
@Composable
fun rememberLoadingPulseAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return alpha
}

/**
 * Success checkmark animation
 */
@Composable
fun rememberSuccessAnimation(
    trigger: Boolean
): Float {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = AnimationEasing.Bounce
                )
            )
        } else {
            progress.snapTo(0f)
        }
    }

    return progress.value
}

/**
 * Error shake animation
 */
@Composable
fun Modifier.errorShakeEffect(trigger: Boolean): Modifier {
    val shake = rememberShakeAnimation(trigger)
    return this.graphicsLayer {
        rotationZ = shake
    }
}

/**
 * Loading rotate animation modifier
 */
@Composable
fun Modifier.loadingRotate(): Modifier {
    val rotation = rememberRotatingAnimation()
    return this.graphicsLayer {
        rotationZ = rotation
    }
}

/**
 * Pulsating scale modifier
 */
@Composable
fun Modifier.pulsatingScale(enabled: Boolean = true): Modifier {
    val scale = if (enabled) rememberPulsatingAnimation() else 1f
    return this.scale(scale)
}

/**
 * Shimmer effect modifier
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val shimmer = rememberShimmerAnimation()
    return this.graphicsLayer {
        translationX = shimmer * size.width * 2
        alpha = 0.5f + (shimmer + 1f) * 0.25f
    }
}
