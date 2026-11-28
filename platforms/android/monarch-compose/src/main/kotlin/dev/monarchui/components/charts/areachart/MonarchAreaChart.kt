package dev.monarchui.components.charts.areachart

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
import dev.monarchui.components.charts.linechart.MonarchLineChartCurve
import dev.monarchui.components.charts.linechart.MonarchLineChartGeometry
import dev.monarchui.components.charts.linechart.MonarchLineChartModel
import dev.monarchui.components.charts.linechart.MonarchLineChartPaint
import dev.monarchui.components.charts.linechart.MonarchLineChartPlotArea
import dev.monarchui.components.charts.linechart.MonarchLineChartPlotText
import dev.monarchui.components.charts.linechart.MonarchLineChartReferenceLine
import dev.monarchui.components.charts.linechart.MonarchLineChartSeries
import dev.monarchui.components.charts.linechart.MonarchLineChartTokens
import dev.monarchui.components.charts.linechart.MonarchLineChartVariant
import dev.monarchui.components.charts.linechart.MonarchLineChartWords
import dev.monarchui.internal.MonarchUtcDates
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartPercentFormat
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * An area chart of volumes over ordered categories, inside a `MonarchChartFrame`: several series overlapping,
 * stacked so the top edge is the total, or stacked to 100% to show shares.
 *
 * Built by Monarch with a Compose `Canvas` on the line chart core: Material 3 has no chart and Monarch adds no chart
 * library. The series, reference lines and curve are the `MonarchLineChart` types; the axis, the touch inspection,
 * the TalkBack elements and the inspection row are the line chart's. Monarch computes the stack itself, so a gap
 * breaks only its own band and a negative value counts as zero in a stack, as on the web. Fills are opaque shades of
 * the series colours (Monarch never fills with transparency); every top edge is drawn as a line over the fills, so
 * an overlapping series is never lost.
 *
 * For TalkBack the plot is one element named by [title] whose state is a summary (the points, each series' low,
 * high and latest value, the total of a stack, the reference lines), followed by one element per x value, read as
 * the x value, each series' real value (with its share in [MonarchAreaChartStacking.Expand]) and the total. A tap
 * or a drag inspects the nearest x value: a crosshair, dots on the top edges and a row of its values announced
 * politely, with Details and Clear. The chart mirrors in right to left and does not animate.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param categories the x values in order, already formatted by the app. They are spaced evenly.
 * @param series the measures, bottom of the stack first, each with one value per category.
 * @param modifier the modifier of the frame.
 * @param description one line under the title: the period, the unit or what the chart answers.
 * @param xTitle the heading of the x column of the table. `null` uses "Category".
 * @param summary replaces the generated spoken summary.
 * @param stacking overlapping areas, a stack whose top edge is the total, or a stack of shares of 100%.
 * @param curve straight edges, or smooth edges that never overshoot.
 * @param status ready, loading, empty or error. Ready with no categories or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param activeIndex the inspected x value, used when [onActiveIndexChange] is set, to share a crosshair.
 * @param onActiveIndexChange makes the inspection controlled: called with the index the user inspects, or `null`
 * when it is cleared.
 * @param referenceLines dashed lines across the plot at a value. Not drawn in [MonarchAreaChartStacking.Expand],
 * whose axis is shares.
 * @param valueRange fixes the value axis. Ignored in [MonarchAreaChartStacking.Expand], which is always 0 to 100%.
 * @param includeZero whether the value axis keeps zero when the areas overlap. Stacks always start at zero.
 * @param legendToggle whether legend entries show and hide their series. The legend shows for two or more series.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart: the source, the last update.
 * @param height the plot height, kept in every status. `null` is 240dp.
 * @param labels replacements for the built-in strings, including the frame's.
 * @param formatValue writes values in the inspection row, TalkBack, the table, the summary and on the axis. `null`
 * writes numbers in the current locale, compact on the axis. Shares and the axis of a 100% stack are percentages
 * in the current locale.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in: called with the x index from Details in the inspection row or from the TalkBack action
 * of that x value.
 */
