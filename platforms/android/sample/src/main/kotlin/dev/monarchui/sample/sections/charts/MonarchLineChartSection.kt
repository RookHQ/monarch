package dev.monarchui.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.linechart.MonarchLineChart
import dev.monarchui.components.charts.linechart.MonarchLineChartCurve
import dev.monarchui.components.charts.linechart.MonarchLineChartReferenceLine
import dev.monarchui.components.charts.linechart.MonarchLineChartSeries
import dev.monarchui.components.charts.linechart.MonarchLineChartVariant
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of LineChart. */
object MonarchLineChartSection : CataloguePage {
    override val title = "LineChart"

    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    private val weeks = listOf("W1", "W2", "W3", "W4", "W5", "W6")
    private val sessions = listOf(
        MonarchLineChartSeries("current", "This week", listOf(820.0, 932.0, 901.0, 1290.0, 1330.0)),
        MonarchLineChartSeries(
            "previous",
            "Last week",
            listOf(700.0, 760.0, 810.0, 1020.0, 1100.0),
            color = MonarchChartColor.Neutral,
            dashed = true,
        ),
    )
    private const val DAY = 86_400_000L
    private const val MARCH_1 = 1_772_323_200_000L
    private val latencyDates = listOf(0, 1, 2, 4, 5, 6, 9).map { MARCH_1 + it * DAY }
    private val latency = listOf(
        MonarchLineChartSeries("p95", "p95", listOf(210.0, 220.0, null, 260.0, 240.0, null, 235.0)),
    )
    private val weeklySessions = listOf(
        MonarchLineChartSeries("s", "Sessions", listOf(820.0, 932.0, 901.0, 1290.0, 1210.0, 1400.0)),
    )
    private val signUps = listOf(
        MonarchLineChartSeries("u", "Sign-ups", listOf(42.0, 51.0, 47.0, 88.0, 70.0, 95.0), MonarchChartColor.Data2),
    )
    private val target = listOf(MonarchLineChartReferenceLine(1200.0, "Target"))

    @Composable
    override fun Content() {
        var variant by rememberSaveable { mutableIntStateOf(0) }
        var curve by rememberSaveable { mutableIntStateOf(0) }
        var status by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Variant")
        Choice(MonarchLineChartVariant.entries.map { it.name }, variant) { variant = it }
        CatalogueCaption("Curve")
        Choice(MonarchLineChartCurve.entries.map { it.name }, curve) { curve = it }
        CatalogueCaption("Status")
        Choice(MonarchChartFrameStatus.entries.map { it.name }, status) { status = it }
        MonarchLineChart(
            title = "Sessions",
            categories = days,
            series = sessions,
            description = "This week against last week",
            variant = MonarchLineChartVariant.entries[variant],
            curve = MonarchLineChartCurve.entries[curve],
            status = MonarchChartFrameStatus.entries[status],
            referenceLines = target,
            onRetry = { status = 0 },
        )
        CatalogueCaption("Dates spaced by time, gaps, without zero")
        MonarchLineChart(
            title = "p95 latency",
            dates = latencyDates,
            series = latency,
            variant = MonarchLineChartVariant.Area,
            curve = MonarchLineChartCurve.Monotone,
            includeZero = false,
            formatValue = { "${it.toInt()} ms" },
        )
        CatalogueCaption("Two charts sharing one crosshair")
        var active by rememberSaveable { mutableStateOf<Int?>(2) }
        MonarchLineChart(
            title = "Sessions",
            categories = weeks,
            series = weeklySessions,
            activeIndex = active,
            onActiveIndexChange = { active = it },
        )
        MonarchLineChart(
            title = "Sign-ups",
            categories = weeks,
            series = signUps,
            activeIndex = active,
            onActiveIndexChange = { active = it },
            showPoints = true,
        )
    }

    @Composable
    private fun Choice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        MonarchSegmentedControl {
            options.forEachIndexed { index, option ->
                Segment(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    label = { Text(option) },
                )
            }
        }
    }
}
