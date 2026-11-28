package dev.monarchui.sample.sections.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.components.scheduling.timegrid.MonarchTimeGrid
import dev.monarchui.components.scheduling.timegrid.MonarchTimeGridUnavailable
import dev.monarchui.components.scheduling.timegrid.MonarchTimeGridWorkingHours
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.util.Calendar
import java.util.TimeZone

private const val Minute = 60_000L
private const val Hour = 60 * Minute
private const val Day = 24 * Hour
private val GridHeight = 420.dp
private const val StandUp = 9 * Hour
private const val StandUpEnd = StandUp + 15 * Minute
private const val Review = 10 * Hour
private const val Interview = Review + 30 * Minute
private const val ReviewEnd = 11 * Hour + 30 * Minute
private const val Lunch = 12 * Hour
private const val LunchEnd = 13 * Hour
private const val OpenMinutes = 9 * 60
private const val CloseMinutes = 17 * 60
private const val WorkDays = 5

/** The catalogue page of TimeGrid: a day view to create and move events, and a work week. */
object MonarchTimeGridSection : CataloguePage {
    override val title = "TimeGrid"

    @Composable
    override fun Content() {
        val zone = remember { TimeZone.getDefault() }
        // Today's date as a day key (UTC midnight), and its local midnight for placing events.
        val today = remember {
            val local = Calendar.getInstance(zone)
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }
        val midnight = remember {
            Calendar.getInstance(zone).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        var events by remember {
            mutableStateOf(
                listOf(
                    MonarchAgendaEvent("standup", "Stand-up", midnight + StandUp, midnight + StandUpEnd),
                    MonarchAgendaEvent(
                        "review",
                        "Design review",
                        midnight + Review,
                        midnight + ReviewEnd,
                        detail = "Room 4",
                    ),
                    MonarchAgendaEvent(
                        "interview",
                        "Interview",
                        midnight + Interview,
                        midnight + ReviewEnd,
                        status = MonarchEventChipStatus.Tentative,
                    ),
                    MonarchAgendaEvent("offsite", "Team offsite", midnight + Day, allDay = true),
                ),
            )
        }
        var said by rememberSaveable {
            mutableStateOf("Tap an empty time to add an event; long press an event to move it.")
        }
        MonarchText(text = said, style = MonarchTextStyle.Caption)
        CatalogueCaption("Day: create, open and move")
        MonarchTimeGrid(
            days = listOf(today),
            modifier = Modifier.height(GridHeight),
            events = events,
            workingHours = (1..WorkDays).map { MonarchTimeGridWorkingHours(it, OpenMinutes, CloseMinutes) },
            unavailable = listOf(
                MonarchTimeGridUnavailable("lunch", midnight + Lunch, midnight + LunchEnd, "Lunch"),
            ),
            onSelectSlot = { slot ->
                events = events + MonarchAgendaEvent("new-${events.size}", "New event", slot.start, slot.start + Hour)
                said = "Added an event"
            },
            onSelectEvent = { said = "Opened ${it.title}" },
            onMoveEvent = { move ->
                events = events.map { event ->
                    if (event.id != move.event.id) {
                        event
                    } else {
                        MonarchAgendaEvent(
                            event.id,
                            event.title,
                            move.start,
                            move.end,
                            event.allDay,
                            event.detail,
                            event.color,
                            event.status,
                        )
                    }
                }
                said = "Moved ${move.event.title}"
            },
        )
        CatalogueCaption("Work week")
        MonarchTimeGrid(
            days = (0 until WorkDays).map { today + it * Day },
            modifier = Modifier.height(GridHeight),
            events = events,
            onSelectDay = { said = "Open the day" },
            onSelectEvent = { said = "Opened ${it.title}" },
        )
    }
}
