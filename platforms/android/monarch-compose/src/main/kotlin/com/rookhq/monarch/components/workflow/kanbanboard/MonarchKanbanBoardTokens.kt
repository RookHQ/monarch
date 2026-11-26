package com.rookhq.monarch.components.workflow.kanbanboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchKanbanBoard: sizes, colours and the work-in-progress state. */
internal object MonarchKanbanBoardTokens {
    /** The narrowest a column gets; a board shows as many columns side by side as fit, at least one. */
    val MinColumnWidth: Dp = MonarchTokens.Space.space24.dp * 5 / 2

    /** A board of this many columns or fewer keeps every column composed. */
    const val COMPOSED_COLUMNS: Int = 6

    /** The space between columns. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The space inside a column. */
    val ColumnPadding: Dp = MonarchTokens.Space.space2.dp

    /** The space between cards, and between the header and the cards. */
    val CardGap: Dp = MonarchTokens.Space.space2.dp

    /** The space between the parts of a card or a header. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a card. */
    val CardPadding: Dp = MonarchTokens.Space.space1.dp

    /** The space inside the text of an empty column. */
    val EmptyPadding: Dp = MonarchTokens.Space.space4.dp

    /** The border width of a card and a column. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The height of the line that shows where a dragged card will land. */
    val InsertionHeight: Dp = MonarchTokens.Space.space0_5.dp

    /** How close to the start or end of the board the finger turns the page while dragging. */
    val EdgeZone: Dp = MonarchTokens.Space.space8.dp

    /** The most a column scrolls in one frame while the finger is near its top or bottom. */
    val AutoScrollStep: Dp = MonarchTokens.Space.space4.dp

    /** How long the finger stays at the edge of the board before the next column is shown. */
    const val EDGE_DWELL_MILLIS: Long = 600L

    /** The z order of the card that follows the finger. */
    const val OVERLAY_Z: Float = 1f

    /** How a column's count compares with its work-in-progress limit. */
    enum class Wip { None, Under, At, Over }

    /** The state of a column of [count] cards with [limit]. */
    fun wip(count: Int, limit: Int?): Wip = when {
        limit == null -> Wip.None
        count > limit -> Wip.Over
        count == limit -> Wip.At
        else -> Wip.Under
    }

    /** The colour of a column's count: warning at the limit, danger over it. The count also says it in words. */
    fun countColor(wip: Wip, colors: MonarchColors): Color = when (wip) {
        Wip.Over -> colors.dangerStrong
        Wip.At -> colors.warningStrong
        else -> colors.textMuted
    }

    /** The fill of a column. */
    fun column(colors: MonarchColors): Color = colors.bgSubtle

    /** The border of a column: the danger border while it refuses the card being dragged. */
    fun columnBorder(blocked: Boolean, colors: MonarchColors): Color =
        if (blocked) colors.dangerBorder else colors.borderSubtle

    /** The fill of a card. */
    fun card(colors: MonarchColors): Color = colors.bgSurface

    /** The border of a card: the accent border on the copy that follows the finger. */
    fun cardBorder(overlay: Boolean, colors: MonarchColors): Color =
        if (overlay) colors.accentBorder else colors.borderSubtle

    /** The fill of the place a dragged card left. */
    fun placeholder(colors: MonarchColors): Color = colors.bgMuted

    /** The line that shows where a dragged card will land. */
    fun insertion(colors: MonarchColors): Color = colors.accentSolid

    /** The colour of quiet text: the empty text and the refusal note. */
    fun meta(colors: MonarchColors): Color = colors.textMuted

    /** The key of a card in a column's lazy list. */
    fun cardKey(key: String): String = "c:$key"
}
