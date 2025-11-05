package com.driftdetector.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * DriftGuardAI Custom Color Palette
 * Sci-Fi / Fantasy Tech Theme
 *
 * Designed for extended dashboard use with high contrast and accessibility
 */

// ========================================
// PRIMARY COLORS - Sci-Fi Theme
// ========================================

/** Main background color - Deep space feel */
val GalaxyCharcoal = Color(0xFF181B24)

/** Primary brand color - Used for CTAs, headers */
val CyberIndigo = Color(0xFF233DFF)

/** Secondary brand color - Navigation, active states */
val MysticViolet = Color(0xFF725AC1)

/** Card backgrounds - General purpose */
val DeepSpaceNavy = Color(0xFF232535)

/** Alternative card background - Lists, data entry */
val CosmicGraphite = Color(0xFF23272D)

// ========================================
// TEXT COLORS
// ========================================

/** Primary text color - High contrast white */
val StardustWhite = Color(0xFFDEE5EF)

/** Secondary text - Stats, labels */
val LapisSilver = Color(0xFFB6BBC4)

/** Disabled/inactive text and elements */
val EclipseSlate = Color(0xFF626A77)

// ========================================
// STATUS & ALERT COLORS
// ========================================

/** Warning state - Moderate drift */
val SolarAmber = Color(0xFFFFA600)

/** Critical/Error state - High drift, failures */
val HyperCrimson = Color(0xFFD72660)

/** Success state - Patches applied, healthy models */
val EmeraldFade = Color(0xFF44D39A)

// ========================================
// LIGHT THEME COLORS
// ========================================
// Inverted palette for light mode (optional)

val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = CyberIndigo
val LightOnPrimary = StardustWhite
val LightSecondary = MysticViolet
val LightOnBackground = Color(0xFF1A1C23)
val LightOnSurface = Color(0xFF1A1C23)

// ========================================
// DARK THEME COLORS (Primary)
// ========================================

// Background
val DarkBackground = GalaxyCharcoal
val DarkSurface = DeepSpaceNavy
val DarkSurfaceVariant = CosmicGraphite

// Primary colors
val DarkPrimary = CyberIndigo
val DarkOnPrimary = StardustWhite
val DarkPrimaryContainer = MysticViolet
val DarkOnPrimaryContainer = StardustWhite

// Secondary colors
val DarkSecondary = MysticViolet
val DarkOnSecondary = StardustWhite
val DarkSecondaryContainer = Color(0xFF4A3F7E) // Darker violet
val DarkOnSecondaryContainer = StardustWhite

// Tertiary colors (for additional accents)
val DarkTertiary = EmeraldFade
val DarkOnTertiary = GalaxyCharcoal
val DarkTertiaryContainer = Color(0xFF2A8A61) // Darker emerald
val DarkOnTertiaryContainer = StardustWhite

// Error colors
val DarkError = HyperCrimson
val DarkOnError = StardustWhite
val DarkErrorContainer = Color(0xFF8A1942) // Darker crimson
val DarkOnErrorContainer = StardustWhite

// Text colors
val DarkOnBackground = StardustWhite
val DarkOnSurface = StardustWhite
val DarkOnSurfaceVariant = LapisSilver

// Border and outline
val DarkOutline = EclipseSlate
val DarkOutlineVariant = Color(0xFF3D4452) // Lighter slate

// ========================================
// CHART & VISUALIZATION COLORS
// ========================================

/** Chart series colors - for multi-line charts */
val ChartSeries1 = CyberIndigo
val ChartSeries2 = MysticViolet
val ChartSeries3 = EmeraldFade
val ChartSeries4 = SolarAmber

/** Heatmap gradient - Low to High drift impact */
val HeatmapLow = EmeraldFade      // Light green
val HeatmapMedium = SolarAmber    // Orange
val HeatmapHigh = HyperCrimson    // Red

/** Chart background */
val ChartBackground = CosmicGraphite

// ========================================
// COMPONENT SPECIFIC COLORS
// ========================================

/** FAB (Floating Action Button) - PatchBot */
val FABBackground = MysticViolet
val FABContent = StardustWhite

/** Navigation Bar */
val NavBarBackground = CyberIndigo
val NavBarSelected = MysticViolet
val NavBarUnselected = LapisSilver

/** Cards */
val CardBackground = DeepSpaceNavy
val CardBorder = MysticViolet
val CardBorderInactive = EclipseSlate

/** Buttons */
val ButtonPrimary = CyberIndigo
val ButtonPrimaryText = StardustWhite
val ButtonSecondary = MysticViolet
val ButtonSecondaryText = StardustWhite
val ButtonSuccess = EmeraldFade
val ButtonSuccessText = GalaxyCharcoal
val ButtonDanger = HyperCrimson
val ButtonDangerText = StardustWhite
val ButtonDisabled = EclipseSlate
val ButtonDisabledText = Color(0xFF4A4F5C)

/** Alerts & Notifications */
val AlertWarningBackground = SolarAmber
val AlertWarningText = GalaxyCharcoal
val AlertCriticalBackground = HyperCrimson
val AlertCriticalText = StardustWhite
val AlertSuccessBackground = EmeraldFade
val AlertSuccessText = GalaxyCharcoal
val AlertInfoBackground = CyberIndigo
val AlertInfoText = StardustWhite

/** Badges */
val BadgeActive = EmeraldFade
val BadgeWarning = SolarAmber
val BadgeCritical = HyperCrimson
val BadgeInactive = EclipseSlate
val BadgeInfo = CyberIndigo

/** Dividers */
val DividerColor = Color(0xFF2D3139)
val DividerColorSubtle = Color(0xFF252830)

/** Ripple effect */
val RippleColor = Color(0x33233DFF) // 20% opacity Cyber Indigo

/** Shimmer/Loading effect */
val ShimmerBase = CosmicGraphite
val ShimmerHighlight = Color(0xFF2F3340)

// ========================================
// DRIFT SEVERITY COLORS
// ========================================

/** No drift detected */
val DriftNone = EmeraldFade

/** Low drift (PSI < 0.2) */
val DriftLow = Color(0xFF67E0A8) // Lighter emerald

/** Moderate drift (PSI 0.2-0.5) */
val DriftModerate = SolarAmber

/** High drift (PSI 0.5-0.7) */
val DriftHigh = Color(0xFFFF8F39) // Orange-red

/** Critical drift (PSI > 0.7) */
val DriftCritical = HyperCrimson

// ========================================
// LEGACY COMPATIBILITY
// (Kept for backward compatibility, can be removed later)
// ========================================

// Light Theme Legacy
val md_theme_light_primary = CyberIndigo
val md_theme_light_onPrimary = StardustWhite
val md_theme_light_primaryContainer = MysticViolet
val md_theme_light_onPrimaryContainer = StardustWhite
val md_theme_light_secondary = MysticViolet
val md_theme_light_onSecondary = StardustWhite
val md_theme_light_secondaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onSecondaryContainer = GalaxyCharcoal
val md_theme_light_tertiary = EmeraldFade
val md_theme_light_onTertiary = GalaxyCharcoal
val md_theme_light_tertiaryContainer = Color(0xFFD0F4E4)
val md_theme_light_onTertiaryContainer = GalaxyCharcoal
val md_theme_light_error = HyperCrimson
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onError = StardustWhite
val md_theme_light_onErrorContainer = GalaxyCharcoal
val md_theme_light_background = LightBackground
val md_theme_light_onBackground = LightOnBackground
val md_theme_light_surface = LightSurface
val md_theme_light_onSurface = LightOnSurface
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_outline = EclipseSlate

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
