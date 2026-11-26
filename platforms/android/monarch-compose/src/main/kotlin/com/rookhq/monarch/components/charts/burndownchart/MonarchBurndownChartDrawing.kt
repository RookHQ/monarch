package com.rookhq.monarch.components.charts.burndownchart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartScale
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTicks
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartTokens
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartGeometry
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartModel
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartPaint
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartPlot
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartTokens
import com.rookhq.monarch.components.charts.linechart.drawMonarchLineChart
import kotlin.math.max

/**
 * Everything the plot of a burndown draws.
 *
 * @property model the work left and the ideal, drawn by the line chart core.
 * @property scope the scope per day, drawn as a step line; empty when the scope is not shown.
 * @property changeLabels the "+5" label of each day whose scope changed, empty for the others.
 * @property today the day of the today marker, or `null`.
 * @property todayLabel the label of the today marker: "Today".
 */
internal class MonarchBurndownChartScene(
    val model: MonarchLineChartModel,
    val scope: List<Double?>,
    val changeLabels: List<String>,
    val today: Int?,
    val todayLabel: String,
)

/** Measures the plot of a burndown: the line chart's margins, with a line of room above for the labels. */
internal object MonarchBurndownChartMeasure {
    /** The plot of [scene] in a box of [width] by [height] pixels. The value axis always starts at zero. */
    @Suppress("LongParameterList")
    fun plot(
        scene: MonarchBurndownChartScene,
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
        val model = scene.model
        val y0 = pad + lineHeight + gap / 2
        val y1 = max(y0 + 1f, height - lineHeight - gap)
        val count = MonarchBarChartTicks.count(y1 - y0, MonarchBarChartTokens.TickSpacingVertical.toPx())
        val axis = MonarchLineChartGeometry.axis(model.extent() + scene.scope.filterNotNull(), count, null, true)
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

/**
 * Draws a burndown: the line chart core draws the grid, the axis, the ideal and the work left with the crosshair;
 * over it come the scope step line with its change dots and labels, and the today marker. Shapes are drawn from the
 * start edge in a mirrored scope in right to left; text is placed mirrored, never flipped.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawMonarchBurndownChart(
    scene: MonarchBurndownChartScene,
    plot: MonarchLineChartPlot,
    paint: MonarchLineChartPaint,
    colors: MonarchBurndownChartColors,
    measurer: TextMeasurer,
    active: Int?,
    rtl: Boolean,
) {
    drawMonarchLineChart(scene.model, plot, paint, measurer, active, rtl)
    val scope = scene.scope.mapIndexed { i, value -> value?.let { Offset(plot.positions[i], plot.value.map(it)) } }
    withTransform({ if (rtl) scale(-1f, 1f, Offset(size.width / 2, 0f)) }) {
        burndownToday(scene, plot, colors)
        drawPath(
            burndownChartStepPath(scope),
            colors.scope,
            style = Stroke(width = MonarchBurndownChartTokens.ScopeStroke.toPx(), join = StrokeJoin.Miter),
        )
        val dot = MonarchBurndownChartTokens.ChangeDotRadius.toPx()
        scope.forEachIndexed { i, point ->
            if (point != null && scene.changeLabels.getOrNull(i).orEmpty().isNotEmpty()) {
                drawCircle(colors.scope, dot, point)
            }
        }
    }
    burndownLabels(scene, plot, scope, paint, colors, measurer, rtl)
}

/**
 * The scope as a step line: it holds its value until the day it changes, then rises or falls there. A missing
 * scope breaks the line.
 */
internal fun burndownChartStepPath(points: List<Offset?>): Path = Path().apply {
    points.forEachIndexed { i, point ->
        if (point == null) return@forEachIndexed
        val before = points.getOrNull(i - 1)
        if (before == null) {
            moveTo(point.x, point.y)
        } else {
            lineTo(point.x, before.y)
            lineTo(point.x, point.y)
        }
    }
}

private fun DrawScope.burndownToday(
    scene: MonarchBurndownChartScene,
    plot: MonarchLineChartPlot,
    colors: MonarchBurndownChartColors,
) {
    val today = scene.today?.takeIf { it in plot.positions.indices } ?: return
    val x = plot.positions[today]
    val dash = MonarchBarChartTokens.Dash.toPx()
    drawLine(
        colors.today, Offset(x, plot.y0), Offset(x, plot.y1), MonarchBarChartTokens.LineWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
}

@Suppress("LongParameterList")
private fun DrawScope.burndownLabels(
    scene: MonarchBurndownChartScene,
    plot: MonarchLineChartPlot,
    scope: List<Offset?>,
    paint: MonarchLineChartPaint,
    colors: MonarchBurndownChartColors,
    measurer: TextMeasurer,
    rtl: Boolean,
) {
    val gap = MonarchLineChartTokens.AxisGap.toPx()
    val room = (plot.x1 - plot.x0).toInt().coerceAtLeast(1)
    fun place(text: TextLayoutResult, color: Color, centre: Float, top: Float) {
        val w = text.size.width.toFloat()
        val start = (centre - w / 2).coerceIn(0f, max(0f, plot.width - w))
        drawText(text, color = color, topLeft = Offset(if (rtl) plot.width - start - w else start, top))
    }
    scene.today?.takeIf { it in plot.positions.indices }?.let { today ->
        val text = burndownLine(measurer, scene.todayLabel, paint, room)
        place(text, colors.todayText, plot.positions[today], plot.y0 - text.size.height - gap / TodayGapParts)
    }
    scope.forEachIndexed { i, point ->
        val label = scene.changeLabels.getOrNull(i).orEmpty()
        if (point == null || label.isEmpty()) return@forEachIndexed
        val text = burndownLine(measurer, label, paint, room)
        place(text, colors.changeText, point.x, max(0f, point.y - gap / 2 - text.size.height))
    }
}

/** One line of text, cut with an ellipsis at [maxWidth] pixels. */
private fun burndownLine(
    measurer: TextMeasurer,
    text: String,
    paint: MonarchLineChartPaint,
    maxWidth: Int,
): TextLayoutResult = measurer.measure(
    text = text,
    style = paint.style,
    overflow = TextOverflow.Ellipsis,
    maxLines = 1,
    constraints = Constraints(maxWidth = maxWidth),
)

/** The today label sits a quarter of the axis gap above the plot. */
private const val TodayGapParts = 4f
