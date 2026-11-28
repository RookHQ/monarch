package com.rookhq.monarch.components.charts.forcegraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.math.roundToInt

/**
 * The canvas of a force graph: links and nodes drawn on a `Canvas`, mirrored
 * by hand right to left, with taps that inspect a node, drags that move a node
 * or pan, and pinches that zoom. On top, one invisible 48dp element per node,
 * in label order, that TalkBack reads and, with [onSelect], activates.
 */
@Composable
@Suppress("LongParameterList")
internal fun ForceGraphCanvas(
    model: ForceGraphModel,
    words: ForceGraphWords,
    view: ForceGraphViewState,
    style: ForceGraphCanvasStyle,
    summary: String,
    onInspect: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    // The web draws the labels at the 2xs size, 11sp, so a dense graph stays readable.
    val caption = MonarchTheme.typography.caption.copy(fontSize = MonarchTokens.FontSize.xs2.sp)
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val labelWidth = with(density) { MonarchForceGraphTokens.LabelMaxWidth.roundToPx() }
    val labels = remember(model, caption, density, labelWidth) {
        model.nodes.map {
            measurer.measure(
                text = it.label,
                style = caption,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = labelWidth),
            )
        }
    }
    val bounds = remember(model) { ForceGraphBounds.of(model.points) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(style.height)
            .clip(MonarchTheme.shapes.md)
            .background(MonarchForceGraphTokens.canvasColor(colors))
            .semantics {
                isTraversalGroup = true
                contentDescription = summary
            },
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val fit = with(density) {
            ForceGraphGeometry.fit(
                bounds,
                width,
                height,
                MonarchForceGraphTokens.Padding.toPx(),
                if (style.showLabels) MonarchForceGraphTokens.LabelRoom.toPx() else 0f,
            )
        }
        val projection = ForceGraphProjection(width, height, fit, bounds, view.zoom, view.pan, rtl)
        val screen = model.nodes.indices.map { i ->
            projection.toScreen(view.moved[model.nodes[i].id] ?: model.points[i])
        }
        val currentProjection by rememberUpdatedState(projection)
        val currentScreen by rememberUpdatedState(screen)
        val inspect by rememberUpdatedState(onInspect)
        val select by rememberUpdatedState(onSelect)
        val hitRadius = with(density) { MonarchForceGraphTokens.HitRadius.toPx() }
        val hitAt: (Offset) -> Int? = { at -> ForceGraphGeometry.hit(currentScreen, at, hitRadius) }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(model, rtl) {
                    detectTapGestures { at ->
                        val hit = hitAt(at)
                        inspect(hit)
                        if (hit != null) select?.invoke(hit)
                    }
                }
                .pointerInput(model, rtl) {
                    forceGraphDrags(view, rtl, hitAt) { node, at ->
                        view.moved[model.nodes[node].id] = currentProjection.fromScreen(at)
                    }
                },
        ) {
            drawForceGraphEdges(model, screen, style, colors)
            drawForceGraphNodes(model, screen, style, colors)
            if (style.showLabels) drawForceGraphLabels(model, screen, labels, style, colors)
        }
        // Positions are measured from the left edge, so the elements start there in both directions.
        Box(modifier = Modifier.matchParentSize(), contentAlignment = AbsoluteAlignment.TopLeft) {
            ForceGraphNodeElements(model, words, screen, style.selected, onInspect, onSelect)
        }
    }
}

/**
 * Drags on the canvas: one finger moves the node it started on, or pans when it started on the background;
 * two fingers zoom and pan. A drag starts after the touch slop, so a tap still reaches the tap detector.
 */
private suspend fun PointerInputScope.forceGraphDrags(
    view: ForceGraphViewState,
    rtl: Boolean,
    hitAt: (Offset) -> Int?,
    moveNode: (node: Int, at: Offset) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val node = hitAt(down.position)
    var dragging = false
    var travelled = Offset.Zero
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size > 1) {
            view.zoomBy(event.calculateZoom().toDouble())
            view.panBy(event.calculatePan(), rtl)
            event.changes.forEach { it.consume() }
            dragging = true
        } else if (pressed.size == 1) {
            val change = pressed.first()
            val delta = change.positionChange()
            if (!dragging) {
                travelled += delta
                dragging = travelled.getDistance() > viewConfiguration.touchSlop
            }
            if (dragging && delta != Offset.Zero) {
                if (node != null) moveNode(node, change.position) else view.panBy(delta, rtl)
                change.consume()
            }
        }
    } while (pressed.isNotEmpty())
}

/**
 * One invisible 48dp element per node, centred on it, in label order: what TalkBack reads, and with
 * [onSelect] a button that inspects and selects the node. They take no pointer input, so touches reach the
 * canvas under them.
 */
@Composable
@Suppress("LongParameterList")
private fun ForceGraphNodeElements(
    model: ForceGraphModel,
    words: ForceGraphWords,
    screen: List<Offset>,
    selectedNode: Int?,
    onInspect: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
) {
    val target = MonarchTouchTarget.Minimum
    val half = with(LocalDensity.current) { target.toPx() / 2 }
    model.order.forEachIndexed { position, i ->
        val spoken = model.describe(i, words)
        val at = screen[i]
        Box(
            Modifier
                // Absolute: the screen position is already mirrored right to left.
                .absoluteOffset { IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt()) }
                .size(target)
                .semantics {
                    contentDescription = spoken
                    traversalIndex = position.toFloat()
                    if (i == selectedNode) selected = true
                    if (onSelect != null) {
                        role = Role.Button
                        onClick {
                            onInspect(i)
                            onSelect(i)
                            true
                        }
                    }
                },
        )
    }
}

