package com.rookhq.monarch.components.datadisplay.leaderboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchLeaderboard, built from primitives and roles only. */
internal object MonarchLeaderboardTokens {
    /** The height of an entry's bar. */
    val BarHeight: Dp = MonarchTokens.Space.space1_5.dp

    /** The fill of an entry's bar: the first data colour, as on the web. */
    fun barColor(colors: MonarchColors): Color = colors.data1

    /** The space between the parts of a row. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the lines inside a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside a row. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The width kept for the rank, so names line up. */
    val RankWidth: Dp = MonarchTokens.Space.space6.dp

    /** The side of the top-three marker. */
    val TopMarker: Dp = MonarchTokens.Space.space6.dp

    /** The width of the text of a placeholder row. */
    val PlaceholderWidth: Dp = (MonarchTokens.Space.space16 * 2).dp

    /** A row: transparent, or for the viewer the accent subtle background with the accent border. */
    fun rowColors(current: Boolean, colors: MonarchColors): MonarchSurfaceColors = if (current) {
        MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentBorder)
    } else {
        MonarchSurfaceColors(Color.Transparent, colors.textDefault, null)
    }

    /** The rank: muted text, or the top-three marker in the subtle accent fill with the strong accent text. */
    fun rankColors(top: Boolean, colors: MonarchColors): MonarchSurfaceColors = if (top) {
        MonarchSurfaceColors(colors.accentSubtle, colors.accentStrong, null)
    } else {
        MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
    }

    /** The secondary line and the "(you)" tag. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The error without a retry. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
