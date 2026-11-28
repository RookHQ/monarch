package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * Everything the grids draw from: the days, the words, the colour, today, the inspected day, the names of the
 * weekdays (from the first day of the week) and months, and what a tap and the Select action do.
 */
@Suppress("LongParameterList")
internal class CalendarHeatmapGridSpec(
    val model: CalendarHeatmapModel,
    val words: CalendarHeatmapWords,
    val slot: MonarchChartColor,
    val today: Long?,
    val inspected: Long?,
    val weekdayNames: List<String>,
    val narrowWeekdayNames: List<String>,
    val monthName: (Long) -> String,
    val showValues: Boolean,
    val selectLabel: String,
    val onTap: (Long) -> Unit,
    val onSelect: ((Long) -> Unit)?,
) {
    /** The style of [day]. */
    fun style(day: Long, colors: MonarchColors): CalendarHeatmapDayStyle {
        val entry = model.entry(day)
        return MonarchCalendarHeatmapTokens.dayStyle(
            level = model.level(day),
            inactive = entry?.inactive == true,
            today = day == today,
            inspected = day == inspected,
            marker = entry?.marker == true,
            slot = slot,
            colors = colors,
        )
    }

    /** The cell of [day] with the corner [radius]. */
    fun cell(day: Long, radius: Dp, colors: MonarchColors): CalendarHeatmapDayCellSpec = CalendarHeatmapDayCellSpec(
        style = style(day, colors),
        spoken = CalendarHeatmapText.describe(model, day, today, words),
        selected = day == inspected,
        radius = radius,
        selectLabel = selectLabel,
        onTap = { onTap(day) },
        onSelect = onSelect?.let { select -> { select(day) } },
    )
}

/**
 * The year layout: weekday names at the start (every other one, as on the web), then the weeks as columns under
 * their month names, scrolling sideways and opening at the latest week. Rows and columns mirror right to left.
 */
