package com.rookhq.monarch.components.collaboration.callstatspanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchCallStatsPanel. */
internal object MonarchCallStatsPanelTokens {
    /** The space between the usage, the people and the footnote. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between two people. */
    val PeerGap: Dp = MonarchTokens.Space.space3.dp

    /** The space inside one block. */
    val InnerGap: Dp = MonarchTokens.Space.space1.dp

    /** The tone of the badge of a grade. */
    fun tone(quality: MonarchCallStatsPanelQuality): MonarchTone = when (quality) {
        MonarchCallStatsPanelQuality.Good -> MonarchTone.Success
        MonarchCallStatsPanelQuality.Fair -> MonarchTone.Warning
        MonarchCallStatsPanelQuality.Poor -> MonarchTone.Danger
        MonarchCallStatsPanelQuality.Unknown -> MonarchTone.Neutral
    }

    /** The usage line, the low data note, the no-peers text and the footnote: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The widths of the columns of the table of people. */
    const val NameWeight: Float = 1.3f
    const val ValueWeight: Float = 1f
    val BarsWidth: Dp = MonarchTokens.Space.space4.dp
    val BarsHeight: Dp = MonarchTokens.Space.space3.dp
    val BarWidth: Dp = MonarchTokens.Space.space1.dp
    val BarGap: Dp = MonarchTokens.Space.space0_5.dp
    const val LinkWeight: Float = 1.2f
    private const val GOOD_BARS = 3

    /** How many of the three link bars are filled; none for an unknown grade. */
    fun bars(quality: MonarchCallStatsPanelQuality): Int = when (quality) {
        MonarchCallStatsPanelQuality.Good -> GOOD_BARS
        MonarchCallStatsPanelQuality.Fair -> GOOD_BARS - 1
        MonarchCallStatsPanelQuality.Poor -> 1
        MonarchCallStatsPanelQuality.Unknown -> 0
    }

    /** The colour of the filled link bars. */
    fun barColor(quality: MonarchCallStatsPanelQuality, colors: MonarchColors): Color = when (quality) {
        MonarchCallStatsPanelQuality.Good -> colors.successStrong
        MonarchCallStatsPanelQuality.Fair -> colors.warningSolid
        MonarchCallStatsPanelQuality.Poor -> colors.dangerStrong
        MonarchCallStatsPanelQuality.Unknown -> colors.textMuted
    }
}
