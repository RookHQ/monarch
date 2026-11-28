package com.rookhq.monarch.components.charts.funnelchart

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewStages = listOf(
    MonarchFunnelChartStage("Visited pricing", 4800.0),
    MonarchFunnelChartStage("Started sign-up", 1900.0),
    MonarchFunnelChartStage("Paid", 310.0),
)

@MonarchPreviews
@Composable
internal fun MonarchFunnelChartPreview() {
    PreviewFrame {
        MonarchFunnelChart(title = "Sign-up funnel", stages = previewStages, description = "Last 30 days")
        MonarchFunnelChart(
            title = "Hiring pipeline",
            stages = previewStages,
            alignment = MonarchFunnelChartAlignment.Start,
            showSteps = false,
            onSelect = {},
        )
        MonarchFunnelChart(title = "Sign-up funnel", stages = emptyList())
        MonarchFunnelChart(title = "Sign-up funnel", stages = previewStages, status = MonarchChartFrameStatus.Error)
    }
}
