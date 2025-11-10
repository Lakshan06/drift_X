package com.driftdetector.app.presentation.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard corner radii
 */
object CornerRadii {
    val NONE = 0.dp
    val SMALL = 4.dp
    val MEDIUM = 8.dp
    val LARGE = 12.dp
    val EXTRA_LARGE = 16.dp
    val XXL = 20.dp
    val XXXL = 24.dp
}

/**
 * Standard elevations
 */
object Elevations {
    val NONE = 0.dp
    val LEVEL1 = 1.dp
    val LEVEL2 = 3.dp
    val LEVEL3 = 6.dp
    val LEVEL4 = 8.dp
    val LEVEL5 = 12.dp
}

/**
 * Standard border widths
 */
object BorderWidths {
    val THIN = 1.dp
    val NORMAL = 2.dp
    val THICK = 3.dp
    val EXTRA_THICK = 4.dp
}

/**
 * Gradient utilities
 */
object GradientUtils {
    /**
     * Create a linear gradient
     */
    fun linearGradient(
        colors: List<Color>,
        start: Offset = Offset(0f, 0f),
        end: Offset = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    ): Brush {
        return Brush.linearGradient(
            colors = colors,
            start = start,
            end = end
        )
    }

    /**
     * Create a vertical gradient
     */
    fun verticalGradient(
        colors: List<Color>
    ): Brush {
        return Brush.verticalGradient(colors)
    }

    /**
     * Create a horizontal gradient
     */
    fun horizontalGradient(
        colors: List<Color>
    ): Brush {
        return Brush.horizontalGradient(colors)
    }

    /**
     * Create a radial gradient
     */
    fun radialGradient(
        colors: List<Color>,
        center: Offset = Offset.Unspecified,
        radius: Float = Float.POSITIVE_INFINITY
    ): Brush {
        return Brush.radialGradient(
            colors = colors,
            center = center,
            radius = radius
        )
    }

    /**
     * Create a sweep gradient (circular)
     */
    fun sweepGradient(
        colors: List<Color>,
        center: Offset = Offset.Unspecified
    ): Brush {
        return Brush.sweepGradient(
            colors = colors,
            center = center
        )
    }
}

/**
 * Shape utilities
 */
object ShapeUtils {
    /**
     * Rounded corner shape
     */
    fun roundedCorner(radius: Dp = CornerRadii.MEDIUM): RoundedCornerShape {
        return RoundedCornerShape(radius)
    }

    /**
     * Top rounded corners only
     */
    fun topRoundedCorner(radius: Dp = CornerRadii.LARGE): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    }

    /**
     * Bottom rounded corners only
     */
    fun bottomRoundedCorner(radius: Dp = CornerRadii.LARGE): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = radius,
            bottomEnd = radius
        )
    }

    /**
     * Left rounded corners only
     */
    fun leftRoundedCorner(radius: Dp = CornerRadii.LARGE): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = radius,
            topEnd = 0.dp,
            bottomStart = radius,
            bottomEnd = 0.dp
        )
    }

    /**
     * Right rounded corners only
     */
    fun rightRoundedCorner(radius: Dp = CornerRadii.LARGE): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = 0.dp,
            topEnd = radius,
            bottomStart = 0.dp,
            bottomEnd = radius
        )
    }

    /**
     * Asymmetric rounded corners
     */
    fun asymmetricRoundedCorner(
        topStart: Dp = CornerRadii.MEDIUM,
        topEnd: Dp = CornerRadii.MEDIUM,
        bottomStart: Dp = CornerRadii.MEDIUM,
        bottomEnd: Dp = CornerRadii.MEDIUM
    ): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomStart = bottomStart,
            bottomEnd = bottomEnd
        )
    }

    /**
     * Circle shape
     */
    fun circle(): Shape {
        return CircleShape
    }
}

/**
 * Modifier extensions for UI polish
 */

/**
 * Add elevated card effect
 */
fun Modifier.elevatedCard(
    elevation: Dp = Elevations.LEVEL2,
    cornerRadius: Dp = CornerRadii.LARGE,
    clip: Boolean = true
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = clip
        )
}

/**
 * Add bordered effect
 */
@Composable
fun Modifier.bordered(
    width: Dp = BorderWidths.NORMAL,
    color: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = CornerRadii.MEDIUM
): Modifier {
    return this.border(
        width = width,
        color = color,
        shape = RoundedCornerShape(cornerRadius)
    )
}

