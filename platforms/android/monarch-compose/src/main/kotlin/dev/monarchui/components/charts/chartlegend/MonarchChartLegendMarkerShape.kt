package dev.monarchui.components.charts.chartlegend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

/** The marker of one legend entry, drawn in [color]. Decoration: the label names the series. */
@Composable
internal fun ChartLegendMarkerShape(marker: MonarchChartLegendMarker, color: Color, dashed: Boolean) {
    when (marker) {
        MonarchChartLegendMarker.Line -> Canvas(
            Modifier.size(MonarchChartLegendTokens.LineLength, MonarchChartLegendTokens.Stroke),
        ) {
            val dash = MonarchChartLegendTokens.Dash.toPx()
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2)) else null,
            )
        }
        MonarchChartLegendMarker.Square -> Canvas(Modifier.size(MonarchChartLegendTokens.Square)) {
            if (dashed) {
                val stroke = MonarchChartLegendTokens.Stroke.toPx()
                drawRect(
                    color = color,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
            } else {
                drawRect(color = color)
            }
        }
        MonarchChartLegendMarker.Dot -> Canvas(Modifier.size(MonarchChartLegendTokens.Dot)) {
            drawCircle(color = color)
        }
    }
}
