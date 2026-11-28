package dev.monarchui.components.workflow.orgchart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.foundation.MonarchTone

/**
 * One person or open position in a `MonarchOrgChart`. Nodes are passed
 * flat; reporting lines come from [parentId].
 *
 * @property id the stable id.
 * @property parentId the manager's id. `null`, a manager not in the chart or the node's own id makes it top level.
 * @property name the person's name, or the title of an open position, already localised.
 * @property title the job title, shown under the name.
 * @property meta a short line under the title, such as the team or "Acting".
 * @property image the person's photo, loaded by the app; `null` shows the initials of [name].
 * @property kind a person or an open position.
 * @property dashed draws a dashed border for a temporary or acting reporting line.
 * @property childCount the number of direct reports, including ones not loaded yet. When it is larger than the
 * reports in the chart, opening the team calls `onLoadChildren` first.
 * @property metaTone the tone of the chip that shows [meta], such as a warning for "Acting"; `null` is neutral.
 */
@Immutable
public class MonarchOrgChartNode(
    public val id: String,
    public val parentId: String? = null,
    public val name: String,
    public val title: String? = null,
    public val meta: String? = null,
    public val image: Painter? = null,
    public val kind: MonarchOrgChartNodeKind = MonarchOrgChartNodeKind.Person,
    public val dashed: Boolean = false,
    public val childCount: Int? = null,
    public val metaTone: MonarchTone? = null,
)