@Composable
public fun MonarchAreaChart(
    title: String,
    categories: List<String>,
    series: List<MonarchLineChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    xTitle: String? = null,
    summary: String? = null,
    stacking: MonarchAreaChartStacking = MonarchAreaChartStacking.Overlap,
    curve: MonarchLineChartCurve = MonarchLineChartCurve.Linear,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenSeries: Set<String>? = null,
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    referenceLines: List<MonarchLineChartReferenceLine> = emptyList(),
    valueRange: ClosedFloatingPointRange<Double>? = null,
    includeZero: Boolean = true,
    legendToggle: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchAreaChartLabels = MonarchAreaChartLabels(),
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    AreaChartBody(
        title = title,
        xLabels = categories,
        fractions = remember(categories.size) { MonarchLineChartGeometry.evenFractions(categories.size) },
        xTitle = xTitle ?: monarchString(labels.category, R.string.monarch_linechart_category),
        options = AreaChartOptions(
            series, modifier, description, summary, stacking, curve, status, hiddenSeries, onHiddenSeriesChange,
            activeIndex, onActiveIndexChange, referenceLines, valueRange, includeZero, legendToggle, tableToggle,
            emptyMessage, footer, height, labels, formatValue, onRetry, onSelect,
        ),
    )
}

/**
 * An area chart of volumes over dates, inside a `MonarchChartFrame`. Points are spaced by time, so uneven dates
 * show. Everything else is as in the overload with categories.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param dates the x values in order, as epoch milliseconds (UTC), as in the Material 3 `DatePickerState`.
 * @param series the measures, bottom of the stack first, each with one value per date.
 * @param modifier the modifier of the frame.
 * @param description one line under the title.
 * @param xTitle the heading of the x column of the table. `null` uses "Date".
 * @param summary replaces the generated spoken summary.
 * @param stacking overlapping areas, a stack whose top edge is the total, or a stack of shares of 100%.
 * @param curve straight edges, or smooth edges that never overshoot.
 * @param status ready, loading, empty or error. Ready with no dates or no series shows the empty state.
 * @param hiddenSeries the keys of the hidden series. `null` lets the chart keep its own set, changed by the legend.
 * @param onHiddenSeriesChange called with the new hidden set when the legend changes it.
 * @param activeIndex the inspected date, used when [onActiveIndexChange] is set.
 * @param onActiveIndexChange makes the inspection controlled: called with the index the user inspects, or `null`
 * when it is cleared.
 * @param referenceLines dashed lines across the plot at a value. Not drawn in a 100% stack.
 * @param valueRange fixes the value axis. Ignored in a 100% stack.
 * @param includeZero whether the value axis keeps zero when the areas overlap.
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
@JvmName("MonarchAreaChartOfDates")
@Composable
public fun MonarchAreaChart(
    title: String,
    dates: List<Long>,
    series: List<MonarchLineChartSeries>,
    modifier: Modifier = Modifier,
    description: String? = null,
    xTitle: String? = null,
    summary: String? = null,
    stacking: MonarchAreaChartStacking = MonarchAreaChartStacking.Overlap,
    curve: MonarchLineChartCurve = MonarchLineChartCurve.Linear,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenSeries: Set<String>? = null,
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    referenceLines: List<MonarchLineChartReferenceLine> = emptyList(),
    valueRange: ClosedFloatingPointRange<Double>? = null,
    includeZero: Boolean = true,
    legendToggle: Boolean = true,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchAreaChartLabels = MonarchAreaChartLabels(),
    formatDate: ((Long) -> String)? = null,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val writeDate = formatDate ?: remember(locale) { areaChartMonthDay(locale) }
    AreaChartBody(
        title = title,
        xLabels = remember(dates, writeDate) { dates.map(writeDate) },
        fractions = remember(dates) { MonarchLineChartGeometry.timeFractions(dates) },
        xTitle = xTitle ?: monarchString(labels.date, R.string.monarch_linechart_date),
        options = AreaChartOptions(
            series, modifier, description, summary, stacking, curve, status, hiddenSeries, onHiddenSeriesChange,
            activeIndex, onActiveIndexChange, referenceLines, valueRange, includeZero, legendToggle, tableToggle,
            emptyMessage, footer, height, labels, formatValue, onRetry, onSelect,
        ),
    )
}

/** The abbreviated month and the day, such as "Mar 4", in [locale] and UTC. */
private fun areaChartMonthDay(locale: Locale): (Long) -> String = MonarchUtcDates.monthDay(locale)

