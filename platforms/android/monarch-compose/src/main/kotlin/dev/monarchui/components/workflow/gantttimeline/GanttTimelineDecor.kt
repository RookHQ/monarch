package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.monarchui.theme.MonarchColors

/**
 * Draws what is not a child of the grid: under the bars the header and row lines, weekends, group rows, gridlines,
 * the today line, group spans and parent brackets; over the bars the dependency arrows.
 */
@Suppress("LongParameterList")
internal fun Modifier.ganttTimelineDecor(
    model: GanttTimelineModel,
    geometry: GanttTimelineGeometry,
    ticks: GanttTimelineTicks,
    scale: GanttTimelineScale,
    drag: GanttTimelineDrag?,
    colors: MonarchColors,
): Modifier = drawWithContent {
    ganttTimelineUnder(model, geometry, ticks, scale, colors)
    drawContent()
    ganttTimelineArrows(model, geometry, scale, drag, colors)
}

@Suppress("LongMethod")
private fun DrawScope.ganttTimelineUnder(
    model: GanttTimelineModel,
    geometry: GanttTimelineGeometry,
    ticks: GanttTimelineTicks,
    scale: GanttTimelineScale,
    colors: MonarchColors,
) {
    val tokens = MonarchGanttTimelineTokens
    val line = tokens.lineColor(colors)
    val shade = tokens.shadeColor(colors)
    val stroke = tokens.LineWidth.toPx()
    val body = geometry.header.toFloat()
    drawRect(shade, Offset.Zero, Size(size.width, body))
    if (tokens.minorGrid(model.zoom)) {
        for (d in 0 until tokens.span(model.zoom)) {
            val day = model.anchor + d
            val weekday = GanttTimelineDays.weekday(day)
            if (weekday == 0 || weekday == SATURDAY) {
                drawRect(shade, Offset(scale.x(day), body), Size(scale.dayWidth, size.height - body))
            }
        }
    }
    model.lines.lines.forEachIndexed { i, row ->
        if (row is GanttTimelineLine.Group) {
            drawRect(
                shade,
                Offset(0f, geometry.rowTops[i].toFloat()),
                Size(size.width, geometry.rowHeights[i].toFloat()),
            )
        }
    }
    val gridDays = if (tokens.minorGrid(model.zoom)) ticks.minor else ticks.major.map { it.day }
    gridDays.forEach { day ->
        val x = scale.x(day)
        drawLine(line, Offset(x, body), Offset(x, size.height), stroke)
    }
    ticks.major.forEach { tick ->
        val x = scale.x(tick.day)
        drawLine(line, Offset(x, 0f), Offset(x, body), stroke)
    }
    drawLine(line, Offset(0f, body - stroke / 2), Offset(size.width, body - stroke / 2), stroke)
    if (geometry.laneHeight > 0) {
        val y = body + geometry.laneHeight - stroke / 2
        drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
    }
    geometry.rowTops.forEachIndexed { i, top ->
        val y = top + geometry.rowHeights[i] - stroke / 2
        drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
    }
    model.today?.takeIf { it in model.anchor..model.rangeEnd }?.let { today ->
        val x = scale.x(today) + scale.dayWidth / 2
        drawLine(tokens.todayColor(colors), Offset(x, body), Offset(x, size.height), tokens.TodayLineWidth.toPx())
    }
    ganttTimelineSpans(model, geometry, scale, colors)
}

