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
import dev.monarchui.components.charts.heatmap.MonarchHeatmap
import dev.monarchui.components.charts.heatmap.MonarchHeatmapAxisItem
import dev.monarchui.components.charts.heatmap.MonarchHeatmapCell
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Heatmap. */
object MonarchHeatmapSection : CataloguePage {
    override val title = "Heatmap"

    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri").map { MonarchHeatmapAxisItem(it) }
    private val hours = (8..17).map { MonarchHeatmapAxisItem("$it:00") }
    private val counts = listOf(
        listOf(2, 7, 12, 9, 4, 6, 11, 8, 3, 1),
        listOf(1, 5, 9, 10, 6, 5, 8, 7, 4, 2),
        listOf(3, 8, 10, 14, 7, 6, 9, 9, 5, 2),
        listOf(2, 6, 8, 7, 5, 4, 7, 6, 3, 1),
        listOf(1, 4, 6, 5, 3, 2, 2, 1),
    )
    private val launch = MonarchHeatmapCell(row = "Wed", column = "12:00", value = 14.0, note = "Launch")

    // The later of two cells at one position wins, so the launch replaces Wednesday at noon.
    private val tickets = days.flatMapIndexed { d, day ->
        counts[d].mapIndexed { h, count -> MonarchHeatmapCell(day.key, hours[h].key, count.toDouble()) }
    } + launch

    private val teams = listOf("Design", "Platform", "Growth").map { MonarchHeatmapAxisItem(it) }
    private val weeks = listOf("W1", "W2", "W3", "W4").map { MonarchHeatmapAxisItem(it) }
    private val hoursLogged = listOf(
        MonarchHeatmapCell("Design", "W1", 32.0),
        MonarchHeatmapCell("Design", "W2", 38.0),
        MonarchHeatmapCell("Design", "W3", 12.0),
        MonarchHeatmapCell("Platform", "W1", 41.0, valueLabel = "41h"),
        MonarchHeatmapCell("Platform", "W3", 26.0),
        MonarchHeatmapCell("Growth", "W2", 8.0),
        MonarchHeatmapCell("Growth", "W4", 0.0),
    )

    private val thresholds = listOf(10.0, 25.0, 40.0)

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableIntStateOf(0) }
        var selected by rememberSaveable { mutableStateOf("") }
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
        MonarchHeatmap(
            title = "Support tickets",
            rows = days,
            columns = hours,
            cells = tickets,
            description = "This week, by hour",
            status = statuses[status],
            emptyMessage = "Tickets appear here as they arrive.",
            footer = "Source: help desk",
            onRetry = { status = 0 },
        )
        CatalogueCaption("Fixed thresholds, values in cells, selectable")
        MonarchHeatmap(
            title = "Hours logged",
            rows = teams,
            columns = weeks,
            cells = hoursLogged,
            color = MonarchChartColor.Data2,
            thresholds = thresholds,
            showValues = true,
            onSelect = { selected = "${it.row}, ${it.column}" },
        )
        if (selected.isNotEmpty()) MonarchText(text = "Selected: $selected")
    }
}
