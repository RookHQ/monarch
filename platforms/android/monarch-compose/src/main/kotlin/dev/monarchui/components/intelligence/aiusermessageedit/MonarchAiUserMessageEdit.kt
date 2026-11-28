package dev.monarchui.components.intelligence.aiusermessageedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.forms.textfield.MonarchTextFieldLabelVisibility
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * An inline editor for a prompt the person already sent: a multi-line field,
 * a note that resending starts a new branch, and Cancel and Send buttons.
 *
 * A multi-line `MonarchTextField` with two
 * `MonarchButton`s. The field starts with [original], grows from two lines to
 * [maxLines] and then scrolls. Send is disabled until the trimmed text is not
 * empty and differs from the trimmed original. Return adds a line, as in every
 * multi-line field on Android; with a hardware keyboard Ctrl+Enter sends and
 * Escape cancels. The text is held by the component and starts again from
 * [original] when [original] changes.
 *
 * @param original the prompt as it was sent. The field starts with it.
 * @param onSubmit called with the trimmed text when the user sends it.
 * @param onCancel called when the user cancels. Leave edit mode here.
 * @param modifier the modifier of the editor.
 * @param enabled whether the field and the buttons can be used, for example `false` while a reply streams.
 * @param maxLines the lines the field grows to before it scrolls. At least 2.
 * @param focusOnAppear moves focus into the field, with the caret at the end, when the editor appears.
 * @param label the TalkBack label of the field. `null` uses `monarch_aiusermessageedit_label`, "Edit message".
 * @param branchHint the note under the field. `null` uses `monarch_aiusermessageedit_branch_hint`.
 * @param sendLabel the title of Send. `null` uses `monarch_aiusermessageedit_send`.
 * @param cancelLabel the title of Cancel. `null` uses `monarch_aiusermessageedit_cancel`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiUserMessageEdit(
    original: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLines: Int = 10,
    focusOnAppear: Boolean = true,
    label: String? = null,
    branchHint: String? = null,
    sendLabel: String? = null,
    cancelLabel: String? = null,
) {
    val fieldLabel = monarchString(label, R.string.monarch_aiusermessageedit_label)
    val hint = monarchString(branchHint, R.string.monarch_aiusermessageedit_branch_hint)
    var value by rememberSaveable(original, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original, TextRange(original.length)))
    }
    val submission = MonarchAiUserMessageEditTokens.submission(value.text, original)
    val canSubmit = enabled && submission != null
    val submit = { if (enabled) submission?.let(onSubmit) }
    val focus = remember { FocusRequester() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchAiUserMessageEditTokens.Gap),
    ) {
        MonarchTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Escape -> {
                            onCancel()
                            true
                        }
                        event.key == Key.Enter && (event.isCtrlPressed || event.isMetaPressed) -> {
                            submit()
                            true
                        }
                        else -> false
                    }
                },
            label = fieldLabel,
            enabled = enabled,
            singleLine = false,
            minLines = MonarchAiUserMessageEditTokens.MIN_LINES,
            maxLines = MonarchAiUserMessageEditTokens.maxLines(maxLines),
            labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
        )
        // The web marks the note with an info glyph.
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiUserMessageEditTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Info() }
            }
            Text(text = hint, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchAiUserMessageEditTokens.Gap, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(MonarchAiUserMessageEditTokens.Gap),
        ) {
            MonarchButton(
                text = monarchString(cancelLabel, R.string.monarch_aiusermessageedit_cancel),
                onClick = onCancel,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            )
            MonarchButton(
                text = monarchString(sendLabel, R.string.monarch_aiusermessageedit_send),
                onClick = { submit() },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                enabled = canSubmit,
            )
        }
    }
    LaunchedEffect(Unit) { if (focusOnAppear && enabled) focus.requestFocus() }
}
