package com.rookhq.monarch.components.charts.linechart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartScale
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTicks
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTokens
import kotlin.math.max

/**
 * Everything the plot of a line chart draws, resolved by MonarchLineChart.
 *
 * @property xLabels the x values as written.
 * @property fractions the position of each x value along the plot, from 0 at the start to 1 at the end.
 * @property series every series, hidden ones too.
 * @property visible the indices of the shown series.
 * @property variant line or area.
 * @property curve straight or monotone.
 * @property references the reference lines.
 * @property valueRange the fixed value axis, or `null` to fit the values.
 * @property includeZero whether the value axis keeps zero.
 * @property showPoints whether every value gets a dot; lone values between gaps always do.
 */
@Suppress("LongParameterList")
internal class MonarchLineChartModel(
    val xLabels: List<String>,
    val fractions: List<Float>,
    val series: List<MonarchLineChartSeries>,
    val visible: List<Int>,
    val variant: MonarchLineChartVariant,
    val curve: MonarchLineChartCurve,
    val references: List<MonarchLineChartReferenceLine>,
    val valueRange: ClosedFloatingPointRange<Double>?,
    val includeZero: Boolean,
    val showPoints: Boolean,
) {
    /** The values the value axis must include: every shown value and every reference line. */
    fun extent(): List<Double> = visible.flatMap { index ->
        xLabels.indices.mapNotNull { MonarchLineChartGeometry.valueAt(series[index], it) }
    } + references.map { it.value }
}

/**
 * How text and colours are drawn.
 *
 * @property style the text style of every label.
 * @property colors the colours around the series.
 * @property lineColors the colour of each series' line and dots, by index in the series list.
 * @property areaColors the fill under each series' line, by index in the series list.
 * @property tickFormat writes the value labels of the axis and the reference lines.
 */
internal class MonarchLineChartPaint(
    val style: TextStyle,
    val colors: MonarchLineChartColors,
    val lineColors: List<Color>,
    val areaColors: List<Color>,
    val tickFormat: (Double) -> String,
)

/**
 * The plot of a line chart in pixels, measured from the start edge: [x0]..[x1] across, [y0]..[y1] down. Drawing
 * mirrors x in right to left.
 *
 * @property value the value scale, from the bottom up.
 * @property ticks the values that get a gridline and a label.
 * @property positions the x of each x value.
 * @property labelled the indices of the x values that get a label.
 */
@Suppress("LongParameterList")
internal class MonarchLineChartPlot(
    val width: Float,
    val height: Float,
    val x0: Float,
    val x1: Float,
    val y0: Float,
    val y1: Float,
    val value: MonarchBarChartScale,
    val ticks: List<Double>,
    val positions: List<Float>,
    val labelled: List<Int>,
) {
    /** The point of [series] at x value [index], or `null` for a gap. */
    fun point(series: MonarchLineChartSeries, index: Int): Offset? =
        MonarchLineChartGeometry.valueAt(series, index)?.let { Offset(positions[index], value.map(it)) }

    /** The points of [series], one per x value, `null` for each gap. */
    fun points(series: MonarchLineChartSeries): List<Offset?> = positions.indices.map { point(series, it) }

    /** The x value nearest to [x], measured from the start edge. */
    fun indexAt(x: Float): Int? = MonarchLineChartGeometry.nearestIndex(positions, x)
}

