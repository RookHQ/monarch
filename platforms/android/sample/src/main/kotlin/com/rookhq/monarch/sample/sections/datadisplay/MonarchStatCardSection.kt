package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.charts.sparkline.MonarchSparkline
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricChange
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricFormat
import com.rookhq.monarch.components.datadisplay.statcard.MonarchStatCard
import com.rookhq.monarch.components.datadisplay.statcard.MonarchStatCardSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private val RevenueTrend = listOf(31.0, 35.0, 38.0, 36.0, 43.0, 48.0)
private const val Revenue = 48_200.0
private const val PreviousRevenue = 43_000.0

/** The catalogue page of StatCard. */
object MonarchStatCardSection : CataloguePage {
    override val title = "StatCard"

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableIntStateOf(0) }
        var failed by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("A KPI with a change, help, a trend and a drill-down")
        MonarchStatCard(
            label = "Revenue",
            value = Revenue,
            format = MonarchMetricFormat.Currency("EUR"),
            change = MonarchMetricChange.between(Revenue, PreviousRevenue),
            caption = "vs last month",
            help = "Invoiced revenue in the period, before refunds and taxes.",
            sparkline = { MonarchSparkline(values = RevenueTrend) },
            onClick = { opened++ },
        )
        MonarchText(text = "Opened $opened times")
        CatalogueCaption("A row of small cards, wrapping on a phone")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            val small = Modifier.widthIn(min = MonarchTheme.spacing.space24)
            MonarchStatCard(label = "Seats", value = 38.0, size = MonarchStatCardSize.Sm, modifier = small)
            MonarchStatCard(
                label = "Churn",
                value = 0.031,
                format = MonarchMetricFormat.Percent,
                tone = MonarchTone.Danger,
                hint = "Above the 2% target",
                size = MonarchStatCardSize.Sm,
                modifier = small,
            )
            MonarchStatCard(label = "Open tickets", value = null, size = MonarchStatCardSize.Sm, modifier = small)
        }
        CatalogueCaption("Highlighted, loading and failed")
        MonarchStatCard(label = "Active users", value = 12_400.0, compact = true, highlighted = true)
        MonarchStatCard(label = "Uptime", value = null, loading = true)
        MonarchStatCard(
            label = "Signups",
            value = 1_204.0,
            error = if (failed) "The server did not answer." else null,
            onRetry = { failed = false },
        )
    }
}
