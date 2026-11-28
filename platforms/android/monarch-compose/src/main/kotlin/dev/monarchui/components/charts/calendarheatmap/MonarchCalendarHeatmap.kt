package dev.monarchui.components.charts.calendarheatmap

import android.icu.text.DateFormat
import android.icu.text.NumberFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.heatmap.HeatmapLegend
import dev.monarchui.components.charts.heatmap.HeatmapLegendWords
import dev.monarchui.components.charts.heatmap.HeatmapText
import dev.monarchui.components.charts.heatmap.HeatmapWords
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * Values by day on a calendar grid: a contribution-style year or a month calendar with day numbers, binned into
 * four steps of one data colour with a Less to More legend.
 *
 * Built by Monarch inside `MonarchChartFrame` from Compose layouts that draw each day: Material 3 has no chart.
 * It reuses `MonarchHeatmap`'s scale, colours and legend. The year layout keeps 14dp days and scrolls sideways
 * on a phone, opening at the latest week; the month layout fits seven columns to the width and writes each
 * day's number and, when there is room, its value. Today is ringed in the accent colour, non-working days are
 * hollow with a dashed outline, flagged days have a dot. A tap inspects a day: it is ringed and its date, value
 * and notes are written under the grid, announced politely.
 *
 * For TalkBack the grid is a traversal group named by the generated summary, and every day is one element read
 * as its date, value, note, Non-working day, Marked and Today, in date order; with [onSelect] each has a Select
 * action. "View as table" lists every day with an entry.
 *
 * @param title the chart's name, shown as the frame's heading.
 * @param days one entry per day with something to show. Days in the range without an entry are no data; a later
 * entry for the same day wins.
 * @param modifier the modifier of the frame.
 * @param from the first day drawn, as epoch milliseconds in UTC. `null` uses the earliest entry.
 * @param to the last day drawn, as epoch milliseconds in UTC. `null` uses the latest entry.
 * @param description one line under the title, such as the period.
 * @param layout `Year`, weeks as columns, or `Month`, a calendar with day numbers.
 * @param today the day ringed as today, as epoch milliseconds in UTC. Passed by the app; the chart never reads
 * the clock.
 * @param firstDayOfWeek the first day of the week, as a `java.util.Calendar` constant such as `Calendar.MONDAY`.
 * `null` uses the current locale's.
 * @param status what the frame shows. Ready without an entry in the range shows the empty state.
 * @param color the data colour of the four steps.
 * @param thresholds three ascending values where levels 2, 3 and 4 start. `null` uses quarters of the largest
 * value.
 * @param showValues the month layout writes each day's value (or note) under its number when the day is wide
 * enough.
 * @param showLegend shows the Less to More legend under the grid.
 * @param summary replaces the generated summary that TalkBack reads for the grid.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatValue writes values. `null` writes a number in the current locale.
 * @param onRetry shows Try again in the error status.
 * @param onSelect called with the day when it is tapped or chosen with the TalkBack Select action; a day without
 * an entry comes with a `null` value.
 */
