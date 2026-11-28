package dev.monarchui.components.workflow.actioneditor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.forms.multiselect.MonarchMultiSelect
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.textfield.MonarchTextField

/** The input of one parameter. A `null` value from [onValueChange] removes the parameter. */
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun MonarchActionEditorParamInput(
    param: MonarchActionParameter,
    value: MonarchActionValue?,
    errorText: String?,
    enabled: Boolean,
    choose: String,
    onValueChange: (MonarchActionValue?) -> Unit,
) {
    val fill = Modifier.fillMaxWidth()
    when (param.kind) {
        MonarchActionParameterKind.Boolean -> MonarchCheckboxRow(
            checked = (value as? MonarchActionValue.Flag)?.checked == true,
            onCheckedChange = { onValueChange(MonarchActionValue.Flag(it)) },
            label = param.label,
            modifier = fill,
            enabled = enabled,
            description = param.description,
        )
        MonarchActionParameterKind.Select -> ParamSelect(param, value, errorText, enabled, choose, onValueChange)
        MonarchActionParameterKind.MultiSelect,
        MonarchActionParameterKind.Tags,
        -> ParamMany(param, value, errorText, enabled, choose, onValueChange)
        MonarchActionParameterKind.Number -> MonarchTextField(
            value = (value as? MonarchActionValue.Number)?.text.orEmpty(),
            onValueChange = { onValueChange(if (it.isEmpty()) null else MonarchActionValue.Number(it)) },
            modifier = fill,
            label = param.label,
            required = param.required,
            placeholder = param.placeholder,
            helperText = param.description,
            errorText = errorText,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        MonarchActionParameterKind.Text,
        MonarchActionParameterKind.TextArea,
        -> MonarchTextField(
            value = (value as? MonarchActionValue.Text)?.text.orEmpty(),
            onValueChange = { onValueChange(MonarchActionValue.Text(it)) },
            modifier = fill,
            label = param.label,
            required = param.required,
            placeholder = param.placeholder,
            helperText = param.description,
            errorText = errorText,
            enabled = enabled,
            singleLine = param.kind == MonarchActionParameterKind.Text,
        )
    }
}

/** One option, with an empty choice when the parameter is optional. */
@Composable
@Suppress("LongParameterList")
private fun ParamSelect(
    param: MonarchActionParameter,
    value: MonarchActionValue?,
    errorText: String?,
    enabled: Boolean,
    choose: String,
    onValueChange: (MonarchActionValue?) -> Unit,
) {
    val current = (value as? MonarchActionValue.Text)?.text?.takeIf { it.isNotEmpty() }
    val none = if (param.required) emptyList() else listOf(MonarchSelectOption("", param.emptyLabel ?: choose))
    MonarchSelect(
        options = none + param.options,
        selected = current ?: "".takeIf { !param.required },
        onSelect = { onValueChange(if (it.isEmpty()) null else MonarchActionValue.Text(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = param.label,
        placeholder = param.placeholder ?: choose,
        helperText = param.description,
        errorText = errorText,
        enabled = enabled,
        required = param.required,
    )
}

/** Several options, or free tags created from the search text. */
@Composable
@Suppress("LongParameterList")
private fun ParamMany(
    param: MonarchActionParameter,
    value: MonarchActionValue?,
    errorText: String?,
    enabled: Boolean,
    choose: String,
    onValueChange: (MonarchActionValue?) -> Unit,
) {
    val list = (value as? MonarchActionValue.Values)?.values
        ?: listOfNotNull((value as? MonarchActionValue.Text)?.text?.takeIf { it.isNotEmpty() })
    val free = param.kind == MonarchActionParameterKind.Tags
    val known = param.options.map {
        MonarchComboboxOption(it.value, it.label, it.description, enabled = it.enabled)
    }
    val extra = list.filter { v -> param.options.none { it.value == v } }.map { MonarchComboboxOption(it, it) }
    MonarchMultiSelect(
        label = param.label,
        options = extra + known,
        selected = list,
        onSelectedChange = { onValueChange(MonarchActionValue.Values(it)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        required = param.required,
        placeholder = param.placeholder ?: choose,
        helperText = param.description,
        errorText = errorText,
        onCreate = if (free) {
            { typed -> typed.trim().takeIf { it.isNotEmpty() }?.let { MonarchComboboxOption(it, it) } }
        } else {
            null
        },
    )
}
