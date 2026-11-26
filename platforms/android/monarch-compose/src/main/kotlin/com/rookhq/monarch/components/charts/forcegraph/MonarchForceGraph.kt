package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrame
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTableSeries
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegend
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendItem
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendMarker
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A network of linked nodes placed by a small built-in force layout, with
 * groups in data colours, link kinds, drag, pan and zoom.
 *
 * Built by Monarch inside `MonarchChartFrame`: Material 3 has no chart. The
 * layout is a deterministic port of the web's force simulation, run once when
 * the nodes, links or options change. The graph is drawn on a `Canvas`,
 * mirrored right to left. A tap inspects a node: its ring, its links and
 * neighbours stay strong while the rest dims, and its group and links are
 * written under the canvas and announced politely. Dragging moves a node or
 * pans; pinching or the Zoom buttons zoom; Reset view undoes all three.
 *
 * For TalkBack the canvas is a group named with a generated summary (nodes,
 * links, groups and the most connected node), and every node is an element,
 * in label order, read as its label, group and links. "View as table" is the
 * list alternative: every node with its group and links as text. Groups are
 * named in the legend and in each node's text, and link kinds are dashed as
 * well as coloured, so colour is never the only key.
 *
 * @param title the chart's name, shown as the frame's heading.
 * @param nodes the nodes. A repeated id keeps the first node.
 * @param edges the links, without direction. Links to unknown nodes and to the same node are ignored.
 * @param modifier the modifier of the frame.
 * @param groups the groups in legend order. `null` uses the distinct group keys of the nodes, labelled by key.
 * @param edgeKinds kinds of link with a colour and dash, listed in a second legend when used.
 * @param description one line under the title.
 * @param status what the frame shows. A ready chart without nodes shows the empty state.
 * @param height the height of the canvas.
 * @param showLabels draws each node's label under it.
 * @param selectedId the id of a node drawn with an accent ring, such as the open document.
 * @param summary replaces the generated summary that TalkBack reads for the graph.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param layoutOptions the forces of the layout.
 * @param onRetry shows Try again in the error status.
 * @param onSelect called with a node when it is tapped or activated with TalkBack, for example to open it.
 * @param labels replacements for the built-in strings, including the frame's.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
public fun MonarchForceGraph(
    title: String,
    nodes: List<MonarchForceGraphNode>,
    edges: List<MonarchForceGraphEdge>,
    modifier: Modifier = Modifier,
    groups: List<MonarchForceGraphGroup>? = null,
    edgeKinds: List<MonarchForceGraphEdgeKind> = emptyList(),
    description: String? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    height: Dp = MonarchForceGraphTokens.DefaultHeight,
    showLabels: Boolean = true,
    selectedId: String? = null,
    summary: String? = null,
    emptyMessage: String? = null,
    footer: String? = null,
    layoutOptions: MonarchForceGraphLayoutOptions = MonarchForceGraphLayoutOptions(),
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchForceGraphNode) -> Unit)? = null,
    labels: MonarchForceGraphLabels = MonarchForceGraphLabels(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val words = forceGraphWords(labels, locale)
    val model = remember(
        nodes,
        edges,
        groups,
        edgeKinds,
        layoutOptions.linkDistance,
        layoutOptions.charge,
        layoutOptions.collide,
        layoutOptions.gravity,
        layoutOptions.ticks,
    ) { ForceGraphModel(nodes, edges, groups, edgeKinds, layoutOptions) }
    val view = remember(model) { ForceGraphViewState() }
    var inspectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val inspected = model.indexOf(inspectedId)
    val density = LocalDensity.current
    var measured by remember { mutableStateOf<Dp?>(null) }
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = if (status == MonarchChartFrameStatus.Ready && model.nodes.isEmpty()) {
            MonarchChartFrameStatus.Empty
        } else {
            status
        },
        table = forceGraphTable(model, words, labels),
        legend = if (model.usedGroups.size > 1) {
            model.usedGroups.map { MonarchChartLegendItem(it.key, it.label, model.groupSlot(it.key)) }
        } else {
            emptyList()
        },
        legendMarker = MonarchChartLegendMarker.Dot,
        emptyMessage = emptyMessage,
        footer = footer,
        height = maxOf(height, measured ?: height),
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { measured = with(density) { it.height.toDp() } },
            verticalArrangement = Arrangement.spacedBy(MonarchForceGraphTokens.SectionGap),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MonarchForceGraphTokens.ControlGap, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(MonarchForceGraphTokens.ControlGap),
            ) {
                MonarchIconButton(
                    label = monarchString(labels.zoomIn, R.string.monarch_forcegraph_zoom_in),
                    onClick = { view.zoomBy(MonarchForceGraphTokens.ZOOM_IN) },
                    size = MonarchButtonSize.Sm,
                    enabled = view.canZoomIn,
                    variant = MonarchButtonVariant.Secondary,
                ) { MonarchDefaultIcons.Plus() }
                MonarchIconButton(
                    label = monarchString(labels.zoomOut, R.string.monarch_forcegraph_zoom_out),
                    onClick = { view.zoomBy(MonarchForceGraphTokens.ZOOM_OUT) },
                    size = MonarchButtonSize.Sm,
                    enabled = view.canZoomOut,
                    variant = MonarchButtonVariant.Secondary,
                ) { MonarchDefaultIcons.Dash() }
                MonarchButton(
                    text = monarchString(labels.resetView, R.string.monarch_forcegraph_reset_view),
                    onClick = { view.reset() },
                    size = MonarchButtonSize.Sm,
                )
            }
            ForceGraphCanvas(
                model = model,
                words = words,
                view = view,
                style = ForceGraphCanvasStyle(height, showLabels, model.indexOf(selectedId), inspected),
                summary = summary ?: model.summary(words),
                onInspect = { inspectedId = it?.let { i -> model.nodes[i].id } },
                onSelect = onSelect?.let { select -> { i: Int -> select(model.nodes[i]) } },
            )
            ForceGraphDetails(model, words, inspected)
            if (model.usedKinds.isNotEmpty()) {
                MonarchChartLegend(
                    items = model.usedKinds.map {
                        MonarchChartLegendItem(it.key, it.label, it.color ?: MonarchChartColor.Neutral, it.dashed)
                    },
                    marker = MonarchChartLegendMarker.Line,
                    legendLabel = monarchString(labels.edgeLegend, R.string.monarch_forcegraph_edge_legend),
                )
            }
        }
    }
}

