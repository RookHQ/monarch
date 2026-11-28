package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/**
 * What a `MonarchGanttTimeline` shows: the first visible day, the zoom, chart or list, and the collapsed parents.
 * The toolbar and the rows change it; the app can read and set it. Create it with
 * `rememberMonarchGanttTimelineState`.
 *
 * @param start the first visible day as a day key, or `null` to start a week before today (or before the earliest
 * item when there is no today).
 * @param zoom the zoom.
 * @param view the chart or the list.
 * @param collapsed the ids of the parent items whose children are hidden.
 */
@Stable
public class MonarchGanttTimelineState(
    start: Long? = null,
    zoom: MonarchGanttTimelineZoom = MonarchGanttTimelineZoom.Month,
    view: MonarchGanttTimelineView = MonarchGanttTimelineView.Chart,
    collapsed: Set<String> = emptySet(),
) {
    /** The first visible day as a day key, or `null` for the automatic start. */
    public var start: Long? by mutableStateOf(start?.let(GanttTimelineDays::key))

    /** The zoom. */
    public var zoom: MonarchGanttTimelineZoom by mutableStateOf(zoom)

    /** The chart or the list. */
    public var view: MonarchGanttTimelineView by mutableStateOf(view)

    /** The ids of the parent items whose children are hidden. */
    public var collapsed: Set<String> by mutableStateOf(collapsed)

    /** Hides the children of [id] when they are shown, and shows them when they are hidden. */
    public fun toggle(id: String) {
        collapsed = if (id in collapsed) collapsed - id else collapsed + id
    }

    /** Saves and restores a [MonarchGanttTimelineState] across configuration changes. */
    public companion object {
        /** The [Saver] used by `rememberMonarchGanttTimelineState`. */
        public val Saver: Saver<MonarchGanttTimelineState, Any> = listSaver(
            save = { listOf(it.start ?: NO_START, it.zoom.ordinal, it.view.ordinal, ArrayList(it.collapsed)) },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                MonarchGanttTimelineState(
                    start = (saved[0] as Long).takeIf { it != NO_START },
                    zoom = MonarchGanttTimelineZoom.entries[saved[1] as Int],
                    view = MonarchGanttTimelineView.entries[saved[2] as Int],
                    collapsed = (saved[3] as List<String>).toSet(),
                )
            },
        )

        private const val NO_START = Long.MIN_VALUE
    }
}
