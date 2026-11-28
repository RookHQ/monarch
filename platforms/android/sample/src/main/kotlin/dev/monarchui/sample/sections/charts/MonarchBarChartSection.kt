package dev.monarchui.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.barchart.MonarchBarChart
import dev.monarchui.components.charts.barchart.MonarchBarChartLayout
import dev.monarchui.components.charts.barchart.MonarchBarChartOrientation
import dev.monarchui.components.charts.barchart.MonarchBarChartReferenceLine
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of BarChart. */
object MonarchBarChartSection : CataloguePage {
    override val title = "BarChart"

    private val months = listOf("Jan", "Feb", "Mar", "Apr")
    private val target = MonarchBarChartReferenceLine(1300.0, "Target")
    private val signups = MonarchBarChartSeries("signups", "Signups", listOf(52.0, 47.0, 60.0, 66.0))
    private const val STACKED = 1
    private const val CURRENT_MONTH = 3
    private val series = listOf(
        MonarchBarChartSeries("web", "Web", listOf(1200.0, 1450.0, 1390.0, 1600.0)),
        MonarchBarChartSeries("ios", "iOS", listOf(800.0, 920.0, 1010.0, 1100.0)),
        MonarchBarChartSeries("android", "Android", listOf(640.0, 700.0, 760.0, 820.0)),
    )

    @Composable
    override fun Content() {
        var orientation by rememberSaveable { mutableIntStateOf(0) }
        var layout by rememberSaveable { mutableIntStateOf(0) }
        var hidden by rememberSaveable { mutableStateOf(listOf<String>()) }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Orientation")
        Choice(MonarchBarChartOrientation.entries.map { it.name }, orientation) { orientation = it }
        CatalogueCaption("Layout")
        Choice(MonarchBarChartLayout.entries.map { it.name }, layout) { layout = it }
        MonarchBarChart(
            title = "Revenue by platform",
            categories = months,
            series = series,
            description = "Monthly, in dollars",
            categoryTitle = "Month",
            orientation = MonarchBarChartOrientation.entries[orientation],
            layout = MonarchBarChartLayout.entries[layout],
            hiddenSeries = hidden.toSet(),
            onHiddenSeriesChange = { hidden = it.toList() },
            referenceLines = listOf(target),
            showValueLabels = layout == STACKED,
            footer = if (opened.isEmpty()) "Tap a month, then Details" else "Opened $opened",
            onSelect = { opened = months[it] },
        )
        CatalogueCaption("One series, current month highlighted")
        MonarchBarChart(
            title = "Signups",
            categories = months,
            series = listOf(signups),
            highlightedIndex = CURRENT_MONTH,
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
