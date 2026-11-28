package dev.monarchui.components.scheduling.agendalist

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus

/**
 * One event in a `MonarchAgendaList`.
 *
 * @property id a stable identifier, unique in the list. A multi-day event keeps its id on every day it is shown.
 * @property title the event title, already localised.
 * @property start when the event starts, in epoch milliseconds.
 * @property end when it ends, in epoch milliseconds, or `null` for no end. A timed event's end is exclusive: an
 * event that ends exactly at midnight does not appear on the next day. An all-day event's end is any time on its
 * last day. An end before the start counts as no end.
 * @property allDay the event takes whole days: it is listed first in each day and reads "All day".
 * @property detail a second, muted line: a location, a service, an organiser.
 * @property color the event's colour, for the chip's bar and tint only. Unspecified is the theme's accent.
 * @property status confirmed, tentative, cancelled or declined, drawn and read by `MonarchEventChip`.
 */
@Immutable
public class MonarchAgendaEvent(
    public val id: String,
    public val title: String,
    public val start: Long,
    public val end: Long? = null,
    public val allDay: Boolean = false,
    public val detail: String? = null,
    public val color: Color = Color.Unspecified,
    public val status: MonarchEventChipStatus = MonarchEventChipStatus.Confirmed,
)
