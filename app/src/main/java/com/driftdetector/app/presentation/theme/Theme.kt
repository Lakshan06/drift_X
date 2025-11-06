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
 * Dark Color Scheme - Professional Oceanic Theme
 * Primary theme for DriftGuardAI
 */
private val DarkColorScheme = darkColorScheme(
    // Primary - Oceanic Teal for main actions
    primary = OceanicTeal,
    onPrimary = Ivory,
    primaryContainer = Color(0xFF236B76),
    onPrimaryContainer = Ivory,

    // Secondary - Coral Blush for accents
    secondary = CoralBlush,
    onSecondary = Ivory,
    secondaryContainer = Color(0xFFCC5850),
    onSecondaryContainer = Ivory,

    // Tertiary - Mint Green for success states
    tertiary = MintGreen,
    onTertiary = SlateGray,
    tertiaryContainer = Color(0xFF2E8A68),
    onTertiaryContainer = Ivory,

    // Error - Flame Red for critical states
    error = FlameRed,
    onError = Ivory,
    errorContainer = Color(0xFF9A1C2A),
    onErrorContainer = Ivory,

    // Background - Slate Gray
    background = SlateGray,
    onBackground = Ivory,

    // Surface - Gunmetal for cards
    surface = Gunmetal,
    onSurface = Ivory,
    surfaceVariant = Color(0xFF434952),
    onSurfaceVariant = MistGray,
    surfaceTint = OceanicTeal,

    // Outline - Mist Gray
    outline = MistGray,
    outlineVariant = Color(0xFF5A6169),

    // Inverse colors
    inverseSurface = Ivory,
    inverseOnSurface = SlateGray,
    inversePrimary = OceanicTeal,

    // Scrim for modal overlays
    scrim = Color(0xCC000000)
)

/**
 * Light Color Scheme
 * Optional light mode
 */
private val LightColorScheme = lightColorScheme(
    // Primary - Oceanic Teal
    primary = OceanicTeal,
    onPrimary = Ivory,
    primaryContainer = Color(0xFFB3E5EC),
    onPrimaryContainer = SlateGray,

    // Secondary - Coral Blush
    secondary = CoralBlush,
    onSecondary = Ivory,
    secondaryContainer = Color(0xFFFFDBD7),
    onSecondaryContainer = SlateGray,

    // Tertiary - Mint Green
    tertiary = MintGreen,
    onTertiary = SlateGray,
    tertiaryContainer = Color(0xFFB8F4D7),
    onTertiaryContainer = SlateGray,

    // Error - Flame Red
    error = FlameRed,
    onError = Ivory,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = SlateGray,

    // Background - Light
    background = LightBackground,
    onBackground = LightOnBackground,

    // Surface - White
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE7E9EB),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceTint = OceanicTeal,

    // Outline
    outline = MistGray,
    outlineVariant = Color(0xFFC7C5D0),

    // Inverse colors
    inverseSurface = SlateGray,
    inverseOnSurface = Ivory,
    inversePrimary = OceanicTeal,

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
                SlateGray.toArgb()
            } else {
                OceanicTeal.toArgb()
            }

            window.navigationBarColor = if (darkTheme) {
                SlateGray.toArgb()
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