@Composable
internal fun CalendarHeatmapYearGrid(spec: CalendarHeatmapGridSpec) {
    val tokens = MonarchCalendarHeatmapTokens
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val model = spec.model
    // At large text sizes a weekday name is taller than the two rows it labels (every other row is named):
    // the rows grow just enough that the names never overlap, instead of cutting or squeezing the text.
    val measurer = rememberTextMeasurer()
    val labelHeight = with(LocalDensity.current) {
        spec.weekdayNames.maxOfOrNull { measurer.measure(it, caption, maxLines = 1).size.height }?.toDp()
    } ?: tokens.YearCell
    val labelled = tokens.yearRow(labelHeight)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    // As the web, a range of months shows whole: the days narrow to fit the width (down to a floor) before the
    // grid scrolls sideways, so every month name is on screen.
    val room = maxWidth - MonarchTokens.Space.space12.dp - tokens.LabelGap
    val weeks = model.weeks.coerceAtLeast(1)
    val fit = (room - tokens.Gap * (weeks - 1)) / weeks
    val row = minOf(labelled, maxOf(fit, MonarchTokens.Space.space2.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clearAndSetSemantics { }) {
            // Takes the height of the month names, so the weekday names line up with their rows.
            Text(text = " ", style = caption, maxLines = 1)
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                spec.weekdayNames.forEachIndexed { index, name ->
                    Box(modifier = Modifier.height(row), contentAlignment = Alignment.CenterEnd) {
                        if (index % 2 == 1) {
                            Text(
                                text = name,
                                modifier = Modifier.wrapContentHeight(unbounded = true),
                                style = caption,
                                color = tokens.labelColor(colors),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(tokens.LabelGap))
        // The largest start value is cut to the end of the content once it is measured: the latest week shows.
        val scroll = rememberScrollState(initial = Int.MAX_VALUE)
        Column(modifier = Modifier.horizontalScroll(scroll)) {
            CalendarHeatmapMonthNames(spec, row) { scroll.value }
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                for (week in 0 until model.weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                        for (weekday in 0 until CalendarHeatmapDates.WEEK) {
                            val day = model.dayOfPlace(week * CalendarHeatmapDates.WEEK + weekday)
                            if (model.inRange(day)) {
                                CalendarHeatmapDayCell(
                                    spec = spec.cell(day, tokens.YearRadius, colors),
                                    modifier = Modifier.size(row),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(row))
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

/** The month names over the year grid, at the week of each month's first day; skipped when they would overlap. */
@Composable
private fun CalendarHeatmapMonthNames(spec: CalendarHeatmapGridSpec, cell: Dp, scrolled: () -> Int) {
    val tokens = MonarchCalendarHeatmapTokens
    val colors = MonarchTheme.colors
    val model = spec.model
    val range = model.range ?: return
    val starts = range.filter { it == range.first || CalendarHeatmapDates.dayOfMonth(it) == 1 }
    Layout(
        modifier = Modifier.clearAndSetSemantics { },
        content = {
            starts.forEach { day ->
                Text(
                    text = spec.monthName(day),
                    style = MonarchTheme.typography.caption,
                    color = tokens.labelColor(colors),
                    maxLines = 1,
                )
            }
        },
    ) { measurables, _ ->
        val step = (cell + tokens.Gap).roundToPx()
        val gap = tokens.LabelGap.roundToPx()
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = maxOf(0, model.weeks * step - tokens.Gap.roundToPx())
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            var free = Int.MIN_VALUE
            placeables.forEachIndexed { index, placeable ->
                val x = ((starts[index] - model.gridStart) / CalendarHeatmapDates.WEEK).toInt() * step
                // A name whose week is scrolled partly out of view would show only its tail ("ar"): it is left out.
                val clipped = layoutDirection == LayoutDirection.Ltr && x < scrolled()
                if (x >= free && !clipped) {
                    // placeRelative mirrors the position right to left, like the weeks.
                    placeable.placeRelative(x, 0)
                    free = x + placeable.width + gap
                }
            }
        }
    }
}

/**
 * The month layout: weekday names over seven columns, then one row per week. Days are as wide as the width
 * allows, up to 56dp, and grow taller when their text needs room.
 */
@Composable
internal fun CalendarHeatmapMonthGrid(spec: CalendarHeatmapGridSpec) {
    val tokens = MonarchCalendarHeatmapTokens
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val model = spec.model
    val week = CalendarHeatmapDates.WEEK
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cell = minOf(tokens.MonthMaxCell, (maxWidth - tokens.Gap * (week - 1)) / week).coerceAtLeast(tokens.Gap)
        // The short names ("Mon") when every one fits its column; the narrow names ("M") when one would be cut,
        // as at the largest font size on a phone.
        val measurer = rememberTextMeasurer()
        val widest = with(LocalDensity.current) {
            spec.weekdayNames.maxOfOrNull { measurer.measure(it, caption, maxLines = 1).size.width }?.toDp()
        } ?: cell
        val names = tokens.weekdayNames(spec.weekdayNames, spec.narrowWeekdayNames, widest, cell)
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            Row(
                modifier = Modifier.clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                names.forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier.width(cell),
                        style = caption,
                        color = tokens.labelColor(colors),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
            for (row in 0 until model.weeks) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                ) {
                    for (weekday in 0 until week) {
                        val day = model.dayOfPlace(row * week + weekday)
                        if (model.inRange(day)) {
                            CalendarHeatmapDayCell(
                                spec = spec.cell(day, tokens.MonthRadius, colors),
                                modifier = Modifier.width(cell).fillMaxHeight().heightIn(min = cell),
                            ) {
                                val wide = cell >= tokens.MonthValueCell
                                CalendarHeatmapMonthDay(spec, day, showValue = spec.showValues && wide)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(cell))
                        }
                    }
                }
            }
        }
    }
}

/** The day number and, when there is room, the value (or the note) of one month day. */
@Composable
private fun CalendarHeatmapMonthDay(spec: CalendarHeatmapGridSpec, day: Long, showValue: Boolean) {
    val tokens = MonarchCalendarHeatmapTokens
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val entry = spec.model.entry(day)
    val style = spec.style(day, colors)
    val quiet = entry?.inactive == true && (spec.model.level(day) ?: 0) == 0
    val chip = if (style.onChip) {
        Modifier.background(tokens.chipColor(colors), MonarchTheme.shapes.xs).padding(horizontal = tokens.ChipPadding)
    } else {
        Modifier
    }
    Column(modifier = Modifier.padding(tokens.MonthPadding)) {
        Text(
            text = CalendarHeatmapDates.dayOfMonth(day).toString(),
            modifier = chip,
            style = caption,
            color = tokens.dayNumberColor(quiet, colors),
            maxLines = 1,
        )
        val value = entry?.valueLabel
            ?: CalendarHeatmapModel.finite(entry)?.takeIf { it != 0.0 }?.let(spec.words.format)
            ?: entry?.note
        if (showValue && value != null) {
            Text(
                text = value,
                modifier = chip,
                style = caption,
                color = colors.textDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
