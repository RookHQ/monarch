package dev.monarchui.components.files.pagetree

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens, colours and reorder rules of MonarchPageTree, built from primitives and roles only. */
internal object MonarchPageTreeTokens {
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum
    val RowPadding: Dp = MonarchTokens.Space.space2.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val GroupGap: Dp = MonarchTokens.Space.space4.dp
    val CurrentBarWidth: Dp = MonarchTokens.Space.space1.dp

    /**
     * The tallest a group's reorderable list may grow. The tree scrolls as a whole, so each group's lazy list
     * needs a finite bound; it is far above the height of any real group, which takes the height of its pages.
     */
    val GroupListMaxHeight: Dp = MonarchTokens.Breakpoint.xl2.dp * 8

    /** The fill of a page row: the subtle accent for the open page. */
    fun rowFill(current: Boolean, colors: MonarchColors): Color =
        if (current) colors.accentSubtle else Color.Transparent

    /** The bar at the start of the open page. */
    fun currentBar(current: Boolean, colors: MonarchColors): Color =
        if (current) colors.accentSolid else Color.Transparent

    /** The title of a page: strong accent when open, default otherwise, disabled when the tree is disabled. */
    fun titleColor(current: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        current -> colors.accentStrong
        else -> colors.textDefault
    }

    /** Page numbers, group counts and the empty group text use the subtle and muted text roles. */
    fun numberColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textSubtle else colors.textDisabled

    /** Group headings use the muted text role. */
    fun headingColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
