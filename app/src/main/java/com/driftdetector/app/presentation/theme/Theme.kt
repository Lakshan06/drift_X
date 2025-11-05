package com.driftdetector.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark Color Scheme - Sci-Fi / Fantasy Tech Theme
 * Primary theme for DriftGuardAI
 */
private val DarkColorScheme = darkColorScheme(
    // Primary - Cyber Indigo for main actions
    primary = CyberIndigo,
    onPrimary = StardustWhite,
    primaryContainer = MysticViolet,
    onPrimaryContainer = StardustWhite,

    // Secondary - Mystic Violet for navigation
    secondary = MysticViolet,
    onSecondary = StardustWhite,
    secondaryContainer = Color(0xFF4A3F7E),
    onSecondaryContainer = StardustWhite,

    // Tertiary - Emerald for success states
    tertiary = EmeraldFade,
    onTertiary = GalaxyCharcoal,
    tertiaryContainer = Color(0xFF2A8A61),
    onTertiaryContainer = StardustWhite,

    // Error - Hyper Crimson for critical states
    error = HyperCrimson,
    onError = StardustWhite,
    errorContainer = Color(0xFF8A1942),
    onErrorContainer = StardustWhite,

    // Background - Galaxy Charcoal
    background = GalaxyCharcoal,
    onBackground = StardustWhite,

    // Surface - Deep Space Navy for cards
    surface = DeepSpaceNavy,
    onSurface = StardustWhite,
    surfaceVariant = CosmicGraphite,
    onSurfaceVariant = LapisSilver,
    surfaceTint = CyberIndigo,

    // Outline - Eclipse Slate
    outline = EclipseSlate,
    outlineVariant = Color(0xFF3D4452),

    // Inverse colors
    inverseSurface = StardustWhite,
    inverseOnSurface = GalaxyCharcoal,
    inversePrimary = CyberIndigo,

    // Scrim for modal overlays
    scrim = Color(0xCC000000)
)

/**
 * Light Color Scheme
 * Optional light mode (most users will prefer dark for dashboards)
 */
private val LightColorScheme = lightColorScheme(
    // Primary - Cyber Indigo
    primary = CyberIndigo,
    onPrimary = StardustWhite,
    primaryContainer = Color(0xFFE8EDFF),
    onPrimaryContainer = GalaxyCharcoal,

    // Secondary - Mystic Violet
    secondary = MysticViolet,
    onSecondary = StardustWhite,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = GalaxyCharcoal,

    // Tertiary - Emerald
    tertiary = EmeraldFade,
    onTertiary = GalaxyCharcoal,
    tertiaryContainer = Color(0xFFD0F4E4),
    onTertiaryContainer = GalaxyCharcoal,

    // Error - Hyper Crimson
    error = HyperCrimson,
    onError = StardustWhite,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = GalaxyCharcoal,

    // Background - Light
    background = LightBackground,
    onBackground = LightOnBackground,

    // Surface - White
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = CyberIndigo,

    // Outline
    outline = EclipseSlate,
    outlineVariant = Color(0xFFC7C5D0),

    // Inverse colors
    inverseSurface = GalaxyCharcoal,
    inverseOnSurface = StardustWhite,
    inversePrimary = CyberIndigo,

    // Scrim
    scrim = Color(0x99000000)
)

/**
 * Main theme composable with theme mode support
 *
 * @param darkTheme Whether to use dark theme (default: system preference)
 * @param dynamicColor Whether to use dynamic color (Android 12+). Set to false to always use custom colors.
 * @param content The app content
 */
@Composable
fun DriftDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                GalaxyCharcoal.toArgb()
            } else {
                CyberIndigo.toArgb()
            }

            window.navigationBarColor = if (darkTheme) {
                GalaxyCharcoal.toArgb()
            } else {
                Color.White.toArgb()
            }

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
