package dev.monarchui.components.workflow.roadmap

import androidx.compose.runtime.Immutable

/**
 * A heading row of a `MonarchRoadmap` that groups projects, such as an initiative, a theme or a team. The heading
 * says how many projects it has and, when given, its health.
 *
 * @property id a stable id, matched by `MonarchRoadmapItem.groupId`.
 * @property label the heading.
 * @property start the first day of an optional span drawn as a thin bar on the group row, as a day key.
 * @property end the last day of that span, as a day key.
 * @property health the health last reported for the group, or `null` to write none.
 */
@Immutable
public class MonarchRoadmapGroup(
    public val id: String,
    public val label: String,
    public val start: Long? = null,
    public val end: Long? = null,
    public val health: MonarchRoadmapHealth? = null,
)