/** Measures the margins of the plot from its labels. */
internal object MonarchLineChartMeasure {
    /** The plot of [model] in a box of [width] by [height] pixels. */
    @Suppress("LongParameterList")
    fun plot(
        model: MonarchLineChartModel,
        width: Float,
        height: Float,
        measurer: TextMeasurer,
        paint: MonarchLineChartPaint,
        density: Density,
    ): MonarchLineChartPlot = with(density) {
        val gap = MonarchLineChartTokens.AxisGap.toPx()
        val pad = MonarchLineChartTokens.EdgePad.toPx()
        fun textWidth(text: String): Float = measurer.measure(text, paint.style).size.width.toFloat()
        val lineHeight = measurer.measure("0", paint.style).size.height.toFloat()
        val y0 = pad
        val y1 = max(y0 + 1f, height - lineHeight - gap)
        val count = MonarchBarChartTicks.count(y1 - y0, MonarchBarChartTokens.TickSpacingVertical.toPx())
        val axis = MonarchLineChartGeometry.axis(model.extent(), count, model.valueRange, model.includeZero)
        val x0 = (axis.ticks.maxOfOrNull { textWidth(paint.tickFormat(it)) } ?: 0f) + gap
        val x1 = max(x0 + 1f, width - pad)
        val positions = model.fractions.map { x0 + it * (x1 - x0) }
        val widest = model.xLabels.maxOfOrNull { textWidth(it) } ?: 0f
        val labelled = MonarchLineChartGeometry.labelIndices(model.xLabels.size, x1 - x0, widest + gap)
        MonarchLineChartPlot(
            width, height, x0, x1, y0, y1, MonarchBarChartScale(axis.domain, y1, y0), axis.ticks, positions, labelled,
        )
    }
}

/** Converts positions measured from the start edge to the canvas, which is never mirrored, for text. */
private class TextMirror(val width: Float, val rtl: Boolean) {
    fun left(start: Float, length: Float): Float = if (rtl) width - start - length else start
}

