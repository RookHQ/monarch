package com.rookhq.monarch.components.navigation.megamenu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchMegaMenu, built from primitives and roles only. */
internal object MonarchMegaMenuTokens {
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space4.dp
    val RowPaddingVertical: Dp = MonarchTokens.Space.space3.dp
    val LinkPaddingVertical: Dp = MonarchTokens.Space.space2.dp
    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val PanelPaddingBottom: Dp = MonarchTokens.Space.space4.dp
    val IconTile: Dp = (MonarchTokens.Space.space8 + MonarchTokens.Space.space1).dp
    val IconGap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val BadgeGap: Dp = MonarchTokens.Space.space2.dp
    val FeaturedPadding: Dp = MonarchTokens.Space.space5.dp
    val FeaturedGap: Dp = MonarchTokens.Space.space2.dp

    /** The tile behind a link icon: the subtle accent, with the icon in the strong accent. */
    fun iconTile(colors: MonarchColors): Color = colors.accentSubtle
    fun iconTint(colors: MonarchColors): Color = colors.accentStrong

    /** The fill of an expanded entry's row, so the open group reads as one. */
    fun expandedRow(expanded: Boolean, colors: MonarchColors): Color =
        if (expanded) colors.bgSubtle else Color.Transparent

    /** The featured card sits on the subtle background with a subtle border; its icon tile on the surface. */
    fun featuredFill(colors: MonarchColors): Color = colors.bgSubtle
    fun featuredBorder(colors: MonarchColors): Color = colors.borderSubtle
    fun featuredTile(colors: MonarchColors): Color = colors.bgSurface
}
