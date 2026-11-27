package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The chart layout: cards under their managers joined by lines, with dotted
 * lines, in a view that scrolls both ways and zooms with the buttons or a
 * pinch. It fills the height its parent gives it.
 */
@Composable
internal fun MonarchOrgChartChartView(
    context: MonarchOrgChartContext,
    zoomable: Boolean,
    modifier: Modifier = Modifier,
) {
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    val transform = rememberTransformableState { _, change, _, _ ->
        zoom = (zoom * change).coerceIn(MonarchOrgChartTokens.ZOOM_MIN, MonarchOrgChartTokens.ZOOM_MAX)
    }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    // A chart wider than the screen starts on its middle, where the top of the tree is, and not on the left edge.
    // With a focused person the card scrolls itself into view instead.
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(horizontal.maxValue, context.focusId) {
        if (!placed && horizontal.maxValue > 0) {
            if (context.focusId == null) horizontal.scrollTo(horizontal.maxValue / 2)
            placed = true
        }
    }
    val root = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val colors = MonarchTheme.colors
    val line = MonarchOrgChartTokens.lineColor(colors)
    val dotted = MonarchOrgChartTokens.dottedColor(colors)
    val cardWidth = MonarchOrgChartTokens.CardWidth * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(modifier) {
        if (zoomable) ZoomBar(zoom, context) { zoom = MonarchOrgChartTokens.clampZoom(it) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxWidth
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { context.positions.area = monarchOrgChartWindowBounds(it) }
                    .then(if (zoomable) Modifier.transformable(transform, canPan = { false }) else Modifier)
                    .horizontalScroll(horizontal)
                    .verticalScroll(vertical),
            ) {
                Row(
                    modifier = Modifier
                        .zoomed(zoom)
                        .widthIn(min = viewport / zoom)
                        .onGloballyPositioned { root[0] = it }
                        .drawBehind {
                            val coordinates = root[0]?.takeIf { it.isAttached } ?: return@drawBehind
                            drawConnectors(context, coordinates, line)
                            drawDotted(context, coordinates, dotted)
                        }
                        .padding(MonarchOrgChartTokens.ChartPadding),
                    horizontalArrangement = Arrangement.spacedBy(
                        MonarchOrgChartTokens.SiblingGap,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.Top,
                ) {
                    context.tree.roots.forEach { Subtree(it, context, cardWidth, emptySet(), zoom) }
                }
            }
        }
    }
}

@Composable
private fun Subtree(
    node: MonarchOrgChartNode,
    context: MonarchOrgChartContext,
    cardWidth: Dp,
    path: Set<String>,
    zoom: Float,
) {
    val next = path + node.id
    val reports = if (node.id in context.open) {
        context.tree.reportsOf(node.id).filter { it.id !in next }
    } else {
        emptyList()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MonarchOrgChartCard(node, context, Modifier.width(cardWidth))
        if (context.tree.reportCount(node) > 0) {
            // The chart is drawn scaled; below 100% the chip's touch area grows by 1 / zoom, so it stays 48dp on
            // the screen while the chip itself is drawn smaller.
            val target = MonarchOrgChartTokens.chipTouchSize(zoom)
            MonarchOrgChartChip(node, context, Modifier.sizeIn(minWidth = target, minHeight = target))
        }
        if (reports.isNotEmpty()) {
            Spacer(Modifier.height(MonarchOrgChartTokens.LevelGap))
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchOrgChartTokens.SiblingGap),
                verticalAlignment = Alignment.Top,
            ) {
                reports.forEach { Subtree(it, context, cardWidth, next, zoom) }
            }
        }
    }
}

