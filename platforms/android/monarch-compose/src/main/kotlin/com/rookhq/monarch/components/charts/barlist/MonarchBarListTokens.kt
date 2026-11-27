package com.rookhq.monarch.components.charts.barlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchBarList, built from primitives and roles only. */
internal object MonarchBarListTokens {
    /** A row's label and value: 14 regular (the value semibold, tabular), under the 16 title of the card. */
    fun bodyStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

    /** The visible height of a row that cannot be tapped. */
    val RowMinHeight: Dp = MonarchTokens.Space.space8.dp
    val RowGap: Dp = MonarchTokens.Space.space1.dp
    val ContentGap: Dp = MonarchTokens.Space.space3.dp
    val BarVerticalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val IconGap: Dp = MonarchTokens.Space.space2.dp
    val RankMinWidth: Dp = MonarchTokens.Space.space5.dp

    /** The bar's track: a quiet 8dp pill under the label (PREMIUM rule 8). */
    val TrackHeight: Dp = MonarchTokens.Space.space2.dp

    /** The space between the label row and the track. */
    val TrackGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The full-width track behind every bar. */
    fun trackColor(colors: MonarchColors): Color = colors.bgMuted

    /**
     * The filled share of the track: the first data colour, as on the web (`--mn-color-data-1`), not the red
     * default accent. A highlighted row stands out by its semibold label.
     */
    fun barColor(colors: MonarchColors): Color = colors.data1

    fun labelColor(colors: MonarchColors): Color = colors.textDefault

    /** The description, the rank, the footer and the dash of a missing value. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
