package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.heatmap.HeatmapLevels
import dev.monarchui.components.charts.heatmap.MonarchHeatmapTokens
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The outline of a day: none, a hairline, or a thick ring for today and the inspected day. */
internal enum class CalendarHeatmapOutline { None, Thin, Thick }

/**
 * How one day is drawn.
 *
 * @property fill the background of the day; transparent for a day without data.
 * @property outline the colour of the outline, used when [outlineWidth] is not `None`.
 * @property outlineWidth hairline or thick.
 * @property dashed whether the outline is dashed: a non-working day.
 * @property marker the colour of the marker dot, or `null` without one.
 * @property onChip whether text in the day sits on a surface chip, on the two strongest levels.
 */
internal class CalendarHeatmapDayStyle(
    val fill: Color,
    val outline: Color,
    val outlineWidth: CalendarHeatmapOutline,
    val dashed: Boolean,
    val marker: Color?,
    val onChip: Boolean,
)

/** Component tokens and the pure rules of MonarchCalendarHeatmap. */
internal object MonarchCalendarHeatmapTokens {
    /**
     * The side of a day in the year layout: [YearCell], or more when a weekday name ([labelHeight] tall) would
     * not fit the two rows and the gap it labels.
     */
    fun yearRow(labelHeight: Dp): Dp = maxOf(YearCell, (labelHeight - Gap) / 2)

    /** The weekday names of the month layout: [short] when the widest fits a [cell], else [narrow]. */
    fun weekdayNames(short: List<String>, narrow: List<String>, widest: Dp, cell: Dp): List<String> =
        if (widest <= cell) short else narrow

    /** The side of a day in the year layout: 14dp, as on the web and iOS. */
    val YearCell: Dp = (MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5).dp

    /** The largest side of a day in the month layout: 56dp, as on the web. */
    val MonthMaxCell: Dp = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp

    /** The month layout writes values from this cell side up, as on the web. */
    val MonthValueCell: Dp = (MonarchTokens.Space.space10 + MonarchTokens.Space.space1).dp

    /** The gap between days. */
    val Gap: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between the weekday names and the days, and between month names. */
    val LabelGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding inside a month day. */
    val MonthPadding: Dp = MonarchTokens.Space.space1.dp

    /** The padding of the chip behind text on a strong day. */
    val ChipPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** The corner radius of a year day and of a month day. */
    val YearRadius: Dp = MonarchTokens.Space.space0_5.dp
    val MonthRadius: Dp = MonarchTokens.Radius.sm.dp

    /** The hairline outline and the thick ring. */
    val ThinStroke: Dp = MonarchTokens.Space.px.dp
    val ThickStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The dash of a non-working day's outline. */
    val Dash: Dp = MonarchTokens.Space.space0_5.dp

    /** The diameter of the marker dot, and its inset from the top end corner. */
    val Marker: Dp = MonarchTokens.Space.space1.dp
    val MarkerInset: Dp = MonarchTokens.Space.space0_5.dp

    /** The largest share of a day's side taken by its corner radius or the marker's inset: a quarter. */
    const val CORNER_SHARE: Float = 0.25f

    /** The space between the grid, the details and the legend. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest kept height: 120dp for a year and 200dp for a month, as on the web. */
    val YearMinHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
    val MonthMinHeight: Dp = (MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space2).dp

    /**
     * How a day is drawn, from what is true of it. A non-working day without a value above zero is quiet: the
     * subtle background with a dashed outline. The inspected day's ring wins over today's; both stay in the text.
     */
    @Suppress("LongParameterList")
    fun dayStyle(
        level: Int?,
        inactive: Boolean,
        today: Boolean,
        inspected: Boolean,
        marker: Boolean,
        slot: MonarchChartColor,
        colors: MonarchColors,
    ): CalendarHeatmapDayStyle {
        val quiet = inactive && (level == null || level == 0)
        val fill = if (quiet) colors.bgSubtle else MonarchHeatmapTokens.levelColor(level, slot, colors)
        val (outline, width) = when {
            inspected -> colors.textDefault to CalendarHeatmapOutline.Thick
            today -> colors.accentSolid to CalendarHeatmapOutline.Thick
            quiet -> colors.borderDefault to CalendarHeatmapOutline.Thin
            level == null -> colors.borderSubtle to CalendarHeatmapOutline.Thin
            else -> Color.Transparent to CalendarHeatmapOutline.None
        }
        return CalendarHeatmapDayStyle(
            fill = fill,
            outline = outline,
            outlineWidth = width,
            dashed = quiet && !inspected && !today,
            marker = if (marker) colors.warningSolid else null,
            onChip = level != null && level >= HeatmapLevels.CHIP,
        )
    }

    /** The day number in a month day: the default text, muted on a quiet non-working day. */
    fun dayNumberColor(quiet: Boolean, colors: MonarchColors): Color =
        if (quiet) colors.textMuted else colors.textDefault

    /** The weekday and month names. */
    fun labelColor(colors: MonarchColors): Color = colors.textMuted

    /** The chip behind text on the two strongest levels. */
    fun chipColor(colors: MonarchColors): Color = colors.bgSurface

    /** The smallest kept height of a layout. */
    fun minHeight(layout: MonarchCalendarHeatmapLayout): Dp = when (layout) {
        MonarchCalendarHeatmapLayout.Year -> YearMinHeight
        MonarchCalendarHeatmapLayout.Month -> MonthMinHeight
    }

    /** The frame's status: ready without an entry in the range shows the empty state. */
    fun status(status: MonarchChartFrameStatus, hasData: Boolean): MonarchChartFrameStatus =
        if (status == MonarchChartFrameStatus.Ready && !hasData) MonarchChartFrameStatus.Empty else status
}
