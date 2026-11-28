package com.rookhq.monarch.components.files.pageoutline

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** A heading as listed: the heading and its nesting depth, from 0. */
internal data class MonarchPageOutlineEntry(val heading: MonarchPageOutlineHeading, val depth: Int)

/** Component tokens and the pure logic of MonarchPageOutline. */
internal object MonarchPageOutlineTokens {
    const val MIN_LEVEL: Int = 1
    const val MAX_LEVEL: Int = 6

    /** The indent of one nesting step. */
    val IndentStep: Dp = MonarchTokens.Space.space3.dp

    /** The width of the bar at the start of each heading. */
    val BarWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the bar and the text. */
    val BarGap: Dp = MonarchTokens.Space.space3.dp

    /** The vertical padding of a heading row. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the title, the list and the stats. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The headings that are listed and their depth. Blank headings and
     * headings deeper than [maxLevel] are left out; levels are clamped to 1
     * through 6. The depth is the nesting in the heading tree, so a skipped
     * level (a level 3 under a level 1) adds one step, not two.
     */
    fun entries(headings: List<MonarchPageOutlineHeading>, maxLevel: Int): List<MonarchPageOutlineEntry> {
        val stack = ArrayDeque<Int>()
        return headings.mapNotNull { heading ->
            val level = heading.level.coerceIn(MIN_LEVEL, MAX_LEVEL)
            if (heading.text.isBlank() || level > maxLevel) return@mapNotNull null
            while (stack.isNotEmpty() && stack.last() >= level) stack.removeLast()
            val entry = MonarchPageOutlineEntry(heading, stack.size)
            stack.addLast(level)
            entry
        }
    }

    /** The colour of a heading's text. */
    fun textColor(current: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        current -> colors.accentStrong
        else -> colors.textMuted
    }

    /** The colour of the bar at a heading's start. */
    fun barColor(current: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.borderSubtle
        current -> colors.accentStrong
        else -> colors.borderSubtle
    }
}
