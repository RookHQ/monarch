package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * Where a child of the grid goes, in pixels. [y] is the top, or the vertical centre when [centerY] is set, or the
 * bottom when [bottom] is set. A [width] or [height] fixes that size; [maxWidth] bounds a text.
 */
@Immutable
internal data class GanttTimelineSlot(
    val x: Int,
    val y: Int,
    val width: Int? = null,
    val height: Int? = null,
    val maxWidth: Int = Constraints.Infinity,
    val centerY: Boolean = false,
    val bottom: Boolean = false,
) {
    fun constraints(): Constraints = Constraints(
        minWidth = width ?: 0,
        maxWidth = width ?: maxWidth,
        minHeight = height ?: 0,
        maxHeight = height ?: Constraints.Infinity,
    )

    fun top(measured: Int): Int = when {
        centerY -> y - measured / 2
        bottom -> y - measured
        else -> y
    }
}

/** The pixel scale of the grid: the first visible day and the width of a day. */
internal class GanttTimelineScale(val anchor: Long, val dayWidth: Float) {
    fun x(day: Long): Float = (day - anchor) * dayWidth
}

/** The dates of [item] as shown: moved by [drag] while it is being dragged. */
internal fun GanttTimelineDrag?.shown(item: MonarchGanttTimelineItem): MonarchGanttTimelineDates =
    if (this != null && id == item.id) item.dates.shifted(shift, days) else item.dates

/**
 * The calendar grid: header ticks, weekends, gridlines, the today line, group spans, parent brackets, bars, one-day
 * markers, milestones and dependency arrows. Its time axis runs left to right in every layout direction, as on the
 * web, so it is laid out left to right and scrolls sideways.
 */
@Suppress("LongMethod")
@Composable
internal fun GanttTimelineGrid(model: GanttTimelineModel, geometry: GanttTimelineGeometry, scroll: ScrollState) {
    val tokens = MonarchGanttTimelineTokens
    val density = LocalDensity.current
    val colors = MonarchTheme.colors
    val dayWidth = tokens.dayWidth(model.zoom)
    val scale = GanttTimelineScale(model.anchor, with(density) { dayWidth.toPx() })
    val span = tokens.span(model.zoom)
    val ticks = remember(model.anchor, model.zoom) { GanttTimelineRules.ticks(model.anchor, model.zoom) }
    var drag by remember { mutableStateOf<GanttTimelineDrag?>(null) }
    val widthPx = (scale.dayWidth * span).roundToInt()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.horizontalScroll(scroll)) {
            Layout(
                modifier = Modifier
                    .width(dayWidth * span)
                    .height(with(density) { geometry.height.toDp() })
                    .ganttTimelineDecor(model, geometry, ticks, scale, drag, colors),
                content = {
                    GanttTimelineTicks(model, ticks, scale, geometry, widthPx)
                    GanttTimelineToday(model, scale, geometry)
                    GanttTimelineLane(model, scale, geometry)
                    model.lines.lines.forEachIndexed { index, row ->
                        if (row is GanttTimelineLine.Item) {
                            key(row.item.id) {
                                GanttTimelineRow(model, row, index, geometry, scale, drag) { drag = it }
                            }
                        }
                    }
                    GanttTimelineRowMilestones(model, scale, geometry)
                },
            ) { measurables, _ ->
                val placed = measurables.map { measurable ->
                    val slot = measurable.layoutId as GanttTimelineSlot
                    measurable.measure(slot.constraints()) to slot
                }
                layout(widthPx, geometry.height) {
                    placed.forEach { (placeable, slot) -> placeable.place(slot.x, slot.top(placeable.height)) }
                }
            }
        }
    }
}

