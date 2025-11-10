package com.driftdetector.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Dual Color Theme System
 * Different color combinations for Light and Dark modes
 */

// ==================== Example: Day & Night Theme ====================

/**
 * Day & Night Theme
 * Light Mode: Sky Blue & Sun Orange (bright, energetic)
 * Dark Mode: Deep Purple & Moon Silver (calm, elegant)
 */
object DayNightTheme {
    // LIGHT MODE - Day Colors (Blue Sky & Sunshine)
    object Day {
        val Primary = Color(0xFF42A5F5)           // Sky Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFB300)         // Sun Orange
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF26C6DA)          // Light Cyan
        val Background = Color(0xFFF5F8FA)        // Light Sky
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE91E63)
    }

    // DARK MODE - Night Colors (Deep Purple & Moonlight)
    object Night {
        val Primary = Color(0xFF9575CD)           // Lavender Purple
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFFB0BEC5)         // Moon Silver
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF7E57C2)          // Deep Purple
        val Background = Color(0xFF0D0D1F)        // Night Sky
        val Surface = Color(0xFF1A1A2E)           // Dark Surface
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Warm & Cool Theme
 * Light Mode: Warm colors (Coral & Amber)
 * Dark Mode: Cool colors (Teal & Blue)
 */
object WarmCoolTheme {
    // LIGHT MODE - Warm Colors
    object Warm {
        val Primary = Color(0xFFFF6B6B)           // Coral Pink
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFD93D)         // Golden Yellow
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFF9F40)          // Warm Orange
        val Background = Color(0xFFFFF5E1)        // Warm Cream
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE63946)
    }

    // DARK MODE - Cool Colors
    object Cool {
        val Primary = Color(0xFF4ECDC4)           // Teal
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF45B7D1)         // Sky Blue
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF96CEB4)          // Mint Green
        val Background = Color(0xFF0F2027)        // Deep Ocean
        val Surface = Color(0xFF1A3A52)           // Ocean Surface
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Nature Dual Theme
 * Light Mode: Spring Garden (Green & Pink)
 * Dark Mode: Forest Night (Deep Green & Earth)
 */
object NatureDualTheme {
    // LIGHT MODE - Spring Garden
    object Spring {
        val Primary = Color(0xFF66BB6A)           // Fresh Green
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFEC407A)         // Cherry Blossom Pink
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFF29B6F6)          // Spring Sky Blue
        val Background = Color(0xFFF1F8E9)        // Light Spring
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE53935)
    }

    // DARK MODE - Forest Night
    object Forest {
        val Primary = Color(0xFF2E7D32)           // Deep Forest Green
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF5D4037)         // Earth Brown
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFF558B2F)          // Moss Green
        val Background = Color(0xFF0A1F0A)        // Dark Forest
        val Surface = Color(0xFF1B2F1B)           // Forest Floor
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Vibrant Dual Theme
 * Light Mode: Electric (Neon colors for energy)
 * Dark Mode: Neon Glow (Dark with neon accents)
 */
object VibrantDualTheme {
    // LIGHT MODE - Electric
    object Electric {
        val Primary = Color(0xFFE91E63)           // Hot Pink
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFEB3B)         // Electric Yellow
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF00BCD4)          // Cyan
        val Background = Color(0xFFF5F5F5)
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFF44336)
    }

    // DARK MODE - Neon Glow
    object Neon {
        val Primary = Color(0xFFFF00FF)           // Neon Magenta
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF00FFFF)         // Neon Cyan
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF39FF14)          // Neon Green
        val Background = Color(0xFF000000)        // Pure Black
        val Surface = Color(0xFF1A1A1A)           // Dark Gray
        val Error = Color(0xFFFF1744)
    }
}

/**
 * Professional Dual Theme
 * Light Mode: Corporate Blue
 * Dark Mode: Elegant Gold & Navy
 */
