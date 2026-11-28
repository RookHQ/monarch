package dev.monarchui.components.datadisplay.periodselector

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.internal.monarchScrollEdgeFade
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * Picks the period a dashboard or a report covers: today, the last 7 or 30
 * days, this quarter and other presets, or custom dates.
 *
 * The presets are a `MonarchSegmentedControl` (it scrolls
 * sideways when it does not fit) or, in the [MonarchPeriodSelectorVariant.Select]
 * variant, a `MonarchSelect`. Choosing Custom opens the themed Material 3
 * `DateRangePicker` in a `DatePickerDialog`; the picked dates then show on a
 * button that opens the picker again. Each segment is read as a radio button
 * with its state and the row is named by [MonarchPeriodSelectorLabels.label].
 * Report the change with [onValueChange] and get the days a period covers
 * with [MonarchPeriodSelectorDefaults.resolve].
 *
 * Dates are UTC midnight in epoch milliseconds, as in the Material 3 date
 * pickers.
 *
 * @param value the period shown.
 * @param onValueChange called with the new period when a preset is chosen or custom dates are saved.
 * @param modifier the modifier of the selector.
 * @param presets the presets offered, in order.
 * @param variant segments, or a select field.
 * @param enabled whether the period can be changed.
 * @param showRange shows the days a preset covers under the control, such as "Nov 9 – 15, 2026".
 * @param today today, UTC midnight; `null` uses the device's date in [timeZone]. Pass it for a workspace time zone.
 * @param firstDayOfWeek the first day of the week for This week, a `java.util.Calendar` day constant; `null` uses
 * [locale].
 * @param minDate the earliest day custom dates may start, UTC midnight.
 * @param maxDate the latest day custom dates may end, UTC midnight; `null` is [today].
 * @param maxDays the longest custom range in days, such as 366 for reports.
 * @param locale the locale of the dates; `null` uses the configuration's.
 * @param timeZone the zone whose calendar day is today when [today] is `null`. An IANA id such as `"Europe/Berlin"`.
 * `null`, or an id the device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchPeriodSelector(
    value: MonarchPeriodValue,
    onValueChange: (MonarchPeriodValue) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<MonarchPeriodPreset> = MonarchPeriodSelectorDefaults.Presets,
    variant: MonarchPeriodSelectorVariant = MonarchPeriodSelectorVariant.Segmented,
    enabled: Boolean = true,
    showRange: Boolean = false,
    today: Long? = null,
    firstDayOfWeek: Int? = null,
    minDate: Long? = null,
    maxDate: Long? = null,
    maxDays: Int? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    labels: MonarchPeriodSelectorLabels = MonarchPeriodSelectorLabels(),
) {
    val tokens = MonarchPeriodSelectorTokens
    val writeIn = locale ?: LocalConfiguration.current.locales[0]
    val zone = monarchTimeZone(timeZone)
    val day = today?.let(PeriodSelectorDays::startOfDay)
        ?: remember(zone) { MonarchPeriodSelectorDefaults.today(timeZone = zone.id) }
    val weekStart = firstDayOfWeek ?: MonarchPeriodSelectorDefaults.firstDayOfWeek(writeIn)
    val shown = MonarchPeriodSelectorDefaults.resolve(value, day, weekStart)
    val name = monarchString(labels.label, R.string.monarch_periodselector_label)
    var picking by rememberSaveable { mutableStateOf(false) }
    val pick: (MonarchPeriodPreset) -> Unit = { preset ->
        if (preset != value.preset) {
            onValueChange(tokens.next(value, preset, shown))
            if (preset == MonarchPeriodPreset.Custom) picking = true
        }
    }
    val presetName: @Composable (MonarchPeriodPreset) -> String = { preset ->
        labels.preset?.invoke(preset) ?: stringResource(tokens.presetName(preset))
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        when (variant) {
            MonarchPeriodSelectorVariant.Segmented -> PeriodSelectorSegments(
                presets = presets,
                selected = value.preset,
                name = name,
                enabled = enabled,
                presetName = presetName,
                onPick = pick,
            )
            MonarchPeriodSelectorVariant.Select -> MonarchSelect(
                options = presets.map { MonarchSelectOption(it, presetName(it)) },
                selected = value.preset,
                onSelect = pick,
                label = name,
                enabled = enabled,
            )
        }
        PeriodSelectorDates(
            value = value,
            shown = shown,
            showRange = showRange,
            enabled = enabled,
            locale = writeIn,
            labels = labels,
            onPick = { picking = true },
        )
    }
    if (picking && enabled) {
        PeriodSelectorRangeDialog(
            initial = value.range ?: shown,
            limits = PeriodSelectorLimits(minDate, maxDate?.let(PeriodSelectorDays::startOfDay) ?: day, maxDays),
            labels = labels,
            onConfirm = { range ->
                picking = false
                onValueChange(MonarchPeriodValue(MonarchPeriodPreset.Custom, range))
            },
            onDismiss = { picking = false },
        )
    }
}

/** The presets as one segmented button row, as wide as its longest label needs, scrolling sideways when narrow. */
@Composable
private fun PeriodSelectorSegments(
    presets: List<MonarchPeriodPreset>,
    selected: MonarchPeriodPreset,
    name: String,
    enabled: Boolean,
    presetName: @Composable (MonarchPeriodPreset) -> String,
    onPick: (MonarchPeriodPreset) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .monarchScrollEdgeFade(scroll, MonarchTheme.colors.textDefault)
            .horizontalScroll(scroll),
    ) {
        MonarchSegmentedControl(
            modifier = Modifier.semantics {
                isTraversalGroup = true
                contentDescription = name
            },
        ) {
            presets.forEach { preset ->
                Segment(
                    selected = preset == selected,
                    onClick = { onPick(preset) },
                    label = presetName(preset),
                    enabled = enabled,
                )
            }
        }
    }
}

/** The custom dates button while the period is custom, or the days a preset covers with [showRange]. */
@Composable
private fun PeriodSelectorDates(
    value: MonarchPeriodValue,
    shown: MonarchPeriodRange?,
    showRange: Boolean,
    enabled: Boolean,
    locale: Locale,
    labels: MonarchPeriodSelectorLabels,
    onPick: () -> Unit,
) {
    val tokens = MonarchPeriodSelectorTokens
    if (value.preset == MonarchPeriodPreset.Custom) {
        val dates = value.range?.let { tokens.rangeText(it, locale) }
        val customName = monarchString(labels.customRange, R.string.monarch_periodselector_custom_range)
        val text = dates ?: monarchString(labels.chooseDates, R.string.monarch_periodselector_choose_dates)
        MonarchButton(
            text = text,
            onClick = onPick,
            modifier = Modifier.semantics { contentDescription = tokens.customText(customName, text) },
            enabled = enabled,
        )
    } else if (showRange && shown != null) {
        Text(
            text = tokens.rangeText(shown, locale),
            style = MonarchTheme.typography.caption,
            color = tokens.rangeColor(enabled, MonarchTheme.colors),
        )
    }
}
