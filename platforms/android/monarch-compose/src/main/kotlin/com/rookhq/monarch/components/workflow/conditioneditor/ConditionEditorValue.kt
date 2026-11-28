package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.forms.multiselect.MonarchMultiSelect
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.MonarchDatePicker
import com.rookhq.monarch.internal.MonarchPickerDialog
import java.util.Date
import java.util.Locale

/** Picks the input for a field and an operator, as the web does. */
internal fun monarchConditionEditorInput(
    field: MonarchConditionField?,
    operator: MonarchConditionOperator,
): MonarchConditionEditorInput {
    val type = field?.type
    val many = operator.takesList || type == MonarchConditionFieldType.MultiSelect ||
        type == MonarchConditionFieldType.Tags
    return when {
        many -> MonarchConditionEditorInput.Many
        field?.options.orEmpty().isNotEmpty() -> MonarchConditionEditorInput.One
        type == MonarchConditionFieldType.Date -> MonarchConditionEditorInput.Day
        type == MonarchConditionFieldType.Number -> MonarchConditionEditorInput.Number
        else -> MonarchConditionEditorInput.Text
    }
}

/** The value input of a condition, chosen by [monarchConditionEditorInput]. */
@Composable
@Suppress("LongParameterList")
internal fun MonarchConditionEditorValue(
    condition: MonarchCondition,
    field: MonarchConditionField?,
    errorText: String?,
    enabled: Boolean,
    text: MonarchConditionEditorText,
    onValueChange: (MonarchConditionValue) -> Unit,
) {
    val value = condition.value
    val options = field?.options.orEmpty()
    val fill = Modifier.fillMaxWidth()
    when (monarchConditionEditorInput(field, condition.operator)) {
        MonarchConditionEditorInput.Many -> {
            val chosen = value?.asList.orEmpty()
            val free = field?.type == MonarchConditionFieldType.Tags || options.isEmpty()
            val known = options.map { MonarchComboboxOption(it.value, it.label, it.description, enabled = it.enabled) }
            val extra = chosen.filter { v -> options.none { it.value == v } }
                .map { MonarchComboboxOption(it, if (free) it else text.removedOption(it)) }
            MonarchMultiSelect(
                label = text.value,
                options = extra + known,
                selected = chosen,
                onSelectedChange = { onValueChange(MonarchConditionValue.Values(it)) },
                modifier = fill,
                enabled = enabled,
                placeholder = text.choose,
                errorText = errorText,
                onCreate = if (free) {
                    { typed -> typed.trim().takeIf { it.isNotEmpty() }?.let { MonarchComboboxOption(it, it) } }
                } else {
                    null
                },
            )
        }
        MonarchConditionEditorInput.One -> {
            val current = value?.asList?.firstOrNull()
            val stale = current != null && options.none { it.value == current }
            MonarchSelect(
                options = if (stale) {
                    listOf(MonarchSelectOption(current, text.removedOption(current))) + options
                } else {
                    options
                },
                selected = current,
                onSelect = { onValueChange(MonarchConditionValue.Text(it)) },
                modifier = fill,
                label = text.value,
                placeholder = text.choose,
                errorText = errorText,
                enabled = enabled,
            )
        }
        MonarchConditionEditorInput.Day -> MonarchConditionEditorDate(
            millis = (value as? MonarchConditionValue.Date)?.millis,
            onMillisChange = { onValueChange(MonarchConditionValue.Date(it)) },
            errorText = errorText,
            enabled = enabled,
            text = text,
        )
        MonarchConditionEditorInput.Number -> MonarchTextField(
            value = (value as? MonarchConditionValue.Number)?.text ?: value?.asList?.firstOrNull().orEmpty(),
            onValueChange = { onValueChange(MonarchConditionValue.Number(it)) },
            modifier = fill,
            label = text.value,
            placeholder = field?.placeholder,
            errorText = errorText,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        MonarchConditionEditorInput.Text -> MonarchTextField(
            value = (value as? MonarchConditionValue.Text)?.text ?: value?.asList?.firstOrNull().orEmpty(),
            onValueChange = { onValueChange(MonarchConditionValue.Text(it)) },
            modifier = fill,
            label = text.value,
            placeholder = field?.placeholder,
            errorText = errorText,
            enabled = enabled,
        )
    }
}

/** A day: a button with the chosen day that opens the themed date picker in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonarchConditionEditorDate(
    millis: Long?,
    onMillisChange: (Long) -> Unit,
    errorText: String?,
    enabled: Boolean,
    text: MonarchConditionEditorText,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    MonarchFormField(label = text.value, enabled = enabled, errorText = errorText) { controlModifier ->
        MonarchButton(
            text = millis?.let { monarchConditionEditorFormatDay(it, locale) } ?: text.chooseDate,
            onClick = { picking = true },
            modifier = controlModifier,
            enabled = enabled,
        )
    }
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        MonarchPickerDialog(
            onDismissRequest = { picking = false },
            confirmLabel = text.confirmDate,
            onConfirm = {
                state.selectedDateMillis?.let(onMillisChange)
                picking = false
            },
            cancelLabel = text.cancel,
            confirmEnabled = state.selectedDateMillis != null,
        ) {
            MonarchDatePicker(state = state)
        }
    }
}

/** A day as the medium date of [locale], read in UTC as the date picker gives it. */
internal fun monarchConditionEditorFormatDay(millis: Long, locale: Locale): String {
    val format = android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.MEDIUM, locale)
    format.timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}
