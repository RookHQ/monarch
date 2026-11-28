package com.rookhq.monarch.components.intelligence.aicontextwindowmeter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.stackedproportionbar.MonarchStackedProportionBarSegment
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The sizes, colours and pure rules of MonarchAiContextWindowMeter. */
internal object MonarchAiContextWindowMeterTokens {
    /** The gap between the header, the bar, the notice and the button. */
    val Gap = MonarchTokens.Space.space2.dp

    /** The gap between the heading and the summary when they share a line. */
    val HeaderGap = MonarchTokens.Space.space2.dp

    /** The thickness of the single bar. */
    val BarHeight = MonarchTokens.Space.space2.dp

    /** The width of the bar of the compact meter. */
    val CompactBarWidth = MonarchTokens.Space.space16.dp

    /** The gap between the bar and the share in the compact meter. */
    val CompactGap = MonarchTokens.Space.space2.dp

    /** The padding inside the compact meter. */
    val CompactPadding = MonarchTokens.Space.space2.dp

    /** The tokens in the window, never below zero. */
    fun used(used: Long): Long = used.coerceAtLeast(0L)

    /** The share of the limit used, from 0 to 1. A limit of zero or less is 0. */
    fun share(used: Long, limit: Long): Float =
        if (limit <= 0L) 0f else (used(used).toDouble() / limit.toDouble()).coerceIn(0.0, 1.0).toFloat()

    /** The fill of the single bar: the accent below the warning share, then warning, then danger. */
    fun fill(state: MonarchAiContextWindowState, colors: MonarchColors): Color = when (state) {
        MonarchAiContextWindowState.Ok -> colors.accentSolid
        MonarchAiContextWindowState.Warning -> colors.warningSolid
        MonarchAiContextWindowState.Full -> colors.dangerSolid
    }

    /** The track behind the fill. */
    fun track(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the share in the compact meter: muted when ok, the strong tone colour otherwise. */
    fun compactText(state: MonarchAiContextWindowState, colors: MonarchColors): Color = when (state) {
        MonarchAiContextWindowState.Ok -> colors.textMuted
        MonarchAiContextWindowState.Warning -> colors.warningStrong
        MonarchAiContextWindowState.Full -> colors.dangerStrong
    }

    /** The tone of the notice, or `null` when there is none. */
    fun noticeTone(state: MonarchAiContextWindowState): MonarchTone? = when (state) {
        MonarchAiContextWindowState.Ok -> null
        MonarchAiContextWindowState.Warning -> MonarchTone.Warning
        MonarchAiContextWindowState.Full -> MonarchTone.Danger
    }

    /**
     * The segments of the bar: one per part in palette order (the sixth and later neutral), then [otherLabel] for
     * the tokens no part covers, when there are any.
     */
    fun segments(
        used: Long,
        breakdown: List<MonarchAiContextWindowPart>,
        otherLabel: String,
    ): List<MonarchStackedProportionBarSegment> {
        val parts = breakdown.mapIndexed { index, part ->
            MonarchStackedProportionBarSegment(
                key = part.id ?: "part-$index",
                label = part.label,
                value = part.tokens.coerceAtLeast(0L).toDouble(),
            )
        }
        val covered = breakdown.sumOf { it.tokens.coerceAtLeast(0L) }
        val rest = used(used) - covered
        if (rest <= 0L) return parts
        return parts + MonarchStackedProportionBarSegment(
            key = OtherKey,
            label = otherLabel,
            value = rest.toDouble(),
            color = MonarchChartColor.Neutral,
        )
    }

    /** The whole of the segmented bar: the limit, or the parts when they add up to more, so every part shows. */
    fun total(used: Long, limit: Long, breakdown: List<MonarchAiContextWindowPart>): Double =
        maxOf(limit.coerceAtLeast(0L), used(used), breakdown.sumOf { it.tokens.coerceAtLeast(0L) }).toDouble()

    const val OtherKey = "monarch-other"
}
