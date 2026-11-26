package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * One row of a `MonarchGanttTimeline`: a task, issue, project or phase.
 *
 * @property id a stable id, unique among the items.
 * @property label the plain-text name, shown in the row heading and on the bar and read by TalkBack.
 * @property start the first day as a day key (the epoch millisecond of its midnight in UTC; any time on the date in
 * UTC is read as that date), or `null`.
 * @property end the last day, inclusive, as a day key, or `null`. With only one of [start] and [end] the item is a
 * one-day marker; with neither it is unscheduled.
 * @property parentId the id of the parent item, for sub-tasks. The parent can be collapsed; without dates of its
 * own it shows a bracket over its children.
 * @property groupId the id of a `MonarchGanttTimelineGroup` the item is listed under.
 * @property progress the completion from 0 to 1, drawn as a strip along the bottom of the bar and read as a
 * percentage, or `null` for none.
 * @property tone the colour of the bar; `null` is the accent colour. Pair a tone with words: colour is never the only
 * signal.
 * @property editable `false` locks this item's dates when the timeline is editable.
 * @property conflict a scheduling problem, such as "Starts before Design ends". The bar gets a dashed danger outline
 * and the text is read with the bar and written in the list.
 * @property meta a short extra text under the row heading, such as a status or a count.
 */
@Immutable
public class MonarchGanttTimelineItem(
    public val id: String,
    public val label: String,
    public val start: Long? = null,
    public val end: Long? = null,
    public val parentId: String? = null,
    public val groupId: String? = null,
    public val progress: Float? = null,
    public val tone: MonarchTone? = null,
    public val editable: Boolean = true,
    public val conflict: String? = null,
    public val meta: String? = null,
) {
    /** The item's own dates. */
    public val dates: MonarchGanttTimelineDates get() = MonarchGanttTimelineDates(start, end)
}
