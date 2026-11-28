package dev.monarchui.components.workflow.fieldrenderer

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.datefield.MonarchDateField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.internal.MonarchDatePicker
import dev.monarchui.internal.MonarchPickerDialog
import dev.monarchui.internal.MonarchTimeInput
import dev.monarchui.internal.MonarchTimePicker
import dev.monarchui.internal.MonarchTimePickerDialog
import java.util.Date
import java.util.Locale

private typealias DateTimeTokens = MonarchFieldRendererTokens

/** A date question: a button with the chosen day that opens the themed date picker in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun MonarchFieldRendererDate(
    field: MonarchFormFieldDefinition,
    millis: Long?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    modifier: Modifier,
    usable: Boolean,
    text: MonarchFieldRendererText,
) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { MonarchFieldRendererFormats.date(locale) }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (field.maxDate == null) {
        // The web shows a date input with a calendar mark; the field-shaped control opens the same picker.
        MonarchDateField(
            value = millis,
            onValueChange = { onValueChange(it?.let(MonarchFormFieldValue::Date)) },
            modifier = modifier.fillMaxWidth(),
            placeholder = text.chooseDate,
            enabled = usable,
            earliest = field.minDate,
        )
        return
    }
    Row(modifier = modifier) {
        MonarchButton(
            text = millis?.let { format.format(Date(it)) } ?: text.chooseDate,
            onClick = { picking = true },
            enabled = usable,
        )
    }
    if (picking && usable) {
        val minDate = field.minDate
        val maxDate = field.maxDate
        val state = rememberDatePickerState(
            initialSelectedDateMillis = millis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    (minDate == null || utcTimeMillis >= minDate) && utcTimeMillis <= maxDate
            },
        )
        MonarchPickerDialog(
            onDismissRequest = { picking = false },
            confirmLabel = text.confirm,
            onConfirm = {
                state.selectedDateMillis?.let { onValueChange(MonarchFormFieldValue.Date(it)) }
                picking = false
            },
            cancelLabel = text.cancel,
        ) {
            MonarchDatePicker(state = state)
        }
    }
}

/** A time question: a button with the chosen time that opens the themed time picker in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonarchFieldRendererTime(
    answer: String?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    modifier: Modifier,
    usable: Boolean,
    text: MonarchFieldRendererText,
) {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val format = remember(locale, is24Hour) { MonarchFieldRendererFormats.time(locale, is24Hour) }
    val minutes = DateTimeTokens.minutesOf(answer)
    var picking by rememberSaveable { mutableStateOf(false) }
    Row(modifier = modifier) {
        MonarchButton(
            text = minutes?.let { format.format(Date(it * MinuteMillis)) } ?: text.chooseTime,
            onClick = { picking = true },
            enabled = usable,
        )
    }
    if (picking && usable) {
        val state = rememberTimePickerState(
            initialHour = (minutes ?: 0) / MinutesPerHour,
            initialMinute = (minutes ?: 0) % MinutesPerHour,
            is24Hour = is24Hour,
        )
        var mode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
        MonarchTimePickerDialog(
            onDismissRequest = { picking = false },
            title = { TimePickerDialogDefaults.Title(displayMode = mode) },
            confirmLabel = text.confirm,
            onConfirm = {
                onValueChange(MonarchFormFieldValue.Text(DateTimeTokens.timeAnswer(state.hour, state.minute)))
                picking = false
            },
            cancelLabel = text.cancel,
            modeToggle = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        mode = if (mode == TimePickerDisplayMode.Picker) {
                            TimePickerDisplayMode.Input
                        } else {
                            TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = mode,
                )
            },
        ) {
            if (mode == TimePickerDisplayMode.Picker) {
                MonarchTimePicker(state = state)
            } else {
                MonarchTimeInput(state = state)
            }
        }
    }
}

/** The date and time formats of a MonarchFieldRenderer, in UTC so a day and a wall-clock time never shift. */
internal object MonarchFieldRendererFormats {
    private val utc = TimeZone.getTimeZone("UTC")

    /** "Nov 14, 2026" in [locale]. */
    fun date(locale: Locale): DateFormat = DateFormat.getInstanceForSkeleton("yMMMd", locale).apply { timeZone = utc }

    /** "1:30 PM" or "13:30" in [locale]. */
    fun time(locale: Locale, is24Hour: Boolean): DateFormat =
        DateFormat.getInstanceForSkeleton(if (is24Hour) "HHmm" else "hmm", locale).apply { timeZone = utc }
}

private const val MinuteMillis = 60_000L
private const val MinutesPerHour = 60
