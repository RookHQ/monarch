package dev.monarchui.components.charts.linechart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendItem
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendMarker
import dev.monarchui.internal.MonarchUtcDates
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A line or area chart of one or more series over ordered categories, inside a `MonarchChartFrame`.
 *
 * Built by Monarch with a Compose `Canvas`: Material 3 has no chart and Monarch adds no chart library. The axis,
 * ticks and labels come from the same chart core as `MonarchBarChart`. The frame gives the title, the legend
 * (with two or more series; its entries show and hide series), the loading, empty and error states and "View as
 * table". Lines take the theme's data colours in series order; a gap breaks the line and a lone value is a dot.
 *
 * For TalkBack the plot is one element named by [title] whose state is a summary (the points, each series' low,
 * high and latest value, the reference lines), followed by one element per x value in order, read as the x value
 * and each series' value; with [onSelect] each has a "Details" action. A tap, or a drag across the plot, inspects
 * the nearest x value: a crosshair and larger dots mark it and a row under the plot shows its values, announced
 * politely, with Details and Clear. The chart is drawn from the start edge, so it mirrors in right to left. It
 * does not animate.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param categories the x values in order, already formatted by the app. They are spaced evenly.
 * @param series the measures, each with one value per category.
 * @param modifier the modifier of the frame.
 * @param description one line under the title: the period, the unit or what the chart answers.
 * @param xTitle the heading of the x column of the table. `null` uses "Category".
 * @param summary replaces the generated spoken summary.
 * @param variant a line, or a line with a soft fill down to the baseline.
 * @param curve straight segments, or a smooth curve that never overshoots.
 * @param status ready, loading, empty or error. Ready with no categories or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param activeIndex the inspected x value, used when [onActiveIndexChange] is set. Give several charts on the same
 * x values one state to share the crosshair.
 * @param onActiveIndexChange makes the inspection controlled: called with the index the user inspects, or `null`
 * when it is cleared. `null` lets the chart keep its own inspection.
 * @param referenceLines dashed lines across the plot at a value, with an optional label.
 * @param valueRange fixes the value axis, such as 0.0..100.0 for percentages. `null` fits the values.
 * @param includeZero whether the value axis keeps zero. Turn it off for values far from zero.
 * @param showPoints a dot on every value. Lone values between gaps always get one.
 * @param legendToggle whether legend entries show and hide their series.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart: the source, the last update.
 * @param height the plot height, kept in every status. `null` is 240dp.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatValue writes values in the inspection row, TalkBack, the table, the summary and on the axis. `null`
 * writes numbers in the current locale, compact on the axis.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in: called with the x index from Details in the inspection row or from the TalkBack action
 * of that x value.
 */
@Composable
public fun MonarchLineChart(
    title: String,
    categories: List<String>,
    series: List<MonarchLineChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    xTitle: String? = null,
    summary: String? = null,
    variant: MonarchLineChartVariant = MonarchLineChartVariant.Line,
    curve: MonarchLineChartCurve = MonarchLineChartCurve.Linear,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenSeries: Set<String>? = null,
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    referenceLines: List<MonarchLineChartReferenceLine> = emptyList(),
    valueRange: ClosedFloatingPointRange<Double>? = null,
    includeZero: Boolean = true,
    showPoints: Boolean = false,
    legendToggle: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchLineChartLabels = MonarchLineChartLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    LineChartBody(
        title = title,
        xLabels = categories,
        fractions = remember(categories.size) { MonarchLineChartGeometry.evenFractions(categories.size) },
        xTitle = xTitle ?: monarchString(labels.category, R.string.monarch_linechart_category),
        options = LineChartOptions(
            series, modifier, description, summary, variant, curve, status, hiddenSeries, onHiddenSeriesChange,
            activeIndex, onActiveIndexChange, referenceLines, valueRange, includeZero, showPoints, legendToggle,
            tableToggle, emptyMessage, footer, height, labels, formatValue, onRetry, onSelect,
        ),
    )
}

/**
 * A line or area chart of one or more series over dates, inside a `MonarchChartFrame`. Points are spaced by time,
 * so uneven dates show. Everything else is as in the overload with categories.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param dates the x values in order, as epoch milliseconds (UTC), as in the Material 3 `DatePickerState`.
 * @param series the measures, each with one value per date.
 * @param modifier the modifier of the frame.
 * @param description one line under the title: the period, the unit or what the chart answers.
 * @param xTitle the heading of the x column of the table. `null` uses "Date".
 * @param summary replaces the generated spoken summary.
 * @param variant a line, or a line with a soft fill down to the baseline.
 * @param curve straight segments, or a smooth curve that never overshoots.
 * @param status ready, loading, empty or error. Ready with no dates or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param activeIndex the inspected date, used when [onActiveIndexChange] is set.
 * @param onActiveIndexChange makes the inspection controlled: called with the index the user inspects, or `null`
 * when it is cleared.
 * @param referenceLines dashed lines across the plot at a value, with an optional label.
 * @param valueRange fixes the value axis. `null` fits the values.
 * @param includeZero whether the value axis keeps zero.
 * @param showPoints a dot on every value. Lone values between gaps always get one.
 * @param legendToggle whether legend entries show and hide their series.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param height the plot height, kept in every status. `null` is 240dp.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatDate writes a date on the axis, in the inspection row, TalkBack, the table and the summary. `null`
 * writes the abbreviated month and the day in the current locale, in UTC.
 * @param formatValue writes values. `null` writes numbers in the current locale, compact on the axis.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in: called with the date's index from Details or from its TalkBack action.
 */
