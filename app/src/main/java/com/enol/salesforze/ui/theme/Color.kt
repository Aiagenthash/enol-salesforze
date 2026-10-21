package com.enol.salesforze.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* =========================================================================
 * ENOL SALESFORZE - INDUSTRIAL AUTOMOTIVE LUBRICANT DESIGN TOKENS
 * ========================================================================= */

// --- Core Dark Palette (Clean Obsidian & Slate) ---
val EnolNavy = Color(0xFF090D16)          // Pure Midnight Background
val EnolNavyDark = Color(0xFF060910)      // Deepest Obsidian
val EnolNavyLight = Color(0xFF161F32)     // Elevated Container
val EnolNavyMuted = Color(0xFF1D283E)     // Subtle Border Accent

// --- Single Spot Color: Premium Lubricant Amber Gold ---
val LubricantAmber = Color(0xFFF5A623)     // Single Spot Color (Primary Accent & CTAs)
val LubricantAmberDark = Color(0xFFD97706) // Pressed State
val LubricantAmberLight = Color(0xFFFCD34D)// Highlight Glow
val LubricantAmberMuted = Color(0xFF452B00)// Subtle Dark Accent

// --- Neutral Monochrome Palette ---
val SlateSteel = Color(0xFF64748B)         // Muted Caption
val SlateSteelLight = Color(0xFF94A3B8)    // Clean Secondary Text
val SlateSteelDark = Color(0xFF1F293D)     // Subtle Hairline Borders
val SlateSteelMuted = Color(0xFF475569)    // Disabled States

// --- Surface & Background Palettes ---
// Light
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFF1F5F9)
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val OnSurfaceLight = Color(0xFF0F172A)
val OnBackgroundLight = Color(0xFF0F172A)

// Dark (Modern Minimalist Graphite)
val BackgroundDark = Color(0xFF090D16)
val SurfaceDark = Color(0xFF111726)
val SurfaceContainerDark = Color(0xFF161F32)
val SurfaceContainerHighDark = Color(0xFF1D283E)
val OnSurfaceDark = Color(0xFFF8FAFC)
val OnBackgroundDark = Color(0xFFF8FAFC)

// --- Operational Status Colors (Aligned with Single Spot Color) ---
val SuccessEmerald = Color(0xFFF5A623)     // Aligned to Spot Color for unified aesthetic
val SuccessEmeraldContainer = Color(0xFF261D0A)
val OnSuccessEmerald = Color(0xFF060910)
val OnSuccessEmeraldContainer = Color(0xFFFCD34D)

val WarningAmber = Color(0xFFF5A623)       // Aligned to Spot Color
val WarningAmberContainer = Color(0xFF261D0A)
val OnWarningAmber = Color(0xFF060910)
val OnWarningAmberContainer = Color(0xFFFCD34D)

val ErrorCrimson = Color(0xFFEF4444)       // Dedicated error red
val ErrorCrimsonContainer = Color(0xFF2E1010)
val OnErrorCrimson = Color(0xFFFFFFFF)
val OnErrorCrimsonContainer = Color(0xFFFCA5A5)

val InfoSky = Color(0xFF94A3B8)            // Neutral slate
val InfoSkyContainer = Color(0xFF161F32)
val OnInfoSky = Color(0xFFF8FAFC)
val OnInfoSkyContainer = Color(0xFF94A3B8)

// --- High-Contrast Outdoor Field Readability Colors ---
// Tuned for direct blinding sunlight at petrol pumps and outdoor auto repair workshops
val HighContrastBackgroundLight = Color(0xFFFFFFFF)
val HighContrastSurfaceLight = Color(0xFFFFFFFF)
val HighContrastOnSurfaceLight = Color(0xFF000000)
val HighContrastPrimaryLight = Color(0xFF050C17)
val HighContrastSecondaryLight = Color(0xFF92400E)
val HighContrastOutlineLight = Color(0xFF000000)

val HighContrastBackgroundDark = Color(0xFF000000)
val HighContrastSurfaceDark = Color(0xFF050C17)
val HighContrastOnSurfaceDark = Color(0xFFFFFFFF)
val HighContrastPrimaryDark = Color(0xFF93C5FD)
val HighContrastSecondaryDark = Color(0xFFFDE68A)
val HighContrastOutlineDark = Color(0xFFFFFFFF)

/* =========================================================================
 * MATERIAL 3 COLOR SCHEMES
 * ========================================================================= */

val EnolLightColorScheme = lightColorScheme(
    primary = EnolNavy,
    onPrimary = Color.White,
    primaryContainer = EnolNavyLight,
    onPrimaryContainer = Color.White,
    secondary = LubricantAmberDark,
    onSecondary = Color.White,
    secondaryContainer = LubricantAmberLight,
    onSecondaryContainer = Color(0xFF452B00),
    tertiary = SlateSteel,
    onTertiary = Color.White,
    tertiaryContainer = SlateSteelLight,
    onTertiaryContainer = Color(0xFF0F172A),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = SlateSteel,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorCrimsonContainer,
    onErrorContainer = OnErrorCrimsonContainer
)

