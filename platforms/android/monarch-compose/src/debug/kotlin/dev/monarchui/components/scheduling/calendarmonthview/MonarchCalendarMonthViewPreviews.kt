package dev.monarchui.components.scheduling.calendarmonthview

import androidx.compose.runtime.Composable
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Calendar
import java.util.TimeZone

private const val PreviewDay = 86_400_000L
private const val PreviewHour = 3_600_000L

/** 1 November 2026, UTC midnight. */
private const val PreviewMonth = 1_793_491_200_000L

private val PreviewEvents = listOf(
    MonarchAgendaEvent("a", "Release day", PreviewMonth + 3 * PreviewDay, allDay = true),
    MonarchAgendaEvent("b", "Stand-up", PreviewMonth + 3 * PreviewDay + 9 * PreviewHour),
    MonarchAgendaEvent("c", "Design review", PreviewMonth + 3 * PreviewDay + 11 * PreviewHour),
    MonarchAgendaEvent(
        "d",
        "Retrospective",
        PreviewMonth + 3 * PreviewDay + 16 * PreviewHour,
        status = MonarchEventChipStatus.Cancelled,
    ),
    MonarchAgendaEvent(
        "e",
        "Vendor call",
        PreviewMonth + 11 * PreviewDay + 14 * PreviewHour,
        status = MonarchEventChipStatus.Tentative,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchCalendarMonthViewPreview() {
    PreviewFrame {
        MonarchCalendarMonthView(
            month = PreviewMonth,
            events = PreviewEvents,
            onMonthChange = {},
            selectedDay = PreviewMonth + 3 * PreviewDay,
            onDayClick = {},
            onEventClick = {},
            today = PreviewMonth + 13 * PreviewDay,
            timeZone = "UTC",
            firstDayOfWeek = Calendar.MONDAY,
            dayState = { day ->
                if (day == PreviewMonth + 25 * PreviewDay) {
                    MonarchCalendarMonthViewDayState(hatched = true, badge = "Holiday")
                } else {
                    null
                }
            },
        )
    }
}