@JvmName("MonarchLineChartOfDates")
@Composable
public fun MonarchLineChart(
    title: String,
    dates: List<Long>,
    series: List<MonarchLineChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    xTitle: String? = null,
    summary: String? = null,
    variant: MonarchLineChartVariant = MonarchLineChartVariant.Line,
    curve: MonarchLineChartCurve = MonarchLineChartCurve.Linear,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenSeries: Set<String>? = null,
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    referenceLines: List<MonarchLineChartReferenceLine> = emptyList(),
    valueRange: ClosedFloatingPointRange<Double>? = null,
    includeZero: Boolean = true,
    showPoints: Boolean = false,
    legendToggle: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchLineChartLabels = MonarchLineChartLabels(),
    formatDate: ((Long) -> String)? = null,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val writeDate = formatDate ?: remember(locale) { monthDay(locale) }
    LineChartBody(
        title = title,
        xLabels = remember(dates, writeDate) { dates.map(writeDate) },
        fractions = remember(dates) { MonarchLineChartGeometry.timeFractions(dates) },
        xTitle = xTitle ?: monarchString(labels.date, R.string.monarch_linechart_date),
        options = LineChartOptions(
            series, modifier, description, summary, variant, curve, status, hiddenSeries, onHiddenSeriesChange,
            activeIndex, onActiveIndexChange, referenceLines, valueRange, includeZero, showPoints, legendToggle,
            tableToggle, emptyMessage, footer, height, labels, formatValue, onRetry, onSelect,
        ),
    )
}

/** The abbreviated month and the day, such as "Mar 4", in [locale] and UTC. */
private fun monthDay(locale: Locale): (Long) -> String = MonarchUtcDates.monthDay(locale)

/** The parameters both overloads share, passed on unchanged. */
@Suppress("LongParameterList")
private class LineChartOptions(
    val series: List<MonarchLineChartSeries>,
    val modifier: Modifier,
    val description: String?,
    val summary: String?,
    val variant: MonarchLineChartVariant,
    val curve: MonarchLineChartCurve,
    val status: MonarchChartFrameStatus,
    val hiddenSeries: Set<String>?,
    val onHiddenSeriesChange: ((Set<String>) -> Unit)?,
    val activeIndex: Int?,
    val onActiveIndexChange: ((Int?) -> Unit)?,
    val referenceLines: List<MonarchLineChartReferenceLine>,
    val valueRange: ClosedFloatingPointRange<Double>?,
    val includeZero: Boolean,
    val showPoints: Boolean,
    val legendToggle: Boolean,
    val tableToggle: Boolean,
    val emptyMessage: String?,
    val footer: String?,
    val height: Dp?,
    val labels: MonarchLineChartLabels,
    val formatValue: ((Double) -> String)?,
    val onRetry: (() -> Unit)?,
    val onSelect: ((Int) -> Unit)?,
)

@Composable
private fun LineChartBody(
    title: String,
    xLabels: List<String>,
    fractions: List<Float>,
    xTitle: String,
    options: LineChartOptions,
) {
    val series = options.series
    var ownHidden by rememberSaveable { mutableStateOf(listOf<String>()) }
    var ownActive by rememberSaveable { mutableStateOf<Int?>(null) }
    val hidden = options.hiddenSeries ?: ownHidden.toSet()
    val changeHidden: (Set<String>) -> Unit = { next ->
        if (options.hiddenSeries == null) ownHidden = next.toList()
        options.onHiddenSeriesChange?.invoke(next)
    }
    val controlled = options.onActiveIndexChange
    val active = (if (controlled != null) options.activeIndex else ownActive)?.takeIf { it in xLabels.indices }
    val changeActive: (Int?) -> Unit = { next -> if (controlled != null) controlled(next) else ownActive = next }
    val format = rememberValueFormat(options.formatValue, compact = false)
    val tickFormat = rememberValueFormat(options.formatValue, compact = true)
    val visible = series.indices.filter { series[it].key !in hidden }
    val labels = options.labels
    val words = lineChartWords(labels)
    val legend = legendItems(series, visible, options.legendToggle)
    val resolved = resolvedStatus(options.status, xLabels.isEmpty() || series.isEmpty())
    MonarchChartFrame(
        title = title,
        modifier = options.modifier,
        description = options.description,
        status = resolved,
        table = MonarchLineChartText.table(xLabels, series, xTitle, format),
        legend = legend,
        hiddenSeries = hidden,
        onHiddenSeriesChange = changeHidden.takeIf { options.legendToggle && legend.isNotEmpty() },
        legendMarker = MonarchChartLegendMarker.Line,
        emptyMessage = options.emptyMessage,
        footer = options.footer,
        height = options.height ?: MonarchLineChartTokens.DefaultHeight,
        tableToggle = options.tableToggle,
        onRetry = options.onRetry,
        labels = labels.frame,
    ) {
        PlotContent(title, xLabels, fractions, options, visible, format, tickFormat, words, active, changeActive)
    }
}

