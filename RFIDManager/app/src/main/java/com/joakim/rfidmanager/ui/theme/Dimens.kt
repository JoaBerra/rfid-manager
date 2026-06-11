package com.joakim.rfidmanager.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Fas 3 locked breathing room / spacing values.
 *
 * Directly implements the measurable rules from:
 * - [[Fas3-Navigation-Spacing-Design]]
 * - Kundrelationer-och-Acceptans UAT-kriterier for "andrum"
 *
 * All new cards, lists, radar, stat cards etc. shall use these.
 * Reference the 3 Figma images (fas2-*.jpg) for visual validation.
 */
object Dimens {
    // Card / container inner padding (min 16 dp rule)
    val cardPadding = 16.dp
    val cardPaddingHorizontal = 16.dp

    // Between list items / rows (min 12 dp, we use 12 for list, 8 inside cards)
    val listItemSpacing = 12.dp
    val sectionSpacing = 16.dp
    val smallGap = 8.dp

    // Radar / live view height cap (35-40 % of available height on typical phone)
    // Use with Modifier.heightIn(max = Dimens.radarMaxHeight)
    val radarMaxHeight = 280.dp

    // Touch targets
    val minTouchTarget = 48.dp

    // General screen horizontal padding when not inside a Card
    val screenHorizontalPadding = 16.dp
}