/** The month labels along the top of the header and the day or week labels under them. Decorative. */
@Composable
private fun GanttTimelineTicks(
    model: GanttTimelineModel,
    ticks: GanttTimelineTicks,
    scale: GanttTimelineScale,
    geometry: GanttTimelineGeometry,
    widthPx: Int,
) {
    val tokens = MonarchGanttTimelineTokens
    val pad = with(LocalDensity.current) { tokens.TickPadding.roundToPx() }
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val style = MonarchTheme.typography.caption
    ticks.major.forEachIndexed { i, tick ->
        val x = scale.x(tick.day).roundToInt()
        val next = ticks.major.getOrNull(i + 1)?.let { scale.x(it.day).roundToInt() } ?: widthPx
        Text(
            text = model.formats.month(tick.day, tick.withYear),
            modifier = Modifier
                .layoutId(GanttTimelineSlot(x + pad, pad, maxWidth = (next - x - pad * 2).coerceAtLeast(0)))
                .clearAndSetSemantics {},
            style = style,
            color = MonarchTheme.colors.textDefault,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
    val week = model.zoom == MonarchGanttTimelineZoom.Week
    ticks.minor.forEachIndexed { i, day ->
        val x = scale.x(day).roundToInt()
        val next = ticks.minor.getOrNull(i + 1)?.let { scale.x(it).roundToInt() } ?: widthPx
        Text(
            text = if (week) model.formats.weekdayDay(day) else model.formats.dayNumber(day),
            modifier = Modifier
                .layoutId(
                    GanttTimelineSlot(
                        x = x + pad / 2,
                        y = geometry.header - pad,
                        maxWidth = (next - x - pad).coerceAtLeast(0),
                        bottom = true,
                    ),
                )
                .clearAndSetSemantics {},
            style = style,
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** The label of the today line, at the top of the rows. Decorative: today is not a place TalkBack goes. */
@Composable
private fun GanttTimelineToday(model: GanttTimelineModel, scale: GanttTimelineScale, geometry: GanttTimelineGeometry) {
    val today = model.today ?: return
    if (today < model.anchor || today > model.rangeEnd) return
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    val line = with(LocalDensity.current) { tokens.TodayLineWidth.toPx() }
    Text(
        text = model.text.today,
        modifier = Modifier
            .layoutId(GanttTimelineSlot((scale.x(today) + scale.dayWidth / 2 + line).roundToInt(), geometry.header))
            .clearAndSetSemantics {}
            .background(tokens.todayColor(colors), MonarchTheme.shapes.xs)
            .padding(horizontal = tokens.TodayLabelPadding),
        style = MonarchTheme.typography.caption,
        color = tokens.onTodayColor(colors),
        maxLines = 1,
    )
}

/** The bar, marker or row text of one item row. */
@Suppress("LongParameterList")
@Composable
private fun GanttTimelineRow(
    model: GanttTimelineModel,
    row: GanttTimelineLine.Item,
    index: Int,
    geometry: GanttTimelineGeometry,
    scale: GanttTimelineScale,
    drag: GanttTimelineDrag?,
    onDrag: (GanttTimelineDrag?) -> Unit,
) {
    val tokens = MonarchGanttTimelineTokens
    val density = LocalDensity.current
    val item = row.item
    val shown = drag.shown(item)
    val span = GanttTimelineRules.span(shown.start, shown.end)
    val top = geometry.rowTops[index]
    val height = geometry.rowHeights[index]
    val pad = with(density) { tokens.OffscreenPadding.roundToPx() }
    val center = top + height / 2
    val text = model.text
    val note = when {
        span == null -> if (row.rollup == null) text.noDates else null
        span.end < model.anchor -> text.ended(model.date(span.end))
        span.start > model.rangeEnd -> text.starts(model.date(span.start))
        else -> null
    }
    if (note != null) {
        Text(
            text = note,
            modifier = Modifier.layoutId(GanttTimelineSlot(pad, center, centerY = true)),
            style = MonarchTheme.typography.caption,
            color = tokens.mutedColor(MonarchTheme.colors),
            maxLines = 1,
        )
        return
    }
    if (span == null) return
    val geometryOfBar = ganttTimelineBarBox(span, GanttTimelineRules.isPoint(shown), scale, density)
    GanttTimelineBar(
        model = model,
        item = item,
        span = span,
        point = GanttTimelineRules.isPoint(shown),
        visibleWidth = with(density) { geometryOfBar.visibleWidth.toDp() },
        barHeight = with(density) { (height - 2 * tokens.BarInset.roundToPx()).coerceAtLeast(0).toDp() },
        scale = scale,
        dragging = drag?.id == item.id,
        onDrag = onDrag,
        modifier = Modifier.layoutId(
            GanttTimelineSlot(geometryOfBar.nodeLeft, top, width = geometryOfBar.nodeWidth, height = height),
        ),
    )
}

/** The visible bar and the at least 48dp wide node around it, in pixels. */
internal class GanttTimelineBarBox(val visibleLeft: Int, val visibleWidth: Int, val nodeLeft: Int, val nodeWidth: Int)

/** Where the bar of [span] goes: a bar spans its days; a one-day marker is a diamond centred on its day. */
internal fun ganttTimelineBarBox(
    span: GanttTimelineSpan,
    point: Boolean,
    scale: GanttTimelineScale,
    density: Density,
): GanttTimelineBarBox = with(density) {
    val tokens = MonarchGanttTimelineTokens
    val visibleWidth = if (point) {
        tokens.PointSize.roundToPx()
    } else {
        maxOf((span.days * scale.dayWidth).roundToInt(), tokens.BarMinWidth.roundToPx())
    }
    val visibleLeft = if (point) {
        (scale.x(span.start) + scale.dayWidth / 2).roundToInt() - visibleWidth / 2
    } else {
        scale.x(span.start).roundToInt()
    }
    val nodeWidth = maxOf(visibleWidth, tokens.RowMinHeight.roundToPx())
    GanttTimelineBarBox(visibleLeft, visibleWidth, visibleLeft - (nodeWidth - visibleWidth) / 2, nodeWidth)
}