val EnolDarkColorScheme = darkColorScheme(
    primary = LubricantAmber,
    onPrimary = EnolNavyDark,
    primaryContainer = SurfaceContainerDark,
    onPrimaryContainer = OnSurfaceDark,
    secondary = LubricantAmber,
    onSecondary = EnolNavyDark,
    secondaryContainer = SurfaceContainerHighDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = SlateSteelLight,
    onTertiary = EnolNavyDark,
    tertiaryContainer = SurfaceContainerDark,
    onTertiaryContainer = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = SlateSteelLight,
    outline = SlateSteelDark,
    outlineVariant = Color(0xFF161F32),
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorCrimsonContainer,
    onErrorContainer = Color(0xFFFCA5A5)
)

val EnolHighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color.White,
    secondary = HighContrastSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF59E0B),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF1E293B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF334155),
    onTertiaryContainer = Color.White,
    background = HighContrastBackgroundLight,
    onBackground = HighContrastOnSurfaceLight,
    surface = HighContrastSurfaceLight,
    onSurface = HighContrastOnSurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color.Black,
    outline = HighContrastOutlineLight,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val EnolHighContrastDarkColorScheme = darkColorScheme(
    primary = HighContrastPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3B82F6),
    onPrimaryContainer = Color.White,
    secondary = HighContrastSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFF59E0B),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFFE2E8F0),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF64748B),
    onTertiaryContainer = Color.White,
    background = HighContrastBackgroundDark,
    onBackground = HighContrastOnSurfaceDark,
    surface = HighContrastSurfaceDark,
    onSurface = HighContrastOnSurfaceDark,
    surfaceVariant = Color(0xFF111827),
    onSurfaceVariant = Color.White,
    outline = HighContrastOutlineDark,
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFFCA5A5),
    onError = Color.Black,
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color.White
)

/* =========================================================================
 * CUSTOM STATUS COLORS TOKENS
 * ========================================================================= */

@Immutable
data class StatusColors(
    val success: Color = SuccessEmerald,
    val onSuccess: Color = OnSuccessEmerald,
    val successContainer: Color = SuccessEmeraldContainer,
    val onSuccessContainer: Color = OnSuccessEmeraldContainer,
    val warning: Color = WarningAmber,
    val onWarning: Color = OnWarningAmber,
    val warningContainer: Color = WarningAmberContainer,
    val onWarningContainer: Color = OnWarningAmberContainer,
    val error: Color = ErrorCrimson,
    val onError: Color = OnErrorCrimson,
    val errorContainer: Color = ErrorCrimsonContainer,
    val onErrorContainer: Color = OnErrorCrimsonContainer,
    val info: Color = InfoSky,
    val onInfo: Color = OnInfoSky,
    val infoContainer: Color = InfoSkyContainer,
    val onInfoContainer: Color = OnInfoSkyContainer
)

val LightStatusColors = StatusColors()

val DarkStatusColors = StatusColors(
    success = Color(0xFF4ADE80),
    onSuccess = Color(0xFF052E16),
    successContainer = Color(0xFF14532D),
    onSuccessContainer = Color(0xFFDCFCE7),
    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFEF3C7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    info = Color(0xFF60A5FA),
    onInfo = Color(0xFF082F49),
    infoContainer = Color(0xFF075985),
    onInfoContainer = Color(0xFFE0F2FE)
)

val HighContrastLightStatusColors = StatusColors(
    success = Color(0xFF15803D),
    onSuccess = Color.White,
    successContainer = Color(0xFFBBF7D0),
    onSuccessContainer = Color(0xFF14532D),
    warning = Color(0xFFB45309),
    onWarning = Color.White,
    warningContainer = Color(0xFFFDE68A),
    onWarningContainer = Color(0xFF78350F),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFECACA),
    onErrorContainer = Color(0xFF7F1D1D),
    info = Color(0xFF0369A1),
    onInfo = Color.White,
    infoContainer = Color(0xFFBAE6FD),
    onInfoContainer = Color(0xFF0C4A6E)
)

val HighContrastDarkStatusColors = StatusColors(
    success = Color(0xFF86EFAC),
    onSuccess = Color.Black,
    successContainer = Color(0xFF166534),
    onSuccessContainer = Color.White,
    warning = Color(0xFFFCD34D),
    onWarning = Color.Black,
    warningContainer = Color(0xFF92400E),
    onWarningContainer = Color.White,
    error = Color(0xFFFCA5A5),
    onError = Color.Black,
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color.White,
    info = Color(0xFF93C5FD),
    onInfo = Color.Black,
    infoContainer = Color(0xFF0369A1),
    onInfoContainer = Color.White
)

val LocalStatusColors = staticCompositionLocalOf { StatusColors() }
