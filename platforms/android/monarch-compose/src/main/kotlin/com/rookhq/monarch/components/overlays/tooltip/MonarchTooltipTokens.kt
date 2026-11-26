package com.rookhq.monarch.components.overlays.tooltip

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchTooltip, from the web's `tooltip.css`. */
internal object MonarchTooltipTokens {
    /** The web's 18rem. */
    val MaxWidth: Dp = (MonarchTokens.Space.space24 * 3).dp

    /** The distance between the control and the label. */
    val Gap: Dp = MonarchTokens.Space.space1.dp
}
