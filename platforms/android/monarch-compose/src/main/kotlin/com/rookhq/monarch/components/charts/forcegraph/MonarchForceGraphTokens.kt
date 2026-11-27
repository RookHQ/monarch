package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The resolved strings of a force graph. Templates use `%1$s`-style placeholders. */
@Suppress("LongParameterList")
internal class ForceGraphWords(
    val locale: Locale,
    val noGroup: String,
    val nodeCount: (Int) -> String,
    val linkCount: (Int) -> String,
    val groupCount: (Int) -> String,
    val connectionCount: (Int) -> String,
    val size: String,
    val inGroups: String,
    val mostConnected: String,
    val describe: String,
    val describeLinked: String,
    val linkWithKind: String,
)

/** One end of a node's link: the other node and the link's kind. */
internal class ForceGraphLink(val node: Int, val kind: String?)

/**
 * A graph ready to draw: unique nodes, valid links, groups and colours,
 * label order and the computed layout, built once from the public data.
 */
internal class ForceGraphModel(
    nodeList: List<MonarchForceGraphNode>,
    edges: List<MonarchForceGraphEdge>,
    groupList: List<MonarchForceGraphGroup>?,
    edgeKinds: List<MonarchForceGraphEdgeKind>,
    options: MonarchForceGraphLayoutOptions,
) {
    /** The nodes; a repeated id keeps the first. */
    val nodes: List<MonarchForceGraphNode> = nodeList.distinctBy { it.id }

    private val index: Map<String, Int> = nodes.withIndex().associate { it.value.id to it.index }

    /** The valid links as index pairs, with their kinds. */
    val links: List<Triple<Int, Int, String?>> = edges.mapNotNull { edge ->
        val s = index[edge.source]
        val t = index[edge.target]
        if (s == null || t == null || s == t) null else Triple(s, t, edge.kind)
    }

    /** Each node's links, in edge order. */
    val neighbours: List<List<ForceGraphLink>> = List(nodes.size) { mutableListOf<ForceGraphLink>() }.also { lists ->
        for ((s, t, kind) in links) {
            lists[s] += ForceGraphLink(t, kind)
            lists[t] += ForceGraphLink(s, kind)
        }
    }

    /** The groups in legend order: the ones given, or the distinct keys of the nodes labelled by key. */
    val groups: List<MonarchForceGraphGroup> = groupList
        ?: nodes.mapNotNull { it.group }.distinct().map { MonarchForceGraphGroup(it, it) }

    private val groupSlots: Map<String, MonarchChartColor> =
        groups.withIndex().associate { (i, g) -> g.key to MonarchChartLegendTokens.slot(g.color, i) }

    val kinds: Map<String, MonarchForceGraphEdgeKind> = edgeKinds.associateBy { it.key }

    /** The node indices sorted by label, the order TalkBack and the table follow. */
    val order: List<Int> = nodes.indices.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { nodes[it].label })

    /** The node with the most links, the first on a tie; `null` without links. */
    val hub: Int? = nodes.indices.filter { neighbours[it].isNotEmpty() }.maxByOrNull { neighbours[it].size }

    /** The number of groups that have a node. */
    val usedGroupCount: Int = nodes.mapNotNull { it.group }.distinct().size

    /** The groups that have a node, for the legend. */
    val usedGroups: List<MonarchForceGraphGroup> = groups.filter { g -> nodes.any { it.group == g.key } }

    /** The link kinds that a link uses, for the second legend. */
    val usedKinds: List<MonarchForceGraphEdgeKind> = edgeKinds.filter { k -> links.any { it.third == k.key } }

    /** The positions from the built-in layout, in node order. */
    val points: List<ForceGraphPoint> =
        ForceGraphSimulation.layout(nodes.map { it.id }, links.map { it.first to it.second }, options)

    /** The palette slot of a node: its group's, or neutral without a group or links. */
    fun slot(node: Int): MonarchChartColor {
        if (neighbours[node].isEmpty()) return MonarchChartColor.Neutral
        return nodes[node].group?.let { groupSlots[it] } ?: MonarchChartColor.Neutral
    }

    /** The palette slot of a group key, for the legend. */
    fun groupSlot(key: String): MonarchChartColor = groupSlots[key] ?: MonarchChartColor.Neutral

    /** The node at [id], or `null`. */
    fun indexOf(id: String?): Int? = id?.let { index[it] }

    /** The name of a node's group. */
    fun groupName(node: Int, words: ForceGraphWords): String =
        nodes[node].group?.let { key -> groups.firstOrNull { it.key == key }?.label ?: key } ?: words.noGroup

    /** A node's linked nodes as text, with the kind of each link when it has one. */
    fun linkTexts(node: Int, words: ForceGraphWords): List<String> = neighbours[node].map { link ->
        val name = nodes[link.node].label
        val kind = link.kind?.let { kinds[it]?.label }
        if (kind != null) String.format(words.locale, words.linkWithKind, name, kind) else name
    }

    /** What TalkBack reads for a node and what the details show: label, group, connections and linked nodes. */
    fun describe(node: Int, words: ForceGraphWords): String {
        val links = linkTexts(node, words)
        val label = nodes[node].label
        val count = words.connectionCount(links.size)
        return if (links.isEmpty()) {
            String.format(words.locale, words.describe, label, groupName(node, words), count)
        } else {
            val linked = links.joinToString(", ")
            String.format(words.locale, words.describeLinked, label, groupName(node, words), count, linked)
        }
    }

    /** The generated summary: nodes, links, groups and the most connected node. */
    fun summary(words: ForceGraphWords): String {
        val size = String.format(words.locale, words.size, words.nodeCount(nodes.size), words.linkCount(links.size))
        val first = if (usedGroupCount > 0) {
            String.format(words.locale, words.inGroups, size, words.groupCount(usedGroupCount))
        } else {
            size
        }
        val hubText = hub?.let {
            String.format(words.locale, words.mostConnected, nodes[it].label, neighbours[it].size.toString())
        }
        return listOfNotNull(first, hubText).joinToString(". ")
    }
}

