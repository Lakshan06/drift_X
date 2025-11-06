package com.driftdetector.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * DriftGuardAI Professional Color Palette
 * Oceanic Theme - Calm, Professional, Status-Focused
 *
 * Designed for clarity, user focus, and professional dashboard experience
 */

// ========================================
// PRIMARY COLORS - Professional Oceanic Theme
// ========================================

/** Primary brand color - Oceanic Teal for buttons, highlights */
val OceanicTeal = Color(0xFF2C8C99)

/** Accent color - Coral Blush for secondary interactive elements */
val CoralBlush = Color(0xFFFF6F61)

/** Main background - Slate Gray for background and dash panels */
val SlateGray = Color(0xFF2F3B45)

/** Card backgrounds - Gunmetal for cards and grouping areas */
val Gunmetal = Color(0xFF393E46)

// ========================================
// STATUS & ALERT COLORS
// ========================================

/** Success - Mint Green for success and applied patches */
val MintGreen = Color(0xFF3EB489)

/** Warning - Goldenrod for drift warnings */
val Goldenrod = Color(0xFFE9B44C)

/** Error - Flame Red for error indicators */
val FlameRed = Color(0xFFD7263D)

// ========================================
// TEXT COLORS
// ========================================

/** Primary text - Ivory for high contrast against dark tones */
val Ivory = Color(0xFFF4F1DE)

/** Secondary text - Mist Gray for secondary text and charts */
val MistGray = Color(0xFFB0B3B8)

/** Disabled/inactive text */
val DisabledGray = Color(0xFF6B7280)

// ========================================
// DARK THEME COLORS (Primary)
// ========================================

// Background
val DarkBackground = SlateGray
val DarkSurface = Gunmetal
val DarkSurfaceVariant = Color(0xFF434952) // Lighter Gunmetal

// Primary colors - Oceanic Teal
val DarkPrimary = OceanicTeal
val DarkOnPrimary = Ivory
val DarkPrimaryContainer = Color(0xFF236B76) // Darker Teal
val DarkOnPrimaryContainer = Ivory

// Secondary colors - Coral Blush
val DarkSecondary = CoralBlush
val DarkOnSecondary = Ivory
val DarkSecondaryContainer = Color(0xFFCC5850) // Darker Coral
val DarkOnSecondaryContainer = Ivory

// Tertiary colors - Mint Green
val DarkTertiary = MintGreen
val DarkOnTertiary = SlateGray
val DarkTertiaryContainer = Color(0xFF2E8A68) // Darker Mint
val DarkOnTertiaryContainer = Ivory

// Error colors - Flame Red
val DarkError = FlameRed
val DarkOnError = Ivory
val DarkErrorContainer = Color(0xFF9A1C2A) // Darker Red
val DarkOnErrorContainer = Ivory

// Text colors
val DarkOnBackground = Ivory
val DarkOnSurface = Ivory
val DarkOnSurfaceVariant = MistGray

// Border and outline
val DarkOutline = MistGray
val DarkOutlineVariant = Color(0xFF5A6169) // Lighter gray

// ========================================
// LIGHT THEME COLORS
// ========================================

val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = OceanicTeal
val LightOnPrimary = Ivory
val LightSecondary = CoralBlush
val LightOnBackground = Color(0xFF1F2937)
val LightOnSurface = Color(0xFF1F2937)

// ========================================
// CHART & VISUALIZATION COLORS
// ========================================

/** Chart series colors */
val ChartSeries1 = OceanicTeal
val ChartSeries2 = CoralBlush
val ChartSeries3 = MintGreen
val ChartSeries4 = Goldenrod

/** Heatmap gradient - Low to High drift impact */
val HeatmapLow = MintGreen       // Mint green
val HeatmapMedium = Goldenrod    // Goldenrod
val HeatmapHigh = FlameRed       // Flame red

/** Chart background */
val ChartBackground = Gunmetal

// ========================================
// COMPONENT SPECIFIC COLORS
// ========================================

/** FAB (Floating Action Button) - PatchBot */
val FABBackground = CoralBlush
val FABContent = Ivory

/** Navigation Bar */
val NavBarBackground = Gunmetal
val NavBarSelected = OceanicTeal
val NavBarUnselected = MistGray

/** Cards */
val CardBackground = Gunmetal
val CardBorder = OceanicTeal
val CardBorderInactive = DisabledGray

/** Buttons */
val ButtonPrimary = OceanicTeal
val ButtonPrimaryText = Ivory
val ButtonSecondary = CoralBlush
val ButtonSecondaryText = Ivory
val ButtonSuccess = MintGreen
val ButtonSuccessText = SlateGray
val ButtonDanger = FlameRed
val ButtonDangerText = Ivory
val ButtonDisabled = DisabledGray
val ButtonDisabledText = Color(0xFF9CA3AF)

