package com.rookhq.monarch.components.forms.select

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.formfield.MonarchFieldFrame
import com.rookhq.monarch.components.forms.formfield.monarchFieldSemantics
import com.rookhq.monarch.components.forms.textfield.MonarchFieldTrigger
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.internal.field.monarchFieldTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Picks one value from a short, fixed list: a read-only field that opens a
 * menu of the options.
 *
 * The trigger is Monarch's field box with a chevron and the label above it, the same anatomy as
 * `MonarchTextField`. The menu stays the platform's Material 3 `ExposedDropdownMenuBox` and
 * `ExposedDropdownMenu`, themed with Monarch shape and colours: Material keeps the expand and collapse
 * actions, the menu's focus handling, keyboard navigation and placement, which TalkBack and a keyboard
 * rely on. Monarch adds the placeholder, helper and error text, group headings, option descriptions,
 * disabled options, and an accent check on the chosen option, which TalkBack reads as selected.
 *
 * For a long list or one the person searches, use `MonarchCombobox`.
 *
 * @param options the options, in order. Consecutive options with the same group share a heading.
 * @param selected the value of the chosen option, or `null` for no choice yet.
 * @param onSelect called with the value of the option the person chooses.
 * @param modifier the modifier of the field.
 * @param label the visible label, above the field.
 * @param placeholder replaces the built-in "Choose an option" (`monarch_select_placeholder`), shown in the
 * muted colour while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message. When not `null` the field is in the error state, the message replaces
 * [helperText] and TalkBack reads it with the field.
 * @param enabled whether the menu can be opened.
 * @param contentDescription the name TalkBack reads for the field when there is no visible [label], for
 * example a select in a table row or a toolbar whose visible label sits elsewhere. `null` lets the label name it.
 * @param initiallyOpen opens the list when the field first appears, as the web example does.
 * @param required shows the required mark after the label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <T> MonarchSelect(
    options: List<MonarchSelectOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    initiallyOpen: Boolean = false,
    required: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    val chosen = MonarchSelectTokens.chosen(options, selected)
    val open = expanded && enabled
    val shown = chosen?.label ?: monarchString(placeholder, R.string.monarch_select_placeholder)
    val name = contentDescription ?: label
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        MonarchFieldFrame(
            label = label,
            required = required,
            enabled = enabled,
            helperText = helperText,
            errorText = errorText,
        ) {
            MonarchFieldTrigger(
                text = shown,
                isPlaceholder = chosen == null,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .semantics(mergeDescendants = true) {
                        stateDescription = shown
                        // The anchor only drops its click when disabled; TalkBack must still hear "disabled".
                        if (!enabled) disabled()
                    }
                    .monarchFieldSemantics(name, errorText),
                expanded = open,
                enabled = enabled,
                invalid = !errorText.isNullOrBlank(),
            )
        }
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { expanded = false },
            shape = MonarchTheme.shapes.lg,
            containerColor = MonarchTheme.colors.bgRaised,
            border = BorderStroke(MonarchControlMetrics.BorderWidth, MonarchTheme.colors.borderSubtle),
        ) {
            MonarchSelectTokens.sections(options).forEach { section ->
                section.group?.let { SelectHeader(it) }
                section.options.forEach { option ->
                    SelectItem(option, isSelected = option.value == selected) {
                        onSelect(option.value)
                        expanded = false
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(
                horizontal = MonarchSelectTokens.HeaderHorizontalPadding,
                vertical = MonarchSelectTokens.HeaderVerticalPadding,
            )
            .semantics { heading() },
        style = MonarchTheme.typography.caption,
        color = MonarchSelectTokens.headerColor(MonarchTheme.colors),
    )
}

@Composable
private fun <T> SelectItem(option: MonarchSelectOption<T>, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    MonarchMenuRow(
        selected = isSelected,
        enabled = option.enabled,
        onClick = onClick,
        mark = isSelected,
    ) {
        Text(
            option.label,
            style = monarchFieldTextStyle(),
            color = MonarchSelectTokens.valueColor(true, option.enabled, colors),
        )
        option.description?.let {
            Text(
                it,
                style = MonarchTheme.typography.caption,
                color = MonarchSelectTokens.descriptionColor(option.enabled, colors),
            )
        }
    }
}
