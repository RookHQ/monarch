package com.rookhq.monarch.components.datagrid.tabletoolbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and rules of MonarchTableToolbar, built from primitives and roles only. */
internal object MonarchTableToolbarTokens {
    /** The space between the controls and between the lines. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** From this width the filters and the controls share one line (the `sm` breakpoint). */
    val WideMinWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** Whether the bulk selection bar replaces the search and the filters. */
    fun selecting(selectedCount: Int, canClear: Boolean): Boolean = canClear && selectedCount > 0

    /**
     * Whether the start part (search, filters or the bulk bar) and the end part (count and controls) share one
     * line: the toolbar is at least [WideMinWidth] wide and the end part takes at most half of it.
     */
    fun wide(maxWidth: Dp, endWidth: Dp): Boolean = maxWidth >= WideMinWidth && endWidth * 2 <= maxWidth

    /** The result count is muted caption text. */
    fun countText(colors: MonarchColors): Color = colors.textMuted
}