/** The parameters both overloads share, passed on unchanged. */
@Suppress("LongParameterList")
private class AreaChartOptions(
    val series: List<MonarchLineChartSeries>,
    val modifier: Modifier,
    val description: String?,
    val summary: String?,
    val stacking: MonarchAreaChartStacking,
    val curve: MonarchLineChartCurve,
    val status: MonarchChartFrameStatus,
    val hiddenSeries: Set<String>?,
    val onHiddenSeriesChange: ((Set<String>) -> Unit)?,
    val activeIndex: Int?,
    val onActiveIndexChange: ((Int?) -> Unit)?,
    val referenceLines: List<MonarchLineChartReferenceLine>,
    val valueRange: ClosedFloatingPointRange<Double>?,
    val includeZero: Boolean,
    val legendToggle: Boolean,
    val tableToggle: Boolean,
    val emptyMessage: String?,
    val footer: String?,
    val height: Dp?,
    val labels: MonarchAreaChartLabels,
    val formatValue: ((Double) -> String)?,
    val onRetry: (() -> Unit)?,
    val onSelect: ((Int) -> Unit)?,
)

@Composable
private fun AreaChartBody(
    title: String,
    xLabels: List<String>,
    fractions: List<Float>,
    xTitle: String,
    options: AreaChartOptions,
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
    val visible = series.indices.filter { series[it].key !in hidden }
    val words = areaChartWords(options.labels, options.formatValue)
    val stack = remember(series, visible, xLabels.size, options.stacking) {
        MonarchAreaChartStacks.stack(series, visible, xLabels.size, options.stacking)
    }
    val legend = areaLegendItems(series, visible, options.legendToggle)
    val noData = xLabels.isEmpty() || series.isEmpty()
    MonarchChartFrame(
        title = title,
        modifier = options.modifier,
        description = options.description,
        status = if (options.status == MonarchChartFrameStatus.Ready && noData) {
            MonarchChartFrameStatus.Empty
        } else {
            options.status
        },
        table = MonarchAreaChartText.table(xLabels, series, xTitle, options.stacking, words),
        legend = legend,
        hiddenSeries = hidden,
        onHiddenSeriesChange = changeHidden.takeIf { options.legendToggle && legend.isNotEmpty() },
        legendMarker = MonarchChartLegendMarker.Square,
        emptyMessage = options.emptyMessage,
        footer = options.footer,
        height = options.height ?: MonarchLineChartTokens.DefaultHeight,
        tableToggle = options.tableToggle,
        onRetry = options.onRetry,
        labels = options.labels.frame,
    ) {
        AreaPlotContent(title, xLabels, fractions, options, visible, stack, words, active, changeActive)
    }
}

