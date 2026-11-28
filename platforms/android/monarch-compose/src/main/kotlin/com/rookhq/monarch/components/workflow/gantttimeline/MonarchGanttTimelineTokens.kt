package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a bar.
 *
 * @property container the fill, a light tint of the tone.
 * @property content the label on the fill.
 * @property border the outline in the tone's strong fill.
 * @property progress the progress strip.
 * @property dashed whether the outline is dashed (a conflict).
 */
@Immutable
internal data class GanttTimelineBarColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val progress: Color,
    val dashed: Boolean,
)

/**
 * The colours of a milestone diamond.
 *
 * @property fill the inside.
 * @property border the outline.
 */
@Immutable
internal data class GanttTimelineMilestoneColors(val fill: Color, val border: Color)

/** Component tokens of MonarchGanttTimeline, built from primitives and roles only. */
@Suppress("TooManyFunctions")
internal object MonarchGanttTimelineTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum
    val HeaderMinHeight: Dp = MonarchTokens.Space.space12.dp
    val LabelMaxWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp
    val LabelPadding: Dp = MonarchTokens.Space.space2.dp
    val LabelVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val DepthIndent: Dp = MonarchTokens.Space.space4.dp
    val BarInset: Dp = MonarchTokens.Space.space2.dp
    val BarPadding: Dp = MonarchTokens.Space.space1.dp
    val BarMinWidth: Dp = MonarchTokens.Space.space1_5.dp
    val PointSize: Dp = MonarchTokens.Space.space4.dp
    val DiamondSize: Dp = MonarchTokens.Space.space3.dp
    val DiamondBorder: Dp = MonarchTokens.Space.space0_5.dp
    val EdgeZone: Dp = MonarchTokens.Space.space3.dp
    val ProgressHeight: Dp = MonarchTokens.Space.space1.dp

    /** How much of the tone's strong fill the finished part of a bar holds, over the tint. */
    const val ProgressFillStrength: Float = 0.35f

    /** The gap between the warning glyph of a conflict bar and its label. */
    val BarIconGap: Dp = MonarchTokens.Space.space1.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val ConflictBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val DashLength: Dp = MonarchTokens.Space.space1.dp
    val LineWidth: Dp = MonarchTokens.Space.px.dp
    val TodayLineWidth: Dp = MonarchTokens.Space.space0_5.dp
    val ArrowStroke: Dp = MonarchTokens.Space.space0_5.dp
    val ArrowGap: Dp = MonarchTokens.Space.space2_5.dp
    val ArrowTip: Dp = MonarchTokens.Space.space0_5.dp
    val ArrowHead: Dp = MonarchTokens.Space.space1_5.dp
    val SpanHeight: Dp = MonarchTokens.Space.space2.dp
    val TickPadding: Dp = MonarchTokens.Space.space1.dp
    val LaneLabelMaxWidth: Dp = MonarchTokens.Space.space24.dp
    val LaneGap: Dp = MonarchTokens.Space.space1.dp
    val TodayLabelPadding: Dp = MonarchTokens.Space.space1.dp
    val OffscreenPadding: Dp = MonarchTokens.Space.space2.dp
    val ListIndent: Dp = MonarchTokens.Space.space4.dp
    val ListGap: Dp = MonarchTokens.Space.space1.dp

    /** The share of the width the row-label column takes at most. */
    const val LABEL_FRACTION: Float = 0.4f

    /** Bars at least this many times the edge zone wide show their label. */
    const val LABEL_MIN_EDGES: Int = 5

    /** The rotation of a square that makes it a diamond. */
    const val DIAMOND_DEGREES: Float = 45f

    private const val YEAR_DAY_WIDTH = 1.6f

    /** The width of a day at [zoom]. */
    fun dayWidth(zoom: MonarchGanttTimelineZoom): Dp = when (zoom) {
        MonarchGanttTimelineZoom.Week -> MonarchTokens.Space.space10.dp
        MonarchGanttTimelineZoom.Month -> MonarchTokens.Space.space4.dp
        MonarchGanttTimelineZoom.Quarter -> (MonarchTokens.Space.space1 + MonarchTokens.Space.px).dp
        MonarchGanttTimelineZoom.Year -> (MonarchTokens.Space.px * YEAR_DAY_WIDTH).dp
    }

    /** How many days the window shows at [zoom]. */
    @Suppress("MagicNumber")
    fun span(zoom: MonarchGanttTimelineZoom): Int = when (zoom) {
        MonarchGanttTimelineZoom.Week -> 28
        MonarchGanttTimelineZoom.Month -> 91
        MonarchGanttTimelineZoom.Quarter -> 273
        MonarchGanttTimelineZoom.Year -> 730
    }

    /** How many days Earlier and Later move the window at [zoom]. */
    @Suppress("MagicNumber")
    fun step(zoom: MonarchGanttTimelineZoom): Int = when (zoom) {
        MonarchGanttTimelineZoom.Week -> 7
        MonarchGanttTimelineZoom.Month -> 30
        MonarchGanttTimelineZoom.Quarter -> 91
        MonarchGanttTimelineZoom.Year -> 182
    }

    /** The built-in name of [zoom]. */
    @StringRes
    fun zoomNameRes(zoom: MonarchGanttTimelineZoom): Int = when (zoom) {
        MonarchGanttTimelineZoom.Week -> R.string.monarch_gantttimeline_zoom_week
        MonarchGanttTimelineZoom.Month -> R.string.monarch_gantttimeline_zoom_month
        MonarchGanttTimelineZoom.Quarter -> R.string.monarch_gantttimeline_zoom_quarter
        MonarchGanttTimelineZoom.Year -> R.string.monarch_gantttimeline_zoom_year
    }

    /** The width of the row-label column in a chart [width] wide. */
    fun labelWidth(width: Dp): Dp = minOf(LabelMaxWidth, width * LABEL_FRACTION)

    /** Whether the gridlines follow the day or week ticks (Week, Month) or the month ticks. */
    fun minorGrid(zoom: MonarchGanttTimelineZoom): Boolean =
        zoom == MonarchGanttTimelineZoom.Week || zoom == MonarchGanttTimelineZoom.Month

    /**
     * A bar: a light tint of its tone with an outline in the tone's strong fill, the label in the default text colour
     * and the progress strip in the strong fill. `null` is the accent colour (Monarch tones have no accent), neutral
     * uses the muted background and the strong border, and a conflict uses danger with a dashed outline.
     */
    fun barColors(colors: MonarchColors, tone: MonarchTone?, conflict: Boolean): GanttTimelineBarColors {
        val shown = if (conflict) MonarchTone.Danger else tone
        val (soft, fill) = when (shown) {
            null -> colors.accentSubtle to colors.accentSolid
            MonarchTone.Neutral -> colors.bgMuted to colors.borderStrong
            else -> MonarchTonePalette.of(shown, colors).let { it.subtle to it.solid }
        }
        return GanttTimelineBarColors(soft, colors.textDefault, fill, fill, dashed = conflict)
    }

    /** A milestone: filled with the accent when done, otherwise the surface with a muted outline. */
    fun milestoneColors(colors: MonarchColors, done: Boolean): GanttTimelineMilestoneColors = if (done) {
        GanttTimelineMilestoneColors(fill = colors.accentSolid, border = colors.accentSolid)
    } else {
        GanttTimelineMilestoneColors(fill = colors.bgSurface, border = colors.textMuted)
    }

    /** A dependency arrow: the subtle text colour, or the strong danger colour (dashed) when it is broken. */
    fun dependencyColor(colors: MonarchColors, conflict: Boolean): Color =
        if (conflict) colors.dangerStrong else colors.textSubtle

    /** Gridlines, row lines and the chart's outline. */
    fun lineColor(colors: MonarchColors): Color = colors.borderSubtle

    /** Weekends, group rows and the header. */
    fun shadeColor(colors: MonarchColors): Color = colors.bgSubtle

    /** The today line and the fill of its label. */
    fun todayColor(colors: MonarchColors): Color = colors.accentSolid

    /** The today label's text. */
    fun onTodayColor(colors: MonarchColors): Color = colors.accentOnSolid

    /** The bracket of a parent without dates and the outline of a group's span. */
    fun bracketColor(colors: MonarchColors): Color = colors.borderStrong

    /** The fill of a group's span. */
    fun groupSpanColor(colors: MonarchColors): Color = colors.bgMuted

    /** Header ticks, captions and row texts such as "No dates". */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The conflict line in the list. */
    fun conflictColor(colors: MonarchColors): Color = colors.dangerStrong
}
