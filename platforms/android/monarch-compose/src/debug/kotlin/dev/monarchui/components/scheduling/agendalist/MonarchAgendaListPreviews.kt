package dev.monarchui.components.scheduling.agendalist

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens
import java.util.TimeZone

private const val PreviewDay = 1_793_750_400_000L
private const val PreviewHour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchAgendaListPreview() {
    PreviewFrame {
        MonarchAgendaList(
            events = listOf(
                MonarchAgendaEvent("1", "Stand-up", PreviewDay + 9 * PreviewHour, PreviewDay + 10 * PreviewHour),
                MonarchAgendaEvent("2", "Offsite", PreviewDay, allDay = true),
                MonarchAgendaEvent(
                    "3",
                    "Release night",
                    PreviewDay + 22 * PreviewHour,
                    PreviewDay + 50 * PreviewHour,
                    status = MonarchEventChipStatus.Tentative,
                    detail = "War room",
                ),
            ),
            modifier = Modifier.height(MonarchTokens.Space.space24.dp * 4),
            today = PreviewDay,
            timeZone = "UTC",
            dayNotes = mapOf(PreviewDay to listOf("Ana is out of office")),
            onSelect = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAgendaListEmptyPreview() {
    PreviewFrame {
        MonarchAgendaList(events = emptyList())
    }
}