/** The bounds of the layout, which the fit uses. */
internal class ForceGraphBounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double) {
    val centreX: Double get() = (minX + maxX) / 2
    val centreY: Double get() = (minY + maxY) / 2

    companion object {
        fun of(points: List<ForceGraphPoint>): ForceGraphBounds = if (points.isEmpty()) {
            ForceGraphBounds(0.0, 0.0, 0.0, 0.0)
        } else {
            ForceGraphBounds(points.minOf { it.x }, points.maxOf { it.x }, points.minOf { it.y }, points.maxOf { it.y })
        }
    }
}

/**
 * Maps layout positions to the canvas and back: the fit, the user's zoom and
 * pan, and the mirror right to left (a canvas is not mirrored for us). Pan is
 * kept in the start-to-end direction, so it mirrors with the graph.
 */
internal class ForceGraphProjection(
    val width: Float,
    val height: Float,
    val fit: Double,
    val bounds: ForceGraphBounds,
    val zoom: Double,
    val pan: Offset,
    val rtl: Boolean,
) {
    private val scale: Double get() = fit * zoom

    /** The canvas position of a layout position. */
    fun toScreen(point: ForceGraphPoint): Offset {
        val x = (width / 2 + (point.x - bounds.centreX) * scale + pan.x).toFloat()
        val y = (height / 2 + (point.y - bounds.centreY) * scale + pan.y).toFloat()
        return Offset(if (rtl) width - x else x, y)
    }

    /** The layout position under a canvas position. */
    fun fromScreen(offset: Offset): ForceGraphPoint {
        val x = if (rtl) width - offset.x else offset.x
        return ForceGraphPoint(
            (x - width / 2 - pan.x) / scale + bounds.centreX,
            (offset.y - height / 2 - pan.y) / scale + bounds.centreY,
        )
    }
}

/** Component tokens and the pure rules of MonarchForceGraph. */
internal object MonarchForceGraphTokens {
    /** The canvas height when none is given: 420dp, as on the web. */
    val DefaultHeight: Dp =
        (MonarchTokens.Space.space24 * HEIGHT_STEPS + MonarchTokens.Space.space8 + MonarchTokens.Space.space1).dp

    /** The radius of a node, the space kept round the graph, and the room under the lowest label. */
    val NodeRadius: Dp = (MonarchTokens.Space.space2 + MonarchTokens.Space.px).dp
    val Padding: Dp = (MonarchTokens.Space.space8 + MonarchTokens.Space.space1).dp
    val LabelRoom: Dp = (MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5).dp

    /** The radius within which a tap reaches a node: half the 48dp target. */
    val HitRadius: Dp = MonarchTokens.Space.space6.dp

    /** The outline of a node, of an orphan node and of the selected or inspected node. */
    val NodeStroke: Dp = MonarchTokens.Space.space0_5.dp
    val OrphanStroke: Dp = MonarchTokens.Space.px.dp * ORPHAN_STROKE
    val RingStroke: Dp = MonarchTokens.Space.space0_5.dp * RING_STROKE

    /** The width of a link, and of a link of the inspected node. */
    val EdgeStroke: Dp = MonarchTokens.Space.px.dp * ORPHAN_STROKE
    val HighlightStroke: Dp = MonarchTokens.Space.space0_5.dp + MonarchTokens.Space.px.dp / 2

