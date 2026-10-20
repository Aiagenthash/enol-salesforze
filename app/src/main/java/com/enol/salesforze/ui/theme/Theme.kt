package com.enol.salesforze.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* =========================================================================
 * ENOL SALESFORZE - MATERIAL 3 THEME ENTRYPOINT
 * Handles industrial lubricant branding, light/dark themes, high-contrast outdoor
 * field readability mode, status bars, and custom design tokens.
 * ========================================================================= */

@Composable
fun EnolSalesforzeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> EnolHighContrastDarkColorScheme
        highContrast && !darkTheme -> EnolHighContrastLightColorScheme
        darkTheme -> EnolDarkColorScheme
        else -> EnolLightColorScheme
    }

    val statusColors = when {
        highContrast && darkTheme -> HighContrastDarkStatusColors
        highContrast && !darkTheme -> HighContrastLightStatusColors
        darkTheme -> DarkStatusColors
        else -> LightStatusColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Harmonize system status bar and navigation bar with current surface/background
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalElevations provides Elevations(),
        LocalStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EnolTypography,
            shapes = EnolShapes,
            content = content
        )
    }
}

/**
 * Accessor extensions on [MaterialTheme] for consistent token resolution.
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

val MaterialTheme.elevations: Elevations
    @Composable
    @ReadOnlyComposable
    get() = LocalElevations.current

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
