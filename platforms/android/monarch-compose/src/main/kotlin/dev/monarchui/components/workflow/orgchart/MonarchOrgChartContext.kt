package dev.monarchui.components.workflow.orgchart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** Everything the cards, chips and layouts of a `MonarchOrgChart` share in one composition. */
@Suppress("LongParameterList") // One holder instead of passing each value through every layout.
@Stable
internal class MonarchOrgChartContext(
    val tree: MonarchOrgChartTree,
    val open: Set<String>,
    val loading: Set<String>,
    val focusId: String?,
    val enabled: Boolean,
    val strings: MonarchOrgChartStrings,
    val dotted: Map<String, List<String>>,
    val dottedLines: List<MonarchOrgChartDottedLine>,
    val positions: MonarchOrgChartDragState,
    val expandIcon: @Composable () -> Unit,
    val zoomInIcon: (@Composable () -> Unit)?,
    val zoomOutIcon: (@Composable () -> Unit)?,
    val onToggle: (MonarchOrgChartNode) -> Unit,
    val onSelect: ((MonarchOrgChartNode) -> Unit)?,
    val onMove: ((MonarchOrgChartNode, MonarchOrgChartMoveTarget) -> Unit)?,
) {
    /** Whether [node] can be picked up and moved. */
    fun movable(node: MonarchOrgChartNode): Boolean =
        enabled && onMove != null && node.kind == MonarchOrgChartNodeKind.Person
}
