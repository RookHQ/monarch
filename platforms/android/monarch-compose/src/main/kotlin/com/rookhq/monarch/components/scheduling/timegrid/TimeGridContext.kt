package com.rookhq.monarch.components.scheduling.timegrid

import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import java.util.TimeZone

/** What every column of one grid shares. */
@Suppress("LongParameterList")
internal class TimeGridContext(
    val range: TimeGridRange,
    val hourHeight: Dp,
    val snap: Int,
    val zone: TimeZone,
    val days: List<Long>,
    val widths: List<Dp>,
    val formats: TimeGridFormats,
    val text: TimeGridText,
    val colors: TimeGridColors,
    val canMoveEvent: ((MonarchAgendaEvent) -> Boolean)?,
    val onSelectSlot: ((MonarchTimeGridSlot) -> Unit)?,
    val onSelectEvent: ((MonarchAgendaEvent) -> Unit)?,
    val onMoveEvent: ((MonarchTimeGridMove) -> Unit)?,
) {
    /** Whether [event] can be moved. */
    fun movable(event: MonarchAgendaEvent): Boolean = onMoveEvent != null && (canMoveEvent?.invoke(event) ?: true)

    /** The distance from the top of the grid to [minutes]. */
    fun y(minutes: Int): Dp = hourHeight * (minutes - range.start) / TimeGridModel.MINUTES_PER_HOUR

    /** The height of [minutes]. */
    fun height(minutes: Int): Dp = hourHeight * minutes / TimeGridModel.MINUTES_PER_HOUR
}