    /** The dash of a dashed link (6 on, 5 off) and of an orphan's outline (3 on, 2 off). */
    val EdgeDash: Dp = MonarchTokens.Space.space1_5.dp
    val EdgeGap: Dp = MonarchTokens.Space.space1.dp + MonarchTokens.Space.px.dp
    val OrphanDash: Dp = MonarchTokens.Space.space0_5.dp + MonarchTokens.Space.px.dp
    val OrphanGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between a node and its label, the widest label and the halo round a label. */
    val LabelGap: Dp = MonarchTokens.Space.space1.dp
    val LabelMaxWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space12).dp
    val LabelHalo: Dp = MonarchTokens.Space.space0_5.dp + MonarchTokens.Space.px.dp

    /** The space between the controls, the canvas, the details and the legend. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp
    val ControlGap: Dp = MonarchTokens.Space.space1.dp

    /** The zoom limits and the step of the buttons. */
    const val ZOOM_MIN: Double = 0.35
    const val ZOOM_MAX: Double = 3.0
    const val ZOOM_IN: Double = 1.25
    const val ZOOM_OUT: Double = 0.8

    private const val HEIGHT_STEPS = 4f
    private const val ORPHAN_STROKE = 1.5f
    private const val RING_STROKE = 1.5f

    /** The fill of a node: its slot's colour, the disabled text colour while another node is inspected. */
    fun nodeColor(slot: MonarchChartColor, dimmed: Boolean, colors: MonarchColors): Color =
        if (dimmed) colors.textDisabled else MonarchChartLegendTokens.color(slot, colors)

    /** The outline of a node: the surface colour, so nodes stand apart from links and each other. */
    fun nodeOutline(colors: MonarchColors): Color = colors.bgSurface

    /** The fill and the dashed outline of a node without links. */
    fun orphanFill(colors: MonarchColors): Color = colors.bgSurface
    fun orphanOutline(dimmed: Boolean, colors: MonarchColors): Color =
        if (dimmed) colors.textDisabled else colors.textSubtle

    /** The ring of the selected node and of the inspected node. */
    fun selectedRing(colors: MonarchColors): Color = colors.accentSolid
    fun inspectedRing(colors: MonarchColors): Color = colors.textDefault

    /** The colour of a link: its kind's, or neutral; the subtle border while another node is inspected. */
    fun edgeColor(kind: MonarchForceGraphEdgeKind?, dimmed: Boolean, colors: MonarchColors): Color =
        if (dimmed) {
            colors.borderSubtle
        } else {
            MonarchChartLegendTokens.color(kind?.color ?: MonarchChartColor.Neutral, colors)
        }

    /** The canvas and the halo round labels. */
    fun canvasColor(colors: MonarchColors): Color = colors.bgSubtle
    fun labelColor(dimmed: Boolean, colors: MonarchColors): Color =
        if (dimmed) colors.textDisabled else colors.textDefault
}

/** The pure geometry of the canvas: the fit, the zoom steps and hit testing. */
internal object ForceGraphGeometry {
    /** The largest fit, so a small graph is not blown up, and the smallest, so a tiny canvas stays valid. */
    private const val FIT_MAX = 2.5
    private const val FIT_MIN = 0.01

    /** The scale that fits the layout's bounds in the canvas, never above 2.5. */
    fun fit(bounds: ForceGraphBounds, width: Float, height: Float, padding: Float, labelRoom: Float): Double {
        val sx = (width - 2 * padding) / maxOf(1.0, bounds.maxX - bounds.minX)
        val sy = (height - 2 * padding - labelRoom) / maxOf(1.0, bounds.maxY - bounds.minY)
        return minOf(sx, sy, FIT_MAX).coerceAtLeast(FIT_MIN)
    }

    /** The zoom after multiplying by [factor], within the limits. */
    fun zoomed(zoom: Double, factor: Double): Double =
        (zoom * factor).coerceIn(MonarchForceGraphTokens.ZOOM_MIN, MonarchForceGraphTokens.ZOOM_MAX)

    /** The node nearest [at] within [radius], or `null`. */
    fun hit(points: List<Offset>, at: Offset, radius: Float): Int? {
        var best: Int? = null
        var bestDistance = radius * radius
        points.forEachIndexed { i, p ->
            val d = (p - at).getDistanceSquared()
            if (d <= bestDistance) {
                best = i
                bestDistance = d
            }
        }
        return best
    }
}

/** What the canvas shows besides the graph: its height, the labels switch and the marked nodes. */
internal class ForceGraphCanvasStyle(
    val height: Dp,
    val showLabels: Boolean,
    val selected: Int?,
    val inspected: Int?,
)
