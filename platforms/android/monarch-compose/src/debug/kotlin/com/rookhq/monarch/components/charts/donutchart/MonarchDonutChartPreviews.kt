package com.rookhq.monarch.components.charts.donutchart

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchDonutChartPreview() {
    PreviewFrame {
        MonarchDonutChart(
            title = "Assets by type",
            segments = listOf(
                MonarchDonutChartSegment("laptop", "Laptops", 412.0),
                MonarchDonutChartSegment("phone", "Phones", 268.0),
                MonarchDonutChartSegment("monitor", "Monitors", 131.0),
                MonarchDonutChartSegment("tablet", "Tablets", 54.0),
            ),
            centerLabel = "assets",
        )
        MonarchDonutChart(
            title = "Storage",
            segments = listOf(
                MonarchDonutChartSegment("files", "Files", 820.0),
                MonarchDonutChartSegment("free", "Free", 640.0, MonarchChartColor.Neutral),
            ),
            centerValue = "56%",
            centerLabel = "used",
            sort = MonarchDonutChartSort.None,
            legendContent = MonarchDonutChartLegendContent.Percent,
        )
        MonarchDonutChart(title = "Assets", segments = emptyList(), status = MonarchChartFrameStatus.Loading)
    }
}