/** The plot inside the frame: text, paint and model resolved for the plot area. */
@Composable
@Suppress("LongParameterList")
private fun PlotContent(
    title: String,
    xLabels: List<String>,
    fractions: List<Float>,
    options: LineChartOptions,
    visible: List<Int>,
    format: (Double) -> String,
    tickFormat: (Double) -> String,
    words: MonarchLineChartWords,
    active: Int?,
    changeActive: (Int?) -> Unit,
) {
    val series = options.series
    val labels = options.labels
    val count = pluralStringResource(R.plurals.monarch_linechart_points, xLabels.size, xLabels.size)
    val details = monarchString(labels.details, R.string.monarch_linechart_details)
    val detailsPattern = stringResource(R.string.monarch_linechart_details_action)
    val text = MonarchLineChartPlotText(
        title = title,
        summary = options.summary ?: MonarchLineChartText.summary(
            xLabels, series, visible, options.referenceLines, count, format, words,
        ),
        describe = { MonarchLineChartText.describe(it, xLabels, series, visible, format, words) },
        detailsAction = { detailsPattern.format(details, xLabels[it]) },
        details = details,
        clear = monarchString(labels.clearSelection, R.string.monarch_linechart_clear),
    )
    val colors = MonarchTheme.colors
    val slots = series.mapIndexed { index, item -> MonarchLineChartTokens.slot(item.color, index) }
    val paint = MonarchLineChartPaint(
        style = MonarchTheme.typography.caption,
        colors = MonarchLineChartTokens.colors(colors),
        lineColors = slots.map { MonarchLineChartTokens.lineColor(it, colors) },
        areaColors = slots.map { MonarchLineChartTokens.areaColor(it, colors) },
        tickFormat = tickFormat,
    )
    val model = remember(
        xLabels, fractions, series, visible, options.variant, options.curve, options.referenceLines,
        options.valueRange, options.includeZero, options.showPoints,
    ) {
        MonarchLineChartModel(
            xLabels = xLabels,
            fractions = fractions,
            series = series,
            visible = visible,
            variant = options.variant,
            curve = options.curve,
            references = options.referenceLines,
            valueRange = options.valueRange,
            includeZero = options.includeZero,
            showPoints = options.showPoints,
        )
    }
    MonarchLineChartPlotArea(
        model = model,
        paint = paint,
        text = text,
        active = active,
        onActiveChange = changeActive,
        onSelect = options.onSelect,
    )
}

/** [formatValue], or numbers in the current locale: compact (1.2K) for the axis when [compact]. */
@Composable
private fun rememberValueFormat(formatValue: ((Double) -> String)?, compact: Boolean): (Double) -> String =
    rememberMonarchChartValueFormat(formatValue, compact)

/** The frame status: Empty for a ready chart without data. */
private fun resolvedStatus(status: MonarchChartFrameStatus, noData: Boolean): MonarchChartFrameStatus =
    if (status == MonarchChartFrameStatus.Ready && noData) MonarchChartFrameStatus.Empty else status

/** The legend entries: every series with two or more, or only the drawn ones when the legend cannot toggle. */
private fun legendItems(
    series: List<MonarchLineChartSeries>,
    visible: List<Int>,
    toggle: Boolean,
): List<MonarchChartLegendItem> {
    if (series.size < 2) return emptyList()
    return series.mapIndexedNotNull { index, item ->
        MonarchChartLegendItem(item.key, item.label, MonarchLineChartTokens.slot(item.color, index), item.dashed)
            .takeIf { toggle || index in visible }
    }
}

/** The built-in words of the chart, with the app's replacements. */
@Composable
private fun lineChartWords(labels: MonarchLineChartLabels): MonarchLineChartWords = MonarchLineChartWords(
    noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
    noData = monarchString(labels.noData, R.string.monarch_linechart_no_data),
    value = stringResource(R.string.monarch_linechart_value),
    pointValues = stringResource(R.string.monarch_linechart_point_values),
    range = stringResource(R.string.monarch_linechart_range),
    seriesSummary = stringResource(R.string.monarch_linechart_series_summary),
)
