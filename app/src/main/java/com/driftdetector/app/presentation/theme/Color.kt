package com.driftdetector.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * DriftGuardAI - Sunset Gradient Theme
 * Warm, dynamic gradients with sunset-inspired color transitions
 *
 * Theme: Creative, energetic, engaging, innovative
 * Designed for modern tech companies and startups
 */

// ========================================
// PRIMARY COLORS - Sunset Gradient Theme
// ========================================

// Background & Surface - Gradient base
val DarkBackground = Color(0xFF1A1625)         // Deep purple-black
val DarkSurface = Color(0xFF2D2438)            // Dark purple card
val DarkSurfaceVariant = Color(0xFF3A2F47)     // Lighter purple variant

// Primary - Sunset Orange
val SunsetOrange = Color(0xFFFF6B35)           // Vibrant sunset orange
val SunsetOrangeDark = Color(0xFFE55A2B)       // Darker orange
val SunsetOrangeLight = Color(0xFFFF8757)      // Light orange glow

// Accent - Electric Violet
val ElectricViolet = Color(0xFF9B59B6)         // Rich purple
val ElectricVioletDark = Color(0xFF8E44AD)     // Darker purple
val ElectricVioletLight = Color(0xFFB370CF)    // Light purple

// Secondary - Rose Gold
val RoseGold = Color(0xFFF39C6B)               // Warm rose-gold
val RoseGoldDark = Color(0xFFE08753)           // Darker rose
val RoseGoldLight = Color(0xFFFDB896)          // Light rose-gold

// ========================================
// STATUS & ALERT COLORS
// ========================================

/** Success - Vibrant Green */
val SuccessGreen = Color(0xFF2ECC71)           // Vibrant success green

/** Warning - Golden Orange */
val WarningGold = Color(0xFFF39C12)            // Golden warning

/** Error - Warm Red */
val ErrorRed = Color(0xFFE74C3C)               // Warm red (not harsh)

// ========================================
// TEXT COLORS
// ========================================

/** Primary text - Warm cream white */
val TextPrimary = Color(0xFFF8F0E3)            // Warm cream white

/** Secondary text - Muted rose-gray */
val TextSecondary = Color(0xFFBDA8A8)          // Muted rose-gray

/** Disabled/inactive text */
val TextDisabled = Color(0xFF7A6B7A)           // Dark muted purple

// ========================================
// DARK THEME COLORS (Primary)
// ========================================

// Background
val DarkPrimary = SunsetOrange
val DarkOnPrimary = Color.White
val DarkPrimaryContainer = SunsetOrangeDark
val DarkOnPrimaryContainer = TextPrimary

// Secondary colors - Rose Gold
val DarkSecondary = RoseGold
val DarkOnSecondary = DarkBackground
val DarkSecondaryContainer = RoseGoldDark
val DarkOnSecondaryContainer = TextPrimary

// Tertiary colors - Electric Violet
val DarkTertiary = ElectricViolet
val DarkOnTertiary = Color.White
val DarkTertiaryContainer = ElectricVioletDark
val DarkOnTertiaryContainer = TextPrimary

// Error colors - Warm Red
val DarkError = ErrorRed
val DarkOnError = Color.White
val DarkErrorContainer = Color(0xFF8B2C24)     // Darker red
val DarkOnErrorContainer = TextPrimary

// Text colors
val DarkOnBackground = TextPrimary
val DarkOnSurface = TextPrimary
val DarkOnSurfaceVariant = TextSecondary

// Border and outline
val DarkOutline = TextSecondary
val DarkOutlineVariant = Color(0xFF9B8C9B)     // Lighter purple-gray

// ========================================
// LIGHT THEME COLORS
// ========================================

val LightBackground = Color(0xFFFFF5F0)        // Warm off-white
val LightSurface = Color(0xFFFFFFFF)           // Pure white
val LightPrimary = SunsetOrange
val LightOnPrimary = Color.White
val LightSecondary = RoseGold
val LightOnBackground = Color(0xFF2C1B1F)      // Dark purple-brown
val LightOnSurface = Color(0xFF2C1B1F)         // Dark purple-brown

// ========================================
// CHART & VISUALIZATION COLORS
// ========================================

/** Chart series colors */
val ChartSeries1 = SunsetOrange
val ChartSeries2 = ElectricViolet
val ChartSeries3 = RoseGold
val ChartSeries4 = SuccessGreen

/** Heatmap gradient - Low to High drift impact */
val HeatmapLow = SuccessGreen        // Green for low drift
val HeatmapMedium = WarningGold      // Gold for medium drift
val HeatmapHigh = ErrorRed           // Red for high drift

/** Chart background */
val ChartBackground = DarkSurface

// ========================================
// COMPONENT SPECIFIC COLORS
// ========================================

