package dev.monarchui.sample.sections.scheduling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/**
 * The catalogue page of DateTimePicker: the Material 3 date and time pickers
 * opened from two buttons in a Monarch field row, with the time zone beside
 * them.
 */
object DateTimePickerSection : CataloguePage {
    override val title = "DateTimePicker"

    private const val MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60
    private const val DEFAULT_MINUTES = 9 * MINUTES_PER_HOUR
    private const val INLINE_HOUR = 14

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var date by rememberSaveable { mutableStateOf<Long?>(null) }
        var minutes by rememberSaveable { mutableIntStateOf(DEFAULT_MINUTES) }
        var pickingDate by rememberSaveable { mutableStateOf(false) }
        var pickingTime by rememberSaveable { mutableStateOf(false) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = date)
        val timeState = rememberTimePickerState(
            initialHour = minutes / MINUTES_PER_HOUR,
            initialMinute = minutes % MINUTES_PER_HOUR,
        )
        val utc = TimeZone.getTimeZone("UTC")
        val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = utc } }
        val timeFormat = remember(context) {
            android.text.format.DateFormat.getTimeFormat(context).apply { timeZone = utc }
        }
        val value = date?.let { it + minutes * MINUTE }

        CatalogueCaption("Two buttons and the time zone")
        MonarchFormField(
            label = "Interview",
            helperText = "The time is a wall-clock time in the zone shown.",
            errorText = if (date == null) "Choose a date." else null,
        ) { controlModifier ->
            FlowRow(
                modifier = controlModifier,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchButton(
                    text = date?.let { dateFormat.format(Date(it)) } ?: "Set date",
                    onClick = { pickingDate = true },
                )
                MonarchButton(text = timeFormat.format(Date(minutes * MINUTE)), onClick = { pickingTime = true })
                MonarchText(
                    text = "Asia/Kolkata",
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
        MonarchText(
            text = value?.let { "Value: ${dateFormat.format(Date(it))}, ${timeFormat.format(Date(it))}" } ?: "No value",
            style = MonarchTextStyle.Caption,
        )
        if (pickingDate) {
            DatePickerDialog(
                onDismissRequest = { pickingDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        date = dateState.selectedDateMillis ?: date
                        pickingDate = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
            ) { DatePicker(state = dateState) }
        }
        if (pickingTime) {
            var mode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
            TimePickerDialog(
                onDismissRequest = { pickingTime = false },
                confirmButton = {
                    TextButton(onClick = {
                        minutes = timeState.hour * MINUTES_PER_HOUR + timeState.minute
                        pickingTime = false
                    }) { Text("OK") }
                },
                title = { TimePickerDialogDefaults.Title(displayMode = mode) },
                dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
                modeToggleButton = {
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
                    TimePicker(state = timeState)
                } else {
                    TimeInput(state = timeState)
                }
            }
        }

        CatalogueCaption("Typed time, inline")
        TimeInput(state = rememberTimePickerState(initialHour = INLINE_HOUR, initialMinute = 0))
    }
}
