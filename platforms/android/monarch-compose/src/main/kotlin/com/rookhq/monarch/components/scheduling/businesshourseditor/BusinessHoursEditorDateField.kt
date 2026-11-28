package com.rookhq.monarch.components.scheduling.businesshourseditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.MonarchDatePickerDialog
import com.rookhq.monarch.internal.monarchString

/**
 * A labelled date of MonarchBusinessHoursEditor: a button with the date (or "Choose a date") that opens the
 * Material 3 `DatePickerDialog`, and a Clear button for an optional date. Read-only, the date is text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BusinessHoursEditorDateField(
    date: BusinessHoursEditorDate,
    enabled: Boolean,
    readOnly: Boolean,
    labels: MonarchBusinessHoursEditorLabels,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val chooseDate = monarchString(labels.chooseDate, R.string.monarch_businesshourseditor_choose_date)
    val text = date.value?.let { MonarchBusinessHoursEditorTokens.date(it, locale) } ?: chooseDate
    MonarchFormField(label = date.label, enabled = enabled, errorText = date.error) { controlModifier ->
        if (readOnly) {
            MonarchText(text = text, modifier = controlModifier)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchBusinessHoursEditorTokens.ControlGap)) {
                MonarchButton(text = text, onClick = { picking = true }, modifier = controlModifier, enabled = enabled)
                if (date.clearable && date.value != null) {
                    MonarchButton(
                        text = monarchString(labels.clearDate, R.string.monarch_businesshourseditor_clear_date),
                        onClick = { date.onValueChange(null) },
                        variant = MonarchButtonVariant.Ghost,
                        enabled = enabled,
                    )
                }
            }
        }
    }
    if (picking) BusinessHoursEditorDateDialog(date, labels) { picking = false }
}

@Composable
private fun BusinessHoursEditorDateDialog(
    date: BusinessHoursEditorDate,
    labels: MonarchBusinessHoursEditorLabels,
    onClose: () -> Unit,
) {
    MonarchDatePickerDialog(
        initial = date.value,
        earliest = date.earliest,
        confirmLabel = monarchString(labels.confirm, R.string.monarch_businesshourseditor_confirm),
        cancelLabel = monarchString(labels.cancel, R.string.monarch_businesshourseditor_cancel),
        onPick = date.onValueChange,
        onClose = onClose,
    )
}
