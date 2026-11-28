package dev.monarchui.components.charts.barchart

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Everything the plot of a bar chart draws, resolved by MonarchBarChart.
 *
 * @property categories the category labels.
 * @property series every series, hidden ones too.
 * @property visible the indices of the shown series.
 * @property stacks the bars of each category.
 * @property horizontal whether the bars grow across.
 * @property stacked whether the bars of a category are stacked.
 * @property highlightedIndex the emphasised category; the others are drawn in the dimmed colour.
 * @property references the reference lines.
 * @property valueRange the fixed value axis, or `null` to fit the values.
 * @property showValueLabels whether each bar (or stack) has its value at its end.
 */
@Suppress("LongParameterList")
internal class MonarchBarChartModel(
    val categories: List<String>,
    val series: List<MonarchBarChartSeries>,
    val visible: List<Int>,
    val stacks: List<MonarchBarChartStack>,
    val horizontal: Boolean,
    val stacked: Boolean,
    val highlightedIndex: Int?,
    val references: List<MonarchBarChartReferenceLine>,
    val valueRange: ClosedFloatingPointRange<Double>?,
    val showValueLabels: Boolean,
) {
    /** Whether the bars of [index] are dimmed because another category is highlighted. */
    fun dimmed(index: Int): Boolean = highlightedIndex != null && highlightedIndex != index

    /** The value labels of category [index]: each bar's end, or the stack's net total once. */
    fun valueLabels(index: Int): List<Pair<MonarchBarChartSegment, Double>> {
        val stack = stacks.getOrNull(index) ?: return emptyList()
        if (!stacked) return stack.segments.map { it to it.to }
        val end = if (stack.total >= 0) stack.positive else stack.negative
        return stack.segments.take(1).map { it to end }
    }

    /** The value written on a label of category [index]: the bar's value, or the stack's net total. */
    fun labelValue(index: Int, segment: MonarchBarChartSegment): Double =
        if (stacked) stacks[index].total else segment.value
}

/**
 * How text and colours are drawn.
 *
 * @property style the text style of every label.
 * @property colors the colours around the bars.
 * @property seriesColors the colour of each series, by index in the series list.
 * @property dimmedColor the colour of the bars of categories that are not highlighted.
 * @property tickFormat writes the tick and value labels.
 */
internal class MonarchBarChartPaint(
    val style: TextStyle,
    val colors: MonarchBarChartColors,
    val seriesColors: List<Color>,
    val dimmedColor: Color,
    val tickFormat: (Double) -> String,
)

/** Measures the margins of the plot from its labels. Shared chart core. */
internal object MonarchBarChartMeasure {
    /** The plot of [model] in a box of [width] by [height] pixels. */
    @Suppress("LongParameterList")
    fun plot(
        model: MonarchBarChartModel,
        width: Float,
        height: Float,
        measurer: TextMeasurer,
        paint: MonarchBarChartPaint,
        density: Density,
    ): MonarchBarChartPlot = with(density) {
        val gap = MonarchBarChartTokens.AxisGap.toPx()
        val pad = MonarchBarChartTokens.EdgePad.toPx()
        val count = model.categories.size.coerceAtLeast(1)
        val lineHeight = measurer.measure("0", paint.style).size.height.toFloat()
        val extent = MonarchBarChartGeometry.extent(model.stacks, model.stacked, model.references.map { it.value })
        fun textWidth(text: String): Float = measurer.measure(text, paint.style).size.width.toFloat()
        if (model.horizontal) {
            val widest = model.categories.maxOfOrNull { textWidth(it) } ?: 0f
            val categoryWidth = min(widest, width * MonarchBarChartTokens.CATEGORY_LABEL_SHARE)
            val valueRoom = if (model.showValueLabels) {
                val labels = model.categories.indices.flatMap { index ->
                    model.valueLabels(index).map { model.labelValue(index, it.first) }
                }
                (labels.maxOfOrNull { textWidth(paint.tickFormat(it)) } ?: 0f) + gap
            } else {
                0f
            }
            val x0 = categoryWidth + gap
            val x1 = max(x0 + 1f, width - pad - valueRoom)
            val y1 = max(1f, height - lineHeight - gap)
            val ticks = MonarchBarChartTicks.count(x1 - x0, MonarchBarChartTokens.TickSpacingHorizontal.toPx())
            val axis = MonarchBarChartTicks.axis(extent, ticks, model.valueRange)
            MonarchBarChartPlot(
                width, height, x0, x1, 0f, y1, true,
                MonarchBarChartScale(axis.domain, x0, x1), 0f, y1 / count, axis.ticks,
            )
        } else {
            val y0 = pad + if (model.showValueLabels) lineHeight + gap else 0f
            val y1 = max(y0 + 1f, height - lineHeight - gap)
            val ticks = MonarchBarChartTicks.count(y1 - y0, MonarchBarChartTokens.TickSpacingVertical.toPx())
            val axis = MonarchBarChartTicks.axis(extent, ticks, model.valueRange)
            val x0 = (axis.ticks.maxOfOrNull { textWidth(paint.tickFormat(it)) } ?: 0f) + gap
            val x1 = max(x0 + 1f, width - pad)
            MonarchBarChartPlot(
                width, height, x0, x1, y0, y1, false,
                MonarchBarChartScale(axis.domain, y1, y0), x0, (x1 - x0) / count, axis.ticks,
            )
        }
    }
}

