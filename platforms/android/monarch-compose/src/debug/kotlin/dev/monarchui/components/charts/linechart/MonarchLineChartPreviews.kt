package dev.monarchui.components.charts.linechart

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

@MonarchPreviews
@Composable
internal fun MonarchLineChartPreview() {
    PreviewFrame {
        MonarchLineChart(
            title = "Sessions",
            categories = days,
            series = listOf(
                MonarchLineChartSeries("current", "This week", listOf(820.0, 932.0, 901.0, 1290.0, 1330.0)),
                MonarchLineChartSeries(
                    "previous",
                    "Last week",
                    listOf(700.0, 760.0, 810.0, 1020.0, 1100.0),
                    color = MonarchChartColor.Neutral,
                    dashed = true,
                ),
            ),
            description = "This week against last week",
            referenceLines = listOf(MonarchLineChartReferenceLine(1200.0, "Target")),
            activeIndex = 3,
            onActiveIndexChange = {},
            height = 160.dp,
        )
        MonarchLineChart(
            title = "p95 latency",
            categories = days,
            series = listOf(MonarchLineChartSeries("p95", "p95", listOf(210.0, 220.0, null, 260.0, 235.0))),
            variant = MonarchLineChartVariant.Area,
            curve = MonarchLineChartCurve.Monotone,
            includeZero = false,
            height = 140.dp,
            formatValue = { "${it.toInt()} ms" },
        )
    }
}
