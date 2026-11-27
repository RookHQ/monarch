package com.rookhq.monarch.components.datadisplay.collapsible

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCollapsible, built from primitives and roles only. */
internal object MonarchCollapsibleTokens {
    /** The header is a touch target as a whole. */
    val HeaderMinHeight: Dp = MonarchTouchTarget.Minimum
    val HeaderGap: Dp = MonarchTokens.Space.space1_5.dp
    val ContentTopPadding: Dp = MonarchTokens.Space.space1.dp

    /** The trailing icon turns half a turn while the content is shown, so a chevron points up. */
    const val EXPANDED_ROTATION: Float = 180f

    /** The colour of the label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the trailing icon. */
    fun iconColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