/**
 * Draws the plot of a line chart: grid, zero line, areas, reference lines, the crosshair, lines, dots and labels.
 * Shapes are drawn from the start edge in a mirrored scope in right to left; text is placed mirrored, never
 * flipped.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawMonarchLineChart(
    model: MonarchLineChartModel,
    plot: MonarchLineChartPlot,
    paint: MonarchLineChartPaint,
    measurer: TextMeasurer,
    active: Int?,
    rtl: Boolean,
) {
    withTransform({ if (rtl) scale(-1f, 1f, Offset(size.width / 2, 0f)) }) {
        drawShapes(model, plot, paint, active)
    }
    drawLabels(model, plot, paint, measurer, TextMirror(size.width, rtl))
}

private fun DrawScope.drawShapes(
    model: MonarchLineChartModel,
    plot: MonarchLineChartPlot,
    paint: MonarchLineChartPaint,
    active: Int?,
) {
    val line = MonarchBarChartTokens.LineWidth.toPx()
    val dash = MonarchBarChartTokens.Dash.toPx()
    plot.ticks.forEach { tick ->
        val y = plot.value.map(tick)
        drawLine(paint.colors.grid, Offset(plot.x0, y), Offset(plot.x1, y), line)
    }
    val domain = plot.value.domain
    if (0.0 >= domain.start && 0.0 <= domain.endInclusive) {
        val zero = plot.value.map(0.0)
        drawLine(paint.colors.zero, Offset(plot.x0, zero), Offset(plot.x1, zero), line)
    }
    if (model.variant == MonarchLineChartVariant.Area) {
        val baseline = plot.value.map(MonarchLineChartGeometry.baseline(domain))
        model.visible.forEach { index ->
            val area = MonarchLineChartPaths.areaPath(plot.points(model.series[index]), baseline, model.curve)
            drawPath(area, paint.areaColors[index])
        }
    }
    model.references.forEach { reference ->
        val y = plot.value.map(reference.value)
        drawLine(
            paint.colors.reference, Offset(plot.x0, y), Offset(plot.x1, y), line,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
    if (active != null && active in plot.positions.indices) {
        val x = plot.positions[active]
        drawLine(paint.colors.crosshair, Offset(x, plot.y0), Offset(x, plot.y1), line)
    }
    drawSeries(model, plot, paint, active)
}

private fun DrawScope.drawSeries(
    model: MonarchLineChartModel,
    plot: MonarchLineChartPlot,
    paint: MonarchLineChartPaint,
    active: Int?,
) {
    val width = MonarchLineChartTokens.Stroke.toPx()
    val seriesDash = MonarchLineChartTokens.SeriesDash.toPx()
    val dot = MonarchLineChartTokens.DotRadius.toPx()
    model.visible.forEach { index ->
        val item = model.series[index]
        val color = paint.lineColors[index]
        val points = plot.points(item)
        val stroke = Stroke(
            width = width,
            cap = if (item.dashed) StrokeCap.Butt else StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (item.dashed) PathEffect.dashPathEffect(floatArrayOf(seriesDash, seriesDash)) else null,
        )
        drawPath(MonarchLineChartPaths.linePath(points, model.curve), color, style = stroke)
        val dots = if (model.showPoints) points.filterNotNull() else MonarchLineChartPaths.isolated(points)
        dots.forEach { drawCircle(color, dot, it) }
    }
    if (active == null) return
    val activeRadius = MonarchLineChartTokens.ActiveDotRadius.toPx()
    val ring = MonarchLineChartTokens.DotRing.toPx()
    model.visible.forEach { index ->
        val point = plot.point(model.series[index], active) ?: return@forEach
        drawCircle(paint.colors.dotRing, activeRadius + ring, point)
        drawCircle(paint.lineColors[index], activeRadius, point)
    }
}

private fun DrawScope.drawLabels(
    model: MonarchLineChartModel,
    plot: MonarchLineChartPlot,
    paint: MonarchLineChartPaint,
    measurer: TextMeasurer,
    mirror: TextMirror,
) {
    val gap = MonarchLineChartTokens.AxisGap.toPx()
    plot.ticks.forEach { tick ->
        val text = measurer.measure(paint.tickFormat(tick), paint.style)
        val w = text.size.width.toFloat()
        val y = plot.value.map(tick) - text.size.height / 2f
        drawLabel(text, paint.colors.axisText, Offset(mirror.left(plot.x0 - gap - w, w), y))
    }
    model.references.forEach { reference ->
        val label = reference.label ?: return@forEach
        // The name and the value it stands for, as the web writes it: "Target 1.2K".
        val written = "$label ${paint.tickFormat(reference.value)}"
        val text = measureLine(measurer, written, paint.style, (plot.x1 - plot.x0).toInt().coerceAtLeast(1))
        val w = text.size.width.toFloat()
        val y = plot.value.map(reference.value) - text.size.height - MonarchBarChartTokens.LabelLift.toPx()
        // At the end where the lines are farther from the reference, so the name is not written over a line.
        val left = referenceClearance(model, plot, reference.value, plot.x0, plot.x0 + w) >
            referenceClearance(model, plot, reference.value, plot.x1 - w, plot.x1)
        val x = if (left) plot.x0 else plot.x1 - w
        drawLabel(text, paint.colors.reference, Offset(mirror.left(x, w), y))
    }
    val count = plot.labelled.size
    if (count == 0) return
    val room = ((plot.x1 - plot.x0) / count).toInt().coerceAtLeast(1)
    val last = model.xLabels.size - 1
    plot.labelled.forEach { index ->
        val text = measureLine(measurer, model.xLabels[index], paint.style, room)
        val w = text.size.width.toFloat()
        val at = plot.positions[index]
        // The first and last labels are anchored inside the plot so they are not cut at the edges.
        val start = when {
            last > 0 && index == 0 -> at
            last > 0 && index == last -> at - w
            else -> at - w / 2
        }.coerceIn(0f, max(0f, plot.width - w))
        drawLabel(text, paint.colors.axisText, Offset(mirror.left(start, w), plot.y1 + gap / 2))
    }
}

/** The smallest distance in pixels between the reference [value] and a series inside the x range [from, to]. */
private fun referenceClearance(
    model: MonarchLineChartModel,
    plot: MonarchLineChartPlot,
    value: Double,
    from: Float,
    to: Float,
): Float {
    val target = plot.value.map(value)
    return model.series
        .flatMap { plot.points(it).filterNotNull() }
        .filter { it.x in from..to }
        .minOfOrNull { kotlin.math.abs(it.y - target) } ?: Float.MAX_VALUE
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
