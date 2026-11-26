package com.rookhq.monarch.sample.sections.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaList
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Calendar

private const val Hour = 3_600_000L
private const val Day = 24 * Hour
private const val Quarter = Hour / 4
private const val StandUp = 9 * Hour
private const val Review = 14 * Hour
private const val Release = 22 * Hour
private const val ReleaseEnd = Release + Day + 4 * Hour
private const val Retro = 3 * Day + 16 * Hour
private val ListHeight = 420.dp
private val EmptyHeight = 240.dp

/** The catalogue page of AgendaList. */
object MonarchAgendaListSection : CataloguePage {
    override val title = "AgendaList"

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf("Nothing opened") }
        val today = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val events = listOf(
            MonarchAgendaEvent("standup", "Stand-up", today + StandUp, today + StandUp + Quarter),
            MonarchAgendaEvent("offsite", "Team offsite", today, allDay = true),
            MonarchAgendaEvent(
                "review",
                "Design review",
                today + Review,
                today + Review + Hour,
                detail = "Room 4",
                color = MonarchTheme.colors.successSolid,
                status = MonarchEventChipStatus.Tentative,
            ),
            MonarchAgendaEvent("release", "Release night", today + Release, today + ReleaseEnd),
            MonarchAgendaEvent(
                "retro",
                "Retro",
                today + Retro,
                status = MonarchEventChipStatus.Cancelled,
            ),
        )
        MonarchText(text = opened, style = MonarchTextStyle.Caption)
        CatalogueCaption("Days")
        MonarchAgendaList(
            events = events,
            modifier = Modifier.height(ListHeight),
            dayNotes = mapOf(today + Day to listOf("Ana is out of office")),
            onSelect = { opened = "Opened ${it.title}" },
        )
        CatalogueCaption("Empty")
        MonarchAgendaList(events = emptyList(), modifier = Modifier.height(EmptyHeight))
    }
}
