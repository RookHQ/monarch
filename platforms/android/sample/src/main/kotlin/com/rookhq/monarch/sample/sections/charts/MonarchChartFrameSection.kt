package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrame
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTableSeries
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendItem
import com.rookhq.monarch.components.charts.sparkline.MonarchSparkline
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of ChartFrame. */
object MonarchChartFrameSection : CataloguePage {
    override val title = "ChartFrame"

    private val values = listOf(32.0, 41.0, 38.0, 52.0)
    private val table = MonarchChartTable(
        categoryTitle = "Week",
        categories = listOf("W1", "W2", "W3", "W4"),
        series = listOf(MonarchChartTableSeries("Signups", values)),
    )

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableIntStateOf(0) }
        var hidden by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("Status")
        val statuses = MonarchChartFrameStatus.entries
        MonarchSegmentedControl {
            statuses.forEachIndexed { index, option ->
                Segment(
                    selected = index == status,
                    onClick = { status = index },
                    label = { Text(option.name) },
                )
            }
        }
        MonarchChartFrame(
            title = "Signups",
            description = "Last four weeks",
            status = statuses[status],
            table = table,
            legend = listOf(MonarchChartLegendItem("signups", "Signups")),
            hiddenSeries = hidden.toSet(),
            onHiddenSeriesChange = { hidden = it.toList() },
            emptyMessage = "Signups appear here after the first week.",
            footer = "Source: billing",
            height = 120.dp,
            onRetry = { status = 0 },
        ) { MonarchSparkline(values = values) }
    }
}
