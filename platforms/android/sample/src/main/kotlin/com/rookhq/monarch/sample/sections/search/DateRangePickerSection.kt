package com.rookhq.monarch.sample.sections.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/**
 * The catalogue page of DateRangePicker: the Material 3 DateRangePicker in a
 * dialog opened from a Monarch field row, with presets, a latest day and a
 * longest length.
 */
object DateRangePickerSection : CataloguePage {
    override val title = "DateRangePicker"

    private const val DAY = 86_400_000L
    private const val WEEK_DAYS = 7
    private const val MONTH_DAYS = 30
    private const val LONGEST_DAYS = 366

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val today = remember { System.currentTimeMillis() / DAY * DAY }
        var start by rememberSaveable { mutableStateOf<Long?>(null) }
        var end by rememberSaveable { mutableStateOf<Long?>(null) }
        var picking by rememberSaveable { mutableStateOf(false) }
        var presets by rememberSaveable { mutableStateOf(false) }
        val format = remember {
            DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = start,
            initialSelectedEndDateMillis = end,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
            },
        )
        val choose = { from: Long, to: Long ->
            start = from
            end = to
            state.setSelection(from, to)
            presets = false
        }
        val first = start
        val last = end
        CatalogueCaption("A report period with presets, up to a year, ending today at the latest")
        MonarchFormField(label = "Report period", helperText = "Up to 366 days.") { controlModifier ->
            FlowRow(
                modifier = controlModifier,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            ) {
                MonarchButton(
                    text = if (first != null && last != null) {
                        "${format.format(Date(first))} – ${format.format(Date(last))}"
                    } else {
                        "Choose dates"
                    },
                    onClick = { picking = true },
                )
                Box {
                    MonarchButton(text = "Presets", onClick = { presets = true }, variant = MonarchButtonVariant.Ghost)
                    MonarchMenu(expanded = presets, onDismissRequest = { presets = false }) {
                        MonarchMenuItem(
                            text = "Last 7 days",
                            onClick = { choose(today - (WEEK_DAYS - 1) * DAY, today) },
                        )
                        MonarchMenuItem(
                            text = "Last 30 days",
                            onClick = { choose(today - (MONTH_DAYS - 1) * DAY, today) },
                        )
                    }
                }
                if (first != null) {
                    MonarchButton(text = "Clear", onClick = {
                        start = null
                        end = null
                        state.setSelection(null, null)
                    }, variant = MonarchButtonVariant.Ghost)
                }
            }
        }
        if (picking) {
            val from = state.selectedStartDateMillis
            val to = state.selectedEndDateMillis
            DatePickerDialog(
                onDismissRequest = { picking = false },
                confirmButton = {
                    TextButton(
                        enabled = from != null && to != null && (to - from) / DAY < LONGEST_DAYS,
                        onClick = {
                            start = from
                            end = to
                            picking = false
                        },
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
            ) {
                DateRangePicker(state = state, modifier = Modifier.weight(1f))
            }
        }
        CatalogueCaption("Typed dates, inline")
        DateRangePicker(
            state = rememberDateRangePickerState(initialDisplayMode = DisplayMode.Input),
            showModeToggle = false,
        )
    }
}
