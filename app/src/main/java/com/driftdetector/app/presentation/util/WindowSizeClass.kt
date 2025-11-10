package com.driftdetector.app.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class for responsive design
 * Supports phones, tablets, foldables, and different orientations
 */
enum class WindowSize {
    COMPACT,    // Phone portrait
    MEDIUM,     // Phone landscape, small tablet portrait
    EXPANDED    // Large tablet, foldable unfolded
}

/**
 * Screen orientation
 */
enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Window size info for responsive layouts
 */
data class WindowSizeInfo(
    val widthSize: WindowSize,
    val heightSize: WindowSize,
    val orientation: ScreenOrientation,
    val screenWidth: Dp,
    val screenHeight: Dp
) {
    val isCompact: Boolean get() = widthSize == WindowSize.COMPACT
    val isMedium: Boolean get() = widthSize == WindowSize.MEDIUM
    val isExpanded: Boolean get() = widthSize == WindowSize.EXPANDED
    val isPortrait: Boolean get() = orientation == ScreenOrientation.PORTRAIT
    val isLandscape: Boolean get() = orientation == ScreenOrientation.LANDSCAPE

    // Convenience properties
    val isPhone: Boolean get() = widthSize == WindowSize.COMPACT
    val isTablet: Boolean get() = widthSize == WindowSize.MEDIUM || widthSize == WindowSize.EXPANDED
    val isLargeTablet: Boolean get() = widthSize == WindowSize.EXPANDED
}

/**
 * Remember window size class
 */
@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current

    return remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.orientation
    ) {
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp

        val widthSize = when {
            screenWidth < 600.dp -> WindowSize.COMPACT
            screenWidth < 840.dp -> WindowSize.MEDIUM
            else -> WindowSize.EXPANDED
        }

        val heightSize = when {
            screenHeight < 480.dp -> WindowSize.COMPACT
            screenHeight < 900.dp -> WindowSize.MEDIUM
            else -> WindowSize.EXPANDED
        }

        val orientation = if (screenWidth > screenHeight) {
            ScreenOrientation.LANDSCAPE
        } else {
            ScreenOrientation.PORTRAIT
        }

        WindowSizeInfo(
            widthSize = widthSize,
            heightSize = heightSize,
            orientation = orientation,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
    }
}

/**
 * Responsive padding values based on screen size
 */
object ResponsivePadding {
    @Composable
    fun small(): Dp {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.widthSize) {
            WindowSize.COMPACT -> 8.dp
            WindowSize.MEDIUM -> 12.dp
            WindowSize.EXPANDED -> 16.dp
        }
    }

    @Composable
    fun medium(): Dp {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.widthSize) {
            WindowSize.COMPACT -> 16.dp
            WindowSize.MEDIUM -> 20.dp
            WindowSize.EXPANDED -> 24.dp
        }
    }

    @Composable
    fun large(): Dp {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.widthSize) {
            WindowSize.COMPACT -> 24.dp
            WindowSize.MEDIUM -> 32.dp
            WindowSize.EXPANDED -> 40.dp
        }
    }
}

/**
 * Responsive column count for grids
 */
object ResponsiveGrid {
    @Composable
    fun columnCount(): Int {
        val windowSize = rememberWindowSizeInfo()
        return when {
            windowSize.isExpanded && windowSize.isLandscape -> 4
            windowSize.isExpanded -> 3
            windowSize.isMedium && windowSize.isLandscape -> 3
            windowSize.isMedium -> 2
            windowSize.isLandscape -> 2
            else -> 1
        }
    }

    @Composable
    fun cardColumns(): Int {
        val windowSize = rememberWindowSizeInfo()
        return when {
            windowSize.isExpanded -> 3
            windowSize.isMedium -> 2
            else -> 1
        }
    }
}

/**
 * Responsive font scaling
 */
object ResponsiveText {
    @Composable
    fun scaleFactor(): Float {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.widthSize) {
            WindowSize.COMPACT -> 1.0f
            WindowSize.MEDIUM -> 1.1f
            WindowSize.EXPANDED -> 1.2f
        }
    }
}

/**
 * Responsive spacing
 */
object ResponsiveSpacing {
    @Composable
    fun horizontal(): Dp {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.widthSize) {
            WindowSize.COMPACT -> 16.dp
            WindowSize.MEDIUM -> 24.dp
            WindowSize.EXPANDED -> 32.dp
        }
    }

    @Composable
    fun vertical(): Dp {
        val windowSize = rememberWindowSizeInfo()
        return when (windowSize.heightSize) {
            WindowSize.COMPACT -> 8.dp
            WindowSize.MEDIUM -> 12.dp
            WindowSize.EXPANDED -> 16.dp
        }
    }
}

/**
 * Calculate responsive max width for content
 */
@Composable
fun rememberMaxContentWidth(): Dp {
    val windowSize = rememberWindowSizeInfo()
    return when (windowSize.widthSize) {
        WindowSize.COMPACT -> windowSize.screenWidth
        WindowSize.MEDIUM -> 600.dp
        WindowSize.EXPANDED -> 840.dp
    }
}

/**
 * Check if should use two-pane layout
 */
@Composable
fun shouldUseTwoPane(): Boolean {
    val windowSize = rememberWindowSizeInfo()
    return windowSize.widthSize >= WindowSize.MEDIUM
}

/**
 * Get appropriate icon size
 */
@Composable
fun rememberIconSize(): Dp {
    val windowSize = rememberWindowSizeInfo()
    return when (windowSize.widthSize) {
        WindowSize.COMPACT -> 24.dp
        WindowSize.MEDIUM -> 28.dp
        WindowSize.EXPANDED -> 32.dp
    }
}
