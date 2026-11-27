package com.rookhq.monarch.components.scheduling.timeoffpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.internal.MonarchDatePickerDialog
import com.rookhq.monarch.internal.MonarchTimePicker
import com.rookhq.monarch.internal.MonarchTimePickerDialog
import com.rookhq.monarch.internal.monarchString

private const val MinutesPerHour = 60

/** The value and limits of one date (and time) field of the add sheet. */
internal class TimeOffPanelPick(
    val day: Long,
    val onDayChange: (Long) -> Unit,
    val minutes: Int? = null,
    val onMinutesChange: (Int) -> Unit = {},
    val earliest: Long? = null,
)

/**
 * A labelled field of the add sheet of MonarchTimeOffPanel: a button with the date that opens the Material 3
 * `DatePickerDialog`, and for timed entries a button with the time that opens the themed time picker dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeOffPanelDateField(
    label: String,
    pick: TimeOffPanelPick,
    error: String?,
    is24Hour: Boolean,
    formats: TimeOffPanelFormats,
    labels: MonarchTimeOffPanelLabels,
) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    MonarchFormField(label = label, errorText = error) { controlModifier ->
        FlowRow(
            modifier = controlModifier,
            horizontalArrangement = Arrangement.spacedBy(MonarchTimeOffPanelTokens.RowGap),
        ) {
            MonarchButton(text = formats.day(pick.day), onClick = { pickingDate = true })
            pick.minutes?.let { minutes ->
                MonarchButton(text = formats.minutes(minutes), onClick = { pickingTime = true })
            }
        }
    }
    if (pickingDate) TimeOffPanelDateDialog(pick, labels) { pickingDate = false }
    val minutes = pick.minutes
    if (pickingTime && minutes != null) {
        TimeOffPanelTimeDialog(minutes, is24Hour, pick.onMinutesChange, labels) { pickingTime = false }
    }
}

@Composable
private fun TimeOffPanelDateDialog(pick: TimeOffPanelPick, labels: MonarchTimeOffPanelLabels, onClose: () -> Unit) {
    MonarchDatePickerDialog(
        initial = pick.day,
        earliest = pick.earliest,
        confirmLabel = monarchString(labels.confirmPick, R.string.monarch_timeoffpanel_confirm_pick),
        cancelLabel = monarchString(labels.cancel, R.string.monarch_timeoffpanel_cancel),
        onPick = pick.onDayChange,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeOffPanelTimeDialog(
    minutes: Int,
    is24Hour: Boolean,
    onMinutesChange: (Int) -> Unit,
    labels: MonarchTimeOffPanelLabels,
    onClose: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = minutes / MinutesPerHour,
        initialMinute = minutes % MinutesPerHour,
        is24Hour = is24Hour,
    )
    MonarchTimePickerDialog(
        onDismissRequest = onClose,
        title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
        confirmLabel = monarchString(labels.confirmPick, R.string.monarch_timeoffpanel_confirm_pick),
        onConfirm = {
            onMinutesChange(state.hour * MinutesPerHour + state.minute)
            onClose()
        },
        cancelLabel = monarchString(labels.cancel, R.string.monarch_timeoffpanel_cancel),
    ) { MonarchTimePicker(state = state) }
}
