package com.driftdetector.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom Color Theme System
 * Easily switch between different color combinations
 */

// ==================== Color Palette Collections ====================

/**
 * Professional Blue & Orange (Default)
 * Modern, trustworthy, energetic
 */
object BlueOrangeTheme {
    // Light Mode
    val LightPrimary = Color(0xFF2196F3)           // Vibrant Blue
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFFFF9800)         // Energetic Orange
    val LightOnSecondary = Color(0xFF000000)
    val LightTertiary = Color(0xFF00BCD4)          // Cyan accent
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFF64B5F6)            // Lighter Blue
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFFFFB74D)          // Lighter Orange
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFF4DD0E1)           // Lighter Cyan
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

/**
 * Purple & Teal (Creative & Modern)
 * Innovative, creative, tech-focused
 */
object PurpleTealTheme {
    // Light Mode
    val LightPrimary = Color(0xFF9C27B0)           // Deep Purple
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF00BCD4)         // Teal
    val LightOnSecondary = Color(0xFF000000)
    val LightTertiary = Color(0xFFE91E63)          // Pink accent
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFFBA68C8)            // Lighter Purple
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFF4DD0E1)          // Lighter Teal
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFFF06292)           // Lighter Pink
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

/**
 * Green & Indigo (Nature & Technology)
 * Fresh, reliable, balanced
 */
object GreenIndigoTheme {
    // Light Mode
    val LightPrimary = Color(0xFF4CAF50)           // Green
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF3F51B5)         // Indigo
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightTertiary = Color(0xFF8BC34A)          // Light Green
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFF81C784)            // Lighter Green
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFF7986CB)          // Lighter Indigo
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFFAED581)           // Lighter Light Green
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

/**
 * Red & Blue (Bold & Professional)
 * Energetic, authoritative, striking
 */
object RedBlueTheme {
    // Light Mode
    val LightPrimary = Color(0xFFE53935)           // Red
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF1E88E5)         // Blue
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightTertiary = Color(0xFFFF5252)          // Light Red
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFD32F2F)

    // Dark Mode
    val DarkPrimary = Color(0xFFEF5350)            // Lighter Red
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFF42A5F5)          // Lighter Blue
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFFFF8A80)           // Lighter Light Red
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFFF6659)
}

/**
 * Ocean Blue (Calm & Professional)
 * Serene, trustworthy, sophisticated
 */
object OceanBlueTheme {
    // Light Mode
    val LightPrimary = Color(0xFF0277BD)           // Deep Blue
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF26C6DA)         // Cyan
    val LightOnSecondary = Color(0xFF000000)
    val LightTertiary = Color(0xFF0097A7)          // Dark Cyan
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFF4FC3F7)            // Lighter Blue
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFF80DEEA)          // Lighter Cyan
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFF4DD0E1)           // Lighter Dark Cyan
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

/**
 * Sunset (Warm & Inviting)
 * Warm, friendly, optimistic
 */
object SunsetTheme {
    // Light Mode
    val LightPrimary = Color(0xFFFF6F00)           // Deep Orange
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFFFFA726)         // Orange
    val LightOnSecondary = Color(0xFF000000)
    val LightTertiary = Color(0xFFFFD54F)          // Amber
    val LightBackground = Color(0xFFFFF8E1)        // Light Amber tint
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFFFFB74D)            // Lighter Orange
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFFFFCC80)          // Lighter Orange
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFFFFE082)           // Lighter Amber
    val DarkBackground = Color(0xFF1A1410)         // Warm dark
    val DarkSurface = Color(0xFF2C2419)            // Warm surface
    val DarkError = Color(0xFFEF5350)
}

/**
 * Forest (Natural & Earthy)
 * Organic, stable, grounded
 */
object ForestTheme {
    // Light Mode
    val LightPrimary = Color(0xFF388E3C)           // Green
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF795548)         // Brown
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightTertiary = Color(0xFF8D6E63)          // Light Brown
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFF44336)

    // Dark Mode
    val DarkPrimary = Color(0xFF81C784)            // Lighter Green
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFFA1887F)          // Lighter Brown
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFFBCAAA4)           // Lighter Light Brown
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

/**
 * Monochrome (Minimalist & Elegant)
 * Clean, professional, timeless
 */
object MonochromeTheme {
    // Light Mode
    val LightPrimary = Color(0xFF212121)           // Dark Gray
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF757575)         // Medium Gray
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightTertiary = Color(0xFF9E9E9E)          // Light Gray
    val LightBackground = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightError = Color(0xFFD32F2F)

    // Dark Mode
    val DarkPrimary = Color(0xFFE0E0E0)            // Light Gray
    val DarkOnPrimary = Color(0xFF000000)
    val DarkSecondary = Color(0xFFBDBDBD)          // Gray
    val DarkOnSecondary = Color(0xFF000000)
    val DarkTertiary = Color(0xFF9E9E9E)           // Medium Gray
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkError = Color(0xFFEF5350)
}

