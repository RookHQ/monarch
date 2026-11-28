package dev.monarchui.components.charts.burndownchart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import dev.monarchui.components.charts.linechart.MonarchLineChartPlotText
import dev.monarchui.components.charts.linechart.MonarchLineChartSeries
import dev.monarchui.components.charts.linechart.MonarchLineChartTokens
import dev.monarchui.components.charts.linechart.MonarchLineChartVariant
import dev.monarchui.internal.MonarchUtcDates
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A sprint burndown inside a `MonarchChartFrame`: the work left each day against the ideal line, the scope as a
 * step line with its changes marked, a today marker and a sentence saying whether the sprint is ahead or behind.
 *
 * Built by Monarch with a Compose `Canvas` on the line chart core: Material 3 has no chart and Monarch adds no chart
 * library. Days are spaced evenly by position, so days left out (weekends) make no flat stretch. The ideal falls in
 * a straight line from [idealStart] to zero on the last day. The work left is the first data colour and the
 * thickest line; the ideal is dashed in the neutral colour; the scope is a thinner step line in the second data
 * colour, with a dot and a "+5" or "−2" label where it changes. The value axis always starts at zero.
 *
 * The generated summary ("Mar 6: 24 remaining, ideal 17.8, 6.2 behind ideal. Scope 43, 5 added and 2 removed since
 * the start") is the visible [description] unless the app gives one, and the spoken state of the plot. For
 * TalkBack the plot is one element named by [title], followed by one element per day read as the day and its
 * values; with [onSelect] each has a "Details" action. A tap, or a drag across the plot, inspects the nearest day
 * with a crosshair and a row of its values announced politely, with Details and Clear. The legend shows and hides
 * the lines and keeps one visible. The chart mirrors in right to left and does not animate.
 *
 * @param title the chart's name: the frame heading and the plot's spoken name.
 * @param points one point per sprint day, in order, including days not reached yet (without work left).
 * @param modifier the modifier of the frame.
 * @param description one line under the title. `null` shows the generated summary.
 * @param summary replaces the generated spoken summary of the plot.
 * @param idealStart the work at the start of the ideal line. `null` uses the first day's scope, else its work left.
 * @param todayIndex the day of the today marker. `null` uses the latest day with work left when later days exist.
 * An index outside the days draws no marker.
 * @param showToday whether the today marker is drawn at all.
 * @param showScope whether the scope line, its changes and its table columns are shown, when any day has a scope.
 * @param status ready, loading, empty or error. Ready with no points shows the empty state.
 * @param hiddenLines the hidden lines. `null` lets the chart keep its own set, changed by the legend.
 * @param onHiddenLinesChange called with the new hidden set when the legend changes it.
 * @param activeIndex the inspected day, used when [onActiveIndexChange] is set, to share a crosshair.
 * @param onActiveIndexChange makes the inspection controlled: called with the day the user inspects, or `null`
 * when it is cleared.
 * @param tableToggle whether "View as table" is offered.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart.
 * @param height the plot height, kept in every status. `null` is 240dp.
 * @param labels replacements for every built-in string, including the frame's.
 * @param formatDate writes a dated day. `null` writes the abbreviated month and the day in the current locale, in
 * UTC.
 * @param formatValue writes a value, such as "24 pts", everywhere the chart writes one but the axis, which writes
 * plain compact numbers. `null` writes numbers in the current locale, compact on the axis.
 * @param onRetry shows Try again in the error status.
 * @param onSelect drill-in, such as the issues closed that day: called with the day's index from Details in the
 * inspection row or from the TalkBack action of that day.
 */
@Composable
public fun MonarchBurndownChart(
    title: String,
    points: List<MonarchBurndownChartPoint>,
    modifier: Modifier = Modifier,
    description: String? = null,
    summary: String? = null,
    idealStart: Double? = null,
    todayIndex: Int? = null,
    showToday: Boolean = true,
    showScope: Boolean = true,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    hiddenLines: Set<MonarchBurndownChartLine>? = null,
    onHiddenLinesChange: ((Set<MonarchBurndownChartLine>) -> Unit)? = null,
    activeIndex: Int? = null,
    onActiveIndexChange: ((Int?) -> Unit)? = null,
    tableToggle: Boolean = true,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp? = null,
    labels: MonarchBurndownChartLabels = MonarchBurndownChartLabels(),
    formatDate: ((Long) -> String)? = null,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val writeDate = formatDate ?: remember(locale) { burndownChartMonthDay(locale) }
    val days = remember(points, writeDate) { points.map { it.label ?: it.date?.let(writeDate).orEmpty() } }
    val data = remember(points, idealStart, todayIndex, showToday) {
        MonarchBurndownChartData.of(points, idealStart, todayIndex, showToday)
    }
    var ownHidden by rememberSaveable { mutableStateOf(listOf<MonarchBurndownChartLine>()) }
    var ownActive by rememberSaveable { mutableStateOf<Int?>(null) }
    val hidden = hiddenLines ?: ownHidden.toSet()
    val active = (if (onActiveIndexChange != null) activeIndex else ownActive)?.takeIf { it in days.indices }
    val changeActive: (Int?) -> Unit = { next ->
        if (onActiveIndexChange != null) onActiveIndexChange(next) else ownActive = next
    }
    val scopeOn = showScope && data.hasScope
    val shown = MonarchBurndownChartShown(
        remaining = MonarchBurndownChartLine.Remaining !in hidden,
        ideal = MonarchBurndownChartLine.Ideal !in hidden,
        scope = scopeOn && MonarchBurndownChartLine.Scope !in hidden,
        scopeOn = scopeOn,
    )
    val words = burndownChartWords(labels, formatValue)
    val generated = MonarchBurndownChartText.summary(days, data, shown, words)
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description ?: generated.takeIf { days.isNotEmpty() },
        status = if (status == MonarchChartFrameStatus.Ready && days.isEmpty()) {
            MonarchChartFrameStatus.Empty
        } else {
            status
        },
        table = MonarchBurndownChartText.table(days, data, shown, words),
        legend = burndownChartLegend(words, scopeOn),
        hiddenSeries = hidden.map { it.name }.toSet(),
        onHiddenSeriesChange = { next ->
            val lines = MonarchBurndownChartLine.entries.filter { it.name in next }.toSet()
            if (hiddenLines == null) ownHidden = lines.toList()
            onHiddenLinesChange?.invoke(lines)
        },
        legendMarker = MonarchChartLegendMarker.Line,
        emptyMessage = emptyMessage,
        footer = footer,
        height = height ?: MonarchLineChartTokens.DefaultHeight,
        tableToggle = tableToggle,
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        BurndownChartPlot(
            title, days, data, shown, words, labels, summary ?: generated, active, changeActive, onSelect,
        )
    }
}