object ProfessionalDualTheme {
    // LIGHT MODE - Corporate Blue
    object Corporate {
        val Primary = Color(0xFF1976D2)           // Corporate Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF757575)         // Professional Gray
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFF0288D1)          // Light Blue
        val Background = Color(0xFFFAFAFA)
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFD32F2F)
    }

    // DARK MODE - Elegant Gold & Navy
    object Elegant {
        val Primary = Color(0xFFFFD700)           // Gold
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF1A237E)         // Deep Navy
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFFFFA726)          // Amber
        val Background = Color(0xFF0A0E27)        // Midnight Navy
        val Surface = Color(0xFF1C2541)           // Navy Surface
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Ocean Sunset Dual Theme
 * Light Mode: Ocean Blue & White
 * Dark Mode: Sunset Orange & Purple
 */
object OceanSunsetTheme {
    // LIGHT MODE - Ocean
    object Ocean {
        val Primary = Color(0xFF0277BD)           // Ocean Blue
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF26C6DA)         // Wave Cyan
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFF4FC3F7)          // Sky Blue
        val Background = Color(0xFFE1F5FE)        // Light Ocean
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE53935)
    }

    // DARK MODE - Sunset
    object Sunset {
        val Primary = Color(0xFFFF6E40)           // Sunset Orange
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF9C27B0)         // Twilight Purple
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFFFF9800)          // Warm Orange
        val Background = Color(0xFF1A1A2E)        // Twilight Sky
        val Surface = Color(0xFF16213E)           // Evening Surface
        val Error = Color(0xFFEF5350)
    }
}

/**
 * Retro Dual Theme
 * Light Mode: Pastel 80s
 * Dark Mode: Neon 80s
 */
object RetroDualTheme {
    // LIGHT MODE - Pastel 80s
    object Pastel {
        val Primary = Color(0xFFFFB3BA)           // Pastel Pink
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFFBAE1FF)         // Pastel Blue
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFFDFBA)          // Pastel Peach
        val Background = Color(0xFFFFFBF0)
        val Surface = Color(0xFFFFFFFF)
        val Error = Color(0xFFE91E63)
    }

    // DARK MODE - Neon 80s
    object Neon80s {
        val Primary = Color(0xFFFF00FF)           // Neon Magenta
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF00FFFF)         // Neon Cyan
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFFFF00)          // Neon Yellow
        val Background = Color(0xFF0D0221)        // Dark Purple
        val Surface = Color(0xFF1B0A2A)           // Purple Black
        val Error = Color(0xFFFF1744)
    }
}

/**
 * Minimal Contrast Theme
 * Light Mode: Clean White & Black
 * Dark Mode: Pure Black & Colorful Accents
 */
object MinimalContrastTheme {
    // LIGHT MODE - Clean
    object Clean {
        val Primary = Color(0xFF000000)           // Pure Black
        val OnPrimary = Color(0xFFFFFFFF)
        val Secondary = Color(0xFF424242)         // Dark Gray
        val OnSecondary = Color(0xFFFFFFFF)
        val Tertiary = Color(0xFF757575)          // Medium Gray
        val Background = Color(0xFFFFFFFF)
        val Surface = Color(0xFFFAFAFA)
        val Error = Color(0xFFD32F2F)
    }

    // DARK MODE - Colorful Accents
    object Colorful {
        val Primary = Color(0xFF00E676)           // Bright Green
        val OnPrimary = Color(0xFF000000)
        val Secondary = Color(0xFF536DFE)         // Bright Blue
        val OnSecondary = Color(0xFF000000)
        val Tertiary = Color(0xFFFF4081)          // Bright Pink
        val Background = Color(0xFF000000)        // Pure Black
        val Surface = Color(0xFF121212)           // OLED Black
        val Error = Color(0xFFFF5252)
    }
}

// ==================== Dual Theme Builders ====================

/**
 * Create dual color schemes (different for light and dark)
 */