/** FAB (Floating Action Button) - Gradient Orb */
val FABBackground = SunsetOrange
val FABContent = Color.White

/** Navigation Bar */
val NavBarBackground = DarkSurface
val NavBarSelected = SunsetOrange
val NavBarUnselected = TextSecondary

/** Cards */
val CardBackground = DarkSurface
val CardBorder = SunsetOrange
val CardBorderInactive = TextDisabled

/** Buttons */
val ButtonPrimary = SunsetOrange
val ButtonPrimaryText = Color.White
val ButtonSecondary = RoseGold
val ButtonSecondaryText = DarkBackground
val ButtonSuccess = SuccessGreen
val ButtonSuccessText = Color.White
val ButtonDanger = ErrorRed
val ButtonDangerText = Color.White
val ButtonDisabled = TextDisabled
val ButtonDisabledText = Color(0xFF9B8C9B)

/** Alerts & Notifications */
val AlertWarningBackground = WarningGold
val AlertWarningText = DarkBackground
val AlertCriticalBackground = ErrorRed
val AlertCriticalText = Color.White
val AlertSuccessBackground = SuccessGreen
val AlertSuccessText = Color.White
val AlertInfoBackground = ElectricViolet
val AlertInfoText = Color.White

/** Badges */
val BadgeActive = SuccessGreen
val BadgeWarning = WarningGold
val BadgeCritical = ErrorRed
val BadgeInactive = TextDisabled
val BadgeInfo = ElectricViolet

/** Dividers */
val DividerColor = Color(0xFF4A3B4E)           // Purple-gray divider
val DividerColorSubtle = Color(0xFF3A2F47)     // Subtle divider

/** Ripple effect */
val RippleColor = Color(0x33FF6B35)            // 20% opacity Sunset Orange

/** Shimmer/Loading effect */
val ShimmerBase = DarkSurface
val ShimmerHighlight = Color(0xFF4A3B54)       // Lighter purple

// ========================================
// DRIFT SEVERITY COLORS
// ========================================

/** No drift detected */
val DriftNone = SuccessGreen

/** Low drift (PSI < 0.2) */
val DriftLow = Color(0xFF52D17C)               // Lighter green

/** Moderate drift (PSI 0.2-0.5) */
val DriftModerate = WarningGold

/** High drift (PSI 0.5-0.7) */
val DriftHigh = Color(0xFFFF8B5F)              // Orange-coral

/** Critical drift (PSI > 0.7) */
val DriftCritical = ErrorRed

// ========================================
// LEGACY COMPATIBILITY
// (Kept for backward compatibility)
// ========================================

// Light Theme Legacy
val md_theme_light_primary = SunsetOrange
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = SunsetOrangeLight
val md_theme_light_onPrimaryContainer = DarkBackground
val md_theme_light_secondary = RoseGold
val md_theme_light_onSecondary = DarkBackground
val md_theme_light_secondaryContainer = RoseGoldLight
val md_theme_light_onSecondaryContainer = DarkBackground
val md_theme_light_tertiary = ElectricViolet
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = ElectricVioletLight
val md_theme_light_onTertiaryContainer = DarkBackground
val md_theme_light_error = ErrorRed
val md_theme_light_errorContainer = Color(0xFFFDDED9)
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = DarkBackground
val md_theme_light_background = LightBackground
val md_theme_light_onBackground = LightOnBackground
val md_theme_light_surface = LightSurface
val md_theme_light_onSurface = LightOnSurface
val md_theme_light_surfaceVariant = Color(0xFFF5EBE7)
val md_theme_light_onSurfaceVariant = Color(0xFF534350)
val md_theme_light_outline = TextSecondary

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

// Map old names to new sunset gradient palette
val OceanicTeal = SunsetOrange               // Primary action color
val CoralBlush = RoseGold                    // Secondary accent
val SlateGray = DarkBackground               // Background
val Gunmetal = DarkSurface                   // Card surface
val MintGreen = SuccessGreen                 // Success state
val Goldenrod = WarningGold                  // Warning state
val FlameRed = ErrorRed                      // Error state
val Ivory = TextPrimary                      // Primary text
val MistGray = TextSecondary                 // Secondary text
val DisabledGray = TextDisabled              // Disabled text

// Additional legacy aliases
val CyberIndigo = SunsetOrange
val MysticViolet = ElectricViolet
val GalaxyCharcoal = DarkBackground
val DeepSpaceNavy = DarkSurface
val CosmicGraphite = DarkSurface
val StardustWhite = TextPrimary
val LapisSilver = TextSecondary
val EclipseSlate = TextDisabled
val SolarAmber = WarningGold
val HyperCrimson = ErrorRed
val EmeraldFade = SuccessGreen
