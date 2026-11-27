package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldTokens
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * An editable cell of the grid: a compact Material 3 field (`BasicTextField` with
 * `OutlinedTextFieldDefaults.DecorationBox`) in the colours of `MonarchTextField`, outlined only while it has
 * focus or an error. Focus selects the text; the
 * text is read when focus leaves or on the Next key, which also moves down; Escape puts the saved value back;
 * with a hardware keyboard the up and down arrows move between rows.
 */
@Composable
internal fun TimesheetGridField(model: TimesheetGridModel, row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay) {
    val saved = row.minutes(day.date)
    val key = model.key(row, day)
    val edit = model.editor.state(key, saved)
    val shown = edit.draft ?: if (saved > 0) model.text.duration(saved) else ""
    var field by remember(shown) { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val colors = MonarchTextFieldTokens.colors()
    val enabled = !edit.busy
    val isError = edit.error != null
    val name = model.cellLabel(row, day)
    val saving = model.text.saving
    val tokens = MonarchTimesheetGridTokens
    BasicTextField(
        value = field,
        onValueChange = { field = it },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .onFocusChanged { state ->
                if (state.isFocused && !focused) field = field.copy(selection = TextRange(0, field.text.length))
                if (focused && !state.isFocused) model.commit(row, day, field.text)
                focused = state.isFocused
            }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && timesheetGridFieldKey(event.key, focusManager) {
                    model.editor.revert(key)
                    val back = if (saved > 0) model.text.duration(saved) else ""
                    field = TextFieldValue(back, TextRange(back.length))
                }
            }
            .semantics {
                contentDescription = name
                if (edit.busy) stateDescription = saving
                edit.error?.let { error(it) }
            },
        enabled = enabled,
        textStyle = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body).copy(
            color = if (enabled) MonarchTheme.colors.textDefault else MonarchTheme.colors.textDisabled,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                model.commit(row, day, field.text)
                focusManager.moveFocus(FocusDirection.Down)
            },
        ),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MonarchTheme.colors.accentStrong),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = field.text,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                placeholder = {
                    Text(
                        text = tokens.EMPTY_MARK,
                        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                        textAlign = TextAlign.Center,
                    )
                },
                colors = colors,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = tokens.FieldPadding,
                    top = tokens.FieldPadding,
                    end = tokens.FieldPadding,
                    bottom = tokens.FieldPadding,
                ),
                // As the web: the cell reads as plain text until it is focused or has an error.
                container = {
                    if (focused || isError) {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = MonarchTheme.shapes.md,
                        )
                    }
                },
            )
        },
    )
}

/** Handles a hardware key in a grid field: Escape runs [revert], the up and down arrows move between rows. */
private fun timesheetGridFieldKey(key: Key, focusManager: FocusManager, revert: () -> Unit): Boolean = when (key) {
    Key.Escape -> {
        revert()
        true
    }
    Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
    Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
    else -> false
}
