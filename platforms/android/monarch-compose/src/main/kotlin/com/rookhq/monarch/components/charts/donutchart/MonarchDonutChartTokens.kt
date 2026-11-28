package com.rookhq.monarch.components.charts.donutchart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a donut other than the slices.
 *
 * @property track the ring under the slices, seen in the gaps.
 * @property centerValue the large text in the middle.
 * @property centerLabel the small text under it.
 */
@Immutable
internal data class MonarchDonutChartColors(val track: Color, val centerValue: Color, val centerLabel: Color)

/** Component tokens and the pure colour rules of MonarchDonutChart. */
internal object MonarchDonutChartTokens {
    /** The outer diameter when the app gives none: 160dp, as on the web. */
    val DefaultSize: Dp = (MonarchTokens.Space.space20 * 2).dp

    /** The thinnest ring. */
    val MinThickness: Dp = MonarchTokens.Space.space1.dp

    /** The gap between slices, along the ring. */
    val Gap: Dp = MonarchTokens.Space.space0_5.dp

    /** The shortest arc a tiny slice keeps. */
    val MinArc: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between the ring and the centre text. */
    val CenterInset: Dp = MonarchTokens.Space.space1.dp

    /** The default thickness: 22% of the size, as on the web. */
    const val THICKNESS_SHARE: Float = 0.22f

    /** The smallest the centre text shrinks to before it is cut, as a share of its size. */
    const val CENTER_MIN_SCALE: Float = 0.5f

    /** The thickness of the ring: [thickness], or 22% of [size], kept between 4dp and half the size. */
    fun thickness(size: Dp, thickness: Dp?): Dp =
        (thickness ?: (size * THICKNESS_SHARE)).coerceIn(MinThickness, maxOf(MinThickness, size / 2))

    /** The muted background for the track, the default text colour for the centre value, muted for its label. */
    fun colors(colors: MonarchColors): MonarchDonutChartColors =
        MonarchDonutChartColors(
            track = colors.bgMuted,
            centerValue = colors.textDefault,
            centerLabel = colors.textMuted,
        )

    /** The colour of a slice: its slot's data colour; Other is neutral. */
    fun sliceColor(slot: MonarchChartColor, colors: MonarchColors): Color = MonarchChartLegendTokens.color(slot, colors)
}
