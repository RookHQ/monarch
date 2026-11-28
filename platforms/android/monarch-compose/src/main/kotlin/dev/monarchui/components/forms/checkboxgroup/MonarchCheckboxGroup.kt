package dev.monarchui.components.forms.checkboxgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.internal.monarchString

/**
 * A labelled set of checkboxes for picking any number of choices, with an
 * optional tri-state Select all.
 *
 * Built from `MonarchCheckbox` rows (as in
 * `MonarchCheckboxRow`) and a tri-state `MonarchCheckbox` for Select all, inside a
 * `MonarchFormField` when [label] is given. Each row toggles as a whole, is
 * one TalkBack element and is at least 48dp high. Select all is checked when
 * every enabled choice is, partly checked when some are, and toggles every
 * enabled choice at once; disabled choices keep their state. The value is
 * hoisted and always kept in the order of [options].
 *
 * Without [label], the group draws only its choices: put it in your own
 * `MonarchFormField` and pass the field's control modifier as [modifier].
 *
 * @param T the type of the values.
 * @param options the choices, in order.
 * @param value the checked values.
 * @param onValueChange called with every checked value, in the order of [options].
 * @param modifier the modifier of the group.
 * @param orientation one choice per line, or side by side and wrapping.
 * @param enabled whether the user can change the group. `false` disables every choice.
 * @param required marks the label as required. A group cannot enforce a minimum: check the count and set [errorText].
 * @param invalid draws the boxes in the danger colour. Also on whenever [errorText] is given.
 * @param selectAll adds a Select all checkbox above the choices.
 * @param label the visible label of the group, and its TalkBack name.
 * @param helperText help text under the choices. Drawn only with [label].
 * @param errorText the error, shown under the choices and set on the group. Drawn only with [label].
 * @param selectAllLabel replaces the built-in "Select all".
 */
@Composable
public fun <T> MonarchCheckboxGroup(
    options: List<MonarchCheckboxGroupOption<T>>,
    value: List<T>,
    onValueChange: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    orientation: MonarchCheckboxGroupOrientation = MonarchCheckboxGroupOrientation.Vertical,
    enabled: Boolean = true,
    required: Boolean = false,
    invalid: Boolean = false,
    selectAll: Boolean = false,
    label: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    selectAllLabel: String? = null,
) {
    val isInvalid = invalid || !errorText.isNullOrBlank()
    val selectAllText = monarchString(selectAllLabel, R.string.monarch_checkboxgroup_select_all)
    val choices: @Composable (Modifier) -> Unit = { choicesModifier ->
        CheckboxGroupChoices(
            options = options,
            value = value,
            onValueChange = onValueChange,
            modifier = choicesModifier,
            orientation = orientation,
            enabled = enabled,
            invalid = isInvalid,
            selectAllText = selectAllText.takeIf { selectAll },
        )
    }
    if (label == null) {
        choices(modifier)
    } else {
        MonarchFormField(
            label = label,
            modifier = modifier,
            enabled = enabled,
            required = required,
            helperText = helperText,
            errorText = errorText,
            control = choices,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun <T> CheckboxGroupChoices(
    options: List<MonarchCheckboxGroupOption<T>>,
    value: List<T>,
    onValueChange: (List<T>) -> Unit,
    modifier: Modifier,
    orientation: MonarchCheckboxGroupOrientation,
    enabled: Boolean,
    invalid: Boolean,
    selectAllText: String?,
) {
    val tokens = MonarchCheckboxGroupTokens
    Column(modifier = modifier.semantics { isTraversalGroup = true }) {
        if (selectAllText != null) {
            val state = tokens.selectAllState(options, value, enabled)
            val canToggle = tokens.changeable(options, enabled).isNotEmpty()
            MonarchCheckbox(
                state = state,
                onClick = { onValueChange(tokens.toggleAll(options, value, enabled)) },
                enabled = canToggle,
                invalid = invalid,
                label = selectAllText,
            )
        }
        val nested = if (selectAllText != null) Modifier.padding(start = tokens.NestedIndent) else Modifier
        val row: @Composable (MonarchCheckboxGroupOption<T>, Modifier) -> Unit = { option, rowModifier ->
            val checked = option.value in value
            val rowEnabled = enabled && option.enabled
            MonarchCheckbox(
                checked = checked,
                onCheckedChange = { onValueChange(tokens.toggle(options, value, option.value, it)) },
                modifier = rowModifier,
                enabled = rowEnabled,
                invalid = invalid,
                label = option.label,
                description = option.description,
            )
        }
        when (orientation) {
            MonarchCheckboxGroupOrientation.Vertical -> Column(nested) {
                options.forEach { row(it, Modifier) }
            }
            MonarchCheckboxGroupOrientation.Horizontal -> FlowRow(
                modifier = nested,
                horizontalArrangement = Arrangement.spacedBy(tokens.HorizontalGap),
            ) {
                options.forEach { row(it, Modifier.width(IntrinsicSize.Max)) }
            }
        }
    }
}