/** The zoom buttons: zoom out, the level (which resets to 100%) and zoom in. */
@Composable
private fun ZoomBar(zoom: Float, context: MonarchOrgChartContext, onZoom: (Float) -> Unit) {
    val strings = context.strings
    val level = "${MonarchOrgChartTokens.zoomPercent(zoom)}%"
    val resetName = strings.zoomReset(level)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchOrgChartTokens.ChipGap, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomButton(
            name = strings.zoomOut,
            symbol = strings.zoomOutSymbol,
            icon = context.zoomOutIcon,
            enabled = zoom > MonarchOrgChartTokens.ZOOM_MIN + EPSILON,
        ) { onZoom(zoom - MonarchOrgChartTokens.ZOOM_STEP) }
        MonarchButton(
            text = level,
            onClick = { onZoom(1f) },
            modifier = Modifier.semantics {
                contentDescription = resetName
                liveRegion = LiveRegionMode.Polite
            },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
        ZoomButton(
            name = strings.zoomIn,
            symbol = strings.zoomInSymbol,
            icon = context.zoomInIcon,
            enabled = zoom < MonarchOrgChartTokens.ZOOM_MAX - EPSILON,
        ) { onZoom(zoom + MonarchOrgChartTokens.ZOOM_STEP) }
    }
}

/** A zoom button: the app's icon in an icon button, else the symbol as a ghost button. Named by [name]. */
@Composable
private fun ZoomButton(
    name: String,
    symbol: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (icon != null) {
        MonarchIconButton(label = name, onClick = onClick, enabled = enabled, size = MonarchButtonSize.Sm) {
            Box(Modifier.clearAndSetSemantics { }) { icon() }
        }
    } else {
        MonarchButton(
            text = symbol,
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}

/** Measures the content at its own size, reports the zoomed size and draws it scaled from the top-left corner. */
private fun Modifier.zoomed(zoom: Float): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout((placeable.width * zoom).roundToInt(), (placeable.height * zoom).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            scaleX = zoom
            scaleY = zoom
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** Lines from each open manager's chip (or card) down to the top of each report's card. */
private fun DrawScope.drawConnectors(
    context: MonarchOrgChartContext,
    root: LayoutCoordinates,
    color: Color,
) {
    val positions = context.positions
    val width = MonarchOrgChartTokens.LineWidth.toPx()
    context.tree.byId.values.forEach { manager ->
        if (manager.id !in context.open) return@forEach
        val from = (positions.chips[manager.id] ?: positions.cards[manager.id]) ?: return@forEach
        val tops = context.tree.reportsOf(manager.id).mapNotNull { positions.cards[it.id] }
        if (tops.isEmpty()) return@forEach
        val start = root.windowToLocal(Offset(from.center.x, from.bottom))
        val ends = tops.map { root.windowToLocal(Offset(it.center.x, it.top)) }
        val middle = start.y + (ends.minOf { it.y } - start.y) / 2
        drawLine(color, start, Offset(start.x, middle), width)
        val left = ends.minOf { it.x }.coerceAtMost(start.x)
        val right = ends.maxOf { it.x }.coerceAtLeast(start.x)
        drawLine(color, Offset(left, middle), Offset(right, middle), width)
        ends.forEach { drawLine(color, Offset(it.x, middle), it, width) }
    }
}

/** Dashed curves from each person to their secondary manager, when both are on screen. */
private fun DrawScope.drawDotted(
    context: MonarchOrgChartContext,
    root: LayoutCoordinates,
    color: Color,
) {
    val positions = context.positions
    val width = MonarchOrgChartTokens.LineWidth.toPx()
    val effect = PathEffect.dashPathEffect(
        floatArrayOf(MonarchOrgChartTokens.DASH * width, MonarchOrgChartTokens.DASH_GAP * width),
    )
    context.dottedLines.forEach { dottedLine ->
        val from = positions.cards[dottedLine.from] ?: return@forEach
        val to = positions.cards[dottedLine.to] ?: return@forEach
        val start = root.windowToLocal(from.center)
        val end = root.windowToLocal(to.center)
        val bend = (end.y - start.y) / 2
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(start.x, start.y + bend, end.x, end.y - bend, end.x, end.y)
        }
        drawPath(path, color, style = Stroke(width = width, pathEffect = effect))
    }
}

private const val EPSILON = 0.001f
