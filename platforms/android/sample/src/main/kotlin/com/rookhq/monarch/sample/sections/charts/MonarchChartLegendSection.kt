package com.rookhq.monarch.sample.sections.charts

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegend
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendItem
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendMarker
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ChartLegend. */
object MonarchChartLegendSection : CataloguePage {
    override val title = "ChartLegend"

    private val items = listOf(
        MonarchChartLegendItem("revenue", "Revenue", value = "$48,200"),
        MonarchChartLegendItem("costs", "Costs", value = "$31,050"),
        MonarchChartLegendItem("target", "Target", color = MonarchChartColor.Neutral, dashed = true),
    )

    @Composable
    override fun Content() {
        var hidden by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("Plain, line markers")
        MonarchChartLegend(items = items)
        CatalogueCaption("Square markers, vertical")
        MonarchChartLegend(items = items, marker = MonarchChartLegendMarker.Square, orientation = Orientation.Vertical)
        CatalogueCaption("Toggles")
        MonarchChartLegend(
            items = items,
            hiddenSeries = hidden.toSet(),
            onHiddenSeriesChange = { hidden = it.toList() },
            marker = MonarchChartLegendMarker.Dot,
        )
        MonarchText(
            text = if (hidden.isEmpty()) "Every series is shown." else "Hidden: ${hidden.joinToString()}",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
