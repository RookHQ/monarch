package dev.monarchui.components.scheduling.timegrid

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens
import java.util.TimeZone

/** Wednesday 4 November 2026, 00:00 UTC. */
private const val PreviewDay = 1_793_750_400_000L
private const val PreviewHour = 3_600_000L
private const val PreviewMinute = 60_000L
private val PreviewUtc = TimeZone.getTimeZone("UTC")

private val PreviewEvents = listOf(
    MonarchAgendaEvent(
        "1",
        "Design review",
        PreviewDay + 9 * PreviewHour + 30 * PreviewMinute,
        PreviewDay + 11 * PreviewHour,
        detail = "Room 4",
    ),
    MonarchAgendaEvent(
        "2",
        "Interview",
        PreviewDay + 10 * PreviewHour,
        PreviewDay + 11 * PreviewHour,
        status = MonarchEventChipStatus.Tentative,
    ),
    MonarchAgendaEvent("3", "Conference", PreviewDay, allDay = true),
)

@MonarchPreviews
@Composable
internal fun MonarchTimeGridDayPreview() {
    PreviewFrame {
        MonarchTimeGrid(
            days = listOf(PreviewDay),
            modifier = Modifier.height(MonarchTokens.Space.space24.dp * 4),
            events = PreviewEvents,
            now = PreviewDay + 10 * PreviewHour + 40 * PreviewMinute,
            workingHours = (1..5).map { MonarchTimeGridWorkingHours(it, 9 * 60, 17 * 60) },
            unavailable = listOf(
                MonarchTimeGridUnavailable(
                    "lunch",
                    PreviewDay + 12 * PreviewHour,
                    PreviewDay + 13 * PreviewHour,
                    "Closed",
                ),
            ),
            scrollToMinutes = 8 * 60,
            timeZone = PreviewUtc.id,
            onSelectSlot = {},
            onSelectEvent = {},
            onMoveEvent = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTimeGridWeekPreview() {
    PreviewFrame {
        MonarchTimeGrid(
            days = (0 until 5).map { PreviewDay - 2 * 24 * PreviewHour + it * 24 * PreviewHour },
            modifier = Modifier.height(MonarchTokens.Space.space24.dp * 4),
            events = PreviewEvents,
            now = PreviewDay + 10 * PreviewHour,
            scrollToMinutes = 9 * 60,
            timeZone = PreviewUtc.id,
            onSelectDay = {},
        )
    }
}
