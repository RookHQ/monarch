package com.rookhq.monarch.components.charts.stackedbarchart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.charts.barchart.MonarchBarChart
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartLayout
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartOrientation
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartReferenceLine
import com.rookhq.monarch.components.charts.barchart.MonarchBarChartSeries
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchChartPercentFormat

/**
 * Stacked bars that show how each category's total splits into parts, as values or as shares of 100%,
 * vertical or horizontal.
 *
 * A composition over `MonarchBarChart` (a Compose `Canvas` inside `MonarchChartFrame`), as the web's
 * StackedBarChart is its BarChart: stacking is on by default, and [offset] `Expand` replaces each value with its
 * share of the category's total so every bar is 100% long. Inspection, legend toggles, drill-in, the data table
 * and TalkBack behave as in `MonarchBarChart`. With the 100% offset the axis runs from 0% to 100%, values are
 * written as percentages in the current locale with at most one decimal, reference lines are not drawn and the
 * spoken summary gives each series' average and largest share.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param categories the categories in display order, already formatted by the app.
 * @param series the parts, each with one value per category. The first series sits on the baseline.
 * @param modifier the modifier of the frame.
 * @param description one line under the title.
 * @param categoryTitle the heading of the category column of the table. `null` uses "Category".
 * @param summary replaces the generated spoken summary.
 * @param orientation vertical columns or horizontal bars.
 * @param layout stacked (one bar of parts per category, the default) or grouped (the parts side by side).
 * @param offset `Normal` stacks the values; `Expand` stacks shares so every bar is 100% long. Only applies when
 * stacked.
 * @param status ready, loading, empty or error. Ready with no categories or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the
 * legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param highlightedIndex a category to emphasise; the bars of the others take the disabled colour.
 * @param referenceLines dashed lines at a value. Not drawn with the 100% offset.
 * @param valueRange fixes the value axis. With the 100% offset the axis is always 0 to 1 (0% to 100%).
 * @param showValueLabels writes each stack's total at its end (each bar's value when grouped). With the 100%
 * offset the total is 100%.
 * @param legendToggle whether legend entries show and hide their series.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param height the plot height. `null` is 240dp for columns; 32dp per category plus one, at least 120dp, for
 * bars.
 * @param labels replacements for the built-in strings: the bar chart's and the share summary sentence.
 * @param formatValue writes values with the normal offset. Ignored with the 100% offset, which writes
 * percentages.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in: called with the category index, also with the 100% offset.
 */
@Composable
public fun MonarchStackedBarChart(
    title: String,
    categories: List<String>,
    series: List<MonarchBarChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    categoryTitle: String? = null,
    summary: String? = null,
    orientation: MonarchBarChartOrientation = MonarchBarChartOrientation.Vertical,
    layout: MonarchBarChartLayout = MonarchBarChartLayout.Stacked,
    offset: MonarchStackedBarChartOffset = MonarchStackedBarChartOffset.Normal,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenSeries: Set<String>? = null,
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    highlightedIndex: Int? = null,
    referenceLines: List<MonarchBarChartReferenceLine> = emptyList(),
    valueRange: ClosedFloatingPointRange<Double>? = null,
    showValueLabels: Boolean = false,
    legendToggle: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchStackedBarChartLabels = MonarchStackedBarChartLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    var ownHidden by rememberSaveable { mutableStateOf(listOf<String>()) }
    val hidden = hiddenSeries ?: ownHidden.toSet()
    val changeHidden: (Set<String>) -> Unit = { next ->
        if (hiddenSeries == null) ownHidden = next.toList()
        onHiddenSeriesChange?.invoke(next)
    }
    val expand = layout == MonarchBarChartLayout.Stacked && offset == MonarchStackedBarChartOffset.Expand
    val percent = rememberMonarchChartPercentFormat(1)
    val drawn = if (expand) {
        remember(series, categories.size) { MonarchStackedBarChartShares.shares(series, categories.size) }
    } else {
        series
    }
    val spoken = summary ?: if (expand) {
        val words = MonarchStackedBarChartWords(
            count = pluralStringResource(R.plurals.monarch_barchart_categories, categories.size, categories.size),
            range = stringResource(R.string.monarch_barchart_range),
            shareSeries = monarchString(labels.shareSeries, R.string.monarch_stackedbarchart_share_series),
            categoryValues = stringResource(R.string.monarch_barchart_category_values),
            noData = monarchString(labels.bar.noData, R.string.monarch_barchart_no_data),
        )
        val visible = drawn.indices.filter { drawn[it].key !in hidden }
        MonarchStackedBarChartShares.summary(categories, drawn, visible, percent, words)
    } else {
        null
    }
    MonarchBarChart(
        title = title,
        categories = categories,
        series = drawn,
        modifier = modifier,
        description = description,
        categoryTitle = categoryTitle,
        summary = spoken,
        orientation = orientation,
        layout = layout,
        status = status,
        hiddenSeries = hidden,
        onHiddenSeriesChange = changeHidden,
        highlightedIndex = highlightedIndex,
        referenceLines = if (expand) emptyList() else referenceLines,
        valueRange = if (expand) MonarchStackedBarChartTokens.ShareRange else valueRange,
        showValueLabels = showValueLabels,
        legendToggle = legendToggle,
        tableToggle = tableToggle,
        emptyMessage = emptyMessage,
        footer = footer,
        height = height,
        labels = labels.bar,
        formatValue = if (expand) percent else formatValue,
        onRetry = onRetry,
        onSelect = onSelect,
    )
}
