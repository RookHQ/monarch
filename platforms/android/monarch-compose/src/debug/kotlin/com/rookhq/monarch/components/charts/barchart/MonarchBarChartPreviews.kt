package com.rookhq.monarch.components.charts.barchart

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val months = listOf("Jan", "Feb", "Mar", "Apr")
private val platforms = listOf(
    MonarchBarChartSeries("web", "Web", listOf(1200.0, 1450.0, 1390.0, 1600.0)),
    MonarchBarChartSeries("ios", "iOS", listOf(800.0, 920.0, 1010.0, -200.0)),
)

@MonarchPreviews
@Composable
internal fun MonarchBarChartPreview() {
    PreviewFrame {
        MonarchBarChart(
            title = "Revenue by platform",
            categories = months,
            series = platforms,
            referenceLines = listOf(MonarchBarChartReferenceLine(1300.0, "Target")),
            height = 160.dp,
        )
        MonarchBarChart(
            title = "Tickets by team",
            categories = listOf("Support", "Sales", "Engineering"),
            series = platforms,
            orientation = MonarchBarChartOrientation.Horizontal,
            layout = MonarchBarChartLayout.Stacked,
            showValueLabels = true,
        )
        MonarchBarChart(
            title = "Signups",
            categories = months,
            series = listOf(platforms.first()),
            highlightedIndex = 2,
            height = 120.dp,
        )
    }
}