/** Converts positions measured from the start edge to the canvas, which is never mirrored. */
private class Mirror(val width: Float, val rtl: Boolean) {
    fun x(start: Float): Float = if (rtl) width - start else start

    fun left(start: Float, length: Float): Float = if (rtl) width - start - length else start

    fun rect(rect: Rect): Rect = Rect(Offset(left(rect.left, rect.width), rect.top), rect.size)
}

/** The edge of a bar that is rounded, on the canvas. */
private enum class BarEnd { Top, Bottom, Left, Right }

/** Draws the plot of a bar chart: band, grid, bars, zero line, reference lines and labels. */
@Suppress("LongParameterList")
internal fun DrawScope.drawMonarchBarChart(
    model: MonarchBarChartModel,
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    measurer: TextMeasurer,
    inspected: Int?,
    rtl: Boolean,
) {
    val mirror = Mirror(size.width, rtl)
    if (inspected != null && inspected in model.categories.indices) {
        val radius = CornerRadius(MonarchBarChartTokens.BarRadius.toPx())
        val band = mirror.rect(plot.categoryRect(inspected))
        drawRoundRect(paint.colors.band, band.topLeft, band.size, radius)
    }
    drawGrid(plot, paint, measurer, mirror)
    drawBars(model, plot, paint, mirror)
    drawZeroAndReferences(model, plot, paint, measurer, mirror)
    if (model.showValueLabels) drawValueLabels(model, plot, paint, measurer, mirror)
    drawCategoryLabels(model, plot, paint, measurer, mirror)
}

private fun DrawScope.drawGrid(
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    measurer: TextMeasurer,
    mirror: Mirror,
) {
    val line = MonarchBarChartTokens.LineWidth.toPx()
    val gap = MonarchBarChartTokens.AxisGap.toPx()
    plot.ticks.forEach { tick ->
        val at = plot.value.map(tick)
        val text = measurer.measure(paint.tickFormat(tick), paint.style)
        if (plot.horizontal) {
            val x = mirror.x(at)
            drawLine(paint.colors.grid, Offset(x, plot.y0), Offset(x, plot.y1), line)
            drawLabel(text, paint.colors.axisText, Offset(x - text.size.width / 2f, plot.y1 + gap / 2))
        } else {
            drawLine(paint.colors.grid, Offset(mirror.x(plot.x0), at), Offset(mirror.x(plot.x1), at), line)
            val left = mirror.left(plot.x0 - gap - text.size.width, text.size.width.toFloat())
            drawLabel(text, paint.colors.axisText, Offset(left, at - text.size.height / 2f))
        }
    }
}

private fun DrawScope.drawBars(
    model: MonarchBarChartModel,
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    mirror: Mirror,
) {
    val bars = model.visible.size
    val group = MonarchBarChartGeometry.groupSize(plot.band, MonarchBarChartTokens.MaxBar.toPx(), bars, model.stacked)
    val gap = MonarchBarChartTokens.BarGap.toPx()
    val radius = MonarchBarChartTokens.BarRadius.toPx()
    model.stacks.forEachIndexed { index, stack ->
        stack.segments.forEach { segment ->
            val (offset, thickness) =
                MonarchBarChartGeometry.cross(segment.slot, plot.band, group, bars, model.stacked, gap)
            val cross = plot.bandAt(index) + offset
            val a = plot.value.map(segment.from)
            val b = plot.value.map(segment.to)
            val positive = segment.value >= 0
            val (rect, end) = if (plot.horizontal) {
                val start = mirror.left(min(a, b), abs(b - a))
                val side = if (positive != mirror.rtl) BarEnd.Right else BarEnd.Left
                Rect(start, cross, start + abs(b - a), cross + thickness) to side
            } else {
                val left = mirror.left(cross, thickness)
                Rect(left, min(a, b), left + thickness, max(a, b)) to (if (positive) BarEnd.Top else BarEnd.Bottom)
            }
            val color = if (model.dimmed(index)) paint.dimmedColor else paint.seriesColors[segment.series]
            drawPath(barPath(rect, if (segment.roundedEnd) radius else 0f, end), color)
        }
    }
}

/** A bar with its far end rounded. */
private fun barPath(rect: Rect, radius: Float, end: BarEnd): Path {
    val r = CornerRadius(min(radius, min(rect.width, rect.height) / 2))
    val none = CornerRadius.Zero
    val shape = when (end) {
        BarEnd.Top -> RoundRect(rect, r, r, none, none)
        BarEnd.Bottom -> RoundRect(rect, none, none, r, r)
        BarEnd.Right -> RoundRect(rect, none, r, r, none)
        BarEnd.Left -> RoundRect(rect, r, none, none, r)
    }
    return Path().apply { addRoundRect(shape) }
}

