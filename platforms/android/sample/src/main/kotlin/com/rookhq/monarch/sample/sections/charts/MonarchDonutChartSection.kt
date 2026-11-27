package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChart
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartLegendContent
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSegment
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSort
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of DonutChart. */
object MonarchDonutChartSection : CataloguePage {
    override val title = "DonutChart"

    private val assets = listOf(
        MonarchDonutChartSegment("laptop", "Laptops", 412.0),
        MonarchDonutChartSegment("phone", "Phones", 268.0),
        MonarchDonutChartSegment("monitor", "Monitors", 131.0),
        MonarchDonutChartSegment("tablet", "Tablets", 54.0),
        MonarchDonutChartSegment("dock", "Docks", 21.0),
        MonarchDonutChartSegment("headset", "Headsets", 12.0),
        MonarchDonutChartSegment("printer", "Printers", 3.0),
    )
    private val storage = listOf(
        MonarchDonutChartSegment("files", "Files", 820.0),
        MonarchDonutChartSegment("backups", "Backups", 540.0),
        MonarchDonutChartSegment("free", "Free", 640.0, MonarchChartColor.Neutral),
    )

    @Composable
    override fun Content() {
        var content by rememberSaveable { mutableIntStateOf(0) }
        var status by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Legend content")
        Choice(MonarchDonutChartLegendContent.entries.map { it.name }, content) { content = it }
        CatalogueCaption("Status")
        Choice(MonarchChartFrameStatus.entries.map { it.name }, status) { status = it }
        MonarchDonutChart(
            title = "Assets by type",
            segments = assets,
            description = "All active assets",
            centerLabel = "assets",
            legendContent = MonarchDonutChartLegendContent.entries[content],
            status = MonarchChartFrameStatus.entries[status],
            onRetry = { status = 0 },
        )
        CatalogueCaption("Given order, a custom center and a neutral slice")
        MonarchDonutChart(
            title = "Storage",
            segments = storage,
            centerValue = "68%",
            centerLabel = "used",
            sort = MonarchDonutChartSort.None,
            legendContent = MonarchDonutChartLegendContent.Percent,
            formatValue = { "${it.toInt()} GB" },
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