/** The plot inside the frame: the work left and the ideal through the line chart core, the rest on top. */
@Composable
@Suppress("LongParameterList")
private fun BurndownChartPlot(
    title: String,
    days: List<String>,
    data: MonarchBurndownChartData,
    shown: MonarchBurndownChartShown,
    words: MonarchBurndownChartWords,
    labels: MonarchBurndownChartLabels,
    summary: String,
    active: Int?,
    changeActive: (Int?) -> Unit,
    onSelect: ((Int) -> Unit)?,
) {
    val details = monarchString(labels.details, R.string.monarch_linechart_details)
    val detailsPattern = stringResource(R.string.monarch_linechart_details_action)
    val text = MonarchLineChartPlotText(
        title = title,
        summary = summary,
        describe = { MonarchBurndownChartText.describe(it, days, data, shown, words) },
        detailsAction = { detailsPattern.format(details, days[it]) },
        details = details,
        clear = monarchString(labels.clearSelection, R.string.monarch_linechart_clear),
    )
    val colors = MonarchBurndownChartTokens.colors(MonarchTheme.colors)
    val paint = MonarchLineChartPaint(
        style = MonarchTheme.typography.caption,
        colors = MonarchLineChartTokens.colors(MonarchTheme.colors),
        lineColors = listOf(colors.remaining, colors.ideal),
        areaColors = listOf(colors.remaining, colors.ideal),
        // As the web: the value axis writes plain compact numbers (60, 40, 20), the unit is for the values.
        tickFormat = rememberBurndownChartFormat(null, compact = true),
    )
    val todayLabel = monarchString(labels.today, R.string.monarch_burndownchart_today)
    val scene = remember(days, data, shown.remaining, shown.ideal, shown.scope, words.format, todayLabel) {
        MonarchBurndownChartScene(
            model = MonarchLineChartModel(
                xLabels = days,
                fractions = MonarchLineChartGeometry.evenFractions(days.size),
                series = listOf(
                    MonarchLineChartSeries(
                        REMAINING, words.remaining, data.remaining, MonarchBurndownChartTokens.RemainingSlot,
                    ),
                    MonarchLineChartSeries(
                        IDEAL, words.ideal, data.ideal, MonarchBurndownChartTokens.IdealSlot, dashed = true,
                    ),
                ),
                visible = listOfNotNull(0.takeIf { shown.remaining }, 1.takeIf { shown.ideal }),
                variant = MonarchLineChartVariant.Line,
                curve = MonarchLineChartCurve.Linear,
                references = emptyList(),
                valueRange = null,
                includeZero = true,
                showPoints = false,
            ),
            scope = if (shown.scope) data.scope else emptyList(),
            changeLabels = data.changes.map { MonarchBurndownChartText.changeText(it, words) },
            today = data.today,
            todayLabel = todayLabel,
        )
    }
    MonarchBurndownChartPlotArea(
        scene = scene,
        paint = paint,
        colors = colors,
        text = text,
        active = active,
        onActiveChange = changeActive,
        onSelect = onSelect,
    )
}

