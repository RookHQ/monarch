package dev.monarchui.components.scheduling.timegrid

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.ceil

/**
 * The colours of a time grid, all theme roles.
 *
 * @property background the grid's surface.
 * @property line hour lines and column separators.
 * @property secondaryText hour labels, weekdays and the all-day label.
 * @property dateText the date number of a day that is not today.
 * @property todayFill the circle behind today's date.
 * @property todayText today's date on its circle.
 * @property shade time outside working hours.
 * @property unavailableFill the fill of an unavailable block.
 * @property unavailableHatch the hatching of an unavailable block.
 * @property unavailableText the label of an unavailable block.
 * @property now the now line and its dot.
 * @property ghostFill the fill of the ghost that shows where a moved event will go.
 * @property ghostText the text of the ghost.
 */
@Immutable
internal data class TimeGridColors(
    val background: Color,
    val line: Color,
    val secondaryText: Color,
    val dateText: Color,
    val todayFill: Color,
    val todayText: Color,
    val shade: Color,
    val unavailableFill: Color,
    val unavailableHatch: Color,
    val unavailableText: Color,
    val now: Color,
    val ghostFill: Color,
    val ghostText: Color,
)

/** Component tokens of MonarchTimeGrid, built from primitives and roles only. */
internal object MonarchTimeGridTokens {
    /** The height of one hour at the default font size. */
    val HourHeight: Dp = MonarchTokens.Space.space16.dp

    /** The hour height grows with the font size from this base, so labels and short events keep room. */
    val ScaledHourBase: Dp = MonarchTokens.Space.space12.dp

    /** The narrowest hour gutter. */
    val MinGutterWidth: Dp = MonarchTouchTarget.Minimum

    /** The space between an hour label and the grid. */
    val GutterPadding: Dp = MonarchTokens.Space.space1.dp

    /** The space between events side by side. */
    val EventGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The end space of a column that events leave free, so an empty strip stays tappable. */
    val ColumnEndInset: Dp = MonarchTokens.Space.space1.dp

    /** The shortest an event that cannot be tapped is drawn. */
    val EventMinHeight: Dp = MonarchTokens.Space.space4.dp

    /** The thickness of the now line. */
    val NowLineWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The diameter of the dot at the start of the now line. */
    val NowDotSize: Dp = MonarchTokens.Space.space2.dp

    /** The thickness of grid lines. */
    val LineWidth: Dp = MonarchTokens.Space.px.dp

    /** The distance between hatch lines of an unavailable block. */
    val HatchSpacing: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside an unavailable block, the ghost and a day heading. */
    val BlockPadding: Dp = MonarchTokens.Space.space1.dp

    /** The vertical space between the parts of a heading and between all-day chips. */
    val HeadingGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The diameter of the circle behind today's date. */
    val TodaySize: Dp = MonarchTokens.Space.space8.dp

    /** The width of the ghost's dashed border and its dash. */
    val GhostBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val GhostDash: Dp = MonarchTokens.Space.space1.dp

    /** Events shorter than this many minutes are drawn on one line. */
    const val COMPACT_BELOW_MINUTES: Int = 45

    /** Events at least this many minutes long also show their detail line. */
    const val DETAIL_FROM_MINUTES: Int = 70

    /** The space above the scroll target, so its hour label is not under the heading. */
    val ScrollMargin: Dp = MonarchTokens.Space.space3.dp

    /** The hour height for a font scale: [HourHeight], or more when the text is larger. */
    fun hourHeight(requested: Dp, fontScale: Float): Dp =
        if (requested != Dp.Unspecified) requested else maxOf(HourHeight, ScaledHourBase * fontScale)

    /**
     * The shortest an event is drawn, in minutes: long enough for a 48dp
     * touch target when it can be tapped or moved, else [EventMinHeight];
     * never shorter than [snap].
     */
    fun minMinutes(hourHeight: Dp, interactive: Boolean, snap: Int): Int {
        val height = if (interactive) MonarchTouchTarget.Minimum else EventMinHeight
        val minutes = ceil(height.value / hourHeight.value * TimeGridModel.MINUTES_PER_HOUR).toInt()
        return maxOf(snap, minutes)
    }

    /**
     * The narrowest a day column may be: room for a 48dp all-day chip with
     * its side gaps, and for [cols] events side by side, each at least 48dp
     * wide, plus the free strip at the end.
     */
    fun minDayWidth(cols: Int): Dp = maxOf(
        MonarchTouchTarget.Minimum + EventGap * 2,
        (MonarchTouchTarget.Minimum + EventGap) * cols + ColumnEndInset,
    )

    /**
     * The width of each day column: an equal share of [available], widened
     * where a column needs room for events side by side. When the sum passes
     * [available], the days scroll sideways.
     */
    fun dayWidths(available: Dp, maxCols: List<Int>): List<Dp> {
        if (maxCols.isEmpty()) return emptyList()
        val share = available / maxCols.size
        return maxCols.map { maxOf(share, minDayWidth(it)) }
    }

    /**
     * The colours of the grid: the surface background, subtle lines, a default
     * line under the headings, muted secondary text, today's date on the accent
     * solid circle, subtle shading, muted unavailable blocks hatched with the
     * default border, the danger solid now line, and a surface ghost.
     */
    fun colors(colors: MonarchColors): TimeGridColors = TimeGridColors(
        background = colors.bgSurface,
        line = colors.borderSubtle,
        secondaryText = colors.textMuted,
        dateText = colors.textDefault,
        todayFill = colors.accentSolid,
        todayText = colors.accentOnSolid,
        shade = colors.bgSubtle,
        unavailableFill = colors.bgMuted,
        unavailableHatch = colors.borderDefault,
        unavailableText = colors.textMuted,
        now = colors.dangerSolid,
        ghostFill = colors.bgSurface,
        ghostText = colors.textDefault,
    )

    /** The colour of the ghost's border: the event's colour, else the accent. */
    fun ghostBorder(colors: MonarchColors, eventColor: Color): Color =
        if (eventColor == Color.Unspecified) colors.accentSolid else eventColor

    /** What TalkBack reads for a day heading: "Wednesday, November 4" with ", Today" on today. */
    fun headingDescription(longDay: String, today: String?): String = listOfNotNull(longDay, today).joinToString(", ")

    /** What TalkBack reads for an unavailable block: "Closed, 12:00 PM to 1:00 PM". */
    fun unavailableDescription(label: String?, range: String): String =
        listOfNotNull(label?.takeIf { it.isNotBlank() }, range).joinToString(", ")
}
