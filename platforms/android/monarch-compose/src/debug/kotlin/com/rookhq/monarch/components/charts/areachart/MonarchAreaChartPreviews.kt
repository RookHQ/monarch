package com.rookhq.monarch.components.charts.areachart

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartCurve
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartReferenceLine
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val platforms = listOf(
    MonarchLineChartSeries("web", "Web", listOf(420.0, 460.0, 445.0, 480.0, 610.0)),
    MonarchLineChartSeries("ios", "iOS", listOf(210.0, 230.0, 220.0, 240.0, 300.0)),
    MonarchLineChartSeries("android", "Android", listOf(150.0, 160.0, 170.0, 160.0, 210.0)),
)

@MonarchPreviews
@Composable
internal fun MonarchAreaChartPreview() {
    PreviewFrame {
        MonarchAreaChart(
            title = "Traffic",
            categories = days,
            series = platforms,
            description = "Daily sessions by platform",
            stacking = MonarchAreaChartStacking.Stacked,
            activeIndex = 3,
            onActiveIndexChange = {},
            height = 160.dp,
        )
        MonarchAreaChart(
            title = "Share of sessions",
            categories = days,
            series = platforms,
            stacking = MonarchAreaChartStacking.Expand,
            height = 140.dp,
        )
        MonarchAreaChart(
            title = "Users",
            categories = listOf("Jan", "Feb", "Mar", "Apr"),
            series = listOf(MonarchLineChartSeries("users", "Users", listOf(1000.0, 1085.0, 1170.0, 1255.0))),
            curve = MonarchLineChartCurve.Monotone,
            referenceLines = listOf(MonarchLineChartReferenceLine(1200.0, "Goal")),
            height = 140.dp,
        )
    }
}
