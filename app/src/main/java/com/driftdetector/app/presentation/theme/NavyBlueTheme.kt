package com.driftdetector.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Navy Blue Custom Theme
 * Dark Mode: Professional Navy Blue
 * Light Mode: Clean Sky Blue & White
 */

object NavyBlueProfessionalTheme {
    // LIGHT MODE - Sky Blue & White (Clean & Professional)
    object Light {
        val Primary = Color(0xFF1976D2)           // Professional Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF42A5F5)         // Sky Blue
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF64B5F6)          // Light Blue
        val Background = Color(0xFFF5F9FC)        // Light Blue Tint
        val Surface = Color(0xFFFFFFFF)           // Pure White
        val Error = Color(0xFFD32F2F)
    }

    // DARK MODE - Navy Blue (Deep & Sophisticated)
    object Dark {
        val Primary = Color(0xFF1A237E)           // Deep Navy Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFD700)         // Gold Accent
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF64FFDA)          // Cyan Accent
        val Background = Color(0xFF0A0E27)        // Midnight Navy
        val Surface = Color(0xFF1C2541)           // Navy Surface
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Alternative 1: Navy Blue with Teal Accents
 * More modern and tech-focused
 */
object NavyTealTheme {
    // LIGHT MODE - Light Blue & Teal
    object Light {
        val Primary = Color(0xFF0288D1)           // Bright Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF26C6DA)         // Teal
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF00BCD4)          // Cyan
        val Background = Color(0xFFE0F7FA)        // Light Cyan Tint
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE53935)
    }

    // DARK MODE - Navy Blue & Teal
    object Dark {
        val Primary = Color(0xFF0D47A1)           // Royal Navy
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF00E5FF)         // Bright Teal
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF18FFFF)          // Neon Cyan
        val Background = Color(0xFF001F3F)        // Deep Navy
        val Surface = Color(0xFF003366)           // Navy Blue Surface
        val Error = Color(0xFFFF5252)
    }
}

/**
 * Alternative 2: Navy Blue with Orange Accents
 * High contrast, attention-grabbing
 */
object NavyOrangeTheme {
    // LIGHT MODE - Blue & Orange
    object Light {
        val Primary = Color(0xFF2196F3)           // Material Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFF9800)         // Orange
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFFB74D)          // Light Orange
        val Background = Color(0xFFF5F5F5)        // Light Gray
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFF44336)
    }

    // DARK MODE - Navy Blue & Orange Glow
    object Dark {
        val Primary = Color(0xFF283593)           // Indigo Navy
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFF6E40)         // Bright Orange
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFFA726)          // Amber
        val Background = Color(0xFF0A0E1A)        // Dark Navy
        val Surface = Color(0xFF1A1F3A)           // Navy Surface
        val Error = Color(0xFFFF5252)
    }
}

/**
 * Alternative 3: Navy Blue with Purple Accents
 * Elegant and premium
 */
object NavyPurpleTheme {
    // LIGHT MODE - Blue & Lavender
    object Light {
        val Primary = Color(0xFF1E88E5)           // Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF9C27B0)         // Purple
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFFBA68C8)          // Light Purple
        val Background = Color(0xFFF3E5F5)        // Light Purple Tint
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFD32F2F)
    }

    // DARK MODE - Navy Blue & Purple Glow
    object Dark {
        val Primary = Color(0xFF1A237E)           // Deep Navy
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF9575CD)         // Lavender
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF7E57C2)          // Deep Purple
        val Background = Color(0xFF0D0D1F)        // Navy Purple
        val Surface = Color(0xFF1A1A2E)           // Dark Surface
        val Error = Color(0xFFEF5350)
    }
}

// ==================== Build Color Schemes ====================

// Navy Blue Professional (Recommended)
val NavyBlueProfessionalLightScheme = createLightColorScheme(
    primary = NavyBlueProfessionalTheme.Light.Primary,
    onPrimary = NavyBlueProfessionalTheme.Light.OnPrimary,
    secondary = NavyBlueProfessionalTheme.Light.Secondary,
    onSecondary = NavyBlueProfessionalTheme.Light.OnSecondary,
    tertiary = NavyBlueProfessionalTheme.Light.Tertiary,
    background = NavyBlueProfessionalTheme.Light.Background,
    surface = NavyBlueProfessionalTheme.Light.Surface,
    error = NavyBlueProfessionalTheme.Light.Error
)

val NavyBlueProfessionalDarkScheme = createDarkColorScheme(
    primary = NavyBlueProfessionalTheme.Dark.Primary,
    onPrimary = NavyBlueProfessionalTheme.Dark.OnPrimary,
    secondary = NavyBlueProfessionalTheme.Dark.Secondary,
    onSecondary = NavyBlueProfessionalTheme.Dark.OnSecondary,
    tertiary = NavyBlueProfessionalTheme.Dark.Tertiary,
    background = NavyBlueProfessionalTheme.Dark.Background,
    surface = NavyBlueProfessionalTheme.Dark.Surface,
    error = NavyBlueProfessionalTheme.Dark.Error
)

