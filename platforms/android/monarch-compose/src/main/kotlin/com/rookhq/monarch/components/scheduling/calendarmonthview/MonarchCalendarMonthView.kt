package com.rookhq.monarch.components.scheduling.calendarmonthview

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.internal.scheduling.MonarchMonthGrid
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Calendar

/**
 * A month of days with their events: a weekday header, today, the selected
 * day and the neighbouring months' days marked, a swipe between months, and
 * the selected day's events listed under the grid, where they can be opened
 * and dragged to another day.
 *
 * Built by Monarch: Material 3 has no month view with events. The months are
 * pages of a Compose `HorizontalPager`; each event is drawn by
 * `MonarchEventChip`. Days narrower than 80dp (a phone) show up to three
 * coloured dots and "+N"; wider days (a tablet) list up to [maxEvents]
 * events by title and "+N more". Every day is one button with a 48dp target
 * reserved in the layout; when the width offered is under seven of them
 * (336dp) the month scrolls sideways instead of shrinking the days.
 *
 * Days are epoch milliseconds of their midnight in UTC, as in the Material 3
 * `DatePickerState`. Events are instants placed on the days they touch in
 * [timeZone]. Weekday and date names follow the locale of the configuration,
 * the first day of the week the locale's calendar, and times the person's
 * 12- or 24-hour setting.
 *
 * TalkBack reads the month as a grid of seven columns with weekday headers;
 * each day is one element read as its full date, its number of events,
 * "Today" and its badge, and the selected day is selected. The selected
 * day's heading announces itself when the selection changes. Moving an event
 * is also offered as TalkBack actions: a day or a week earlier or later.
 *
 * @param month any day of the month shown. Swiping to another month calls [onMonthChange]; changing it moves
 * the pager, with an animation unless animations are off.
 * @param events the events in any order. A longer event is shown on every day it touches.
 * @param onMonthChange called with the first day of the month the person swipes to. Store it in [month].
 * @param modifier the modifier of the view.
 * @param selectedDay the day whose events are listed under the grid, or `null` for no list.
 * @param onDayClick called with the day the person taps, for example to set [selectedDay] or open a day view.
 * Without it the days are not buttons.
 * @param onEventClick opens an event of the selected day's list. Without it the events are labels.
 * @param onEventMove called when the person drags an event of the selected day's list onto another day, or uses
 * one of the TalkBack move actions, with the event and the number of days to move it by (negative is earlier).
 * Without it events cannot be moved.
 * @param today today's date. `null` is today in [timeZone]. Pass a fixed value in tests and previews.
 * @param timeZone the zone events are placed and their times shown in. An IANA id such as `"Europe/Berlin"`. `null`, or
 * an id the device does not know, is the device's zone.
 * @param firstDayOfWeek the first column, `Calendar.SUNDAY` to `Calendar.SATURDAY`. `null` follows the locale.
 * @param fixedWeeks always six week rows, so the height does not change between months.
 * @param maxEvents the events a wide day lists before "+N more"; when there are more, one fewer is listed so the
 * overflow line takes the last place. Narrow days always show at most three dots.
 * @param is24Hour whether times are shown on the 24-hour clock. `null` follows the device setting.
 * @param listMonth list every event of the shown month under the grid, a heading with the date and the day's
 * events, instead of only the selected day's, so a phone shows what a wide grid prints in its cells.
 * @param chips keeps the grid of a wide month on a narrow screen: the seven columns share the width, and each day lists
 * its events as small truncated chips with "+N more", instead of dots and a list below. `false` is the default.
 * @param dayState hatching and a short badge for a day, such as time off or a holiday. Called for every day shown.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchCalendarMonthView(
    month: Long,
    events: List<MonarchAgendaEvent>,
    onMonthChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selectedDay: Long? = null,
    onDayClick: ((Long) -> Unit)? = null,
    onEventClick: ((MonarchAgendaEvent) -> Unit)? = null,
    onEventMove: ((MonarchAgendaEvent, Int) -> Unit)? = null,
    today: Long? = null,
    timeZone: String? = null,
    firstDayOfWeek: Int? = null,
    fixedWeeks: Boolean = true,
    maxEvents: Int = 3,
    is24Hour: Boolean? = null,
    listMonth: Boolean = false,
    dayState: ((Long) -> MonarchCalendarMonthViewDayState?)? = null,
    labels: MonarchCalendarMonthViewLabels = MonarchCalendarMonthViewLabels(),
    chips: Boolean = false,
) {
    val zone = monarchTimeZone(timeZone)
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val clock24 = is24Hour ?: DateFormat.is24HourFormat(context)
    val formats = remember(locale, zone, clock24) { CalendarMonthViewFormats(locale, zone, clock24) }
    val weekStart = firstDayOfWeek ?: remember(locale) { Calendar.getInstance(locale).firstDayOfWeek }
    val todayKey = today?.let(MonarchMonthGrid::dayOf)
        ?: remember(zone) { MonarchMonthGrid.dayOf(System.currentTimeMillis(), zone) }
    val selected = selectedDay?.let(MonarchMonthGrid::dayOf)
    val text = calendarMonthViewText(labels)
    val drag = remember { CalendarMonthViewDrag() }

    val page = MonarchMonthGrid.pageOf(month)
    val pager = rememberPagerState(initialPage = page) { MonarchMonthGrid.PAGE_COUNT }
    val currentPage by rememberUpdatedState(page)
    val changeMonth by rememberUpdatedState(onMonthChange)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { settled ->
            if (settled != currentPage) changeMonth(MonarchMonthGrid.monthOfPage(settled))
        }
    }
    val motion = MonarchTheme.motion
    LaunchedEffect(page) {
        if (pager.currentPage != page) {
            if (motion.reducedMotion) {
                pager.scrollToPage(page)
            } else {
                pager.animateScrollToPage(page, animationSpec = motion.spec())
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tokens = MonarchCalendarMonthViewTokens
            val offered = if (constraints.hasBoundedWidth) maxWidth else tokens.CellMinWidth * DAYS
            val cellWidth = if (chips) offered / DAYS else MonarchCalendarMonthViewTokens.cellWidth(offered)
            val scrolls = cellWidth * DAYS > offered
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                key = { it },
            ) { shown ->
                val shownMonth = MonarchMonthGrid.monthOfPage(shown)
                val weeks = remember(shownMonth, weekStart, fixedWeeks) {
                    MonarchMonthGrid.weeks(shownMonth, weekStart, fixedWeeks)
                }
                val byDay = remember(events, weeks, zone) {
                    CalendarMonthViewEvents.eventsByDay(events, weeks.flatten(), zone)
                }
                Box(modifier = if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier) {
                    CalendarMonthViewPage(
                        page = shown,
                        month = shownMonth,
                        weeks = weeks,
                        byDay = byDay,
                        cellWidth = cellWidth,
                        today = todayKey,
                        selectedDay = selected,
                        maxEvents = maxEvents.coerceAtLeast(1),
                        zone = zone,
                        dayState = dayState,
                        formats = formats,
                        text = text,
                        drag = drag,
                        onDayClick = onDayClick,
                        chips = chips,
                    ).Content()
                }
            }
        }
        if (listMonth) {
            val shownMonth = MonarchMonthGrid.monthOfPage(page)
            val monthDays = remember(shownMonth) { MonarchMonthGrid.daysOfMonth(shownMonth) }
            val byDay = remember(events, monthDays, zone) {
                CalendarMonthViewEvents.eventsByDay(events, monthDays, zone)
            }
            monthDays.filter { byDay[it].orEmpty().isNotEmpty() }.forEach { day ->
                CalendarMonthViewDayPanel(
                    day = day,
                    events = byDay[day].orEmpty(),
                    zone = zone,
                    formats = formats,
                    text = text,
                    page = pager.currentPage,
                    drag = drag,
                    onEventClick = onEventClick,
                    onEventMove = onEventMove,
                )
            }
        } else if (selected != null) {
            val dayEvents = remember(events, selected, zone) {
                CalendarMonthViewEvents.eventsByDay(events, listOf(selected), zone)[selected].orEmpty()
            }
            CalendarMonthViewDayPanel(
                day = selected,
                events = dayEvents,
                zone = zone,
                formats = formats,
                text = text,
                page = pager.currentPage,
                drag = drag,
                onEventClick = onEventClick,
                onEventMove = onEventMove,
            )
        }
    }
}

private const val DAYS = MonarchMonthGrid.DAYS_PER_WEEK
private const val WEEK = 7

/** The built-in strings, each replaced by its label when given. */
@Composable
private fun calendarMonthViewText(labels: MonarchCalendarMonthViewLabels): CalendarMonthViewText {
    val dayFormat = stringResource(R.string.monarch_calendarmonthview_day)
    val resources = LocalResources.current
    val moreShort = stringResource(R.string.monarch_calendarmonthview_more_short)
    val until = stringResource(R.string.monarch_calendarmonthview_until)
    val range = stringResource(R.string.monarch_calendarmonthview_range)
    return CalendarMonthViewText(
        today = monarchString(labels.today, R.string.monarch_calendarmonthview_today),
        day = labels.day ?: { date, count ->
            dayFormat.format(
                date,
                resources.getQuantityString(R.plurals.monarch_calendarmonthview_count, count, count),
            )
        },
        more = labels.more ?: { count ->
            resources.getQuantityString(R.plurals.monarch_calendarmonthview_more, count, count)
        },
        moreShort = labels.moreShort ?: { count -> moreShort.format(count) },
        noEvents = monarchString(labels.noEvents, R.string.monarch_calendarmonthview_no_events),
        allDay = monarchString(labels.allDay, R.string.monarch_calendarmonthview_all_day),
        until = labels.until ?: { end -> until.format(end) },
        range = labels.range ?: { start, end -> range.format(start, end) },
        moves = listOf(
            monarchString(labels.moveDayEarlier, R.string.monarch_calendarmonthview_move_day_earlier) to -1,
            monarchString(labels.moveDayLater, R.string.monarch_calendarmonthview_move_day_later) to 1,
            monarchString(labels.moveWeekEarlier, R.string.monarch_calendarmonthview_move_week_earlier) to -WEEK,
            monarchString(labels.moveWeekLater, R.string.monarch_calendarmonthview_move_week_later) to WEEK,
        ),
    )
}