@Composable
public fun MonarchCalendarHeatmap(
    title: String,
    days: List<MonarchCalendarHeatmapDay>,
    modifier: Modifier = Modifier,
    from: Long? = null,
    to: Long? = null,
    description: String? = null,
    layout: MonarchCalendarHeatmapLayout = MonarchCalendarHeatmapLayout.Year,
    today: Long? = null,
    firstDayOfWeek: Int? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    color: MonarchChartColor = MonarchChartColor.Data1,
    thresholds: List<Double>? = null,
    showValues: Boolean = true,
    showLegend: Boolean = true,
    summary: String? = null,
    emptyMessage: String? = null,
    footer: String? = null,
    labels: MonarchCalendarHeatmapLabels = MonarchCalendarHeatmapLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchCalendarHeatmapDay) -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formats = remember(locale) { CalendarHeatmapFormats(locale) }
    val write = formatValue ?: formats.number
    val firstWeekday = Math.floorMod((firstDayOfWeek ?: formats.firstDayOfWeek) - 1, CalendarHeatmapDates.WEEK)
    val model = remember(days, from, to, firstWeekday, thresholds) {
        CalendarHeatmapModel(days, from, to, firstWeekday, thresholds)
    }
    val words = calendarHeatmapWords(labels, locale, write, formats)
    val todayDay = today?.let(CalendarHeatmapDates::dayNumber)
    var inspected by rememberSaveable { mutableStateOf<Long?>(null) }
    val shown = inspected?.takeIf(model::inRange)
    val choose: (Long) -> Unit = { day ->
        inspected = day
        onSelect?.invoke(model.dayAt(day))
    }
    val density = LocalDensity.current
    var measured by remember { mutableStateOf<Dp?>(null) }
    val minHeight = MonarchCalendarHeatmapTokens.minHeight(layout)
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = MonarchCalendarHeatmapTokens.status(status, model.hasData),
        table = calendarHeatmapTable(model, words, labels),
        emptyMessage = emptyMessage,
        footer = footer,
        height = maxOf(minHeight, measured ?: minHeight),
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        val spoken = summary ?: CalendarHeatmapText.summary(model, words)
        val spec = CalendarHeatmapGridSpec(
            model = model,
            words = words,
            slot = color,
            today = todayDay,
            inspected = shown,
            weekdayNames = (0 until CalendarHeatmapDates.WEEK).map { formats.weekday(firstWeekday + it) },
            narrowWeekdayNames = (0 until CalendarHeatmapDates.WEEK).map { formats.narrowWeekday(firstWeekday + it) },
            monthName = formats.month,
            showValues = showValues,
            selectLabel = monarchString(labels.select, R.string.monarch_calendarheatmap_select),
            onTap = choose,
            onSelect = if (onSelect != null) choose else null,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { measured = with(density) { it.height.toDp() } }
                .semantics {
                    isTraversalGroup = true
                    contentDescription = spoken
                },
            verticalArrangement = Arrangement.spacedBy(MonarchCalendarHeatmapTokens.SectionGap),
        ) {
            when (layout) {
                MonarchCalendarHeatmapLayout.Year -> CalendarHeatmapYearGrid(spec)
                MonarchCalendarHeatmapLayout.Month -> CalendarHeatmapMonthGrid(spec)
            }
            val detail = shown?.let { CalendarHeatmapText.describe(model, it, todayDay, words) }
            // Always present, so TalkBack announces the details of each newly inspected day.
            Box(
                Modifier.clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    if (detail != null) contentDescription = detail
                },
            ) {
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textDefault,
                    )
                }
            }
            if (showLegend) CalendarHeatmapLegend(model, color, labels, words, locale)
        }
    }
}

/** The Less to More legend of `MonarchHeatmap`, with a No data step when a day in the range has no value. */
@Composable
private fun CalendarHeatmapLegend(
    model: CalendarHeatmapModel,
    color: MonarchChartColor,
    labels: MonarchCalendarHeatmapLabels,
    words: CalendarHeatmapWords,
    locale: Locale,
) {
    val steps = HeatmapWords(
        locale = locale,
        format = words.format,
        noData = words.noData,
        rangeBelow = monarchString(labels.rangeBelow, R.string.monarch_heatmap_range_below),
        above = monarchString(labels.above, R.string.monarch_heatmap_above),
        range = monarchString(labels.range, R.string.monarch_heatmap_range),
        rangeAbove = monarchString(labels.rangeAbove, R.string.monarch_heatmap_range_above),
        rowCount = { "" },
        columnCount = { "" },
        size = "",
        highest = "",
        cell = "",
        withNote = "",
    )
    HeatmapLegend(
        slot = color,
        showEmpty = model.hasGaps,
        words = HeatmapLegendWords(
            name = monarchString(labels.frame.legend, R.string.monarch_chartlegend_legend),
            less = monarchString(labels.less, R.string.monarch_heatmap_less),
            more = monarchString(labels.more, R.string.monarch_heatmap_more),
            steps = (if (model.hasGaps) listOf(words.noData) else emptyList()) +
                HeatmapText.legendSteps(model.scale, steps),
        ),
    )
}