// Navy Teal
val NavyTealLightScheme = createLightColorScheme(
    primary = NavyTealTheme.Light.Primary,
    onPrimary = NavyTealTheme.Light.OnPrimary,
    secondary = NavyTealTheme.Light.Secondary,
    onSecondary = NavyTealTheme.Light.OnSecondary,
    tertiary = NavyTealTheme.Light.Tertiary,
    background = NavyTealTheme.Light.Background,
    surface = NavyTealTheme.Light.Surface,
    error = NavyTealTheme.Light.Error
)

val NavyTealDarkScheme = createDarkColorScheme(
    primary = NavyTealTheme.Dark.Primary,
    onPrimary = NavyTealTheme.Dark.OnPrimary,
    secondary = NavyTealTheme.Dark.Secondary,
    onSecondary = NavyTealTheme.Dark.OnSecondary,
    tertiary = NavyTealTheme.Dark.Tertiary,
    background = NavyTealTheme.Dark.Background,
    surface = NavyTealTheme.Dark.Surface,
    error = NavyTealTheme.Dark.Error
)

// Navy Orange
val NavyOrangeLightScheme = createLightColorScheme(
    primary = NavyOrangeTheme.Light.Primary,
    onPrimary = NavyOrangeTheme.Light.OnPrimary,
    secondary = NavyOrangeTheme.Light.Secondary,
    onSecondary = NavyOrangeTheme.Light.OnSecondary,
    tertiary = NavyOrangeTheme.Light.Tertiary,
    background = NavyOrangeTheme.Light.Background,
    surface = NavyOrangeTheme.Light.Surface,
    error = NavyOrangeTheme.Light.Error
)

val NavyOrangeDarkScheme = createDarkColorScheme(
    primary = NavyOrangeTheme.Dark.Primary,
    onPrimary = NavyOrangeTheme.Dark.OnPrimary,
    secondary = NavyOrangeTheme.Dark.Secondary,
    onSecondary = NavyOrangeTheme.Dark.OnSecondary,
    tertiary = NavyOrangeTheme.Dark.Tertiary,
    background = NavyOrangeTheme.Dark.Background,
    surface = NavyOrangeTheme.Dark.Surface,
    error = NavyOrangeTheme.Dark.Error
)

// Navy Purple
val NavyPurpleLightScheme = createLightColorScheme(
    primary = NavyPurpleTheme.Light.Primary,
    onPrimary = NavyPurpleTheme.Light.OnPrimary,
    secondary = NavyPurpleTheme.Light.Secondary,
    onSecondary = NavyPurpleTheme.Light.OnSecondary,
    tertiary = NavyPurpleTheme.Light.Tertiary,
    background = NavyPurpleTheme.Light.Background,
    surface = NavyPurpleTheme.Light.Surface,
    error = NavyPurpleTheme.Light.Error
)

val NavyPurpleDarkScheme = createDarkColorScheme(
    primary = NavyPurpleTheme.Dark.Primary,
    onPrimary = NavyPurpleTheme.Dark.OnPrimary,
    secondary = NavyPurpleTheme.Dark.Secondary,
    onSecondary = NavyPurpleTheme.Dark.OnSecondary,
    tertiary = NavyPurpleTheme.Dark.Tertiary,
    background = NavyPurpleTheme.Dark.Background,
    surface = NavyPurpleTheme.Dark.Surface,
    error = NavyPurpleTheme.Dark.Error
)

// ==================== Quick Access Function ====================

/**
 * Get Navy Blue theme colors
 * Choose between 4 variations
 */
enum class NavyThemeVariant {
    PROFESSIONAL,  // Navy + Gold/Cyan (Recommended)
    TEAL,         // Navy + Bright Teal
    ORANGE,       // Navy + Orange Glow
    PURPLE        // Navy + Lavender
}

fun getNavyThemeColors(variant: NavyThemeVariant, isDark: Boolean): ColorScheme {
    return when (variant) {
        NavyThemeVariant.PROFESSIONAL -> if (isDark) NavyBlueProfessionalDarkScheme else NavyBlueProfessionalLightScheme
        NavyThemeVariant.TEAL -> if (isDark) NavyTealDarkScheme else NavyTealLightScheme
        NavyThemeVariant.ORANGE -> if (isDark) NavyOrangeDarkScheme else NavyOrangeLightScheme
        NavyThemeVariant.PURPLE -> if (isDark) NavyPurpleDarkScheme else NavyPurpleLightScheme
    }
}
