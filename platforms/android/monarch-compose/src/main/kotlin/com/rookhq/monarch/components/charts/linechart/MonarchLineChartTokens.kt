package com.rookhq.monarch.components.charts.linechart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTokens
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameTokens
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of the parts of a line chart other than the series.
 *
 * @property grid the value gridlines.
 * @property zero the zero line.
 * @property axisText the tick and x labels.
 * @property reference the dashed reference lines and their labels.
 * @property crosshair the vertical line at the inspected x value.
 * @property dotRing the ring around the dots at the inspected x value, so they stand off the line.
 */
@Immutable
internal data class MonarchLineChartColors(
    val grid: Color,
    val zero: Color,
    val axisText: Color,
    val reference: Color,
    val crosshair: Color,
    val dotRing: Color,
)

/** Component tokens and the pure colour rules of MonarchLineChart. Axis sizes come from the bar chart core. */
internal object MonarchLineChartTokens {
    /** The plot height when the app gives none: the frame's 240dp. */
    val DefaultHeight: Dp = MonarchChartFrameTokens.DefaultHeight

    /** The thickness of a line. */
    val Stroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The radius of a dot on a value: 3dp. */
    val DotRadius: Dp = (MonarchTokens.Space.space1_5 / 2).dp

    /** The radius of a dot at the inspected x value: 5dp. */
    val ActiveDotRadius: Dp = (MonarchTokens.Space.space2_5 / 2).dp

    /** The ring around an inspected dot. */
    val DotRing: Dp = MonarchTokens.Space.space0_5.dp

    /** The dash of a dashed series. */
    val SeriesDash: Dp = MonarchTokens.Space.space1_5.dp

    /** The space between an axis label and the plot. */
    val AxisGap: Dp = MonarchBarChartTokens.AxisGap

    /** The space kept at the end edge and the top of the plot. */
    val EdgePad: Dp = MonarchBarChartTokens.EdgePad

    /** The space between the plot and the inspection row. */
    val InspectionGap: Dp = MonarchBarChartTokens.InspectionGap

    /**
     * How far the area fill moves from the series colour towards the surface: an opaque, lighter (or, in dark
     * themes, darker) shade of the same hue. Monarch never fills with transparency.
     */
    const val AREA_MIX: Float = 0.8f

    /**
     * The colours around the series: the bar chart's subtle gridlines, strong zero line, muted labels and subtle
     * reference text; the crosshair in the strong border colour and the dot ring in the surface colour.
     */
    fun colors(colors: MonarchColors): MonarchLineChartColors {
        val axis = MonarchBarChartTokens.colors(colors)
        return MonarchLineChartColors(
            grid = axis.grid,
            zero = axis.zero,
            axisText = axis.axisText,
            reference = axis.reference,
            crosshair = colors.borderStrong,
            dotRing = colors.bgSurface,
        )
    }

    /** The palette slot of the series at [index]: its own, or the one of its position, as in the legend. */
    fun slot(color: MonarchChartColor?, index: Int): MonarchChartColor = MonarchChartLegendTokens.slot(color, index)

    /** The colour of a series' line and dots: its slot's data colour. */
    fun lineColor(slot: MonarchChartColor, colors: MonarchColors): Color = MonarchChartLegendTokens.color(slot, colors)

    /** The fill under a series' line: its data colour mixed [AREA_MIX] of the way towards the surface colour. */
    fun areaColor(slot: MonarchChartColor, colors: MonarchColors): Color =
        lerp(lineColor(slot, colors), colors.bgSurface, AREA_MIX)
}
