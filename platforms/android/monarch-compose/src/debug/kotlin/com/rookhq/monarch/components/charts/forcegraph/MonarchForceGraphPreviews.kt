package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewNodes = listOf(
    MonarchForceGraphNode("Gateway", group = "edge"),
    MonarchForceGraphNode("Billing", group = "core"),
    MonarchForceGraphNode("Ledger", group = "core"),
    MonarchForceGraphNode("Search", group = "edge"),
    MonarchForceGraphNode("Archive"),
)
private val previewEdges = listOf(
    MonarchForceGraphEdge("Gateway", "Billing"),
    MonarchForceGraphEdge("Billing", "Ledger", kind = "async"),
    MonarchForceGraphEdge("Gateway", "Search"),
)

@MonarchPreviews
@Composable
internal fun MonarchForceGraphPreview() {
    PreviewFrame {
        MonarchForceGraph(
            title = "Services",
            nodes = previewNodes,
            edges = previewEdges,
            groups = listOf(MonarchForceGraphGroup("edge", "Edge"), MonarchForceGraphGroup("core", "Core")),
            edgeKinds = listOf(MonarchForceGraphEdgeKind("async", "Asynchronous", dashed = true)),
            height = 240.dp,
            selectedId = "Billing",
        )
        MonarchForceGraph(title = "Services", nodes = emptyList(), edges = emptyList(), height = 160.dp)
    }
}