/** Group spans as pills and the brackets of parents without dates over their children. */
private fun DrawScope.ganttTimelineSpans(
    model: GanttTimelineModel,
    geometry: GanttTimelineGeometry,
    scale: GanttTimelineScale,
    colors: MonarchColors,
) {
    val tokens = MonarchGanttTimelineTokens
    val height = tokens.SpanHeight.toPx()
    val bracket = tokens.bracketColor(colors)
    model.lines.lines.forEachIndexed { i, row ->
        val center = geometry.rowTops[i] + geometry.rowHeights[i] / 2f
        val span = when (row) {
            is GanttTimelineLine.Group -> GanttTimelineRules.span(row.group.start, row.group.end)
            is GanttTimelineLine.Item -> row.rollup.takeIf {
                GanttTimelineRules.span(row.item.start, row.item.end) == null
            }
        } ?: return@forEachIndexed
        if (span.end < model.anchor || span.start > model.rangeEnd) return@forEachIndexed
        val left = maxOf(0f, scale.x(span.start))
        val width = maxOf(height / 2, scale.x(span.end) + scale.dayWidth - left)
        val top = center - height / 2
        if (row is GanttTimelineLine.Group) {
            val radius = CornerRadius(height / 2)
            drawRoundRect(tokens.groupSpanColor(colors), Offset(left, top), Size(width, height), radius)
            drawRoundRect(
                bracket,
                Offset(left, top),
                Size(width, height),
                radius,
                style = Stroke(tokens.LineWidth.toPx()),
            )
        } else {
            val stroke = tokens.ConflictBorderWidth.toPx()
            val path = Path().apply {
                moveTo(left + stroke / 2, top + height)
                lineTo(left + stroke / 2, top + stroke / 2)
                lineTo(left + width - stroke / 2, top + stroke / 2)
                lineTo(left + width - stroke / 2, top + height)
            }
            drawPath(path, bracket, style = Stroke(stroke))
        }
    }
}

/** The dependency arrows between the bars as shown, dashed in the danger colour when the link is broken. */
private fun DrawScope.ganttTimelineArrows(
    model: GanttTimelineModel,
    geometry: GanttTimelineGeometry,
    scale: GanttTimelineScale,
    drag: GanttTimelineDrag?,
    colors: MonarchColors,
) {
    val tokens = MonarchGanttTimelineTokens
    val rows = HashMap<String, Int>()
    model.lines.lines.forEachIndexed { i, row -> if (row is GanttTimelineLine.Item) rows[row.item.id] = i }
    val dash = tokens.DashLength.toPx()
    model.dependencies.forEach { dependency ->
        val a = model.itemsById[dependency.from] ?: return@forEach
        val b = model.itemsById[dependency.to] ?: return@forEach
        val ra = rows[a.id] ?: return@forEach
        val rb = rows[b.id] ?: return@forEach
        val sa = drag.shown(a).let { GanttTimelineRules.span(it.start, it.end) } ?: return@forEach
        val sb = drag.shown(b).let { GanttTimelineRules.span(it.start, it.end) } ?: return@forEach
        val points = GanttTimelineRules.dependencyPath(
            kind = dependency.kind,
            from = sa,
            to = sb,
            y1 = geometry.rowTops[ra] + geometry.rowHeights[ra] / 2f,
            y2 = geometry.rowTops[rb] + geometry.rowHeights[rb] / 2f,
            x = scale::x,
            dayWidth = scale.dayWidth,
            gap = tokens.ArrowGap.toPx(),
            tip = tokens.ArrowTip.toPx(),
        )
        val color = tokens.dependencyColor(colors, dependency.conflict)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val effect = if (dependency.conflict) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null
        drawPath(path, color, style = Stroke(tokens.ArrowStroke.toPx(), pathEffect = effect))
        ganttTimelineArrowHead(points[points.size - 2], points.last(), color, tokens.ArrowHead.toPx())
    }
}

/** A filled triangle at [tip], pointing away from [from] along a horizontal last segment. */
private fun DrawScope.ganttTimelineArrowHead(
    from: Offset,
    tip: Offset,
    color: Color,
    size: Float,
) {
    val direction = if (tip.x >= from.x) 1f else -1f
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - direction * size, tip.y - size / 2)
        lineTo(tip.x - direction * size, tip.y + size / 2)
        close()
    }
    drawPath(head, color)
}

private const val SATURDAY = 6
