package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable

/**
 * A heading row of a `MonarchGanttTimeline` that groups items, such as an initiative or an epic.
 *
 * @property id a stable id, matched by `MonarchGanttTimelineItem.groupId`.
 * @property label the heading.
 * @property start the first day of an optional span drawn as a thin bar on the group row, as a day key.
 * @property end the last day of that span, as a day key.
 * @property meta a short extra text under the heading, such as a count or a health word.
 */
@Immutable
public class MonarchGanttTimelineGroup(
    public val id: String,
    public val label: String,
    public val start: Long? = null,
    public val end: Long? = null,
    public val meta: String? = null,
)
