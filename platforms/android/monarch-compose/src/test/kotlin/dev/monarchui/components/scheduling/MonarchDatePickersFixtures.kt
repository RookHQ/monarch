package dev.monarchui.components.scheduling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.internal.MonarchDatePickerDialog
import dev.monarchui.internal.monarchDatePickerColors
import dev.monarchui.internal.monarchFitWidth
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.theme.MonarchTheme

/**
 * Fixed dates for the date picker fixtures. Material's DatePicker outlines today from the real clock and a test
 * cannot set that clock, so every picker shows January 2026: a month that is in the past on every day a test can
 * run, so the image never has today in it. All values are midnight UTC, as `DatePickerState` gives them.
 */
private const val January14 = 1_768_348_800_000L
private const val January12 = 1_768_176_000_000L

private val FromJanuary12 = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= January12
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDatePicker(selectable: SelectableDates = DatePickerAll) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = January14,
        initialDisplayedMonthMillis = January14,
        selectableDates = selectable,
    )
    DatePicker(
        state = state,
        modifier = Modifier.monarchFitWidth(),
        showModeToggle = false,
        colors = monarchDatePickerColors(MonarchTheme.colors),
    )
}

private val DatePickerAll = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
}

@OptIn(ExperimentalMaterial3Api::class)
internal object MonarchDatePickerFixtures : ComponentFixtures {
    override val id = "date-picker"
    override val fixtures = listOf(
        fixture("a due date with presets", tall = false) {
            MonarchFormField(label = "Due date") { controlModifier ->
                Row(
                    modifier = controlModifier,
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                ) {
                    MonarchButton(text = "Jan 14, 2026", onClick = {})
                    MonarchButton(text = "Presets", onClick = {}, variant = MonarchButtonVariant.Ghost)
                }
            }
        },
        fixture("inline calendar, every day selectable", tall = true) { InlineDatePicker() },
        fixture("inline calendar, from January 12", tall = true) { InlineDatePicker(FromJanuary12) },
        fixture("in a dialog", tall = true) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = January14,
                initialDisplayedMonthMillis = January14,
            )
            MonarchDatePickerDialog(state, confirmLabel = "OK", cancelLabel = "Cancel", onPick = {}, onClose = {})
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal object MonarchDateTimePickerFixtures : ComponentFixtures {
    override val id = "date-time-picker"
    override val fixtures = listOf(
        fixture("date, time and time zone beside them", tall = false) {
            MonarchFormField(label = "Interview", helperText = "Starts Jan 14, 2026, 9:30 AM") { controlModifier ->
                FlowRow(
                    modifier = controlModifier,
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchButton(text = "Jan 14, 2026", onClick = {})
                    MonarchButton(text = "9:30 AM", onClick = {})
                    Text(
                        text = "Asia/Kolkata",
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
            }
        },
        fixture("inline day with the time zone note", tall = true) {
            val date = rememberDatePickerState(
                initialSelectedDateMillis = January14,
                initialDisplayedMonthMillis = January14,
            )
            Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
                DatePicker(
                    state = date,
                    modifier = Modifier.monarchFitWidth(),
                    showModeToggle = false,
                    title = null,
                    headline = null,
                    colors = monarchDatePickerColors(MonarchTheme.colors),
                )
                Text(
                    text = "Times in Europe/London",
                    style = MonarchTheme.typography.caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        },
    )
}
