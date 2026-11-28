package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates and remembers the [MonarchGanttTimelineState] of a `MonarchGanttTimeline`, kept across configuration
 * changes.
 *
 * @param initialStart the first visible day as a day key, or `null` to start a week before today.
 * @param initialZoom the zoom at first.
 * @param initialView the chart or the list at first.
 */
@Composable
public fun rememberMonarchGanttTimelineState(
    initialStart: Long? = null,
    initialZoom: MonarchGanttTimelineZoom = MonarchGanttTimelineZoom.Month,
    initialView: MonarchGanttTimelineView = MonarchGanttTimelineView.Chart,
): MonarchGanttTimelineState = rememberSaveable(saver = MonarchGanttTimelineState.Saver) {
    MonarchGanttTimelineState(start = initialStart, zoom = initialZoom, view = initialView)
}