/** The data table: every day with an entry, its date, its value and, when any day has one, its note. */
@Composable
private fun calendarHeatmapTable(
    model: CalendarHeatmapModel,
    words: CalendarHeatmapWords,
    labels: MonarchCalendarHeatmapLabels,
): MonarchChartTable {
    val entries = model.entries
    val value = MonarchChartTableSeries(
        name = monarchString(labels.value, R.string.monarch_calendarheatmap_value),
        // The table writes the formatted text; a day with text but no number is still not a gap.
        values = entries.map { (_, entry) -> CalendarHeatmapModel.finite(entry) ?: entry.valueLabel?.let { 0.0 } },
        formattedValues = entries.map { (_, entry) -> CalendarHeatmapText.valueText(entry, words) },
    )
    val note = MonarchChartTableSeries(
        name = monarchString(labels.note, R.string.monarch_calendarheatmap_note),
        values = entries.map { (_, entry) -> entry.note?.let { 0.0 } },
        formattedValues = entries.map { (_, entry) -> entry.note.orEmpty() },
    )
    return MonarchChartTable(
        categoryTitle = monarchString(labels.date, R.string.monarch_calendarheatmap_date),
        categories = entries.map { (day, _) -> words.fullDate(day) },
        series = if (model.hasNotes) listOf(value, note) else listOf(value),
    )
}

/** The built-in strings, or their replacements. */
@Composable
private fun calendarHeatmapWords(
    labels: MonarchCalendarHeatmapLabels,
    locale: Locale,
    format: (Double) -> String,
    formats: CalendarHeatmapFormats,
): CalendarHeatmapWords {
    val resources = LocalResources.current
    return CalendarHeatmapWords(
        locale = locale,
        format = format,
        fullDate = formats.fullDate,
        noData = monarchString(labels.noData, R.string.monarch_heatmap_no_data),
        today = monarchString(labels.today, R.string.monarch_calendarheatmap_today),
        inactive = monarchString(labels.inactive, R.string.monarch_calendarheatmap_inactive),
        marker = monarchString(labels.marker, R.string.monarch_calendarheatmap_marker),
        day = stringResource(R.string.monarch_calendarheatmap_day),
        dayCount = labels.dayCount ?: { resources.getQuantityString(R.plurals.monarch_calendarheatmap_days, it, it) },
        summary = monarchString(labels.summary, R.string.monarch_calendarheatmap_summary),
        highest = monarchString(labels.highest, R.string.monarch_calendarheatmap_highest),
    )
}

/** The locale's number format, date formats in UTC and first day of the week. */
private class CalendarHeatmapFormats(locale: Locale) {
    private val numberFormat = NumberFormat.getInstance(locale)
    private val dateFormat = utc(DateFormat.getInstanceForSkeleton(FULL_DATE, locale))
    private val monthFormat = utc(DateFormat.getInstanceForSkeleton(MONTH, locale))
    private val weekdayFormat = utc(DateFormat.getInstanceForSkeleton(WEEKDAY, locale))
    private val narrowWeekdayFormat = utc(DateFormat.getInstanceForSkeleton(NARROW_WEEKDAY, locale))

    val number: (Double) -> String = { numberFormat.format(it) }
    val fullDate: (Long) -> String = { dateFormat.format(CalendarHeatmapDates.millis(it)) }
    val month: (Long) -> String = { monthFormat.format(CalendarHeatmapDates.millis(it)) }

    /** The `java.util.Calendar` constant of the locale's first day of the week. */
    val firstDayOfWeek: Int = Calendar.getInstance(locale).firstDayOfWeek

    /** The short name of [weekday], counting from 0 for Sunday; wraps around. */
    fun weekday(weekday: Int): String =
        weekdayFormat.format(CalendarHeatmapDates.millis(SUNDAY + Math.floorMod(weekday, CalendarHeatmapDates.WEEK)))

    /** The narrow name of [weekday] ("M"), for columns too narrow for the short name at large text sizes. */
    fun narrowWeekday(weekday: Int): String = narrowWeekdayFormat.format(
        CalendarHeatmapDates.millis(SUNDAY + Math.floorMod(weekday, CalendarHeatmapDates.WEEK)),
    )

    private fun utc(format: DateFormat): DateFormat = format.apply { timeZone = TimeZone.getTimeZone("UTC") }

    private companion object {
        const val FULL_DATE = "yMMMEd"
        const val MONTH = "MMM"
        const val WEEKDAY = "EEE"
        const val NARROW_WEEKDAY = "EEEEE"

        /** 1970-01-04, the first Sunday after the epoch. */
        const val SUNDAY = 3L
    }
}
