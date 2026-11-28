package dev.monarchui.components.charts.barchart

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
import dev.monarchui.R
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendItem
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendMarker
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme

/**
 * A bar or column chart of one or more series per category, grouped or stacked, vertical or horizontal,
 * inside a `MonarchChartFrame`.
 *
 * Built by Monarch with a Compose `Canvas`: Material 3 has no chart and Monarch adds no chart library. The
 * frame gives the title, the legend (with two or more series; its entries show and hide series), the
 * loading, empty and error states and "View as table". Bars take the theme's data colours in series order.
 *
 * For TalkBack the plot is one element named by [title] whose state is a summary (the categories, each
 * series' total and highest category, the reference lines), followed by one element per category in order,
 * read as the category and each series' value; with [onSelect] each has a "Details" action. A tap, or a drag
 * along columns, inspects a category: its band is shaded and a row under the plot shows its values, announced
 * politely, with Details and Clear. The chart is drawn from the start edge, so it mirrors in right to left.
 * It does not animate.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param categories the categories in display order, already formatted by the app.
 * @param series the measures, each with one value per category.
 * @param modifier the modifier of the frame.
 * @param description one line under the title.
 * @param categoryTitle the heading of the category column of the table. `null` uses "Category".
 * @param summary replaces the generated spoken summary.
 * @param orientation vertical columns or horizontal bars.
 * @param layout with several series, grouped side by side or stacked.
 * @param status ready, loading, empty or error. Ready with no categories or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the
 * legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param highlightedIndex a category to emphasise; the bars of the others take the disabled colour and
 * TalkBack reads "Highlighted" on it.
 * @param referenceLines dashed lines across the bars at a value, with an optional label.
 * @param valueRange fixes the value axis. `null` fits the values, zero and the reference lines.
 * @param showValueLabels writes each bar's value at its end; a stack shows its total once.
 * @param legendToggle whether legend entries show and hide their series.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param height the plot height. `null` is 240dp for columns, and 32dp per category plus one, at least
 * 120dp, for horizontal bars.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatValue writes values on the axis, in value labels, the inspection row, TalkBack, the table and
 * the summary. `null` writes numbers in the current locale, compact on the axis and on the bars.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in: called with the category index from Details in the inspection row or from the
 * TalkBack action of that category.
 */
@Composable
public fun MonarchBarChart(
    title: String,
    categories: List<String>,
    series: List<MonarchBarChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    categoryTitle: String? = null,
    summary: String? = null,
    orientation: MonarchBarChartOrientation = MonarchBarChartOrientation.Vertical,
    layout: MonarchBarChartLayout = MonarchBarChartLayout.Grouped,
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
    labels: MonarchBarChartLabels = MonarchBarChartLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    var ownHidden by rememberSaveable { mutableStateOf(listOf<String>()) }
    var inspected by rememberSaveable { mutableStateOf<Int?>(null) }
    val hidden = hiddenSeries ?: ownHidden.toSet()
    val changeHidden: (Set<String>) -> Unit = { next ->
        if (hiddenSeries == null) ownHidden = next.toList()
        onHiddenSeriesChange?.invoke(next)
    }
    val format = rememberMonarchChartValueFormat(formatValue)
    val tickFormat = rememberMonarchChartValueFormat(formatValue, compact = true)
    val stacked = layout == MonarchBarChartLayout.Stacked
    val visible = series.indices.filter { series[it].key !in hidden }
    val stacks = remember(series, visible, categories.size, stacked) {
        MonarchBarChartGeometry.stacks(series, visible, categories.size, stacked)
    }
    val words = barChartWords(labels)
    val total = words.total
    val table = MonarchBarChartText.table(
        categories = categories,
        series = series,
        stacked = stacked,
        categoryTitle = categoryTitle ?: monarchString(labels.category, R.string.monarch_barchart_category),
        total = total,
        format = format,
    )
    // A legend that cannot toggle lists only the series that are drawn.
    val legend = if (series.size > 1) {
        series.mapIndexedNotNull { index, item ->
            MonarchChartLegendItem(item.key, item.label, MonarchBarChartTokens.slot(item.color, index))
                .takeIf { legendToggle || index in visible }
        }
    } else {
        emptyList()
    }
    val resolved = if (status == MonarchChartFrameStatus.Ready && (categories.isEmpty() || series.isEmpty())) {
        MonarchChartFrameStatus.Empty
    } else {
        status
    }
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = resolved,
        table = table,
        legend = legend,
        hiddenSeries = hidden,
        onHiddenSeriesChange = changeHidden.takeIf { legendToggle && legend.isNotEmpty() },
        legendMarker = MonarchChartLegendMarker.Square,
        emptyMessage = emptyMessage,
        footer = footer,
        height = height ?: MonarchBarChartTokens.height(orientation, categories.size),
        tableToggle = tableToggle,
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        val count = pluralStringResource(R.plurals.monarch_barchart_categories, categories.size, categories.size)
        val details = monarchString(labels.details, R.string.monarch_barchart_details)
        val detailsPattern = stringResource(R.string.monarch_barchart_details_action)
        val text = MonarchBarChartPlotText(
            title = title,
            summary = summary ?: MonarchBarChartText.summary(
                categories, series, visible, referenceLines, count, format, words,
            ),
            describe = { index ->
                MonarchBarChartText.describe(index, categories, series, visible, stacks, stacked, format, words)
            },
            detailsAction = { detailsPattern.format(details, categories[it]) },
            details = details,
            clear = monarchString(labels.clearSelection, R.string.monarch_barchart_clear),
            highlighted = monarchString(labels.highlighted, R.string.monarch_barchart_highlighted),
        )
        val colors = MonarchTheme.colors
        val paint = MonarchBarChartPaint(
            style = MonarchTheme.typography.caption,
            colors = MonarchBarChartTokens.colors(colors),
            seriesColors = series.mapIndexed { index, item ->
                MonarchBarChartTokens.barColor(MonarchBarChartTokens.slot(item.color, index), dimmed = false, colors)
            },
            dimmedColor = MonarchBarChartTokens.barColor(MonarchChartColor.Neutral, dimmed = true, colors),
            tickFormat = tickFormat,
        )
        val horizontal = orientation == MonarchBarChartOrientation.Horizontal
        val model = remember(
            categories, stacks, horizontal, highlightedIndex, referenceLines, valueRange, showValueLabels,
        ) {
            MonarchBarChartModel(
                categories = categories,
                series = series,
                visible = visible,
                stacks = stacks,
                horizontal = horizontal,
                stacked = stacked,
                highlightedIndex = highlightedIndex,
                references = referenceLines,
                valueRange = valueRange,
                showValueLabels = showValueLabels,
            )
        }
        MonarchBarChartPlotArea(
            model = model,
            paint = paint,
            text = text,
            inspected = inspected?.takeIf { it in categories.indices },
            onInspect = { inspected = it },
            onSelect = onSelect,
        )
    }
}

/** The built-in words of the chart, with the app's replacements. */
@Composable
private fun barChartWords(labels: MonarchBarChartLabels): MonarchBarChartWords = MonarchBarChartWords(
    total = monarchString(labels.total, R.string.monarch_barchart_total),
    noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
    noData = monarchString(labels.noData, R.string.monarch_barchart_no_data),
    value = stringResource(R.string.monarch_barchart_value),
    categoryValues = stringResource(R.string.monarch_barchart_category_values),
    range = stringResource(R.string.monarch_barchart_range),
    seriesSummary = stringResource(R.string.monarch_barchart_series_summary),
)
