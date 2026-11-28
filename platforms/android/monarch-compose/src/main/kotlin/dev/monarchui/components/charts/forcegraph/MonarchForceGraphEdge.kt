package dev.monarchui.components.charts.forcegraph

import androidx.compose.runtime.Immutable

/**
 * A link between two nodes of a `MonarchForceGraph`, without direction.
 * A link to an unknown node or from a node to itself is ignored.
 *
 * @property source the id of one end.
 * @property target the id of the other end.
 * @property kind the key of the link's kind, which sets its colour and dash.
 */
@Immutable
public class MonarchForceGraphEdge(
    public val source: String,
    public val target: String,
    public val kind: String? = null,
)