// ==================== Color Scheme Builders ====================

/**
 * Create a light color scheme from theme colors
 */
fun createLightColorScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    background: Color = Color(0xFFF5F5F5),
    surface: Color = Color(0xFFFFFFFF),
    error: Color = Color(0xFFF44336)
): ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,

        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondary.copy(alpha = 0.12f),
        onSecondaryContainer = secondary,

        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = tertiary.copy(alpha = 0.12f),
        onTertiaryContainer = tertiary,

        background = background,
        onBackground = Color(0xFF1C1B1F),

        surface = surface,
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),

        error = error,
        onError = Color.White,
        errorContainer = error.copy(alpha = 0.12f),
        onErrorContainer = error,

        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0)
    )
}

/**
 * Create a dark color scheme from theme colors
 */
fun createDarkColorScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    background: Color = Color(0xFF121212),
    surface: Color = Color(0xFF1E1E1E),
    error: Color = Color(0xFFEF5350)
): ColorScheme {
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = 0.3f),
        onPrimaryContainer = primary,

        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondary.copy(alpha = 0.3f),
        onSecondaryContainer = secondary,

        tertiary = tertiary,
        onTertiary = Color.Black,
        tertiaryContainer = tertiary.copy(alpha = 0.3f),
        onTertiaryContainer = tertiary,

        background = background,
        onBackground = Color(0xFFE6E1E5),

        surface = surface,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),

        error = error,
        onError = Color.Black,
        errorContainer = error.copy(alpha = 0.3f),
        onErrorContainer = error,

        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F)
    )
}

// ==================== Pre-built Color Schemes ====================

// Blue & Orange
val BlueOrangeLightScheme = createLightColorScheme(
    primary = BlueOrangeTheme.LightPrimary,
    onPrimary = BlueOrangeTheme.LightOnPrimary,
    secondary = BlueOrangeTheme.LightSecondary,
    onSecondary = BlueOrangeTheme.LightOnSecondary,
    tertiary = BlueOrangeTheme.LightTertiary,
    background = BlueOrangeTheme.LightBackground,
    surface = BlueOrangeTheme.LightSurface,
    error = BlueOrangeTheme.LightError
)

val BlueOrangeDarkScheme = createDarkColorScheme(
    primary = BlueOrangeTheme.DarkPrimary,
    onPrimary = BlueOrangeTheme.DarkOnPrimary,
    secondary = BlueOrangeTheme.DarkSecondary,
    onSecondary = BlueOrangeTheme.DarkOnSecondary,
    tertiary = BlueOrangeTheme.DarkTertiary,
    background = BlueOrangeTheme.DarkBackground,
    surface = BlueOrangeTheme.DarkSurface,
    error = BlueOrangeTheme.DarkError
)

// Purple & Teal
val PurpleTealLightScheme = createLightColorScheme(
    primary = PurpleTealTheme.LightPrimary,
    onPrimary = PurpleTealTheme.LightOnPrimary,
    secondary = PurpleTealTheme.LightSecondary,
    onSecondary = PurpleTealTheme.LightOnSecondary,
    tertiary = PurpleTealTheme.LightTertiary
)

val PurpleTealDarkScheme = createDarkColorScheme(
    primary = PurpleTealTheme.DarkPrimary,
    onPrimary = PurpleTealTheme.DarkOnPrimary,
    secondary = PurpleTealTheme.DarkSecondary,
    onSecondary = PurpleTealTheme.DarkOnSecondary,
    tertiary = PurpleTealTheme.DarkTertiary
)

// Green & Indigo
val GreenIndigoLightScheme = createLightColorScheme(
    primary = GreenIndigoTheme.LightPrimary,
    onPrimary = GreenIndigoTheme.LightOnPrimary,
    secondary = GreenIndigoTheme.LightSecondary,
    onSecondary = GreenIndigoTheme.LightOnSecondary,
    tertiary = GreenIndigoTheme.LightTertiary
)

val GreenIndigoDarkScheme = createDarkColorScheme(
    primary = GreenIndigoTheme.DarkPrimary,
    onPrimary = GreenIndigoTheme.DarkOnPrimary,
    secondary = GreenIndigoTheme.DarkSecondary,
    onSecondary = GreenIndigoTheme.DarkOnSecondary,
    tertiary = GreenIndigoTheme.DarkTertiary
)

// Red & Blue
val RedBlueLightScheme = createLightColorScheme(
    primary = RedBlueTheme.LightPrimary,
    onPrimary = RedBlueTheme.LightOnPrimary,
    secondary = RedBlueTheme.LightSecondary,
    onSecondary = RedBlueTheme.LightOnSecondary,
    tertiary = RedBlueTheme.LightTertiary
)

val RedBlueDarkScheme = createDarkColorScheme(
    primary = RedBlueTheme.DarkPrimary,
    onPrimary = RedBlueTheme.DarkOnPrimary,
    secondary = RedBlueTheme.DarkSecondary,
    onSecondary = RedBlueTheme.DarkOnSecondary,
    tertiary = RedBlueTheme.DarkTertiary
)

