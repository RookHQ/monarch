package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.semantics.CustomAccessibilityAction

/** A bar being dragged: which item, which dates move and by how many days so far. */
@Immutable
internal data class GanttTimelineDrag(val id: String, val shift: MonarchGanttTimelineShift, val days: Int)

/** Everything the chart, the list and the toolbar share: the data, the window, the strings and the callbacks. */
@Suppress("LongParameterList")
internal class GanttTimelineModel(
    val items: List<MonarchGanttTimelineItem>,
    val groups: List<MonarchGanttTimelineGroup>,
    val dependencies: List<MonarchGanttTimelineDependency>,
    val milestones: List<MonarchGanttTimelineMilestone>,
    val lines: GanttTimelineLines,
    val state: MonarchGanttTimelineState,
    val anchor: Long,
    val today: Long?,
    val onChange: ((MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Unit)?,
    val onOpen: ((MonarchGanttTimelineItem) -> Unit)?,
    val text: GanttTimelineText,
    val formats: GanttTimelineFormats,
    val announce: (String) -> Unit,
    val itemMeta: (@Composable (MonarchGanttTimelineItem) -> Unit)? = null,
    val groupMeta: (@Composable (MonarchGanttTimelineGroup) -> Unit)? = null,
) {
    val zoom: MonarchGanttTimelineZoom get() = state.zoom

    /** The last visible day. */
    val rangeEnd: Long = anchor + MonarchGanttTimelineTokens.span(state.zoom) - 1

    val itemsById: Map<String, MonarchGanttTimelineItem> = items.associateBy { it.id }

    /** Milestones that share the lane at the top. */
    val laneMilestones: List<MonarchGanttTimelineMilestone> = milestones.filter { it.itemId == null }

    /** Whether the timeline can change dates at all. */
    val editable: Boolean get() = onChange != null

    /** Whether [item]'s dates can be changed. */
    fun editable(item: MonarchGanttTimelineItem): Boolean = onChange != null && item.editable

    /** "Jan 12, 2026". */
    fun date(day: Long): String = formats.date(day)

    /** The visible window: "Jan 5, 2026 – Apr 5, 2026". */
    fun range(): String = text.range(date(anchor), date(rangeEnd))

    /** "60%", or `null` without progress. */
    fun percent(item: MonarchGanttTimelineItem): String? =
        item.progress?.let { formats.percent(it.coerceIn(0f, 1f)) }

    /** What TalkBack reads for a bar: the dates, the progress and the conflict. */
    fun describe(item: MonarchGanttTimelineItem, span: GanttTimelineSpan): String {
        val bar = text.bar(item.label, date(span.start), date(span.end), percent(item))
        return item.conflict?.let { "$bar. ${text.conflict(it)}" } ?: bar
    }

    /** The labels of the items [item] depends on, in the order of the dependencies. */
    fun dependsOn(item: MonarchGanttTimelineItem): List<String> =
        dependencies.filter { it.to == item.id }.mapNotNull { itemsById[it.from]?.label }

    /** Reports new dates for [item] and announces them. Does nothing when they did not change or it is locked. */
    fun commit(item: MonarchGanttTimelineItem, dates: MonarchGanttTimelineDates) {
        val change = onChange ?: return
        if (!item.editable) return
        val before = item.dates.shifted(MonarchGanttTimelineShift.Move, 0)
        if (dates == before) return
        change(item, dates)
        GanttTimelineRules.span(dates.start, dates.end)?.let { span ->
            announce(text.changed(item.label, date(span.start), date(span.end)))
        }
    }

    /** Moves [item] by [days] with [shift]. */
    fun shift(item: MonarchGanttTimelineItem, shift: MonarchGanttTimelineShift, days: Int) {
        commit(item, item.dates.shifted(shift, days))
    }

    /** The TalkBack actions of an editable bar: move it, and for a bar of two dates, move either end. */
    fun actions(item: MonarchGanttTimelineItem, point: Boolean): List<CustomAccessibilityAction> {
        fun action(label: String, shift: MonarchGanttTimelineShift, days: Int) =
            CustomAccessibilityAction(label) {
                shift(item, shift, days)
                true
            }
        val move = listOf(
            action(text.moveEarlier, MonarchGanttTimelineShift.Move, -1),
            action(text.moveLater, MonarchGanttTimelineShift.Move, 1),
        )
        if (point) return move
        return move + listOf(
            action(text.startEarlier, MonarchGanttTimelineShift.Start, -1),
            action(text.startLater, MonarchGanttTimelineShift.Start, 1),
            action(text.endEarlier, MonarchGanttTimelineShift.End, -1),
            action(text.endLater, MonarchGanttTimelineShift.End, 1),
        )
    }
}
