package dev.monarchui.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.charts.barchart.MonarchBarChartOrientation
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChart
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartOffset
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of StackedBarChart. */
object MonarchStackedBarChartSection : CataloguePage {
    override val title = "StackedBarChart"

    private val months = listOf("Jan", "Feb", "Mar", "Apr")
    private val series = listOf(
        MonarchBarChartSeries("resolved", "Resolved", listOf(120.0, 132.0, 101.0, 140.0)),
        MonarchBarChartSeries("pending", "Pending", listOf(22.0, 18.0, 30.0, 16.0)),
        MonarchBarChartSeries("escalated", "Escalated", listOf(8.0, 6.0, 12.0, 4.0)),
    )

    @Composable
    override fun Content() {
        var offset by rememberSaveable { mutableIntStateOf(0) }
        var orientation by rememberSaveable { mutableIntStateOf(0) }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Offset")
        StackedChoice(MonarchStackedBarChartOffset.entries.map { it.name }, offset) { offset = it }
        CatalogueCaption("Orientation")
        StackedChoice(MonarchBarChartOrientation.entries.map { it.name }, orientation) { orientation = it }
        MonarchStackedBarChart(
            title = "Tickets by status",
            categories = months,
            series = series,
            description = "First four months",
            categoryTitle = "Month",
            orientation = MonarchBarChartOrientation.entries[orientation],
            offset = MonarchStackedBarChartOffset.entries[offset],
            showValueLabels = true,
            footer = if (opened.isEmpty()) "Tap a month, then Details" else "Opened $opened",
            onSelect = { opened = months[it] },
        )
    }

    @Composable
    private fun StackedChoice(names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        MonarchSegmentedControl {
            names.forEachIndexed { index, name ->
                Segment(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    label = { Text(name) },
                )
            }
        }
    }
}