/** The plot inside the frame: the stack's top edges drawn by the line chart core as areas. */
@Composable
@Suppress("LongParameterList")
private fun AreaPlotContent(
    title: String,
    xLabels: List<String>,
    fractions: List<Float>,
    options: AreaChartOptions,
    visible: List<Int>,
    stack: MonarchAreaChartStack,
    words: MonarchAreaChartWords,
    active: Int?,
    changeActive: (Int?) -> Unit,
) {
    val series = options.series
    val labels = options.labels
    val stacking = options.stacking
    val expand = stacking == MonarchAreaChartStacking.Expand
    val references = if (expand) emptyList() else options.referenceLines
    val count = pluralStringResource(R.plurals.monarch_linechart_points, xLabels.size, xLabels.size)
    val details = monarchString(labels.details, R.string.monarch_linechart_details)
    val detailsPattern = stringResource(R.string.monarch_linechart_details_action)
    val text = MonarchLineChartPlotText(
        title = title,
        summary = options.summary ?: MonarchAreaChartText.summary(
            xLabels, series, visible, stack, stacking, references, count, words,
        ),
        describe = { MonarchAreaChartText.describe(it, xLabels, series, visible, stack, stacking, words) },
        detailsAction = { detailsPattern.format(details, xLabels[it]) },
        details = details,
        clear = monarchString(labels.clearSelection, R.string.monarch_linechart_clear),
    )
    // Each shown series becomes its top edge, with the colour of its place among all series.
    val slots = visible.map { MonarchLineChartTokens.slot(series[it].color, it) }
    val edges = remember(series, visible, stack) {
        visible.mapIndexed { position, index ->
            val item = series[index]
            MonarchLineChartSeries(item.key, item.label, stack.edges[position], slots[position], item.dashed)
        }
    }
    val colors = MonarchTheme.colors
    val stacked = stacking != MonarchAreaChartStacking.Overlap
    val tickFormat = rememberAreaValueFormat(options.formatValue, compact = true)
    val percentTick = rememberAreaPercentFormat(fractionDigits = 0)
    val paint = MonarchLineChartPaint(
        style = MonarchTheme.typography.caption,
        colors = MonarchLineChartTokens.colors(colors),
        lineColors = slots.map { MonarchLineChartTokens.lineColor(it, colors) },
        areaColors = edges.mapIndexed { i, edge ->
            MonarchAreaChartTokens.areaColor(slots[i], stacked, edge.dashed, colors)
        },
        tickFormat = if (expand) percentTick else tickFormat,
    )
    val model = remember(xLabels, fractions, edges, stacking, options.curve, references, options.valueRange) {
        MonarchLineChartModel(
            xLabels = xLabels,
            fractions = fractions,
            series = edges,
            // A stack is painted from the top band down, each fill reaching the baseline, so every band shows
            // between its own edge and the edge below.
            visible = if (stacked) edges.indices.reversed().toList() else edges.indices.toList(),
            variant = MonarchLineChartVariant.Area,
            curve = options.curve,
            references = references,
            valueRange = if (expand) ExpandRange else options.valueRange,
            includeZero = stacked || options.includeZero,
            showPoints = false,
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
private fun rememberAreaValueFormat(formatValue: ((Double) -> String)?, compact: Boolean): (Double) -> String =
    rememberMonarchChartValueFormat(formatValue, compact)

/** Shares from 0 to 1 written as percentages in the current locale, with up to [fractionDigits] decimals. */
@Composable
private fun rememberAreaPercentFormat(fractionDigits: Int): (Double) -> String =
    rememberMonarchChartPercentFormat(fractionDigits)

/** The legend entries: every series with two or more, or only the drawn ones when the legend cannot toggle. */
private fun areaLegendItems(
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
private fun areaChartWords(labels: MonarchAreaChartLabels, formatValue: ((Double) -> String)?): MonarchAreaChartWords {
    val sharePattern = stringResource(R.string.monarch_areachart_share)
    return MonarchAreaChartWords(
        line = MonarchLineChartWords(
            noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
            noData = monarchString(labels.noData, R.string.monarch_linechart_no_data),
            value = stringResource(R.string.monarch_linechart_value),
            pointValues = stringResource(R.string.monarch_linechart_point_values),
            range = stringResource(R.string.monarch_linechart_range),
            seriesSummary = stringResource(R.string.monarch_linechart_series_summary),
        ),
        total = monarchString(labels.total, R.string.monarch_areachart_total),
        share = labels.share ?: { value, share -> sharePattern.format(value, share) },
        format = rememberAreaValueFormat(formatValue, compact = false),
        percent = rememberAreaPercentFormat(fractionDigits = 1),
    )
}

private val ExpandRange = 0.0..1.0
