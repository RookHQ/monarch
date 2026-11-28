package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChart
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChartStacking
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartCurve
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartReferenceLine
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AreaChart. */
object MonarchAreaChartSection : CataloguePage {
    override val title = "AreaChart"

    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    private val platforms = listOf(
        MonarchLineChartSeries("web", "Web", listOf(420.0, 460.0, 445.0, 480.0, 610.0)),
        MonarchLineChartSeries("ios", "iOS", listOf(210.0, 230.0, 220.0, 240.0, 300.0)),
        MonarchLineChartSeries("android", "Android", listOf(150.0, 160.0, 170.0, 160.0, 210.0)),
    )
    private const val DAY = 86_400_000L
    private const val GOAL = 1200.0
    private const val MARCH_1 = 1_772_323_200_000L
    private val months = listOf(0, 31, 59, 90).map { MARCH_1 + it * DAY }
    private val users = listOf(MonarchLineChartSeries("users", "Users", listOf(1000.0, 1085.0, 1170.0, 1255.0)))

    @Composable
    override fun Content() {
        var stacking by rememberSaveable { mutableIntStateOf(1) }
        var curve by rememberSaveable { mutableIntStateOf(0) }
        var status by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Stacking")
        Choice(MonarchAreaChartStacking.entries.map { it.name }, stacking) { stacking = it }
        CatalogueCaption("Curve")
        Choice(MonarchLineChartCurve.entries.map { it.name }, curve) { curve = it }
        CatalogueCaption("Status")
        Choice(MonarchChartFrameStatus.entries.map { it.name }, status) { status = it }
        MonarchAreaChart(
            title = "Traffic",
            categories = days,
            series = platforms,
            description = "Daily sessions by platform",
            stacking = MonarchAreaChartStacking.entries[stacking],
            curve = MonarchLineChartCurve.entries[curve],
            status = MonarchChartFrameStatus.entries[status],
            onRetry = { status = 0 },
        )
        CatalogueCaption("One volume over dates with a goal")
        var active by rememberSaveable { mutableStateOf<Int?>(null) }
        MonarchAreaChart(
            title = "Users",
            dates = months,
            series = users,
            curve = MonarchLineChartCurve.Monotone,
            referenceLines = listOf(MonarchLineChartReferenceLine(GOAL, "Goal")),
            activeIndex = active,
            onActiveIndexChange = { active = it },
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
