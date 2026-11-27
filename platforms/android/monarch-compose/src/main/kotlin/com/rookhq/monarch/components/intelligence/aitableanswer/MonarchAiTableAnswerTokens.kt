package com.rookhq.monarch.components.intelligence.aitableanswer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchNumberText
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the colours of MonarchAiTableAnswer. */
internal object MonarchAiTableAnswerTokens {
    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp
    val EyebrowGap: Dp = MonarchTokens.Space.space1.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp
    val CellHorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val CellVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val SortIconGap: Dp = MonarchTokens.Space.space1.dp
    val SortIconSize: Dp = MonarchTokens.Space.space3.dp
    val PlaceholderHeight: Dp = MonarchTokens.Space.space3.dp
    val CellPadding: PaddingValues = PaddingValues(horizontal = CellHorizontalPadding, vertical = CellVerticalPadding)
    val Zero: Dp = MonarchTokens.Space.space0.dp
    const val DEFAULT_VISIBLE_ROWS: Int = 200

    /** Shown in an empty cell; TalkBack reads the empty label instead. */
    const val EMPTY_MARK: String = MonarchNumberText.EMPTY_MARK

    /** The provenance label colour. */
    fun eyebrowColor(colors: MonarchColors): Color = colors.aiStrong

    /** The header text colour. */
    fun headerColor(colors: MonarchColors): Color = colors.textMuted

    /** The fill of the header row. */
    fun headerBackground(colors: MonarchColors): Color = colors.bgSubtle

    /** The table's border and row separators. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** The colour of a cell: muted for an empty one. */
    fun cellColor(blank: Boolean, colors: MonarchColors): Color = if (blank) colors.textSubtle else colors.textDefault

    /** The colour of a sort chevron: strong when it is the active direction, subtle otherwise. */
    fun chevronColor(active: Boolean, colors: MonarchColors): Color =
        if (active) colors.textDefault else colors.textSubtle
}
