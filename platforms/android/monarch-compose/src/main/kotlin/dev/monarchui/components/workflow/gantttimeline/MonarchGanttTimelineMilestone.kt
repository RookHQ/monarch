package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable

/**
 * A dated checkpoint of a `MonarchGanttTimeline`, drawn as a diamond.
 *
 * @property id a stable id.
 * @property label the name, such as "Beta".
 * @property date the day, as a day key.
 * @property done whether it is reached: a filled diamond, and read as done.
 * @property itemId draws it on this item's row; without it, milestones share a lane at the top with their names.
 */
@Immutable
public class MonarchGanttTimelineMilestone(
    public val id: String,
    public val label: String,
    public val date: Long,
    public val done: Boolean = false,
    public val itemId: String? = null,
)
