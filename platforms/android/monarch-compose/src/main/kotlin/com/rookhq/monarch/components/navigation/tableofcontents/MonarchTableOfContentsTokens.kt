package com.rookhq.monarch.components.navigation.tableofcontents

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of one entry. */
internal data class MonarchTableOfContentsEntryColors(val text: Color, val line: Color)

/** A heading's place in the scrolling list, for the spy: its item index and, when visible, its top. */
internal data class MonarchTableOfContentsHeading(val id: String, val index: Int, val top: Int?)

/** Component tokens and the pure logic of MonarchTableOfContents. */
internal object MonarchTableOfContentsTokens {
    /** The share of the viewport, from the top, in which a heading becomes the current section. */
    const val ACTIVE_ZONE: Float = 0.4f

    /** The width of the line beside each entry. */
    val LineWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The indent of one level of nesting. */
    val IndentStep: Dp = MonarchTokens.Space.space4.dp

    /** The space between the line and the text. */
    val TextPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space above and below the text of an entry. */
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the title and the entries. */
    val TitleGap: Dp = MonarchTokens.Space.space2.dp

    /** How many steps an entry is indented: its level below the shallowest level in the list. */
    fun depth(level: Int, items: List<MonarchTableOfContentsItem>): Int {
        val top = items.minOfOrNull { it.level } ?: level
        return (level - top).coerceAtLeast(0)
    }

    /** The colours of an entry: the default text and an accent line when current, muted with a subtle line not. */
    fun entryColors(current: Boolean, enabled: Boolean, colors: MonarchColors): MonarchTableOfContentsEntryColors =
        MonarchTableOfContentsEntryColors(
            text = when {
                !enabled -> colors.textDisabled
                current -> colors.textDefault
                else -> colors.textMuted
            },
            line = if (current && enabled) colors.accentSolid else colors.borderSubtle,
        )

    /**
     * The current section: the first heading whose top is between [inset] and 40% of the viewport, else
     * the last heading scrolled above [inset], else none. Headings above the first visible item count as
     * scrolled past; those below the last visible item have not been reached.
     */
    fun current(
        headings: List<MonarchTableOfContentsHeading>,
        firstVisibleIndex: Int,
        viewport: Int,
        inset: Int,
    ): String? {
        val line = inset + ((viewport - inset) * ACTIVE_ZONE).toInt()
        val sorted = headings.sortedBy { it.index }
        sorted.firstOrNull { it.top != null && it.top in inset..line }?.let { return it.id }
        return sorted.lastOrNull { heading ->
            val top = heading.top
            if (top != null) top < inset else heading.index < firstVisibleIndex
        }?.id
    }
}