/** The legend: Remaining, the dashed Ideal and, with a scope, Scope. */
private fun burndownChartLegend(words: MonarchBurndownChartWords, scopeOn: Boolean): List<MonarchChartLegendItem> =
    listOfNotNull(
        MonarchChartLegendItem(REMAINING, words.remaining, MonarchBurndownChartTokens.RemainingSlot),
        MonarchChartLegendItem(IDEAL, words.ideal, MonarchBurndownChartTokens.IdealSlot, dashed = true),
        MonarchChartLegendItem(SCOPE, words.scope, MonarchBurndownChartTokens.ScopeSlot).takeIf { scopeOn },
    )

/** The abbreviated month and the day, such as "Mar 4", in [locale] and UTC. */
private fun burndownChartMonthDay(locale: Locale): (Long) -> String = MonarchUtcDates.monthDay(locale)

/** [formatValue], or numbers in the current locale: compact (1.2K) for the axis when [compact]. */
@Composable
private fun rememberBurndownChartFormat(formatValue: ((Double) -> String)?, compact: Boolean): (Double) -> String =
    rememberMonarchChartValueFormat(formatValue, compact)

/** The built-in words of the chart, with the app's replacements. */
@Composable
private fun burndownChartWords(
    labels: MonarchBurndownChartLabels,
    formatValue: ((Double) -> String)?,
): MonarchBurndownChartWords {
    val added = stringResource(R.string.monarch_burndownchart_change_added)
    val removed = stringResource(R.string.monarch_burndownchart_change_removed)
    val status = stringResource(R.string.monarch_burndownchart_status)
    val ahead = stringResource(R.string.monarch_burndownchart_ahead)
    val behind = stringResource(R.string.monarch_burndownchart_behind)
    val scopeSummary = stringResource(R.string.monarch_burndownchart_scope_summary)
    return MonarchBurndownChartWords(
        remaining = monarchString(labels.remaining, R.string.monarch_burndownchart_remaining),
        ideal = monarchString(labels.ideal, R.string.monarch_burndownchart_ideal),
        scope = monarchString(labels.scope, R.string.monarch_burndownchart_scope),
        day = monarchString(labels.day, R.string.monarch_burndownchart_day),
        scopeChange = monarchString(labels.scopeChange, R.string.monarch_burndownchart_scope_change),
        noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
        value = stringResource(R.string.monarch_linechart_value),
        pointValues = stringResource(R.string.monarch_linechart_point_values),
        change = labels.change ?: { amount, more -> (if (more) added else removed).format(amount) },
        status = labels.status ?: { day, left, ideal -> status.format(day, left, ideal) },
        statusTrend = stringResource(R.string.monarch_burndownchart_status_trend),
        ahead = labels.ahead ?: { ahead.format(it) },
        behind = labels.behind ?: { behind.format(it) },
        onTrack = monarchString(labels.onTrack, R.string.monarch_burndownchart_on_track),
        scopeSummary = labels.scopeSummary ?: { scope, more, less -> scopeSummary.format(scope, more, less) },
        notStarted = monarchString(labels.notStarted, R.string.monarch_burndownchart_not_started),
        format = rememberBurndownChartFormat(formatValue, compact = false),
    )
}

private const val REMAINING = "Remaining"
private const val IDEAL = "Ideal"
private const val SCOPE = "Scope"
