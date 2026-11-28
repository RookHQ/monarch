package com.rookhq.monarch.components.layout.divider

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchDivider, built from primitives only. */
internal object MonarchDividerTokens {
    val Thickness: Dp = MonarchTokens.Space.px.dp

    /** The colour of the line. */
    fun color(variant: MonarchDividerVariant, colors: MonarchColors): Color = when (variant) {
        MonarchDividerVariant.Default -> colors.borderDefault
        MonarchDividerVariant.Subtle -> colors.borderSubtle
    }
}
