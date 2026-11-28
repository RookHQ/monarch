package dev.monarchui.sample.sections.scheduling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/** The catalogue page of DatePicker: the Material 3 date picker in a Monarch field row, with presets. */
object DatePickerSection : CataloguePage {
    override val title = "DatePicker"

    private const val DAY = 86_400_000L
    private const val WEEK_DAYS = 7

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val today = remember { System.currentTimeMillis() / DAY * DAY }
        var due by rememberSaveable { mutableStateOf<Long?>(null) }
        var picking by rememberSaveable { mutableStateOf(false) }
        var presets by rememberSaveable { mutableStateOf(false) }
        val format = remember {
            DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = due,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            },
        )
        CatalogueCaption("A due date with presets, from today on")
        MonarchFormField(label = "Due date", helperText = "Past dates cannot be chosen.") { controlModifier ->
            Row(modifier = controlModifier, horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchButton(text = due?.let { format.format(Date(it)) } ?: "Set date", onClick = { picking = true })
                Box {
                    MonarchButton(text = "Presets", onClick = { presets = true }, variant = MonarchButtonVariant.Ghost)
                    MonarchMenu(expanded = presets, onDismissRequest = { presets = false }) {
                        MonarchMenuItem(text = "Today", onClick = {
                            due = today
                            presets = false
                        })
                        MonarchMenuItem(text = "In a week", onClick = {
                            due = today + WEEK_DAYS * DAY
                            presets = false
                        })
                    }
                }
                if (due != null) {
                    MonarchButton(text = "Clear", onClick = { due = null }, variant = MonarchButtonVariant.Ghost)
                }
            }
        }
        if (picking) {
            DatePickerDialog(
                onDismissRequest = { picking = false },
                confirmButton = {
                    TextButton(onClick = {
                        due = state.selectedDateMillis
                        picking = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
            ) { DatePicker(state = state) }
        }
        CatalogueCaption("An inline calendar")
        DatePicker(state = rememberDatePickerState(), showModeToggle = false)
    }
}
