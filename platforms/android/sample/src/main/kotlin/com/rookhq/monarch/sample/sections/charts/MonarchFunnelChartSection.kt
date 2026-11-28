package com.rookhq.monarch.sample.sections.charts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChart
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartAlignment
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartStage
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of FunnelChart. */
object MonarchFunnelChartSection : CataloguePage {
    override val title = "FunnelChart"

    private val signup = listOf(
        MonarchFunnelChartStage("Visited pricing", 4800.0),
        MonarchFunnelChartStage("Started sign-up", 1900.0),
        MonarchFunnelChartStage("Verified email", 1200.0),
        MonarchFunnelChartStage("Paid", 310.0),
    )

    private val hiring = listOf(
        MonarchFunnelChartStage("Applied", 120.0),
        MonarchFunnelChartStage("Interviewed", 30.0),
        MonarchFunnelChartStage("Hired", 4.0),
    )

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
        MonarchFunnelChart(
            title = "Sign-up funnel",
            stages = signup,
            description = "Last 30 days",
            status = statuses[status],
            emptyMessage = "The funnel fills in after the first visits.",
            footer = "Source: product analytics",
            onRetry = { status = 0 },
        )
        CatalogueCaption("Start-aligned, selectable, without step lines")
        MonarchFunnelChart(
            title = "Hiring pipeline",
            stages = hiring,
            alignment = MonarchFunnelChartAlignment.Start,
            showSteps = false,
            onSelect = { selected = it.label },
        )
        if (selected.isNotEmpty()) MonarchText(text = "Selected: $selected")
    }
}
