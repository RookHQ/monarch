package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraph
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphEdge
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphEdgeKind
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphGroup
import com.rookhq.monarch.components.charts.forcegraph.MonarchForceGraphNode
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of ForceGraph. */
object MonarchForceGraphSection : CataloguePage {
    override val title = "ForceGraph"

    private val notes = listOf(
        MonarchForceGraphNode("Roadmap", group = "plans", description = "Plan"),
        MonarchForceGraphNode("Pricing", group = "plans"),
        MonarchForceGraphNode("Launch checklist", group = "plans"),
        MonarchForceGraphNode("Interviews", group = "research", description = "Research notes"),
        MonarchForceGraphNode("Survey results", group = "research"),
        MonarchForceGraphNode("Competitors", group = "research"),
        MonarchForceGraphNode("Onboarding", group = "design"),
        MonarchForceGraphNode("Empty states", group = "design"),
        MonarchForceGraphNode("Ideas inbox"),
    )
    private val links = listOf(
        MonarchForceGraphEdge("Roadmap", "Pricing"),
        MonarchForceGraphEdge("Roadmap", "Launch checklist"),
        MonarchForceGraphEdge("Roadmap", "Interviews", kind = "cites"),
        MonarchForceGraphEdge("Pricing", "Competitors", kind = "cites"),
        MonarchForceGraphEdge("Interviews", "Survey results"),
        MonarchForceGraphEdge("Interviews", "Onboarding", kind = "cites"),
        MonarchForceGraphEdge("Onboarding", "Empty states"),
        MonarchForceGraphEdge("Launch checklist", "Onboarding"),
    )
    private val groups = listOf(
        MonarchForceGraphGroup("plans", "Plans"),
        MonarchForceGraphGroup("research", "Research"),
        MonarchForceGraphGroup("design", "Design"),
    )

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableIntStateOf(0) }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Status")
        val statuses = MonarchChartFrameStatus.entries
        MonarchSegmentedControl {
            statuses.forEachIndexed { index, option ->
                Segment(
                    selected = index == status,
                    onClick = { status = index },
                    label = { Text(option.name) },
                )
            }
        }
        MonarchForceGraph(
            title = "Linked notes",
            nodes = notes,
            edges = links,
            groups = groups,
            edgeKinds = listOf(MonarchForceGraphEdgeKind("cites", "Cites", dashed = true)),
            description = "Tap a note to see its links and mark it",
            status = statuses[status],
            height = 360.dp,
            selectedId = selected,
            emptyMessage = "Link two notes to see them here.",
            onRetry = { status = 0 },
            onSelect = { selected = it.id },
        )
        CatalogueCaption("Without labels")
        MonarchForceGraph(
            title = "Services",
            nodes = notes,
            edges = links,
            height = 240.dp,
            showLabels = false,
        )
    }
}
