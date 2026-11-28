package dev.monarchui.components.charts.chartframe

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendItem
import dev.monarchui.components.charts.sparkline.MonarchSparkline
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewTable = MonarchChartTable(
    categoryTitle = "Week",
    categories = listOf("W1", "W2", "W3", "W4"),
    series = listOf(MonarchChartTableSeries("Signups", listOf(32.0, 41.0, 38.0, 52.0))),
)

@MonarchPreviews
@Composable
internal fun MonarchChartFramePreview() {
    PreviewFrame {
        MonarchChartFrame(
            title = "Signups",
            description = "Last four weeks",
            table = previewTable,
            legend = listOf(MonarchChartLegendItem("signups", "Signups")),
            footer = "Source: billing",
            height = 96.dp,
        ) { MonarchSparkline(values = listOf(32.0, 41.0, 38.0, 52.0)) }
        MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Empty, height = 96.dp) {}
        MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Error, onRetry = {}, height = 96.dp) {}
    }
}
