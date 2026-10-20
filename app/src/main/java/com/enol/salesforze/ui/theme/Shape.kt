package com.enol.salesforze.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* =========================================================================
 * ENOL SALESFORZE - INDUSTRIAL SHAPE TOKENS
 * Rugged and ergonomic rounded corners optimized for tactile field usage.
 * ========================================================================= */

val EnolShapes = Shapes(
    // Extra Small: Badges, tiny tags, precision indicators
    extraSmall = RoundedCornerShape(4.dp),

    // Small: Field Action Buttons, Filter Chips, Metric Pills
    small = RoundedCornerShape(8.dp),

    // Medium: Order Cards, Customer Visit Cards, Dialogs, Input Fields
    medium = RoundedCornerShape(12.dp),

    // Large: Persistent Bottom Sheets, Modal Drawers, Large Section Containers
    large = RoundedCornerShape(16.dp),

    // Extra Large: Floating Action Buttons (Extended), Hero Summary Sheets
    extraLarge = RoundedCornerShape(28.dp)
)