fun createDualTheme(
    lightTheme: LightThemeColors,
    darkTheme: DarkThemeColors
): Pair<ColorScheme, ColorScheme> {
    val lightScheme = createLightColorScheme(
        primary = lightTheme.Primary,
        onPrimary = lightTheme.OnPrimary,
        secondary = lightTheme.Secondary,
        onSecondary = lightTheme.OnSecondary,
        tertiary = lightTheme.Tertiary,
        background = lightTheme.Background,
        surface = lightTheme.Surface,
        error = lightTheme.Error
    )

    val darkScheme = createDarkColorScheme(
        primary = darkTheme.Primary,
        onPrimary = darkTheme.OnPrimary,
        secondary = darkTheme.Secondary,
        onSecondary = darkTheme.OnSecondary,
        tertiary = darkTheme.Tertiary,
        background = darkTheme.Background,
        surface = darkTheme.Surface,
        error = darkTheme.Error
    )

    return Pair(lightScheme, darkScheme)
}

// Helper interfaces for cleaner code
interface LightThemeColors {
    val Primary: Color
    val OnPrimary: Color
    val Secondary: Color
    val OnSecondary: Color
    val Tertiary: Color
    val Background: Color
    val Surface: Color
    val Error: Color
}

interface DarkThemeColors {
    val Primary: Color
    val OnPrimary: Color
    val Secondary: Color
    val OnSecondary: Color
    val Tertiary: Color
    val Background: Color
    val Surface: Color
    val Error: Color
}

// ==================== Pre-built Dual Themes ====================

// Day & Night
val DayNightLightScheme = createLightColorScheme(
    primary = DayNightTheme.Day.Primary,
    onPrimary = DayNightTheme.Day.OnPrimary,
    secondary = DayNightTheme.Day.Secondary,
    onSecondary = DayNightTheme.Day.OnSecondary,
    tertiary = DayNightTheme.Day.Tertiary,
    background = DayNightTheme.Day.Background,
    surface = DayNightTheme.Day.Surface,
    error = DayNightTheme.Day.Error
)

val DayNightDarkScheme = createDarkColorScheme(
    primary = DayNightTheme.Night.Primary,
    onPrimary = DayNightTheme.Night.OnPrimary,
    secondary = DayNightTheme.Night.Secondary,
    onSecondary = DayNightTheme.Night.OnSecondary,
    tertiary = DayNightTheme.Night.Tertiary,
    background = DayNightTheme.Night.Background,
    surface = DayNightTheme.Night.Surface,
    error = DayNightTheme.Night.Error
)

// Warm & Cool
val WarmCoolLightScheme = createLightColorScheme(
    primary = WarmCoolTheme.Warm.Primary,
    onPrimary = WarmCoolTheme.Warm.OnPrimary,
    secondary = WarmCoolTheme.Warm.Secondary,
    onSecondary = WarmCoolTheme.Warm.OnSecondary,
    tertiary = WarmCoolTheme.Warm.Tertiary,
    background = WarmCoolTheme.Warm.Background,
    surface = WarmCoolTheme.Warm.Surface,
    error = WarmCoolTheme.Warm.Error
)

val WarmCoolDarkScheme = createDarkColorScheme(
    primary = WarmCoolTheme.Cool.Primary,
    onPrimary = WarmCoolTheme.Cool.OnPrimary,
    secondary = WarmCoolTheme.Cool.Secondary,
    onSecondary = WarmCoolTheme.Cool.OnSecondary,
    tertiary = WarmCoolTheme.Cool.Tertiary,
    background = WarmCoolTheme.Cool.Background,
    surface = WarmCoolTheme.Cool.Surface,
    error = WarmCoolTheme.Cool.Error
)

// Nature Dual
val NatureDualLightScheme = createLightColorScheme(
    primary = NatureDualTheme.Spring.Primary,
    onPrimary = NatureDualTheme.Spring.OnPrimary,
    secondary = NatureDualTheme.Spring.Secondary,
    onSecondary = NatureDualTheme.Spring.OnSecondary,
    tertiary = NatureDualTheme.Spring.Tertiary,
    background = NatureDualTheme.Spring.Background,
    surface = NatureDualTheme.Spring.Surface,
    error = NatureDualTheme.Spring.Error
)

val NatureDualDarkScheme = createDarkColorScheme(
    primary = NatureDualTheme.Forest.Primary,
    onPrimary = NatureDualTheme.Forest.OnPrimary,
    secondary = NatureDualTheme.Forest.Secondary,
    onSecondary = NatureDualTheme.Forest.OnSecondary,
    tertiary = NatureDualTheme.Forest.Tertiary,
    background = NatureDualTheme.Forest.Background,
    surface = NatureDualTheme.Forest.Surface,
    error = NatureDualTheme.Forest.Error
)