/** The nodes that stay strong while a node is inspected: it and its neighbours. `null` when none is. */
private fun ForceGraphModel.lit(inspected: Int?): Set<Int>? =
    inspected?.let { i -> setOf(i) + neighbours[i].map { it.node } }

/** Draws the links: the kind's colour and dash, thicker for the inspected node's, subtle for the others. */
private fun DrawScope.drawForceGraphEdges(
    model: ForceGraphModel,
    screen: List<Offset>,
    style: ForceGraphCanvasStyle,
    colors: MonarchColors,
) {
    val inspected = style.inspected
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(MonarchForceGraphTokens.EdgeDash.toPx(), MonarchForceGraphTokens.EdgeGap.toPx()),
    )
    val stroke = MonarchForceGraphTokens.EdgeStroke.toPx()
    val highlight = MonarchForceGraphTokens.HighlightStroke.toPx()
    for ((s, t, kindKey) in model.links) {
        val on = inspected != null && (s == inspected || t == inspected)
        val kind = kindKey?.let { model.kinds[it] }
        drawLine(
            color = MonarchForceGraphTokens.edgeColor(kind, dimmed = inspected != null && !on, colors),
            start = screen[s],
            end = screen[t],
            strokeWidth = if (on) highlight else stroke,
            pathEffect = if (kind?.dashed == true) dash else null,
        )
    }
}

/** Draws the nodes: a dot in the group's colour, or a dashed outline without links, then the rings. */
private fun DrawScope.drawForceGraphNodes(
    model: ForceGraphModel,
    screen: List<Offset>,
    style: ForceGraphCanvasStyle,
    colors: MonarchColors,
) {
    val lit = model.lit(style.inspected)
    val radius = MonarchForceGraphTokens.NodeRadius.toPx()
    val ring = MonarchForceGraphTokens.RingStroke.toPx()
    val orphan = Stroke(
        MonarchForceGraphTokens.OrphanStroke.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(MonarchForceGraphTokens.OrphanDash.toPx(), MonarchForceGraphTokens.OrphanGap.toPx()),
        ),
    )
    val outline = Stroke(MonarchForceGraphTokens.NodeStroke.toPx())
    model.nodes.indices.forEach { i ->
        val centre = screen[i]
        val dimmed = lit != null && i !in lit
        if (model.neighbours[i].isEmpty()) {
            drawCircle(MonarchForceGraphTokens.orphanFill(colors), radius, centre)
            drawCircle(MonarchForceGraphTokens.orphanOutline(dimmed, colors), radius, centre, style = orphan)
        } else {
            drawCircle(MonarchForceGraphTokens.nodeColor(model.slot(i), dimmed, colors), radius, centre)
            drawCircle(MonarchForceGraphTokens.nodeOutline(colors), radius, centre, style = outline)
        }
        if (i == style.selected) {
            drawCircle(MonarchForceGraphTokens.selectedRing(colors), radius + ring / 2, centre, style = Stroke(ring))
        }
        if (i == style.inspected) {
            val outer = radius + ring + ring / 2
            drawCircle(MonarchForceGraphTokens.inspectedRing(colors), outer, centre, style = Stroke(ring))
        }
    }
}

/** Draws each label under its node, on a halo of the canvas colour so links never cross the text. */
private fun DrawScope.drawForceGraphLabels(
    model: ForceGraphModel,
    screen: List<Offset>,
    labels: List<TextLayoutResult>,
    style: ForceGraphCanvasStyle,
    colors: MonarchColors,
) {
    val lit = model.lit(style.inspected)
    val below = MonarchForceGraphTokens.NodeRadius.toPx() + MonarchForceGraphTokens.LabelGap.toPx()
    // The halo is the label drawn in the canvas colour at eight offsets: a stroked glyph path smears small text.
    val reach = MonarchForceGraphTokens.LabelHalo.toPx() / 2f
    val haloColor = MonarchForceGraphTokens.canvasColor(colors)
    model.nodes.indices.forEach { i ->
        val layout = labels[i]
        // A label never leaves the canvas: a node near the edge keeps its whole name in view.
        val left = (screen[i].x - layout.size.width / 2f).coerceIn(0f, maxOf(0f, size.width - layout.size.width))
        val topLeft = Offset(left, screen[i].y + below)
        HALO_DIRECTIONS.forEach { (dx, dy) ->
            drawText(layout, color = haloColor, topLeft = topLeft + Offset(dx * reach, dy * reach))
        }
        drawText(
            layout,
            color = MonarchForceGraphTokens.labelColor(dimmed = lit != null && i !in lit, colors),
            topLeft = topLeft,
        )
    }
}

/** The eight directions of the label halo. */
private val HALO_DIRECTIONS: List<Pair<Float, Float>> = listOf(
    -1f to -1f, 0f to -1f, 1f to -1f, -1f to 0f, 1f to 0f, -1f to 1f, 0f to 1f, 1f to 1f,
)
