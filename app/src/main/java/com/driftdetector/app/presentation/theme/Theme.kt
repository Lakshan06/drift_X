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
 * Navy Blue Professional Theme
 * Light Mode: Sky Blue & White (Clean & Professional)
 * Dark Mode: Deep Navy Blue with Gold & Blue accents (Sophisticated)
 */

/**
 * Light Color Scheme - Sky Blue & White
 * Professional, clean, and trustworthy
 */
private val LightColorScheme = lightColorScheme(
    // Primary - Professional Blue
    primary = Color(0xFF1976D2),              // Professional Blue
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFFBBDEFB),     // Light Blue Container
    onPrimaryContainer = Color(0xFF01579B),   // Dark Blue

    // Secondary - Sky Blue
    secondary = Color(0xFF42A5F5),            // Sky Blue
    onSecondary = Color(0xFF000000),          // Black
    secondaryContainer = Color(0xFFE3F2FD),   // Very Light Blue
    onSecondaryContainer = Color(0xFF0D47A1), // Deep Blue

    // Tertiary - Light Blue Accent
    tertiary = Color(0xFF64B5F6),             // Light Blue
    onTertiary = Color(0xFF000000),           // Black
    tertiaryContainer = Color(0xFFE1F5FE),    // Cyan Tint
    onTertiaryContainer = Color(0xFF01579B),  // Dark Blue

    // Error
    error = Color(0xFFD32F2F),                // Red
    onError = Color(0xFFFFFFFF),              // White
    errorContainer = Color(0xFFFFCDD2),       // Light Red
    onErrorContainer = Color(0xFFB71C1C),     // Dark Red

    // Background - Light Blue Tint
    background = Color(0xFFF5F9FC),           // Light Blue Tint
    onBackground = Color(0xFF1C1B1F),         // Dark Text

    // Surface - Pure White
    surface = Color(0xFFFFFFFF),              // Pure White
    onSurface = Color(0xFF1C1B1F),            // Dark Text
    surfaceVariant = Color(0xFFE7F3FF),       // Very Light Blue
    onSurfaceVariant = Color(0xFF424242),     // Medium Gray
    surfaceTint = Color(0xFF1976D2),          // Professional Blue

    // Outline
    outline = Color(0xFF757575),              // Medium Gray
    outlineVariant = Color(0xFFBDBDBD),       // Light Gray

    // Inverse colors
    inverseSurface = Color(0xFF1C1B1F),       // Dark
    inverseOnSurface = Color(0xFFE6E1E5),     // Light
    inversePrimary = Color(0xFF42A5F5),       // Sky Blue

    // Scrim
    scrim = Color(0x99000000)                 // Semi-transparent Black
)

/**
 * Dark Color Scheme - Deep Navy Blue with Gold & Blue
 * Sophisticated, premium, and tech-forward
 * UPDATED: Improved contrast for better visibility, replaced teal with blue (#2B35AF)
 */
private val DarkColorScheme = darkColorScheme(
    // Primary - Lighter Navy Blue for better visibility
    primary = Color(0xFF5C6BC0),              // Lighter Indigo-Navy (was 0xFF1A237E)
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFF3949AB),     // Medium Navy (was 0xFF283593)
    onPrimaryContainer = Color(0xFFE8EAF6),   // Very Light Blue (was 0xFFC5CAE9)

    // Secondary - Gold Accent
    secondary = Color(0xFFFFD700),            // Gold Accent 
    onSecondary = Color(0xFF000000),          // Black
    secondaryContainer = Color(0xFF4A5F8C),   // Lighter Navy Container for better text visibility
    onSecondaryContainer = Color(0xFFFFFFFF), // White text on containers

    // Tertiary - Blue for text boxes (#2B35AF instead of cyan)
    tertiary = Color(0xFF2B35AF),             // Blue for text boxes (was 0xFF64FFDA)
    onTertiary = Color(0xFFFFFFFF),           // White (was 0xFF000000)
    tertiaryContainer = Color(0xFF1E2875),    // Darker blue container (was 0xFF00BFA5)
    onTertiaryContainer = Color(0xFFE0E6FF),  // Light blue tint (was 0xFFE0F7FA)

    // Error
    error = Color(0xFFEF5350),                // Bright Red
    onError = Color(0xFF000000),              // Black
    errorContainer = Color(0xFFB71C1C),       // Dark Red
    onErrorContainer = Color(0xFFFFCDD2),     // Light Red

    // Background - Lighter Midnight Navy for better visibility
    background = Color(0xFF121828),           // Lighter Midnight Navy (was 0xFF0A0E27)
    onBackground = Color(0xFFF5F5F5),         // Brighter Light Text (was 0xFFE6E1E5)

    // Surface - Lighter Navy Surface for better contrast
    surface = Color(0xFF1E2A3A),              // Lighter Navy Surface (was 0xFF1C2541)
    onSurface = Color(0xFFF5F5F5),            // Brighter Light Text (was 0xFFE6E1E5)
    surfaceVariant = Color(0xFF344A5F),       // Lighter Navy Surface Variant (was 0xFF283B5B)
    onSurfaceVariant = Color(0xFFE0E6ED),     // Brighter Light Blue Text (was 0xFFC5CAE9)
    surfaceTint = Color(0xFF5C6BC0),          // Lighter Navy Blue (was 0xFF1A237E)

    // Outline - Better visibility for borders and dividers
    outline = Color(0xFF9FA8BE),              // Lighter Medium Blue (was 0xFF7986CB)
    outlineVariant = Color(0xFF5C6BC0),       // Lighter Indigo (was 0xFF3F51B5)

    // Inverse colors
    inverseSurface = Color(0xFFE6E1E5),       // Light
    inverseOnSurface = Color(0xFF1C1B1F),     // Dark
    inversePrimary = Color(0xFF1976D2),       // Professional Blue

    // Scrim
    scrim = Color(0xCC000000)                 // Semi-transparent Black
)

/**
 * Main theme composable with Navy Blue Professional theme
 *
 * Features:
 * - Light Mode: Sky Blue & White (Professional, clean)
 * - Dark Mode: Deep Navy Blue with Gold & Blue accents (Sophisticated, premium)
 *
 * @param darkTheme Whether to use dark theme (default: system preference)
 * @param dynamicColor Whether to use dynamic color (Android 12+). Set to false to use custom Navy theme.
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

            // Navy Blue Professional theme status bar
            window.statusBarColor = if (darkTheme) {
                Color(0xFF1E2A3A).toArgb()  // Lighter Navy Surface (was 0xFF0A0E27)
            } else {
                Color(0xFF1976D2).toArgb()  // Professional Blue
            }

            // Navigation bar color
            window.navigationBarColor = if (darkTheme) {
                Color(0xFF1E2A3A).toArgb()  // Lighter Navy Surface (was 0xFF1C2541)
            } else {
                Color.White.toArgb()        // White
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
