package dev.monarchui.components.datadisplay.metric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicator
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * One number with its label, an optional unit and an optional change, such as
 * "Open tickets 128 ↑ 12%", with no surface around it.
 *
 * Built by Monarch from Compose `Text` and `MonarchDeltaIndicator`: Material 3
 * has no metric. The number is written by the platform's
 * `android.icu.text.NumberFormat` in the locale, in tabular figures. The whole
 * metric is one TalkBack element: the label, the value with its unit and the
 * change as a sentence. A compact value is read in full, and a missing or
 * infinite value shows a dash read as "No data".
 *
 * @param label what the number measures, for example "Open tickets". Always visible.
 * @param value the number, or `null` when there is none.
 * @param modifier the modifier of the metric.
 * @param unit written smaller and muted after the value, for example "ms" or "/ month". Hidden for an empty value.
 * @param change the change against an earlier period, drawn by `MonarchDeltaIndicator`.
 * @param format a number, a percentage (a ratio: 0.12 is 12%) or an amount of money.
 * @param size `Sm` for dense rows, `Md`, `Lg` for a headline figure.
 * @param layout the label above the value, or everything on one line that wraps.
 * @param alignment the alignment of the stacked layout; `End` for numeric columns.
 * @param compact writes 12K or €3.4M; TalkBack reads the full value.
 * @param locale the locale to write in. `null` uses the first locale of the configuration.
 * @param emptyLabel replaces the built-in "No data" read for a missing value (`monarch_metric_empty`).
 */
@Composable
public fun MonarchMetric(
    label: String,
    value: Double?,
    modifier: Modifier = Modifier,
    unit: String? = null,
    change: MonarchMetricChange? = null,
    format: MonarchMetricFormat = MonarchMetricFormat.Number,
    size: MonarchMetricSize = MonarchMetricSize.Md,
    layout: MonarchMetricLayout = MonarchMetricLayout.Stacked,
    alignment: Alignment.Horizontal = Alignment.Start,
    compact: Boolean = false,
    locale: Locale? = null,
    emptyLabel: String? = null,
) {
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    MetricLayout(
        label = label,
        shown = MonarchMetricTokens.shown(value, format, compact, resolvedLocale),
        spoken = MonarchMetricTokens.spoken(value, format, compact, resolvedLocale),
        unit = unit,
        change = change,
        size = size,
        layout = layout,
        alignment = alignment,
        emptyLabel = emptyLabel,
        modifier = modifier,
    )
}

/**
 * A metric whose value the app has already written, such as "1.2K" or "3 of 5".
 *
 * @param label what the value measures. Always visible.
 * @param text the value as written by the app. `null` or empty shows the dash read as "No data".
 * @param modifier the modifier of the metric.
 * @param spokenText what TalkBack reads instead of [text], for example the full number of an abbreviation.
 * @param unit written smaller and muted after the value. Hidden for an empty value.
 * @param change the change against an earlier period, drawn by `MonarchDeltaIndicator`.
 * @param size `Sm` for dense rows, `Md`, `Lg` for a headline figure.
 * @param layout the label above the value, or everything on one line that wraps.
 * @param alignment the alignment of the stacked layout.
 * @param emptyLabel replaces the built-in "No data" read for a missing value.
 */
@Composable
public fun MonarchMetric(
    label: String,
    text: String?,
    modifier: Modifier = Modifier,
    spokenText: String? = null,
    unit: String? = null,
    change: MonarchMetricChange? = null,
    size: MonarchMetricSize = MonarchMetricSize.Md,
    layout: MonarchMetricLayout = MonarchMetricLayout.Stacked,
    alignment: Alignment.Horizontal = Alignment.Start,
    emptyLabel: String? = null,
) {
    MetricLayout(
        label = label,
        shown = text?.takeIf { it.isNotEmpty() },
        spoken = spokenText,
        unit = unit,
        change = change,
        size = size,
        layout = layout,
        alignment = alignment,
        emptyLabel = emptyLabel,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricLayout(
    label: String,
    shown: String?,
    spoken: String?,
    unit: String?,
    change: MonarchMetricChange?,
    size: MonarchMetricSize,
    layout: MonarchMetricLayout,
    alignment: Alignment.Horizontal,
    emptyLabel: String?,
    modifier: Modifier,
) {
    val tokens = MonarchMetricTokens
    val typography = MonarchTheme.typography
    val colors = MonarchTheme.colors
    val merged = modifier.semantics(mergeDescendants = true) { }
    val labelText: @Composable () -> Unit = {
        Text(text = label, style = tokens.labelStyle(size, typography), color = tokens.labelColor(colors))
    }
    val figureAlignment = if (layout == MonarchMetricLayout.Stacked) alignment else Alignment.Start
    val figure: @Composable () -> Unit = { Figure(shown, spoken, unit, size, figureAlignment, emptyLabel) }
    val delta: @Composable () -> Unit = { change?.let { MetricChange(it, size) } }
    when (layout) {
        MonarchMetricLayout.Stacked -> Column(
            modifier = merged,
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.spacedBy(tokens.StackGap),
        ) {
            labelText()
            figure()
            delta()
        }
        MonarchMetricLayout.Inline -> FlowRow(
            modifier = merged,
            horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
            verticalArrangement = Arrangement.spacedBy(tokens.StackGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            labelText()
            figure()
            delta()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Figure(
    shown: String?,
    spoken: String?,
    unit: String?,
    size: MonarchMetricSize,
    alignment: Alignment.Horizontal,
    emptyLabel: String?,
) {
    val tokens = MonarchMetricTokens
    val typography = MonarchTheme.typography
    val colors = MonarchTheme.colors
    val empty = shown == null
    val emptySpoken = monarchString(emptyLabel, R.string.monarch_metric_empty)
    val description = if (empty) emptySpoken else spoken
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.UnitGap, alignment),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = shown ?: monarchString(null, R.string.monarch_metric_dash),
            modifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
            style = tokens.valueStyle(size, typography),
            color = tokens.valueColor(empty, colors),
        )
        if (!empty && !unit.isNullOrEmpty()) {
            Text(text = unit, style = tokens.unitStyle(size, typography), color = tokens.unitColor(colors))
        }
    }
}

@Composable
private fun MetricChange(change: MonarchMetricChange, size: MonarchMetricSize) {
    val deltaSize = MonarchMetricTokens.deltaSize(size)
    if (change.source.fromValues) {
        MonarchDeltaIndicator(
            current = change.source.current,
            previous = change.source.previous,
            comparison = change.comparison,
            goodDirection = change.goodDirection,
            variant = change.variant,
            size = deltaSize,
            flatThreshold = change.flatThreshold,
            icon = change.icon,
            formatValue = change.formatValue,
        )
    } else {
        MonarchDeltaIndicator(
            value = change.source.value,
            comparison = change.comparison,
            goodDirection = change.goodDirection,
            variant = change.variant,
            size = deltaSize,
            flatThreshold = change.flatThreshold,
            icon = change.icon,
            formatValue = change.formatValue,
        )
    }
}
