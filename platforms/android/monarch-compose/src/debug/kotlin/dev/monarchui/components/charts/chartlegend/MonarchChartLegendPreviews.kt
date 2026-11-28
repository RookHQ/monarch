package dev.monarchui.components.charts.chartlegend

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewItems = listOf(
    MonarchChartLegendItem("revenue", "Revenue", value = "$48,200"),
    MonarchChartLegendItem("costs", "Costs", value = "$31,050"),
    MonarchChartLegendItem("target", "Target", color = MonarchChartColor.Neutral, dashed = true),
)

@MonarchPreviews
@Composable
internal fun MonarchChartLegendPreview() {
    PreviewFrame {
        MonarchChartLegendMarker.entries.forEach { marker -> MonarchChartLegend(items = previewItems, marker = marker) }
        MonarchChartLegend(items = previewItems, orientation = Orientation.Vertical)
        MonarchChartLegend(items = previewItems, hiddenSeries = setOf("costs"), onHiddenSeriesChange = {})
        MonarchChartLegend(items = previewItems, hiddenSeries = emptySet(), onHiddenSeriesChange = {}, enabled = false)
    }
}
