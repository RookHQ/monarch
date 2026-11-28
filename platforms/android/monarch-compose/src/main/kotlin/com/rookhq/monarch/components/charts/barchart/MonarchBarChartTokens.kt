package com.rookhq.monarch.components.charts.barchart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameTokens
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of the parts of a bar chart other than the bars.
 *
 * @property grid the value gridlines.
 * @property zero the zero line.
 * @property axisText the tick and category labels, and the value labels.
 * @property reference the dashed reference lines and their labels.
 * @property band the shaded band of the inspected category.
 */
@Immutable
internal data class MonarchBarChartColors(
    val grid: Color,
    val zero: Color,
    val axisText: Color,
    val reference: Color,
    val band: Color,
)

/** Component tokens and the pure colour rules of MonarchBarChart. */
internal object MonarchBarChartTokens {
    /** The widest a single bar gets: 40dp. The web allows 56dp; a thin bar reads quieter on a phone (rule 8). */
    val MaxBar: Dp = MonarchTokens.Space.space10.dp

    /** How far a bar fill moves from its data colour towards the surface: a soft fill, not a saturated slab. */
    const val BAR_SOFTEN: Float = 0.2f

    /** The radius of the rounded end of a bar. */
    val BarRadius: Dp = MonarchTokens.Radius.xs.dp

    /** The space between grouped bars. */
    val BarGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between an axis label and the plot, and between a bar and its value label. */
    val AxisGap: Dp = MonarchTokens.Space.space2.dp

    /** The space kept at the end edge and the top of the plot. */
    val EdgePad: Dp = MonarchTokens.Space.space2.dp

    /**
     * The least space between two value ticks of a column chart. The web counts one tick per 48px of a plot that
     * is about a quarter shorter than this one (its 240px frame includes the margins), so the ticks read the same.
     */
    val TickSpacingVertical: Dp = MonarchTokens.Space.space16.dp

    /** The least space between two value ticks of a horizontal bar chart. */
    val TickSpacingHorizontal: Dp = MonarchTokens.Space.space20.dp

    /** The height given to each category of a horizontal bar chart. */
    val CategoryRow: Dp = MonarchTokens.Space.space8.dp

    /** The least height of a horizontal bar chart: 120dp. */
    val MinHorizontalHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp

    /** The space between a reference line's label and its dashed line. */
    val LabelLift: Dp = MonarchTokens.Space.space1.dp

    /** The width of gridlines, the zero line and reference lines. */
    val LineWidth: Dp = MonarchTokens.Space.px.dp

    /** The dash of a reference line. */
    val Dash: Dp = MonarchTokens.Space.space1.dp

    /** The space between the plot and the inspection row. */
    val InspectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The largest share of the width the category labels of horizontal bars take. */
    const val CATEGORY_LABEL_SHARE: Float = 0.4f

    /**
     * The height of the plot when the app gives none: the frame's 240dp for columns; one row per category plus
     * one, at least 120dp, for horizontal bars.
     */
    fun height(orientation: MonarchBarChartOrientation, categories: Int): Dp = when (orientation) {
        MonarchBarChartOrientation.Vertical -> MonarchChartFrameTokens.DefaultHeight
        MonarchBarChartOrientation.Horizontal -> maxOf(MinHorizontalHeight, CategoryRow * (categories + 1))
    }

    /**
     * The colours around the bars: subtle gridlines, a strong zero line, muted labels, subtle text for
     * reference lines and the muted background for the inspected band.
     */
    fun colors(colors: MonarchColors): MonarchBarChartColors = MonarchBarChartColors(
        grid = colors.borderSubtle,
        zero = colors.borderStrong,
        axisText = colors.textMuted,
        reference = colors.textSubtle,
        band = colors.bgMuted,
    )

    /** The palette slot of the series at [index]: its own, or the one of its position, as in the legend. */
    fun slot(color: MonarchChartColor?, index: Int): MonarchChartColor = MonarchChartLegendTokens.slot(color, index)

    /**
     * The colour of a bar: its slot's data colour softened [BAR_SOFTEN] of the way towards the surface, or, when
     * another category is highlighted, the disabled text colour. Monarch never dims with transparency.
     */
    fun barColor(slot: MonarchChartColor, dimmed: Boolean, colors: MonarchColors): Color =
        if (dimmed) {
            colors.textDisabled
        } else {
            lerp(MonarchChartLegendTokens.color(slot, colors), colors.bgSurface, BAR_SOFTEN)
        }
}
