package com.rookhq.monarch.components.layout.pricingsection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchPricingSection, built from primitives and roles only. */
internal object MonarchPricingSectionTokens {
    val Gap: Dp = MonarchTokens.Space.space6.dp
    val CardGap: Dp = MonarchTokens.Space.space4.dp
    val ComparisonGap: Dp = MonarchTokens.Space.space4.dp

    /** The least width of a comparison column at font scale 1; the grid scrolls sideways when it needs more. */
    val ComparisonColumn: Dp =
        (MonarchTokens.Space.space24 + MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp

    /** The note under the plans is muted. */
    fun note(colors: MonarchColors): Color = colors.textMuted
}
