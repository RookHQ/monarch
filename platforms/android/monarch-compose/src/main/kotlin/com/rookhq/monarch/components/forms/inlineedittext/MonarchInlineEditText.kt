package com.rookhq.monarch.components.forms.inlineedittext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.MonarchEditSession
import com.rookhq.monarch.internal.MonarchEditSessionState
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Text that turns into a field in place, with an asynchronous save, a saving
 * state and an error that keeps the editor open.
 *
 * In view mode it is the value as text, one button that opens the editor. In
 * edit mode it is a `MonarchTextField` (the Material 3 `OutlinedTextField`)
 * with Save and Cancel `MonarchButton`s under it. The Done key saves a single
 * line, a hardware Escape cancels, and Ctrl or Cmd with Enter saves several
 * lines. While [onSave] runs the field is disabled and Save shows progress;
 * when it throws, the editor stays open with the message and the draft.
 *
 * The saved value is hoisted: [onSave] stores the text and the app passes the
 * new [value]. It is not called when the trimmed text did not change.
 *
 * @param value the saved text.
 * @param onSave saves the new text. Throw to keep the editor open; the exception's message is shown.
 * @param label what the text is, for example "Title". The field's label, and the spoken name in view mode.
 * @param modifier the modifier of the component.
 * @param placeholder shown in view mode while the value is empty, and as the field's placeholder.
 * @param errorText an error from the app, for example an edit conflict, shown while editing.
 * @param enabled whether the text can be edited. When `false` it is plain text in the disabled colour.
 * @param multiline uses a field of several lines: Enter adds a line and Save saves.
 * @param startEditing opens the editor when the component first appears, for a new item.
 * @param trim trims the text before comparing and saving.
 * @param saveOnFocusLoss saves when focus leaves the field.
 * @param validate returns a message to refuse the text before saving, or `null` to allow it.
 * @param errorMessage turns a failed save into a message. By default the exception's message, or "Couldn't save.
 * Try again."
 * @param onCancel called when the user cancels.
 * @param onEditingChange called when the editor opens or closes.
 * @param editIcon an icon after the text in view mode, for example a pencil. It is decoration.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchInlineEditText(
    value: String,
    onSave: suspend (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    multiline: Boolean = false,
    startEditing: Boolean = false,
    trim: Boolean = true,
    saveOnFocusLoss: Boolean = true,
    validate: ((String) -> String?)? = null,
    errorMessage: ((Throwable) -> String?)? = null,
    onCancel: (() -> Unit)? = null,
    onEditingChange: ((Boolean) -> Unit)? = null,
    editIcon: (@Composable () -> Unit)? = null,
    labels: MonarchInlineEditTextLabels = MonarchInlineEditTextLabels(),
) {
    val editing = rememberSaveable { mutableStateOf(startEditing && enabled) }
    val draft = rememberSaveable { mutableStateOf(value) }
    val saving = remember { mutableStateOf(false) }
    val failure = remember { mutableStateOf<String?>(null) }
    val session = InlineEditSession(
        value = value,
        options = SessionOptions(trim, validate, errorMessage, onSave, onCancel),
        edit = MonarchEditSession(
            MonarchEditSessionState(editing, draft, saving, failure),
            rememberCoroutineScope(),
            onEditingChange,
        ),
        saveFailed = monarchString(labels.saveFailed, R.string.monarch_inlineedittext_save_failed),
    )

    if (editing.value && enabled) {
        Editor(
            draft = draft.value,
            onDraftChange = session::edit,
            label = label,
            placeholder = placeholder,
            errorText = failure.value ?: errorText,
            multiline = multiline,
            saving = saving.value,
            saveOnFocusLoss = saveOnFocusLoss,
            labels = labels,
            actions = EditorActions(save = session::save, cancel = session::cancel),
            modifier = modifier,
        )
    } else {
        Viewer(
            value = value,
            label = label,
            placeholder = placeholder,
            enabled = enabled,
            editIcon = editIcon,
            editHint = monarchString(labels.editHint, R.string.monarch_inlineedittext_edit),
            onOpen = session::open,
            modifier = modifier,
        )
    }
}

/** What the app passed that the session needs. */
private class SessionOptions(
    val trim: Boolean,
    val validate: ((String) -> String?)?,
    val errorMessage: ((Throwable) -> String?)?,
    val onSave: suspend (String) -> Unit,
    val onCancel: (() -> Unit)?,
)

/** Opens, saves and cancels an inline editor, on the shared [MonarchEditSession]. */
private class InlineEditSession(
    private val value: String,
    private val options: SessionOptions,
    private val edit: MonarchEditSession,
    private val saveFailed: String,
) {
    fun open() = edit.open(value)

    fun edit(text: String) = edit.edit(text)

    fun cancel() {
        if (edit.saving) return
        edit.close()
        options.onCancel?.invoke()
    }

    fun save() {
        if (edit.saving || !edit.editing) return
        val next = MonarchInlineEditTextTokens.normalise(edit.state.draft.value, options.trim)
        val refused = options.validate?.invoke(next)
        when {
            next == MonarchInlineEditTextTokens.normalise(value, options.trim) -> edit.setEditing(false)
            refused != null -> edit.refuse(refused)
            else -> edit.persist(message = { options.errorMessage?.invoke(it) ?: it.message ?: saveFailed }) {
                options.onSave(next)
            }
        }
    }
}

/** What the buttons and keys of the editor do. */
private class EditorActions(val save: () -> Unit, val cancel: () -> Unit)

@Composable
private fun Viewer(
    value: String,
    label: String,
    placeholder: String?,
    enabled: Boolean,
    editIcon: (@Composable () -> Unit)?,
    editHint: String,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val tokens = MonarchInlineEditTextTokens
    val colors = MonarchTheme.colors
    val shown = value.ifEmpty { placeholder.orEmpty() }
    val shape = MonarchTheme.shapes.sm
    val interaction = if (enabled) {
        Modifier
            .monarchFocusRing(shape)
            .clip(shape)
            .clickable(onClickLabel = editHint, role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = label
                stateDescription = shown
            }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .heightIn(min = tokens.MinSize)
            .widthIn(min = tokens.MinSize)
            .then(interaction)
            .padding(vertical = tokens.ViewPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shown,
            // In view mode the button carries the label and the value; the text itself is not read twice.
            modifier = if (enabled) Modifier.weight(1f, fill = false).clearAndSetSemantics { } else Modifier,
            style = MonarchTheme.typography.body,
            color = tokens.viewText(value.isEmpty(), enabled, colors),
        )
        if (editIcon != null && enabled) {
            Box(modifier = Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(LocalContentColor provides tokens.icon(enabled, colors), content = editIcon)
            }
        }
    }
}

@Composable
private fun Editor(
    draft: String,
    onDraftChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    errorText: String?,
    multiline: Boolean,
    saving: Boolean,
    saveOnFocusLoss: Boolean,
    labels: MonarchInlineEditTextLabels,
    actions: EditorActions,
    modifier: Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    // Focus the field when the editor opens, and again when a failed save gives it back.
    LaunchedEffect(saving) { if (!saving) focusRequester.requestFocus() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchInlineEditTextTokens.Gap)) {
        MonarchTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    val now = state.isFocused || state.hasFocus
                    if (focused && !now && saveOnFocusLoss) actions.save()
                    focused = now
                }
                .onPreviewKeyEvent { event ->
                    val command = event.isCtrlPressed || event.isMetaPressed
                    handleKey(event.type, event.key, command, multiline, actions)
                },
            label = label,
            placeholder = placeholder,
            errorText = errorText,
            enabled = !saving,
            singleLine = !multiline,
            keyboardOptions = KeyboardOptions(imeAction = if (multiline) ImeAction.Default else ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.save() }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchInlineEditTextTokens.Gap)) {
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_inlineedittext_save),
                onClick = actions.save,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                loading = saving,
                loadingLabel = monarchString(labels.saving, R.string.monarch_inlineedittext_saving),
            )
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_inlineedittext_cancel),
                onClick = actions.cancel,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = !saving,
            )
        }
    }
}

/** Escape cancels; Ctrl or Cmd with Enter saves several lines. Returns whether the key was used. */
private fun handleKey(
    type: KeyEventType,
    key: Key,
    command: Boolean,
    multiline: Boolean,
    actions: EditorActions,
): Boolean = when {
    type != KeyEventType.KeyDown -> false
    key == Key.Escape -> {
        actions.cancel()
        true
    }
    multiline && command && key == Key.Enter -> {
        actions.save()
        true
    }
    else -> false
}
