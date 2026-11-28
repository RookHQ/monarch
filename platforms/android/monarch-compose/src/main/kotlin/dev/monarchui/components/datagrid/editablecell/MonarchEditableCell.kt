package dev.monarchui.components.datagrid.editablecell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.internal.MonarchEditSession
import dev.monarchui.internal.MonarchEditSessionState
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A table cell that edits its value in place: text, a number, a choice or a date.
 *
 * In view mode the cell is the formatted value, one button that opens the
 * editor. A text or number cell turns into a `MonarchTextField` (the
 * Material 3 `OutlinedTextField`) with Save and Cancel under it: the Done key
 * and Save save, Cancel, the system back gesture and a hardware Escape cancel,
 * and leaving the field saves. A select cell opens a Material 3
 * `DropdownMenu` from the cell and saves the choice. A date cell opens the
 * Material 3 `DatePickerDialog`, limited to the editor's range.
 *
 * The value is hoisted: [onCommit] stores the new value and the app passes it
 * back as [value]. It is not called when the value did not change. While it
 * runs the cell shows a progress indicator; when it throws, the error is shown
 * and a text or number editor stays open with the draft. Required values,
 * ranges and [validate] refuse a value before it is saved.
 *
 * @param value the saved value, or `null` for an empty cell.
 * @param label what the cell holds, for example "Status for Acme": the spoken name of the cell and the label of
 * its editor.
 * @param editor the editor the cell opens, which also fixes the type of the value.
 * @param onCommit saves the new value. Throw to keep the cell editing with the error; the message is shown.
 * @param modifier the modifier of the cell.
 * @param enabled whether the cell can be edited. When `false` it is plain text in the disabled colour.
 * @param required refuses an empty value with "Enter a value." and leaves out the select cell's None item.
 * @param placeholder shown in view mode while the cell is empty, and as the field's placeholder.
 * @param startEditing opens the editor when the cell first appears, for a new row.
 * @param saveOnFocusLoss saves a text or number when focus leaves the field.
 * @param format writes the value for view mode. By default the text, the number in the locale's format, the
 * option's label or the medium date.
 * @param validate returns a message to refuse a value before it is saved, or `null` to allow it.
 * @param onCancel called when editing is cancelled.
 * @param onEditingChange called when the editor opens or closes.
 * @param labels replacements for the built-in strings.
 * @param bare edits in the bare field the web shows: no visible label, and Save and Cancel only when leaving the field
 * does not save. `false` shows the label above the field and Save and Cancel under it, as in a form.
 */