private fun DrawScope.drawZeroAndReferences(
    model: MonarchBarChartModel,
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    measurer: TextMeasurer,
    mirror: Mirror,
) {
    val line = MonarchBarChartTokens.LineWidth.toPx()
    val dash = MonarchBarChartTokens.Dash.toPx()
    val dashed = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
    val zero = plot.value.map(0.0)
    acrossLine(plot, mirror, zero).let { (from, to) -> drawLine(paint.colors.zero, from, to, line) }
    model.references.forEach { reference ->
        val at = plot.value.map(reference.value)
        val (from, to) = acrossLine(plot, mirror, at)
        drawLine(paint.colors.reference, from, to, line, pathEffect = dashed)
        val label = reference.label ?: return@forEach
        val text = measurer.measure(label, paint.style)
        val width = text.size.width.toFloat()
        val topLeft = if (plot.horizontal) {
            Offset(mirror.left(at + line, width), plot.y0)
        } else {
            Offset(mirror.left(plot.x1 - width, width), at - text.size.height - MonarchBarChartTokens.LabelLift.toPx())
        }
        drawLabel(text, paint.colors.reference, topLeft)
    }
}

/** The ends of a line across the bars at [at] on the value axis. */
private fun acrossLine(plot: MonarchBarChartPlot, mirror: Mirror, at: Float): Pair<Offset, Offset> =
    if (plot.horizontal) {
        Offset(mirror.x(at), plot.y0) to Offset(mirror.x(at), plot.y1)
    } else {
        Offset(mirror.x(plot.x0), at) to Offset(mirror.x(plot.x1), at)
    }

private fun DrawScope.drawValueLabels(
    model: MonarchBarChartModel,
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    measurer: TextMeasurer,
    mirror: Mirror,
) {
    val bars = model.visible.size
    val group = MonarchBarChartGeometry.groupSize(plot.band, MonarchBarChartTokens.MaxBar.toPx(), bars, model.stacked)
    val gap = MonarchBarChartTokens.AxisGap.toPx() / 2
    val labels = model.categories.indices.flatMap { index -> model.valueLabels(index).map { index to it } }
    labels.forEach { (index, label) ->
        val (segment, end) = label
        val value = model.labelValue(index, segment)
        val (offset, thickness) = MonarchBarChartGeometry.cross(
            segment.slot, plot.band, group, bars, model.stacked, MonarchBarChartTokens.BarGap.toPx(),
        )
        val center = plot.bandAt(index) + offset + thickness / 2
        val at = plot.value.map(end)
        val text = measurer.measure(paint.tickFormat(value), paint.style)
        val w = text.size.width.toFloat()
        val h = text.size.height.toFloat()
        val topLeft = if (plot.horizontal) {
            val start = if (value >= 0) at + gap else at - gap - w
            Offset(mirror.left(start, w), center - h / 2)
        } else {
            Offset(mirror.left(center - w / 2, w), if (value >= 0) at - gap - h else at + gap)
        }
        drawLabel(text, paint.colors.axisText, topLeft)
    }
}

private fun DrawScope.drawCategoryLabels(
    model: MonarchBarChartModel,
    plot: MonarchBarChartPlot,
    paint: MonarchBarChartPaint,
    measurer: TextMeasurer,
    mirror: Mirror,
) {
    val gap = MonarchBarChartTokens.AxisGap.toPx()
    if (plot.horizontal) {
        val room = (plot.x0 - gap).toInt()
        if (room <= 0) return
        model.categories.forEachIndexed { index, label ->
            val text = measureLine(measurer, label, paint.style, room)
            val w = text.size.width.toFloat()
            val center = plot.bandAt(index) + plot.band / 2
            val topLeft = Offset(mirror.left(plot.x0 - gap - w, w), center - text.size.height / 2f)
            drawLabel(text, paint.colors.axisText, topLeft)
        }
    } else {
        val widest = model.categories.maxOfOrNull { measurer.measure(it, paint.style).size.width.toFloat() } ?: 0f
        val step = MonarchBarChartGeometry.labelStep(plot.band, widest + gap)
        val room = (plot.band * step - gap).toInt()
        if (room <= 0) return
        model.categories.forEachIndexed { index, label ->
            if (index % step != 0) return@forEachIndexed
            val text = measureLine(measurer, label, paint.style, room)
            val w = text.size.width.toFloat()
            val center = plot.bandAt(index) + plot.band / 2
            drawLabel(text, paint.colors.axisText, Offset(mirror.left(center - w / 2, w), plot.y1 + gap / 2))
        }
    }
}

/** One line of text, cut with an ellipsis at [maxWidth] pixels. */
private fun measureLine(measurer: TextMeasurer, text: String, style: TextStyle, maxWidth: Int): TextLayoutResult =
    measurer.measure(
        text = text,
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth),
    )

private fun DrawScope.drawLabel(text: TextLayoutResult, color: Color, topLeft: Offset) {
    drawText(textLayoutResult = text, color = color, topLeft = topLeft)
}