/**
 * Add gradient background
 */
fun Modifier.gradientBackground(
    brush: Brush
): Modifier {
    return this.background(brush)
}

/**
 * Add glassmorphism effect
 */
@Composable
fun Modifier.glassmorphism(
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    cornerRadius: Dp = CornerRadii.LARGE,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
): Modifier {
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Add neumorphism effect (soft shadow)
 */
fun Modifier.neumorphism(
    cornerRadius: Dp = CornerRadii.LARGE,
    elevation: Dp = Elevations.LEVEL2
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = Color.Black.copy(alpha = 0.15f),
            spotColor = Color.Black.copy(alpha = 0.15f)
        )
}

/**
 * Add pressed effect (darker on press)
 */
@Composable
fun Modifier.pressedEffect(
    pressed: Boolean,
    pressedAlpha: Float = 0.7f
): Modifier {
    return this.graphicsLayer {
        alpha = if (pressed) pressedAlpha else 1f
        scaleX = if (pressed) 0.98f else 1f
        scaleY = if (pressed) 0.98f else 1f
    }
}

/**
 * Add hover effect (for large screens)
 */
@Composable
fun Modifier.hoverEffect(
    hovered: Boolean,
    hoverElevation: Dp = Elevations.LEVEL3,
    cornerRadius: Dp = CornerRadii.MEDIUM
): Modifier {
    return if (hovered) {
        this.shadow(
            elevation = hoverElevation,
            shape = RoundedCornerShape(cornerRadius)
        )
    } else {
        this
    }
}

/**
 * Add focus ring effect
 */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    ringWidth: Dp = BorderWidths.NORMAL,
    cornerRadius: Dp = CornerRadii.MEDIUM
): Modifier {
    return if (focused) {
        this.border(
            width = ringWidth,
            color = ringColor,
            shape = RoundedCornerShape(cornerRadius)
        )
    } else {
        this
    }
}

/**
 * Add shimmer loading effect
 */
@Composable
fun Modifier.shimmerLoading(
    isLoading: Boolean
): Modifier {
    return if (isLoading) {
        this.shimmerEffect()
    } else {
        this
    }
}

/**
 * Add circular clipping
 */
fun Modifier.circularClip(): Modifier {
    return this.clip(CircleShape)
}

/**
 * Add rounded clipping
 */
fun Modifier.roundedClip(radius: Dp = CornerRadii.MEDIUM): Modifier {
    return this.clip(RoundedCornerShape(radius))
}

/**
 * Add top rounded clipping
 */
fun Modifier.topRoundedClip(radius: Dp = CornerRadii.LARGE): Modifier {
    return this.clip(ShapeUtils.topRoundedCorner(radius))
}

/**
 * Add bottom rounded clipping
 */
fun Modifier.bottomRoundedClip(radius: Dp = CornerRadii.LARGE): Modifier {
    return this.clip(ShapeUtils.bottomRoundedCorner(radius))
}

/**
 * Color utilities
 */
object ColorUtils {
    /**
     * Lighten a color
     */
    fun Color.lighten(factor: Float = 0.1f): Color {
        return copy(
            red = (red + (1f - red) * factor).coerceIn(0f, 1f),
            green = (green + (1f - green) * factor).coerceIn(0f, 1f),
            blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f)
        )
    }

    /**
     * Darken a color
     */
    fun Color.darken(factor: Float = 0.1f): Color {
        return copy(
            red = (red * (1f - factor)).coerceIn(0f, 1f),
            green = (green * (1f - factor)).coerceIn(0f, 1f),
            blue = (blue * (1f - factor)).coerceIn(0f, 1f)
        )
    }

    /**
     * Adjust alpha
     */
    fun Color.withAlpha(alpha: Float): Color {
        return copy(alpha = alpha.coerceIn(0f, 1f))
    }

    /**
     * Get contrast color (black or white)
     */
    fun Color.contrastColor(): Color {
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
        return if (luminance > 0.5) Color.Black else Color.White
    }
}

/**
 * Loading state overlay
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Loading indicator would go here
            }
        }
    }
}

/**
 * Success/Error state colors
 */
object StateColors {
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFF44336)
    val Warning = Color(0xFFFF9800)
    val Info = Color(0xFF2196F3)
}
