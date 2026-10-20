package com.enol.salesforze.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* =========================================================================
 * ENOL SALESFORZE - 4PT / 8PT SPACING GRID & ELEVATION DIMENSION SYSTEM
 * High-precision dimensional tokens ensuring consistent rhythm and tap targets
 * across field sales, catalog navigation, and warehouse inventory tools.
 * ========================================================================= */

// --- Base Spacing Grid Constants ---
val grid_0_5: Dp = 2.dp
val grid_1: Dp = 4.dp
val grid_1_5: Dp = 6.dp
val grid_2: Dp = 8.dp
val grid_3: Dp = 12.dp
val grid_4: Dp = 16.dp
val grid_5: Dp = 20.dp
val grid_6: Dp = 24.dp
val grid_8: Dp = 32.dp
val grid_10: Dp = 40.dp
val grid_12: Dp = 48.dp

// Minimum accessibility touch target for gloved or outdoor hands
val minTouchTarget: Dp = 48.dp

// --- Elevation Levels ---
val elevation_0: Dp = 0.dp
val elevation_1: Dp = 1.dp
val elevation_2: Dp = 3.dp
val elevation_3: Dp = 6.dp
val elevation_4: Dp = 8.dp
val elevation_5: Dp = 12.dp

/**
 * Spacing data structure available via [LocalSpacing] composition local.
 */
@Immutable
data class Spacing(
    val grid0_5: Dp = grid_0_5,
    val grid1: Dp = grid_1,
    val grid1_5: Dp = grid_1_5,
    val grid2: Dp = grid_2,
    val grid3: Dp = grid_3,
    val grid4: Dp = grid_4,
    val grid5: Dp = grid_5,
    val grid6: Dp = grid_6,
    val grid8: Dp = grid_8,
    val grid10: Dp = grid_10,
    val grid12: Dp = grid_12,

    // Semantic spacing shortcuts
    val defaultScreenPadding: Dp = grid_4,
    val cardContentPadding: Dp = grid_4,
    val listGap: Dp = grid_2,
    val sectionGap: Dp = grid_6,
    val touchTargetMin: Dp = minTouchTarget
)

/**
 * Elevation token structure.
 */
@Immutable
data class Elevations(
    val level0: Dp = elevation_0,
    val level1: Dp = elevation_1,
    val level2: Dp = elevation_2,
    val level3: Dp = elevation_3,
    val level4: Dp = elevation_4,
    val level5: Dp = elevation_5
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalElevations = staticCompositionLocalOf { Elevations() }