// Vibrant Dual
val VibrantDualLightScheme = createLightColorScheme(
    primary = VibrantDualTheme.Electric.Primary,
    onPrimary = VibrantDualTheme.Electric.OnPrimary,
    secondary = VibrantDualTheme.Electric.Secondary,
    onSecondary = VibrantDualTheme.Electric.OnSecondary,
    tertiary = VibrantDualTheme.Electric.Tertiary,
    background = VibrantDualTheme.Electric.Background,
    surface = VibrantDualTheme.Electric.Surface,
    error = VibrantDualTheme.Electric.Error
)

val VibrantDualDarkScheme = createDarkColorScheme(
    primary = VibrantDualTheme.Neon.Primary,
    onPrimary = VibrantDualTheme.Neon.OnPrimary,
    secondary = VibrantDualTheme.Neon.Secondary,
    onSecondary = VibrantDualTheme.Neon.OnSecondary,
    tertiary = VibrantDualTheme.Neon.Tertiary,
    background = VibrantDualTheme.Neon.Background,
    surface = VibrantDualTheme.Neon.Surface,
    error = VibrantDualTheme.Neon.Error
)

// Professional Dual
val ProfessionalDualLightScheme = createLightColorScheme(
    primary = ProfessionalDualTheme.Corporate.Primary,
    onPrimary = ProfessionalDualTheme.Corporate.OnPrimary,
    secondary = ProfessionalDualTheme.Corporate.Secondary,
    onSecondary = ProfessionalDualTheme.Corporate.OnSecondary,
    tertiary = ProfessionalDualTheme.Corporate.Tertiary,
    background = ProfessionalDualTheme.Corporate.Background,
    surface = ProfessionalDualTheme.Corporate.Surface,
    error = ProfessionalDualTheme.Corporate.Error
)

val ProfessionalDualDarkScheme = createDarkColorScheme(
    primary = ProfessionalDualTheme.Elegant.Primary,
    onPrimary = ProfessionalDualTheme.Elegant.OnPrimary,
    secondary = ProfessionalDualTheme.Elegant.Secondary,
    onSecondary = ProfessionalDualTheme.Elegant.OnSecondary,
    tertiary = ProfessionalDualTheme.Elegant.Tertiary,
    background = ProfessionalDualTheme.Elegant.Background,
    surface = ProfessionalDualTheme.Elegant.Surface,
    error = ProfessionalDualTheme.Elegant.Error
)

// Ocean Sunset
val OceanSunsetLightScheme = createLightColorScheme(
    primary = OceanSunsetTheme.Ocean.Primary,
    onPrimary = OceanSunsetTheme.Ocean.OnPrimary,
    secondary = OceanSunsetTheme.Ocean.Secondary,
    onSecondary = OceanSunsetTheme.Ocean.OnSecondary,
    tertiary = OceanSunsetTheme.Ocean.Tertiary,
    background = OceanSunsetTheme.Ocean.Background,
    surface = OceanSunsetTheme.Ocean.Surface,
    error = OceanSunsetTheme.Ocean.Error
)

val OceanSunsetDarkScheme = createDarkColorScheme(
    primary = OceanSunsetTheme.Sunset.Primary,
    onPrimary = OceanSunsetTheme.Sunset.OnPrimary,
    secondary = OceanSunsetTheme.Sunset.Secondary,
    onSecondary = OceanSunsetTheme.Sunset.OnSecondary,
    tertiary = OceanSunsetTheme.Sunset.Tertiary,
    background = OceanSunsetTheme.Sunset.Background,
    surface = OceanSunsetTheme.Sunset.Surface,
    error = OceanSunsetTheme.Sunset.Error
)

