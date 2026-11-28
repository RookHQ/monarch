package com.rookhq.monarch.components.datagrid.editablecell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldLabelVisibility
import com.rookhq.monarch.foundation.MonarchFieldSize
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.internal.MonarchDatePicker
import com.rookhq.monarch.internal.MonarchPickerDialog
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/** The text or number editor: a field with Save and Cancel under it. */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any> EditableCellTextEditor(
    draft: String,
    label: String,
    placeholder: String?,
    numeric: Boolean,
    saveOnFocusLoss: Boolean,
    saving: Boolean,
    failure: String?,
    session: EditableCellSession<T>,
    labels: MonarchEditableCellLabels,
    bare: Boolean,
    modifier: Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // Focus the field when the editor opens, and again when a failed save gives it back.
    LaunchedEffect(saving) { if (!saving) focusRequester.requestFocus() }
    BackHandler(enabled = !saving) { session.cancel() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchEditableCellTokens.Gap)) {
        MonarchTextField(
            value = draft,
            onValueChange = session::edit,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    val now = state.isFocused || state.hasFocus
                    if (focused && !now && saveOnFocusLoss) session.commitDraft()
                    focused = now
                }
                .onPreviewKeyEvent { event ->
                    val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (escape) session.cancel()
                    escape
                },
            label = label,
            labelVisibility = if (bare) {
                MonarchTextFieldLabelVisibility.Hidden
            } else {
                MonarchTextFieldLabelVisibility.Visible
            },
            size = if (bare) MonarchFieldSize.Small else MonarchFieldSize.Medium,
            placeholder = placeholder,
            errorText = failure,
            enabled = !saving,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { session.commitDraft() }),
        )
        // As on the web, the editor is the bare field: Done saves, Back cancels and leaving the field saves. The
        // buttons appear only when leaving the field does not save.
        if (!bare || !saveOnFocusLoss || saving) Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchEditableCellTokens.Gap),
        ) {
            MonarchButton(
                text = session.texts.save,
                onClick = session::commitDraft,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                loading = saving,
                loadingLabel = session.texts.saving,
            )
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_editablecell_cancel),
                onClick = session::cancel,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = !saving,
            )
        }
    }
}

/** The menu of a select cell, opened from the cell. Choosing an item saves it. */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any> EditableCellMenu(
    editor: MonarchEditableCellEditor.Select<T>,
    expanded: Boolean,
    selected: T?,
    required: Boolean,
    noValue: String,
    onChoose: (T?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MonarchTheme.colors
    MonarchMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (!required || selected == null) {
            MonarchMenuItem(onClick = { onChoose(null) }, selected = selected == null) {
                Text(noValue, color = colors.textMuted)
            }
        }
        editor.options.forEach { option ->
            MonarchMenuItem(
                onClick = { onChoose(option.value) },
                enabled = option.enabled,
                selected = option.value == selected,
            ) {
                Text(option.label)
                option.description?.let {
                    Text(
                        it,
                        style = MonarchTheme.typography.caption,
                        color = if (option.enabled) colors.textMuted else colors.textDisabled,
                    )
                }
            }
        }
    }
}

/** The themed date picker of a date cell, limited to the editor's range, with None when not required. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun EditableCellDateDialog(
    editor: MonarchEditableCellEditor.Date,
    value: Long?,
    required: Boolean,
    texts: EditableCellTexts,
    noValue: String,
    onChoose: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value,
        selectableDates = remember(editor) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    MonarchEditableCellTokens.selectable(editor, utcTimeMillis)
            }
        },
    )
    MonarchPickerDialog(
        onDismissRequest = onDismiss,
        confirmLabel = texts.save,
        onConfirm = { onChoose(state.selectedDateMillis) },
        cancelLabel = texts.cancel,
        confirmEnabled = state.selectedDateMillis != null,
        // "None" is a third action next to Cancel and Save, not a button lost under the calendar.
        neutralLabel = noValue.takeIf { !required && value != null },
        onNeutral = { onChoose(null) },
    ) {
        MonarchDatePicker(state = state)
    }
}
