package dev.monarchui.components.charts.forcegraph

import androidx.compose.runtime.Immutable

/**
 * A node of a `MonarchForceGraph`.
 *
 * @property label the name drawn under the node and used in the details, the table and what TalkBack reads.
 * Already localised.
 * @property id the stable id that edges refer to. Defaults to the label. A repeated id keeps the first node.
 * @property group the key of the node's group, which sets its colour.
 * @property description one line shown in the details of the inspected node, such as its type.
 */
@Immutable
public class MonarchForceGraphNode(
    public val label: String,
    public val id: String = label,
    public val group: String? = null,
    public val description: String? = null,
)
