package com.rookhq.monarch.components.feedback.loadingstate

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and rules of MonarchLoadingState, built from primitives and roles only. */
internal object MonarchLoadingStateTokens {
    val RowGap: Dp = MonarchTokens.Space.space4.dp
    val LineGap: Dp = MonarchTokens.Space.space2.dp
    val ItemGap: Dp = MonarchTokens.Space.space3.dp
    val AvatarSize: Dp = MonarchTokens.Space.space8.dp
    val CardHeight: Dp = MonarchTokens.Space.space24.dp
    val SpinnerGap: Dp = MonarchTokens.Space.space3.dp

    /** The default number of rows or cards. */
    const val DEFAULT_ROWS: Int = 4

    /** The width of the short value of the rows layout, as a share of the row. */
    const val META_WIDTH: Float = 0.18f

    /** The widths of the title and the paragraph lines of the detail layout, as shares of the width. */
    val DetailWidths: List<Float> = listOf(0.4f, 0.9f, 0.75f, 0.6f)

    /** The number of placeholder rows drawn for [rows]: the whole part, never below zero. */
    fun rowCount(rows: Int): Int = rows.coerceAtLeast(0)

    /**
     * The widths of the two text lines of row [index], as shares of the width,
     * varied from row to row as on the web: 55–84% and 30–54%.
     */
    @Suppress("MagicNumber")
    fun lineWidths(index: Int): Pair<Float, Float> =
        (55 + (index * 17) % 30) / 100f to (30 + (index * 11) % 25) / 100f
}