/** The details of the inspected node, in a polite live region that is always present. */
@Composable
private fun ForceGraphDetails(model: ForceGraphModel, words: ForceGraphWords, inspected: Int?) {
    val detail = inspected?.let { model.describe(it, words) }
    val note = inspected?.let { model.nodes[it].description }
    Box(
        Modifier.clearAndSetSemantics {
            liveRegion = LiveRegionMode.Polite
            if (detail != null) contentDescription = listOfNotNull(detail, note).joinToString(". ")
        },
    ) {
        if (detail != null) {
            Column {
                Text(text = detail, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textDefault)
                if (note != null) {
                    Text(text = note, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
                }
            }
        }
    }
}

/** The list alternative: every node in label order with its group and links. */
@Composable
private fun forceGraphTable(
    model: ForceGraphModel,
    words: ForceGraphWords,
    labels: MonarchForceGraphLabels,
): MonarchChartTable = MonarchChartTable(
    categoryTitle = monarchString(labels.node, R.string.monarch_forcegraph_node),
    categories = model.order.map { model.nodes[it].label },
    series = listOf(
        // Text columns: the value is a placeholder, the formatted text is what is shown and read.
        MonarchChartTableSeries(
            name = monarchString(labels.group, R.string.monarch_forcegraph_group),
            values = model.order.map { 0.0 },
            formattedValues = model.order.map { model.groupName(it, words) },
        ),
        MonarchChartTableSeries(
            name = monarchString(labels.connections, R.string.monarch_forcegraph_connections),
            values = model.order.map { model.neighbours[it].size.toDouble() },
            formattedValues = model.order.map { i ->
                model.linkTexts(i, words).joinToString(", ").ifEmpty { words.connectionCount(0) }
            },
        ),
    ),
)

/** The built-in strings, or their replacements. */
@Composable
private fun forceGraphWords(labels: MonarchForceGraphLabels, locale: Locale): ForceGraphWords {
    val resources = LocalResources.current
    fun plural(id: Int): (Int) -> String = { count -> resources.getQuantityString(id, count, count) }
    return ForceGraphWords(
        locale = locale,
        noGroup = monarchString(labels.noGroup, R.string.monarch_forcegraph_no_group),
        nodeCount = labels.nodeCount ?: plural(R.plurals.monarch_forcegraph_node_count),
        linkCount = labels.linkCount ?: plural(R.plurals.monarch_forcegraph_link_count),
        groupCount = labels.groupCount ?: plural(R.plurals.monarch_forcegraph_group_count),
        connectionCount = labels.connectionCount ?: plural(R.plurals.monarch_forcegraph_connection_count),
        size = monarchString(labels.size, R.string.monarch_forcegraph_size),
        inGroups = monarchString(labels.inGroups, R.string.monarch_forcegraph_in_groups),
        mostConnected = monarchString(labels.mostConnected, R.string.monarch_forcegraph_most_connected),
        describe = monarchString(labels.describe, R.string.monarch_forcegraph_describe),
        describeLinked = monarchString(labels.describeLinked, R.string.monarch_forcegraph_describe_linked),
        linkWithKind = monarchString(labels.linkWithKind, R.string.monarch_forcegraph_link_with_kind),
    )
}
