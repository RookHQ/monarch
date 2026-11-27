package com.rookhq.monarch.components.scheduling.calendarmonthview

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.rookhq.monarch.internal.scheduling.MonarchMonthGrid

/**
 * Dragging an event from the selected day's list onto a day of the grid.
 * Positions are in the root's coordinates. The cells of the page on screen
 * report their bounds; the day under the finger is the drop target.
 */
@Stable
internal class CalendarMonthViewDrag {
    private val cells = HashMap<Int, HashMap<Long, Rect>>()

    /** The id of the event being dragged, or `null`. */
    var eventId: String? by mutableStateOf(null)
        private set

    /** How far the dragged event has moved from where it was picked up. */
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    /** The day under the finger, or `null`. */
    var target: Long? by mutableStateOf(null)
        private set

    private var start: Offset = Offset.Zero
    private var page: Int = 0

    /** Records where the cell of [day] is on pager page [page]. */
    fun place(page: Int, day: Long, bounds: Rect) {
        cells.getOrPut(page) { HashMap() }[day] = bounds
    }

    /** The day of page [page] whose cell contains [position], or `null`. */
    fun dayAt(page: Int, position: Offset): Long? =
        cells[page]?.entries?.firstOrNull { (_, bounds) -> bounds.contains(position) }?.key

    /** Picks up the event [id] at [position], on pager page [page]. */
    fun begin(id: String, position: Offset, page: Int) {
        eventId = id
        start = position
        offset = Offset.Zero
        this.page = page
        target = dayAt(page, position)
    }

    /** Moves the event by [delta]. */
    fun move(delta: Offset) {
        offset += delta
        target = dayAt(page, start + offset)
    }

    /** Puts the event down and returns the day it was dropped on, or `null`. */
    fun end(): Long? {
        val day = target
        cancel()
        return day
    }

    /** Puts the event back without moving it. */
    fun cancel() {
        eventId = null
        offset = Offset.Zero
        target = null
    }

    /** The whole days between [from] and [to], day keys; negative when [to] is earlier. */
    fun daysBetween(from: Long, to: Long): Int = ((to - from) / MonarchMonthGrid.DAY_MILLIS).toInt()
}
