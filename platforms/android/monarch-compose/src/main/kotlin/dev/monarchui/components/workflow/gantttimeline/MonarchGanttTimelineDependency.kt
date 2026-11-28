package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.runtime.Immutable

/**
 * A link between two items of a `MonarchGanttTimeline`, drawn as an arrow and written as "Depends on" in the list.
 *
 * @property id a stable id.
 * @property from the id of the item that must happen first.
 * @property to the id of the item that depends on it.
 * @property kind how the two depend on each other.
 * @property conflict the dates break this dependency: the arrow is dashed and in the danger colour.
 */
@Immutable
public class MonarchGanttTimelineDependency(
    public val id: String,
    public val from: String,
    public val to: String,
    public val kind: MonarchGanttTimelineDependencyKind = MonarchGanttTimelineDependencyKind.FinishToStart,
    public val conflict: Boolean = false,
)