/** Alerts & Notifications */
val AlertWarningBackground = Goldenrod
val AlertWarningText = SlateGray
val AlertCriticalBackground = FlameRed
val AlertCriticalText = Ivory
val AlertSuccessBackground = MintGreen
val AlertSuccessText = SlateGray
val AlertInfoBackground = OceanicTeal
val AlertInfoText = Ivory

/** Badges */
val BadgeActive = MintGreen
val BadgeWarning = Goldenrod
val BadgeCritical = FlameRed
val BadgeInactive = DisabledGray
val BadgeInfo = OceanicTeal

/** Dividers */
val DividerColor = Color(0xFF4B5563)
val DividerColorSubtle = Color(0xFF374151)

/** Ripple effect */
val RippleColor = Color(0x332C8C99) // 20% opacity Oceanic Teal

/** Shimmer/Loading effect */
val ShimmerBase = Gunmetal
val ShimmerHighlight = Color(0xFF4A5260)

// ========================================
// DRIFT SEVERITY COLORS
// ========================================

/** No drift detected */
val DriftNone = MintGreen

/** Low drift (PSI < 0.2) */
val DriftLow = Color(0xFF5BC999) // Lighter mint

/** Moderate drift (PSI 0.2-0.5) */
val DriftModerate = Goldenrod

/** High drift (PSI 0.5-0.7) */
val DriftHigh = Color(0xFFEF8A5C) // Orange-coral

/** Critical drift (PSI > 0.7) */
val DriftCritical = FlameRed

// ========================================
// LEGACY COMPATIBILITY
// (Kept for backward compatibility)
// ========================================

// Light Theme Legacy
val md_theme_light_primary = OceanicTeal
val md_theme_light_onPrimary = Ivory
val md_theme_light_primaryContainer = Color(0xFFB3E5EC)
val md_theme_light_onPrimaryContainer = SlateGray
val md_theme_light_secondary = CoralBlush
val md_theme_light_onSecondary = Ivory
val md_theme_light_secondaryContainer = Color(0xFFFFDBD7)
val md_theme_light_onSecondaryContainer = SlateGray
val md_theme_light_tertiary = MintGreen
val md_theme_light_onTertiary = SlateGray
val md_theme_light_tertiaryContainer = Color(0xFFB8F4D7)
val md_theme_light_onTertiaryContainer = SlateGray
val md_theme_light_error = FlameRed
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onError = Ivory
val md_theme_light_onErrorContainer = SlateGray
val md_theme_light_background = LightBackground
val md_theme_light_onBackground = LightOnBackground
val md_theme_light_surface = LightSurface
val md_theme_light_onSurface = LightOnSurface
val md_theme_light_surfaceVariant = Color(0xFFE7E9EB)
val md_theme_light_onSurfaceVariant = Color(0xFF4B5563)
val md_theme_light_outline = MistGray

// Dark Theme Legacy
val md_theme_dark_primary = DarkPrimary
val md_theme_dark_onPrimary = DarkOnPrimary
val md_theme_dark_primaryContainer = DarkPrimaryContainer
val md_theme_dark_onPrimaryContainer = DarkOnPrimaryContainer
val md_theme_dark_secondary = DarkSecondary
val md_theme_dark_onSecondary = DarkOnSecondary
val md_theme_dark_secondaryContainer = DarkSecondaryContainer
val md_theme_dark_onSecondaryContainer = DarkOnSecondaryContainer
val md_theme_dark_tertiary = DarkTertiary
val md_theme_dark_onTertiary = DarkOnTertiary
val md_theme_dark_tertiaryContainer = DarkTertiaryContainer
val md_theme_dark_onTertiaryContainer = DarkOnTertiaryContainer
val md_theme_dark_error = DarkError
val md_theme_dark_errorContainer = DarkErrorContainer
val md_theme_dark_onError = DarkOnError
val md_theme_dark_onErrorContainer = DarkOnErrorContainer
val md_theme_dark_background = DarkBackground
val md_theme_dark_onBackground = DarkOnBackground
val md_theme_dark_surface = DarkSurface
val md_theme_dark_onSurface = DarkOnSurface
val md_theme_dark_surfaceVariant = DarkSurfaceVariant
val md_theme_dark_onSurfaceVariant = DarkOnSurfaceVariant
val md_theme_dark_outline = DarkOutline

// ========================================
// BACKWARD COMPATIBILITY ALIASES
// (For existing code that references old names)
// ========================================

// Map old names to new palette
val CyberIndigo = OceanicTeal
val MysticViolet = CoralBlush
val GalaxyCharcoal = SlateGray
val DeepSpaceNavy = Gunmetal
val CosmicGraphite = Gunmetal
val StardustWhite = Ivory
val LapisSilver = MistGray
val EclipseSlate = DisabledGray
val SolarAmber = Goldenrod
val HyperCrimson = FlameRed
val EmeraldFade = MintGreen
