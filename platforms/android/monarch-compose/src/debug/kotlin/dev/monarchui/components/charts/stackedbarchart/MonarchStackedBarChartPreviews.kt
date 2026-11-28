package dev.monarchui.components.charts.stackedbarchart

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.barchart.MonarchBarChartLayout
import dev.monarchui.components.charts.barchart.MonarchBarChartOrientation
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val quarter = listOf("Jan", "Feb", "Mar")
private val tickets = listOf(
    MonarchBarChartSeries("resolved", "Resolved", listOf(120.0, 132.0, 101.0)),
    MonarchBarChartSeries("pending", "Pending", listOf(22.0, 18.0, 30.0)),
    MonarchBarChartSeries("escalated", "Escalated", listOf(8.0, 6.0, 12.0)),
)

@MonarchPreviews
@Composable
internal fun MonarchStackedBarChartPreview() {
    PreviewFrame {
        MonarchStackedBarChart(
            title = "Tickets by status",
            categories = quarter,
            series = tickets,
            showValueLabels = true,
            height = 160.dp,
        )
        MonarchStackedBarChart(
            title = "Share of tickets",
            categories = quarter,
            series = tickets,
            orientation = MonarchBarChartOrientation.Horizontal,
            offset = MonarchStackedBarChartOffset.Expand,
            showValueLabels = true,
        )
        MonarchStackedBarChart(
            title = "Tickets side by side",
            categories = quarter,
            series = tickets,
            layout = MonarchBarChartLayout.Grouped,
            height = 160.dp,
        )
    }
}