@Composable
@Suppress("LongParameterList")
public fun <T : Any> MonarchEditableCell(
    value: T?,
    label: String,
    editor: MonarchEditableCellEditor<T>,
    onCommit: suspend (T?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    required: Boolean = false,
    placeholder: String? = null,
    startEditing: Boolean = false,
    saveOnFocusLoss: Boolean = true,
    format: ((T) -> String)? = null,
    validate: ((T?) -> String?)? = null,
    onCancel: (() -> Unit)? = null,
    onEditingChange: ((Boolean) -> Unit)? = null,
    labels: MonarchEditableCellLabels = MonarchEditableCellLabels(),
    bare: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    val state = MonarchEditSessionState(
        editing = rememberSaveable { mutableStateOf(startEditing && enabled) },
        draft = rememberSaveable { mutableStateOf(editableCellDraft(editor, value)) },
        saving = remember { mutableStateOf(false) },
        failure = remember { mutableStateOf<String?>(null) },
    )
    val session = EditableCellSession(
        value = value,
        editor = editor,
        options = EditableCellOptions(required, validate, onCommit, onCancel, locale),
        edit = MonarchEditSession(state, rememberCoroutineScope(), onEditingChange),
        texts = editableCellTexts(labels),
    )
    val editing = state.editing.value && enabled
    val noValue = monarchString(labels.noValue, R.string.monarch_editablecell_no_value)
    val shown = value?.let { format?.invoke(it) ?: editableCellDisplay(editor, it, locale) }
    if (editing && (editor is MonarchEditableCellEditor.Text || editor is MonarchEditableCellEditor.Number)) {
        EditableCellTextEditor(
            draft = state.draft.value,
            label = label,
            placeholder = placeholder,
            numeric = editor is MonarchEditableCellEditor.Number,
            saveOnFocusLoss = saveOnFocusLoss,
            saving = state.saving.value,
            failure = state.failure.value,
            session = session,
            labels = labels,
            bare = bare,
            modifier = modifier,
        )
    } else {
        Column(modifier = modifier) {
            Box {
                EditableCellView(
                    shown = shown,
                    label = label,
                    placeholder = placeholder,
                    numeric = editor is MonarchEditableCellEditor.Number,
                    enabled = enabled,
                    saving = state.saving.value,
                    editHint = labels.edit?.invoke(label) ?: stringResource(R.string.monarch_editablecell_edit, label),
                    savingLabel = session.texts.saving,
                    onOpen = session::open,
                )
                when (editor) {
                    is MonarchEditableCellEditor.Select -> EditableCellMenu(
                        editor = editor,
                        expanded = editing,
                        selected = value,
                        required = required,
                        noValue = noValue,
                        onChoose = session::commit,
                        onDismiss = session::cancel,
                    )
                    is MonarchEditableCellEditor.Date -> if (editing) {
                        EditableCellDateDialog(
                            editor = editor,
                            value = value as Long?,
                            required = required,
                            texts = session.texts,
                            noValue = noValue,
                            onChoose = { session.commit(editor.cast(it)) },
                            onDismiss = session::cancel,
                        )
                    }
                    else -> Unit
                }
            }
            EditableCellError(state.failure.value)
        }
    }
}

/** A date chosen in the dialog, as the cell's value type. */
@Suppress("UNCHECKED_CAST")
private fun <T : Any> MonarchEditableCellEditor<T>.cast(millis: Long?): T? = millis as T?

/** The text an editor starts from: the text, or the number without grouping. */
internal fun <T : Any> editableCellDraft(editor: MonarchEditableCellEditor<T>, value: T?): String = when (editor) {
    is MonarchEditableCellEditor.Number -> MonarchEditableCellTokens.numberDraft(value as Double?)
    else -> value?.toString().orEmpty()
}

/** The default view text of a value. */
internal fun <T : Any> editableCellDisplay(editor: MonarchEditableCellEditor<T>, value: T, locale: Locale): String =
    when (editor) {
        is MonarchEditableCellEditor.Number -> MonarchEditableCellTokens.formatNumber(value as Double, locale)
        is MonarchEditableCellEditor.Date -> MonarchEditableCellTokens.formatDate(value as Long, locale)
        is MonarchEditableCellEditor.Select -> editor.options.firstOrNull { it.value == value }?.label
            ?: value.toString()
        MonarchEditableCellEditor.Text -> value.toString()
    }

/** The resolved built-in strings. */
internal class EditableCellTexts(
    val save: String,
    val cancel: String,
    val saving: String,
    val saveFailed: String,
    val messages: EditableCellMessages,
)

@Composable
private fun editableCellTexts(labels: MonarchEditableCellLabels): EditableCellTexts {
    val belowMin = stringResource(R.string.monarch_editablecell_below_min)
    val aboveMax = stringResource(R.string.monarch_editablecell_above_max)
    val locale = LocalConfiguration.current.locales[0]
    return EditableCellTexts(
        save = monarchString(labels.save, R.string.monarch_editablecell_save),
        cancel = monarchString(labels.cancel, R.string.monarch_editablecell_cancel),
        saving = monarchString(labels.saving, R.string.monarch_editablecell_saving),
        saveFailed = monarchString(labels.saveFailed, R.string.monarch_editablecell_save_failed),
        messages = EditableCellMessages(
            required = monarchString(labels.required, R.string.monarch_editablecell_required),
            invalidNumber = monarchString(labels.invalidNumber, R.string.monarch_editablecell_invalid_number),
            belowMin = labels.belowMin ?: { String.format(locale, belowMin, it) },
            aboveMax = labels.aboveMax ?: { String.format(locale, aboveMax, it) },
        ),
    )
}

/** What the app passed that the session needs. */
internal class EditableCellOptions<T : Any>(
    val required: Boolean,
    val validate: ((T?) -> String?)?,
    val onCommit: suspend (T?) -> Unit,
    val onCancel: (() -> Unit)?,
    val locale: Locale,
)

/** Opens, commits and cancels an editable cell, on the shared [MonarchEditSession]. */
internal class EditableCellSession<T : Any>(
    private val value: T?,
    private val editor: MonarchEditableCellEditor<T>,
    private val options: EditableCellOptions<T>,
    private val edit: MonarchEditSession,
    val texts: EditableCellTexts,
) {
    fun open() {
        if (edit.saving) return
        edit.open(editableCellDraft(editor, value))
    }

    fun edit(text: String) = edit.edit(text)

    fun cancel() {
        if (edit.saving || !edit.editing) return
        edit.close()
        options.onCancel?.invoke()
    }

    /** Commits the draft of a text or number editor. */
    @Suppress("UNCHECKED_CAST")
    fun commitDraft() {
        if (edit.saving || !edit.editing) return
        val draft = edit.state.draft.value
        if (editor is MonarchEditableCellEditor.Number) {
            val parsed = MonarchEditableCellTokens.parseNumber(draft, options.locale)
            parsed.onSuccess { commit(it as T?) }.onFailure { edit.refuse(texts.messages.invalidNumber) }
        } else {
            commit(MonarchEditableCellTokens.textValue(draft) as T?)
        }
    }

    /** Checks and saves [next]. */
    fun commit(next: T?) {
        if (edit.saving) return
        val range = MonarchEditableCellTokens.rangeError(editor, next, options.locale, texts.messages)
        val outcome = MonarchEditableCellTokens.decide(
            next = next,
            current = value,
            required = options.required,
            rangeError = range,
            validate = options.validate,
            messages = texts.messages,
        )
        when (outcome) {
            EditableCellOutcome.Unchanged -> edit.close()
            is EditableCellOutcome.Refused -> edit.refuse(outcome.message)
            // A menu or a dialog closes while the value is saved; a text editor stays open.
            is EditableCellOutcome.Commit -> edit.persist(
                closeWhileSaving = editor !is MonarchEditableCellEditor.Text &&
                    editor !is MonarchEditableCellEditor.Number,
                message = { it.message ?: texts.saveFailed },
            ) { options.onCommit(outcome.value) }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun EditableCellView(
    shown: String?,
    label: String,
    placeholder: String?,
    numeric: Boolean,
    enabled: Boolean,
    saving: Boolean,
    editHint: String,
    savingLabel: String,
    onOpen: () -> Unit,
) {
    val tokens = MonarchEditableCellTokens
    val colors = MonarchTheme.colors
    val empty = shown.isNullOrEmpty()
    val text = if (empty) placeholder.orEmpty() else shown.orEmpty()
    val shape = MonarchTheme.shapes.sm
    val interaction = if (enabled) {
        Modifier
            .monarchFocusRing(shape)
            .clip(shape)
            .clickable(enabled = !saving, onClickLabel = editHint, role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = label
                stateDescription = text
            }
    } else {
        Modifier
    }
    val style = if (numeric) {
        MonarchTheme.typography.body.copy(fontFeatureSettings = MonarchEditableCellTokens.TABULAR_FIGURES)
    } else {
        MonarchTheme.typography.body
    }
    Row(
        modifier = Modifier
            .heightIn(min = tokens.MinSize)
            .widthIn(min = tokens.MinSize)
            .then(interaction)
            .padding(horizontal = tokens.ViewPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            // The button carries the label and the value; the text itself is not read twice.
            modifier = if (enabled) Modifier.weight(1f).clearAndSetSemantics { } else Modifier.weight(1f),
            style = style,
            color = tokens.viewText(empty, enabled, colors),
            textAlign = if (numeric) TextAlign.End else TextAlign.Start,
        )
        if (saving) {
            MonarchProgress(
                modifier = Modifier.size(tokens.SpinnerSize),
                variant = MonarchProgressVariant.Circular,
                label = savingLabel,
            )
        }
    }
}

/** The error of a select or date cell, announced when it appears. */
@Composable
private fun EditableCellError(message: String?) {
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (message != null) contentDescription = message
        },
    ) {
        if (message != null) {
            Text(
                text = message,
                modifier = Modifier
                    .padding(horizontal = MonarchEditableCellTokens.ViewPadding)
                    .clearAndSetSemantics { },
                style = MonarchTheme.typography.caption,
                color = MonarchEditableCellTokens.errorText(MonarchTheme.colors),
            )
        }
    }
}
