package com.rookhq.monarch.sample.sections.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChart
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartPoint
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of BurndownChart. */
object MonarchBurndownChartSection : CataloguePage {
    override val title = "BurndownChart"

    private val sprint = listOf(
        MonarchBurndownChartPoint("Mon", remaining = 40.0, scope = 40.0),
        MonarchBurndownChartPoint("Tue", remaining = 38.0, scope = 40.0),
        MonarchBurndownChartPoint("Wed", remaining = 37.0, scope = 45.0),
        MonarchBurndownChartPoint("Thu", remaining = 31.0, scope = 43.0),
        MonarchBurndownChartPoint("Fri"),
        MonarchBurndownChartPoint("Mon"),
        MonarchBurndownChartPoint("Tue"),
        MonarchBurndownChartPoint("Wed"),
    )
    private val finished = listOf(30.0, 24.0, 18.0, 11.0, 3.0, 0.0).mapIndexed { i, left ->
        MonarchBurndownChartPoint("Day ${i + 1}", remaining = left)
    }

    @Composable
    override fun Content() {
        var showScope by rememberSaveable { mutableStateOf(true) }
        MonarchSwitchRow(checked = showScope, onCheckedChange = { showScope = it }, label = "Show scope")
        MonarchBurndownChart(
            title = "Sprint 14",
            points = sprint,
            showScope = showScope,
            formatValue = { "${it.toInt()} pts" },
        )
        CatalogueCaption("A finished sprint without scope")
        MonarchBurndownChart(title = "Sprint 13", points = finished)
    }
}