// Retro Dual
val RetroDualLightScheme = createLightColorScheme(
    primary = RetroDualTheme.Pastel.Primary,
    onPrimary = RetroDualTheme.Pastel.OnPrimary,
    secondary = RetroDualTheme.Pastel.Secondary,
    onSecondary = RetroDualTheme.Pastel.OnSecondary,
    tertiary = RetroDualTheme.Pastel.Tertiary,
    background = RetroDualTheme.Pastel.Background,
    surface = RetroDualTheme.Pastel.Surface,
    error = RetroDualTheme.Pastel.Error
)

val RetroDualDarkScheme = createDarkColorScheme(
    primary = RetroDualTheme.Neon80s.Primary,
    onPrimary = RetroDualTheme.Neon80s.OnPrimary,
    secondary = RetroDualTheme.Neon80s.Secondary,
    onSecondary = RetroDualTheme.Neon80s.OnSecondary,
    tertiary = RetroDualTheme.Neon80s.Tertiary,
    background = RetroDualTheme.Neon80s.Background,
    surface = RetroDualTheme.Neon80s.Surface,
    error = RetroDualTheme.Neon80s.Error
)

// Minimal Contrast
val MinimalContrastLightScheme = createLightColorScheme(
    primary = MinimalContrastTheme.Clean.Primary,
    onPrimary = MinimalContrastTheme.Clean.OnPrimary,
    secondary = MinimalContrastTheme.Clean.Secondary,
    onSecondary = MinimalContrastTheme.Clean.OnSecondary,
    tertiary = MinimalContrastTheme.Clean.Tertiary,
    background = MinimalContrastTheme.Clean.Background,
    surface = MinimalContrastTheme.Clean.Surface,
    error = MinimalContrastTheme.Clean.Error
)

val MinimalContrastDarkScheme = createDarkColorScheme(
    primary = MinimalContrastTheme.Colorful.Primary,
    onPrimary = MinimalContrastTheme.Colorful.OnPrimary,
    secondary = MinimalContrastTheme.Colorful.Secondary,
    onSecondary = MinimalContrastTheme.Colorful.OnSecondary,
    tertiary = MinimalContrastTheme.Colorful.Tertiary,
    background = MinimalContrastTheme.Colorful.Background,
    surface = MinimalContrastTheme.Colorful.Surface,
    error = MinimalContrastTheme.Colorful.Error
)

// ==================== Dual Theme Selection ====================

enum class DualThemeVariant {
    DAY_NIGHT,           // Blue Day / Purple Night
    WARM_COOL,           // Warm Coral / Cool Teal
    NATURE_DUAL,         // Spring Garden / Forest Night
    VIBRANT_DUAL,        // Electric / Neon Glow
    PROFESSIONAL_DUAL,   // Corporate Blue / Elegant Gold
    OCEAN_SUNSET,        // Ocean Blue / Sunset Orange
    RETRO_DUAL,          // Pastel 80s / Neon 80s
    MINIMAL_CONTRAST     // Clean B&W / Colorful OLED
}

/**
 * Get dual theme colors (different for light and dark)
 */
fun getDualThemeColors(variant: DualThemeVariant, isDark: Boolean): ColorScheme {
    return when (variant) {
        DualThemeVariant.DAY_NIGHT -> if (isDark) DayNightDarkScheme else DayNightLightScheme
        DualThemeVariant.WARM_COOL -> if (isDark) WarmCoolDarkScheme else WarmCoolLightScheme
        DualThemeVariant.NATURE_DUAL -> if (isDark) NatureDualDarkScheme else NatureDualLightScheme
        DualThemeVariant.VIBRANT_DUAL -> if (isDark) VibrantDualDarkScheme else VibrantDualLightScheme
        DualThemeVariant.PROFESSIONAL_DUAL -> if (isDark) ProfessionalDualDarkScheme else ProfessionalDualLightScheme
        DualThemeVariant.OCEAN_SUNSET -> if (isDark) OceanSunsetDarkScheme else OceanSunsetLightScheme
        DualThemeVariant.RETRO_DUAL -> if (isDark) RetroDualDarkScheme else RetroDualLightScheme
        DualThemeVariant.MINIMAL_CONTRAST -> if (isDark) MinimalContrastDarkScheme else MinimalContrastLightScheme
    }
}
