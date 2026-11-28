package com.rookhq.monarch.components.datadisplay.periodselector

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchDateRangePicker
import com.rookhq.monarch.internal.MonarchPickerDialog
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Material 3 `DateRangePicker` in the themed picker dialog, opened for a
 * custom period. Days outside the limits cannot be picked; Save stays off
 * until a first day is picked and the range is not longer than allowed,
 * which is said in words above the calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodSelectorRangeDialog(
    initial: MonarchPeriodRange?,
    limits: PeriodSelectorLimits,
    labels: MonarchPeriodSelectorLabels,
    onConfirm: (MonarchPeriodRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MonarchPeriodSelectorTokens
    val selectable = remember(limits.minDate, limits.latest) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                tokens.selectable(utcTimeMillis, limits.minDate, limits.latest)
        }
    }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.startMillis,
        initialSelectedEndDateMillis = initial?.endMillis,
        initialDisplayedMonthMillis = initial?.startMillis ?: limits.latest,
        selectableDates = selectable,
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis
    MonarchPickerDialog(
        onDismissRequest = onDismiss,
        confirmLabel = monarchString(labels.confirm, R.string.monarch_periodselector_confirm),
        onConfirm = { if (start != null) onConfirm(MonarchPeriodRange(start, end ?: start)) },
        cancelLabel = monarchString(labels.cancel, R.string.monarch_periodselector_cancel),
        confirmEnabled = tokens.accepts(start, end, limits.maxDays),
    ) {
        val maxDays = limits.maxDays
        if (maxDays != null && tokens.tooLong(start, end, maxDays)) {
            Text(
                text = labels.tooLong?.invoke(maxDays)
                    ?: pluralStringResource(R.plurals.monarch_periodselector_too_long, maxDays, maxDays),
                modifier = Modifier
                    .padding(start = tokens.DialogPadding, end = tokens.DialogPadding, top = tokens.Gap)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = tokens.errorColor(MonarchTheme.colors),
            )
        }
        MonarchDateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
