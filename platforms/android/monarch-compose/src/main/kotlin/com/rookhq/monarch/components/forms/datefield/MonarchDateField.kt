package com.rookhq.monarch.components.forms.datefield

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.formfield.MonarchFieldFrame
import com.rookhq.monarch.components.forms.formfield.monarchFieldName
import com.rookhq.monarch.components.forms.formfield.monarchFieldSemantics
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.forms.textfield.MonarchFieldIconButton
import com.rookhq.monarch.components.forms.textfield.MonarchFieldTrigger
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldLabelVisibility
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.internal.MonarchDatePickerDialog
import com.rookhq.monarch.internal.monarchString

/**
 * A date in a form: the Monarch field box with the date (or a placeholder) and a calendar icon, which opens
 * the platform's date picker dialog in the Monarch theme.
 *
 * Looks like Monarch, behaves like the platform: the label sits above the 48dp box, as in every Monarch field;
 * the calendar is the Material 3 `DatePickerDialog`, which formats and parses in the person's locale and has
 * its own typed input mode. Dates are epoch milliseconds at midnight UTC, as `DatePickerState` gives them; the
 * field shows them in the locale's medium format, read at UTC so the day never shifts.
 *
 * TalkBack reads the label (and "required"), the date or the placeholder as the state, "Button", the error,
 * and "double-tap to choose a date". The clear button is its own 48dp button, "Clear date".
 *
 * @param value the chosen date in epoch milliseconds at midnight UTC, or `null` for none.
 * @param onValueChange called with the new date when the person confirms the dialog, or `null` when they clear it.
 * @param modifier the modifier of the field.
 * @param label the visible label above the field. Give one, or give it with [labelVisibility] `Hidden`.
 * @param placeholder shown while there is no date. `null` uses "Choose a date" (`monarch_datefield_placeholder`).
 * @param helperText guidance below the field.
 * @param errorText the error message; the border turns red and TalkBack announces it.
 * @param enabled whether the date can be changed.
 * @param required marks the field as required: an asterisk after the label, "required" after the spoken name.
 * @param earliest the first date that can be chosen, in epoch milliseconds at midnight UTC, or `null` for none.
 * @param clearable shows a clear button while there is a date, for an optional date.
 * @param labelVisibility whether [label] is drawn. `Hidden` keeps it as the TalkBack name only.
 * @param requiredLabel replaces the spoken name of a required field.
 * @param confirmLabel replaces the dialog's "OK".
 * @param cancelLabel replaces the dialog's "Cancel".
 * @param clearLabel replaces "Clear date".
 * @param size the height step: 40dp `Small`, 48dp `Medium` (default) or 52dp `Large`. The hit area stays 48dp.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchDateField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    required: Boolean = false,
    earliest: Long? = null,
    clearable: Boolean = false,
    labelVisibility: MonarchTextFieldLabelVisibility = MonarchTextFieldLabelVisibility.Visible,
    requiredLabel: String? = null,
    confirmLabel: String? = null,
    cancelLabel: String? = null,
    clearLabel: String? = null,
    size: MonarchFieldSize = MonarchFieldSize.Medium,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val placeholderText = monarchString(placeholder, R.string.monarch_datefield_placeholder)
    val shown = value?.let { MonarchDateFieldTokens.format(it, locale) } ?: placeholderText
    val openLabel = monarchString(null, R.string.monarch_datefield_open)
    val clearText = monarchString(clearLabel, R.string.monarch_datefield_clear)
    val name = monarchFieldName(label, required, requiredLabel)
    MonarchFieldFrame(
        modifier = modifier,
        label = label,
        labelVisible = labelVisibility == MonarchTextFieldLabelVisibility.Visible,
        required = required,
        enabled = enabled,
        helperText = helperText,
        errorText = errorText,
    ) {
        MonarchFieldTrigger(
            text = shown,
            isPlaceholder = value == null,
            modifier = Modifier
                .clickable(enabled = enabled, onClickLabel = openLabel, role = Role.Button) { picking = true }
                .semantics(mergeDescendants = true) { stateDescription = shown }
                .monarchFieldSemantics(name, errorText),
            expanded = picking,
            enabled = enabled,
            invalid = !errorText.isNullOrBlank(),
            glyph = MonarchFieldGlyph.Calendar,
            extra = if (clearable && value != null && enabled) {
                { DateFieldClear(clearText) { onValueChange(null) } }
            } else {
                null
            },
            size = size,
        )
    }
    if (picking) {
        MonarchDatePickerDialog(
            initial = value,
            earliest = earliest,
            confirmLabel = monarchString(confirmLabel, R.string.monarch_datefield_confirm),
            cancelLabel = monarchString(cancelLabel, R.string.monarch_datefield_cancel),
            onPick = onValueChange,
            onClose = { picking = false },
        )
    }
}

/** The clear button before the calendar icon; its target stops at the icon, so the two never overlap. */
@Composable
private fun DateFieldClear(name: String, onClear: () -> Unit) {
    MonarchFieldIconButton(name = name, onClick = onClear, bleedEnd = false) {
        MonarchFieldGlyphIcon(MonarchFieldGlyph.Close)
    }
}
