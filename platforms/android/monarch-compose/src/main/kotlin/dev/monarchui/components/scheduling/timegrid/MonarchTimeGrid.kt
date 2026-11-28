package dev.monarchui.components.scheduling.timegrid

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme

/**
 * The day and week view of a calendar: an hour gutter, an all-day row and
 * one column per day, with overlapping events side by side, a now line,
 * working-hours shading, unavailable blocks, tap to create and long press
 * and drag to move.
 *
 * Built by Monarch: Material 3 has no day or week grid. Events are
 * `MonarchEventChip` blocks (bars in the all-day row) placed by wall-clock
 * time in [timeZone], so a daylight-saving day still has 24 rows. The hours
 * scroll vertically inside the grid and open at [scrollToMinutes]; give the
 * grid a bounded height (a screen's content, `Modifier.weight` or
 * `Modifier.height`) and do not put it in another vertical scroll. Day
 * columns share the width; a column widens so events side by side keep a
 * 48dp touch target, and the days then scroll sideways together with their
 * headings. Tappable events are drawn at least 48dp tall.
 *
 * Moving is long press then drag, as in Google Calendar, with system haptics
 * for the pick-up and each step; TalkBack gets the actions Move earlier,
 * Move later, Move to previous day and Move to next day instead. The grid
 * never changes the events: [onMoveEvent] receives the move to apply.
 *
 * @param days the day columns in order, one for a day view, seven for a week, as day keys: the epoch
 * millisecond of the UTC midnight of each date, as `DatePickerState` and `MonarchMiniMonth` give. Any instant of
 * that UTC date is read as the date.
 * @param modifier the modifier of the grid.
 * @param events the events, the same model as `MonarchAgendaList`. Timed events are placed by start and end
 * (exclusive); all-day events go in the all-day row of each day they cover. Ids must be unique.
 * @param now the current time for the now line and today's heading, in epoch milliseconds, or `null` to hide
 * both. Pass a value that changes every minute to move the line; pass a fixed value in tests and previews.
 * @param startHour the first visible hour, 0 to 23.
 * @param endHour the end of the visible hours (exclusive), 1 to 24. At least one hour is shown.
 * @param hourHeight the height of one hour. Unspecified is 64dp, more at large font sizes.
 * @param snapMinutes taps, drags and TalkBack moves snap to this many minutes, 1 to 60.
 * @param workingHours working hours per ISO weekday; time outside them is shaded. `null` shades nothing.
 * @param unavailable blocks drawn hatched with an optional label, such as time off or a closure.
 * @param scrollToMinutes minutes from midnight to scroll to when the grid appears and when the first day or the
 * number of days changes. `null` is an hour before the earliest visible event, else 08:00.
 * @param showAllDayRow shows the all-day row even when no all-day event is visible.
 * @param timeZone the zone events are placed and times are written in. An IANA id such as `"Europe/Berlin"`. `null`, or
 * an id the device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 * @param canMoveEvent which events may be moved. `null` allows all, when [onMoveEvent] is set.
 * @param onSelectDay makes the headings of a multi-day grid buttons, for example to open the day view. Receives
 * the day key.
 * @param onSelectSlot called when an empty part of a column is tapped, with the time snapped down to
 * [snapMinutes]. Also makes every hour of every column a TalkBack button.
 * @param onSelectEvent opens an event, for example in `MonarchEventPopover` or a sheet. Without it events are
 * not buttons.
 * @param onMoveEvent turns on moving events by long press and drag and by TalkBack actions.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchTimeGrid(
    days: List<Long>,
    modifier: Modifier = Modifier,
    events: List<MonarchAgendaEvent> = emptyList(),
    now: Long? = System.currentTimeMillis(),
    startHour: Int = 0,
    endHour: Int = 24,
    hourHeight: Dp = Dp.Unspecified,
    snapMinutes: Int = 15,
    workingHours: List<MonarchTimeGridWorkingHours>? = null,
    unavailable: List<MonarchTimeGridUnavailable> = emptyList(),
    scrollToMinutes: Int? = null,
    showAllDayRow: Boolean = false,
    timeZone: String? = null,
    labels: MonarchTimeGridLabels = MonarchTimeGridLabels(),
    canMoveEvent: ((MonarchAgendaEvent) -> Boolean)? = null,
    onSelectDay: ((Long) -> Unit)? = null,
    onSelectSlot: ((MonarchTimeGridSlot) -> Unit)? = null,
    onSelectEvent: ((MonarchAgendaEvent) -> Unit)? = null,
    onMoveEvent: ((MonarchTimeGridMove) -> Unit)? = null,
) {
    val tokens = MonarchTimeGridTokens
    val zone = monarchTimeZone(timeZone)
    val snap = snapMinutes.coerceIn(1, TimeGridModel.MINUTES_PER_HOUR)
    val range = TimeGridModel.range(startHour, endHour)
    val dayKeys = remember(days) { days.map(TimeGridModel::dayKey) }
    val density = LocalDensity.current
    val hour = tokens.hourHeight(hourHeight, density.fontScale)
    val minMinutes = tokens.minMinutes(hour, onSelectEvent != null || onMoveEvent != null, snap)
    val columns = remember(dayKeys, events, unavailable, workingHours, range, zone, minMinutes) {
        TimeGridLayout.columns(dayKeys, events, unavailable, workingHours, range, zone, minMinutes)
    }
    val hasAllDay = showAllDayRow || columns.any { it.allDay.isNotEmpty() }
    val locale = LocalConfiguration.current.locales[0]
    val formats = remember(locale, zone) { TimeGridFormats(locale, zone) }
    val text = timeGridText(labels)
    val colors = tokens.colors(MonarchTheme.colors)
    val todayKey = now?.let { TimeGridModel.wall(it, zone) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    var drag by remember { mutableStateOf<TimeGridDrag?>(null) }

    val target = TimeGridLayout.scrollTarget(scrollToMinutes, columns).coerceIn(range.start, range.end)
    LaunchedEffect(dayKeys.firstOrNull(), dayKeys.size) {
        val offset = hour * (target - range.start) / TimeGridModel.MINUTES_PER_HOUR - tokens.ScrollMargin
        vertical.scrollTo(with(density) { offset.roundToPx() }.coerceAtLeast(0))
    }

    val measurer = rememberTextMeasurer()
    val caption = MonarchTheme.typography.caption
    val gutter = remember(range, formats, caption, density) {
        val widest = range.hours.drop(1).maxOfOrNull { h ->
            measurer.measure(formats.minutes(h * TimeGridModel.MINUTES_PER_HOUR), caption).size.width
        } ?: 0
        with(density) { widest.toDp() } + tokens.GutterPadding * 2
    }.coerceAtLeast(tokens.MinGutterWidth)

    BoxWithConstraints(modifier = modifier.background(colors.background)) {
        val available = if (constraints.hasBoundedWidth) maxWidth - gutter else Dp.Hairline
        val widths = tokens.dayWidths(available, columns.map { it.maxCols })
        val context = TimeGridContext(
            range = range,
            hourHeight = hour,
            snap = snap,
            zone = zone,
            days = dayKeys,
            widths = widths,
            formats = formats,
            text = text,
            colors = colors,
            canMoveEvent = canMoveEvent,
            onSelectSlot = onSelectSlot,
            onSelectEvent = onSelectEvent,
            onMoveEvent = onMoveEvent,
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Row {
                Spacer(modifier = Modifier.width(gutter))
                Row(modifier = Modifier.horizontalScroll(horizontal)) {
                    columns.forEachIndexed { index, column ->
                        TimeGridHeading(
                            day = column.day,
                            width = widths[index],
                            today = column.day == todayKey?.day,
                            formats = formats,
                            text = text,
                            colors = colors,
                            onClick = onSelectDay
                                ?.takeIf { columns.size > 1 }
                                ?.let { select -> { select(column.day) } },
                        )
                    }
                }
            }
            if (hasAllDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(gutter)
                            .padding(horizontal = tokens.GutterPadding)
                            .clearAndSetSemantics {},
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = text.allDay,
                            style = MonarchTheme.typography.caption,
                            color = colors.secondaryText,
                            textAlign = TextAlign.End,
                        )
                    }
                    Row(modifier = Modifier.horizontalScroll(horizontal)) {
                        columns.forEachIndexed { index, column ->
                            TimeGridAllDayCell(column.allDay, widths[index], text, onSelectEvent)
                        }
                    }
                }
            }
            MonarchDivider(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(vertical),
            ) {
                TimeGridGutter(range.hours, hour, gutter, formats, colors)
                TimeGridColumns(
                    columns = columns,
                    context = context,
                    now = todayKey,
                    drag = drag,
                    onDrag = { drag = it },
                    modifier = Modifier.horizontalScroll(horizontal),
                )
            }
        }
    }
}

/** The built-in strings, or their replacements. */
@Composable
private fun timeGridText(labels: MonarchTimeGridLabels): TimeGridText {
    val create = stringResource(R.string.monarch_timegrid_create_event)
    val open = stringResource(R.string.monarch_timegrid_open_day)
    val range = stringResource(R.string.monarch_timegrid_range)
    val until = stringResource(R.string.monarch_timegrid_until)
    return TimeGridText(
        allDay = monarchString(labels.allDay, R.string.monarch_timegrid_all_day),
        createEvent = labels.createEvent ?: { create.format(it) },
        today = monarchString(labels.today, R.string.monarch_timegrid_today),
        openDay = labels.openDay ?: { open.format(it) },
        range = labels.range ?: { start, end -> range.format(start, end) },
        until = labels.until ?: { until.format(it) },
        moves = TimeGridMoveText(
            earlier = monarchString(labels.moveEarlier, R.string.monarch_timegrid_move_earlier),
            later = monarchString(labels.moveLater, R.string.monarch_timegrid_move_later),
            previousDay = monarchString(labels.moveToPreviousDay, R.string.monarch_timegrid_move_previous_day),
            nextDay = monarchString(labels.moveToNextDay, R.string.monarch_timegrid_move_next_day),
        ),
    )
}
