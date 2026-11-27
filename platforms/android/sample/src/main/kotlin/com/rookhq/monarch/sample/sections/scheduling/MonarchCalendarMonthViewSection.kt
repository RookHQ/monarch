package com.rookhq.monarch.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthView
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthViewDayState
import com.rookhq.monarch.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import com.rookhq.monarch.components.scheduling.calendartoolbar.MonarchCalendarToolbarView
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** The catalogue page of CalendarMonthView, under a MonarchCalendarToolbar. */
object MonarchCalendarMonthViewSection : CataloguePage {
    override val title = "CalendarMonthView"

    private const val DAY = 86_400_000L
    private const val HOUR = 3_600_000L
    private const val MONTHS_PER_YEAR = 12
    private const val QUARTERS = 4
    private const val STANDUP = 9
    private const val REVIEW = 11
    private const val LUNCH = 12
    private const val RETRO = 16
    private const val CALL = 14
    private const val CONFERENCE = 5

    @Composable
    override fun Content() {
        val utc = TimeZone.getTimeZone("UTC")
        val today = remember { System.currentTimeMillis() / DAY * DAY }
        var month by rememberSaveable { mutableLongStateOf(firstOfMonth(today)) }
        var selected by rememberSaveable { mutableStateOf<Long?>(today) }
        var events by remember { mutableStateOf(sampleEvents(today)) }
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        val monthTitle = remember(month) {
            android.icu.text.DateFormat.getInstanceForSkeleton("yMMMM").apply {
                timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
            }.format(Date(month))
        }

        CatalogueCaption("Swipe between months, tap a day, long-press an event to drag it")
        MonarchCalendarToolbar(
            title = monthTitle,
            view = MonarchCalendarToolbarView.Month,
            onToday = {
                month = firstOfMonth(today)
                selected = today
            },
            onPrevious = { month = addMonths(month, -1) },
            onNext = { month = addMonths(month, 1) },
            previousIcon = { MonarchIcon(imageVector = SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                        MonarchIcon(
                    imageVector = SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            views = listOf(MonarchCalendarToolbarView.Month),
        )
        MonarchCalendarMonthView(
            month = month,
            events = events,
            onMonthChange = { month = it },
            selectedDay = selected,
            onDayClick = { selected = it },
            onEventClick = { opened = it.title },
            onEventMove = { event, days ->
                events = events.map {
                    if (it.id != event.id) {
                        it
                    } else {
                        MonarchAgendaEvent(
                            id = it.id,
                            title = it.title,
                            start = it.start + days * DAY,
                            end = it.end?.plus(days * DAY),
                            allDay = it.allDay,
                            detail = it.detail,
                            color = it.color,
                            status = it.status,
                        )
                    }
                }
                selected = selected?.plus(days * DAY)
            },
            timeZone = utc.id,
            dayState = { day ->
                if (dayOfWeek(day) == Calendar.SUNDAY) {
                    MonarchCalendarMonthViewDayState(hatched = true, badge = "Closed")
                } else {
                    null
                }
            },
        )
        MonarchText(text = opened?.let { "Opened: $it" } ?: "No event opened", style = MonarchTextStyle.Caption)
    }

    private fun sampleEvents(today: Long): List<MonarchAgendaEvent> {
        fun at(hour: Int) = today + hour * HOUR
        return listOf(
            MonarchAgendaEvent("1", "Release day", today, allDay = true),
            MonarchAgendaEvent("2", "Stand-up", at(STANDUP), end = at(STANDUP) + HOUR / QUARTERS),
            MonarchAgendaEvent("3", "Design review", at(REVIEW), end = at(REVIEW + 1), detail = "Room 4"),
            MonarchAgendaEvent("4", "Lunch and learn", at(LUNCH), end = at(LUNCH + 1)),
            MonarchAgendaEvent(
                "5",
                "Retrospective",
                at(RETRO),
                end = at(RETRO + 1),
                status = MonarchEventChipStatus.Cancelled,
            ),
            MonarchAgendaEvent(
                "6",
                "Vendor call",
                today + 2 * DAY + CALL * HOUR,
                status = MonarchEventChipStatus.Tentative,
            ),
            MonarchAgendaEvent(
                "7",
                "Conference",
                today + CONFERENCE * DAY,
                end = today + (CONFERENCE + 2) * DAY,
                allDay = true,
            ),
        )
    }

    private fun utc(date: Long): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = date }

    private fun dayOfWeek(date: Long): Int = utc(date).get(Calendar.DAY_OF_WEEK)

    private fun firstOfMonth(date: Long): Long = utc(date).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    private fun addMonths(month: Long, count: Int): Long {
        val calendar = utc(month)
        val index = calendar.get(Calendar.YEAR) * MONTHS_PER_YEAR + calendar.get(Calendar.MONTH) + count
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(index / MONTHS_PER_YEAR, index % MONTHS_PER_YEAR, 1)
        }.timeInMillis
    }
}
