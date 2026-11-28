package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.barchart.MonarchBarChart
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportButton
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportFormat
import dev.monarchui.components.datadisplay.downloadreportbutton.MonarchDownloadReportResult
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodPreset
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelector
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodSelectorVariant
import dev.monarchui.components.datadisplay.periodselector.MonarchPeriodValue
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.statcard.MonarchStatCard
import dev.monarchui.components.datagrid.datatable.MonarchDataTable
import dev.monarchui.components.datagrid.datatable.MonarchDataTableColumn
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.errorstate.MonarchErrorStateKind
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingState
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingStateVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

private class BrandRow(val brand: String, val orders: Int, val revenue: Double)

private val Rows = listOf(
    BrandRow("Northwind", 412, 48_200.0),
    BrandRow("Contoso", 318, 39_100.0),
    BrandRow("Fabrikam", 96, 12_800.0),
)

private val Columns = listOf(
    MonarchDataTableColumn<BrandRow>("Brand") { it.brand },
    MonarchDataTableColumn<BrandRow>("Orders", numeric = true, comparator = compareBy { it.orders }) {
        it.orders.toString()
    },
    MonarchDataTableColumn<BrandRow>("Revenue", numeric = true, comparator = compareBy { it.revenue }) {
        "€" + it.revenue.toInt()
    },
)

private enum class ReportState { Ready, Refreshing, Loading, Forbidden }

private const val ScreenHeightSteps = 6

/** The catalogue page of ReportViewer: one report on its own screen, as a recipe of existing components. */
object ReportViewerSection : CataloguePage {
    override val title = "ReportViewer"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableStateOf(ReportState.Ready) }
        var period by remember { mutableStateOf(MonarchPeriodValue(MonarchPeriodPreset.Last30Days)) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("State")
        ReportStatePicker(state) { state = it }
        CatalogueCaption("The report screen")
        Column(Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * ScreenHeightSteps)) {
            TopAppBar(
                title = { Text("Revenue by brand") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        MonarchIcon(SampleIcons.ArrowBack, contentDescription = "Back to reports")
                    }
                },
                actions = {
                    MonarchDownloadReportButton(
                        onDownload = { MonarchDownloadReportResult.Queued("We will email you the file.") },
                        formats = listOf(MonarchDownloadReportFormat.Csv),
                    )
                },
            )
            if (state == ReportState.Refreshing) {
                MonarchProgress(modifier = Modifier.fillMaxWidth(), label = "Updating report")
            }
            ReportHeader(period) { period = it }
            when (state) {
                ReportState.Loading -> MonarchLoadingState(
                    label = "Loading report",
                    variant = MonarchLoadingStateVariant.Rows,
                )
                ReportState.Forbidden -> MonarchErrorState(
                    title = "You cannot open this report",
                    kind = MonarchErrorStateKind.Forbidden,
                )
                else -> ReportBody(tab) { tab = it }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportStatePicker(state: ReportState, onState: (ReportState) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
        ReportState.entries.forEach {
            MonarchButton(
                text = it.name,
                onClick = { onState(it) },
                variant = if (it == state) MonarchButtonVariant.Primary else MonarchButtonVariant.Ghost,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportHeader(period: MonarchPeriodValue, onPeriod: (MonarchPeriodValue) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = MonarchTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchText(text = "Generated Nov 7, 2026, 10:00 AM · All brands", style = MonarchTextStyle.Caption)
            MonarchBadge(text = "Contains pay figures", tone = MonarchTone.Warning)
        }
        MonarchPeriodSelector(value = period, onValueChange = onPeriod, variant = MonarchPeriodSelectorVariant.Select)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ReportBody(tab: Int, onTab: (Int) -> Unit) {
    SecondaryTabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { onTab(0) }, text = { Text("Summary") })
        Tab(selected = tab == 1, onClick = { onTab(1) }, text = { Text("Table") })
    }
    if (tab == 0) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
        ) {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                MonarchStatCard(
                    label = "Revenue",
                    value = Rows.sumOf { it.revenue },
                    modifier = Modifier.weight(1f),
                    format = MonarchMetricFormat.Currency("EUR"),
                )
                MonarchStatCard(
                    label = "Orders",
                    value = Rows.sumOf { it.orders }.toDouble(),
                    modifier = Modifier.weight(1f),
                )
            }
            MonarchBarChart(
                title = "Revenue by brand",
                categories = Rows.map { it.brand },
                series = listOf(MonarchBarChartSeries("revenue", "Revenue", Rows.map { it.revenue })),
                formatValue = { "€" + it.toInt() },
            )
            MonarchText(text = "Revenue excludes tax and refunds.", style = MonarchTextStyle.Caption)
        }
    } else {
        MonarchDataTable(
            caption = "Revenue by brand",
            rows = Rows,
            columns = Columns,
            rowKey = { it.brand },
            modifier = Modifier.weight(1f),
            footer = {
                MonarchText(
                    text = "Only the first rows are shown. Download the report for every row.",
                    modifier = Modifier.padding(MonarchTheme.spacing.space4),
                    style = MonarchTextStyle.Caption,
                )
            },
        )
    }
}