// Ocean Blue
val OceanBlueLightScheme = createLightColorScheme(
    primary = OceanBlueTheme.LightPrimary,
    onPrimary = OceanBlueTheme.LightOnPrimary,
    secondary = OceanBlueTheme.LightSecondary,
    onSecondary = OceanBlueTheme.LightOnSecondary,
    tertiary = OceanBlueTheme.LightTertiary
)

val OceanBlueDarkScheme = createDarkColorScheme(
    primary = OceanBlueTheme.DarkPrimary,
    onPrimary = OceanBlueTheme.DarkOnPrimary,
    secondary = OceanBlueTheme.DarkSecondary,
    onSecondary = OceanBlueTheme.DarkOnSecondary,
    tertiary = OceanBlueTheme.DarkTertiary
)

// Sunset
val SunsetLightScheme = createLightColorScheme(
    primary = SunsetTheme.LightPrimary,
    onPrimary = SunsetTheme.LightOnPrimary,
    secondary = SunsetTheme.LightSecondary,
    onSecondary = SunsetTheme.LightOnSecondary,
    tertiary = SunsetTheme.LightTertiary,
    background = SunsetTheme.LightBackground
)

val SunsetDarkScheme = createDarkColorScheme(
    primary = SunsetTheme.DarkPrimary,
    onPrimary = SunsetTheme.DarkOnPrimary,
    secondary = SunsetTheme.DarkSecondary,
    onSecondary = SunsetTheme.DarkOnSecondary,
    tertiary = SunsetTheme.DarkTertiary,
    background = SunsetTheme.DarkBackground,
    surface = SunsetTheme.DarkSurface
)

// Forest
val ForestLightScheme = createLightColorScheme(
    primary = ForestTheme.LightPrimary,
    onPrimary = ForestTheme.LightOnPrimary,
    secondary = ForestTheme.LightSecondary,
    onSecondary = ForestTheme.LightOnSecondary,
    tertiary = ForestTheme.LightTertiary
)

val ForestDarkScheme = createDarkColorScheme(
    primary = ForestTheme.DarkPrimary,
    onPrimary = ForestTheme.DarkOnPrimary,
    secondary = ForestTheme.DarkSecondary,
    onSecondary = ForestTheme.DarkOnSecondary,
    tertiary = ForestTheme.DarkTertiary
)

// Monochrome
val MonochromeLightScheme = createLightColorScheme(
    primary = MonochromeTheme.LightPrimary,
    onPrimary = MonochromeTheme.LightOnPrimary,
    secondary = MonochromeTheme.LightSecondary,
    onSecondary = MonochromeTheme.LightOnSecondary,
    tertiary = MonochromeTheme.LightTertiary,
    background = MonochromeTheme.LightBackground
)

val MonochromeDarkScheme = createDarkColorScheme(
    primary = MonochromeTheme.DarkPrimary,
    onPrimary = MonochromeTheme.DarkOnPrimary,
    secondary = MonochromeTheme.DarkSecondary,
    onSecondary = MonochromeTheme.DarkOnSecondary,
    tertiary = MonochromeTheme.DarkTertiary
)

// ==================== Theme Selection Enum ====================

enum class ThemeVariant {
    BLUE_ORANGE,     // Default - Professional
    PURPLE_TEAL,     // Creative & Modern
    GREEN_INDIGO,    // Natural & Tech
    RED_BLUE,        // Bold & Professional
    OCEAN_BLUE,      // Calm & Professional
    SUNSET,          // Warm & Inviting
    FOREST,          // Natural & Earthy
    MONOCHROME       // Minimalist & Elegant
}

/**
 * Get light and dark color schemes for a theme variant
 */
fun getThemeColors(variant: ThemeVariant, isDark: Boolean): ColorScheme {
    return when (variant) {
        ThemeVariant.BLUE_ORANGE -> if (isDark) BlueOrangeDarkScheme else BlueOrangeLightScheme
        ThemeVariant.PURPLE_TEAL -> if (isDark) PurpleTealDarkScheme else PurpleTealLightScheme
        ThemeVariant.GREEN_INDIGO -> if (isDark) GreenIndigoDarkScheme else GreenIndigoLightScheme
        ThemeVariant.RED_BLUE -> if (isDark) RedBlueDarkScheme else RedBlueLightScheme
        ThemeVariant.OCEAN_BLUE -> if (isDark) OceanBlueDarkScheme else OceanBlueLightScheme
        ThemeVariant.SUNSET -> if (isDark) SunsetDarkScheme else SunsetLightScheme
        ThemeVariant.FOREST -> if (isDark) ForestDarkScheme else ForestLightScheme
        ThemeVariant.MONOCHROME -> if (isDark) MonochromeDarkScheme else MonochromeLightScheme
    }
}
