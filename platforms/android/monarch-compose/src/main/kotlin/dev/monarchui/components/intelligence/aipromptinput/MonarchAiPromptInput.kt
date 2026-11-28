package dev.monarchui.components.intelligence.aipromptinput

import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTray
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import dev.monarchui.components.intelligence.aistopbutton.MonarchAiStopButton
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The prompt box of an AI conversation: text that grows with the prompt, a
 * Send button that becomes Stop while a response runs, attachments, a slot
 * for slash commands and mentions, a toolbar, a character counter and a
 * token estimate.
 *
 * Built by Monarch on a Foundation `BasicTextField` with a hoisted
 * `TextFieldState`, so the app owns the draft and can save and restore it.
 * The text grows from [minLines] to [maxLines] and then scrolls. The
 * on-screen keyboard's Enter adds a line and Send sends; on a hardware
 * keyboard Enter (or Ctrl+Enter, see [submitOn]) sends and Escape stops a
 * running response.
 *
 * Keyboard avoidance: with [keyboardPadding] the component pads itself by
 * the on-screen keyboard (`Modifier.imePadding()`), so a prompt docked at the
 * bottom of the screen stays above the keyboard; the padding is consumed, so
 * a parent that already pads by the keyboard is not doubled.
 *
 * Files: [onAttach] adds the attach button, which opens the app's picker.
 * [onReceiveFiles] takes files that reach the field without a picker, through
 * `Modifier.contentReceiver`: images sent by the keyboard, pasted from the
 * clipboard or dropped onto the box, filtered by [accept]. Pasted text is
 * still typed as text. The app uploads the files and passes them back in
 * [attachments], shown by `MonarchAiAttachmentTray` inside the box.
 *
 * Sending clears the text at once and calls [onSubmit]. If it throws, the
 * text comes back unless something new was typed. Return from it when the
 * request has started and show the running response with [busy].
 *
 * @param label the accessible name of the text field, such as "Message the assistant". Shown above the box
 * with [showLabel].
 * @param state the text. The app owns it, so it can save and restore drafts.
 * @param onSubmit receives the text as typed when the person sends. Throw to give the text back.
 * @param modifier the modifier of the prompt input.
 * @param enabled off, nothing can be typed or sent; [disabledReason] says why.
 * @param busy a response is running: Send becomes Stop and Escape calls [onStop].
 * @param showLabel shows [label] above the box. TalkBack reads it either way.
 * @param submitOn which hardware key sends.
 * @param minLines the fewest visible lines.
 * @param maxLines the lines the text grows to before it scrolls.
 * @param maxLength the most characters. Shows the counter; over the limit sending is off and the text is
 * never cut.
 * @param warnAt the share of [maxLength] at which the counter turns to the warning colour.
 * @param countTokens estimates the tokens of the text for the "≈ N tokens" readout. Called on every change,
 * so keep it cheap.
 * @param attachments the attached files, in the order they were added. Allow sending without text.
 * @param accept the MIME types [onReceiveFiles] takes, such as `image/` and a star.
 * @param keyboardPadding pads the component by the on-screen keyboard.
 * @param placeholder shown while the text is empty. The built-in "Send a message…" without it.
 * @param disabledReason why the input is disabled, shown under it while [enabled] is `false`.
 * @param focusRequester moves focus to the text field, for example when the panel that holds the input opens.
 * Attached to the field itself, not to the box, so the field gets the focus and the keyboard.
 * @param labels replacements for the built-in strings.
 * @param onStop stops the running response, from the Stop button and Escape. Without it Stop is disabled.
 * @param onAttach adds the attach button. Open the app's file or photo picker.
 * @param onReceiveFiles receives the content URIs of files sent by the keyboard, pasted or dropped.
 * @param onRemoveAttachment removes an attachment by id; take it out of [attachments].
 * @param onRetryAttachment retries a failed upload by id.
 * @param attachIcon the attach button's icon, a `MonarchIcon` with a `null` description. Text without it.
 * @param sendIcon the Send button's icon. The button shows "Send" as text without it, and is named Send
 * either way.
 * @param triggerMenu a menu shown above the box, given the text with its caret and a way to change both,
 * such as `MonarchAiSlashCommands` or `MonarchAiMentionContext`.
 * @param toolbar controls in the bar under the text, after the attach button, such as a model picker or
 * `MonarchAiPromptToolbar`.
 * @param footer under the box, such as a disclaimer.
 * @param leadingIcon an icon at the start of the text, such as the AI robot of an inline prompt.
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiPromptInput(
    label: String,
    state: TextFieldState,
    onSubmit: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    showLabel: Boolean = false,
    submitOn: MonarchAiPromptInputSubmitOn = MonarchAiPromptInputSubmitOn.Enter,
    minLines: Int = 1,
    maxLines: Int = MonarchAiPromptInputTokens.MAX_LINES,
    maxLength: Int? = null,
    warnAt: Float = MonarchAiPromptInputTokens.WARN_AT,
    countTokens: ((CharSequence) -> Int)? = null,
    attachments: List<MonarchAiAttachmentTrayFile> = emptyList(),
    accept: List<String> = MonarchAiPromptInputTokens.DefaultAccept,
    keyboardPadding: Boolean = true,
    placeholder: String? = null,
    disabledReason: String? = null,
    focusRequester: FocusRequester? = null,
    labels: MonarchAiPromptInputLabels = MonarchAiPromptInputLabels(),
    onStop: (() -> Unit)? = null,
    onAttach: (() -> Unit)? = null,
    onReceiveFiles: ((List<Uri>) -> Unit)? = null,
    onRemoveAttachment: ((String) -> Unit)? = null,
    onRetryAttachment: ((String) -> Unit)? = null,
    attachIcon: (@Composable () -> Unit)? = null,
    sendIcon: (@Composable () -> Unit)? = null,
    triggerMenu: (@Composable (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) -> Unit)? = null,
    toolbar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchAiPromptInputTokens
    val colors = MonarchTheme.colors
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf("") }
    val text = state.text
    val count = text.length
    val limit = tokens.limit(count, maxLength, warnAt)
    val canSubmit = tokens.canSubmit(text, enabled, busy, submitting, limit, attachments.isNotEmpty())
    val submitHandler by rememberUpdatedState(onSubmit)

    val warning: (Int) -> String = labels.counterWarning
        ?: { left -> resources.getQuantityString(R.plurals.monarch_aipromptinput_counter_warning, left, left) }
    val over: (Int) -> String = labels.counterOver
        ?: { excess -> resources.getQuantityString(R.plurals.monarch_aipromptinput_counter_over, excess, excess) }
    var previousLimit by remember { mutableStateOf(limit) }
    LaunchedEffect(limit) {
        tokens.limitAnnouncement(previousLimit, limit, count, maxLength, warning, over)?.let { announcement = it }
        previousLimit = limit
    }

    val submit: () -> Unit = {
        if (canSubmit) {
            val written = state.text.toString()
            state.clearText()
            submitting = true
            scope.launch {
                val error = runCatching { submitHandler(written) }.exceptionOrNull()
                submitting = false
                if (error is CancellationException) throw error
                if (error != null && state.text.isEmpty()) state.setTextAndPlaceCursorAtEnd(written)
            }
        }
    }
    val stop: () -> Unit = { onStop?.invoke() }

    Column(
        modifier = modifier.then(if (keyboardPadding) Modifier.imePadding() else Modifier),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (showLabel) MonarchText(text = label, style = MonarchTextStyle.Label)
        if (triggerMenu != null) {
            val value = TextFieldValue(state.text.toString(), state.selection)
            triggerMenu(value) { next ->
                state.edit {
                    replace(0, length, next.text)
                    selection = TextRange(next.selection.start, next.selection.end)
                }
            }
        }
        val shape = MonarchShapeDefaults.Container
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onReceiveFiles != null && enabled) {
                            Modifier.aiPromptInputReceiver(accept, onReceiveFiles) { dragging = it }
                        } else {
                            Modifier
                        },
                    )
                    .monarchFocusRing(shape)
                    .monarchSurface(
                        tokens.boxColors(enabled, limit == AiPromptInputLimit.Over, dragging, colors),
                        shape,
                    )
                    .padding(tokens.BoxPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
            ) {
                if (attachments.isNotEmpty()) {
                    MonarchAiAttachmentTray(
                        files = attachments,
                        onRemove = { id -> onRemoveAttachment?.invoke(id) },
                        enabled = enabled && onRemoveAttachment != null,
                        onRetry = onRetryAttachment,
                        labels = labels.attachments,
                    )
                }
                val overError = if (limit == AiPromptInputLimit.Over && maxLength != null) {
                    over(count - maxLength)
                } else {
                    null
                }
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .fillMaxWidth()
                        .heightIn(min = MonarchTouchTarget.Minimum)
                        .padding(tokens.FieldPadding)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val press = AiPromptInputKey(
                                key = event.key,
                                shift = event.isShiftPressed,
                                command = event.isCtrlPressed || event.isMetaPressed,
                                hardware = event.nativeKeyEvent.flags and KeyEvent.FLAG_SOFT_KEYBOARD == 0,
                            )
                            when (MonarchAiPromptInputRules.keyAction(press, submitOn, busy, onStop != null)) {
                                AiPromptInputKeyAction.Submit -> submit()
                                AiPromptInputKeyAction.Stop -> stop()
                                AiPromptInputKeyAction.None -> return@onPreviewKeyEvent false
                            }
                            true
                        }
                        .semantics {
                            contentDescription = label
                            if (overError != null) error(overError)
                        },
                    enabled = enabled,
                    textStyle = MonarchTheme.typography.body.copy(color = tokens.textColor(enabled, colors)),
                    lineLimits = TextFieldLineLimits.MultiLine(minLines, maxOf(minLines, maxLines)),
                    cursorBrush = SolidColor(tokens.cursorColor(colors)),
                    decorator = { field ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                            verticalAlignment = Alignment.Top,
                        ) {
                        leadingIcon?.let { icon ->
                            CompositionLocalProvider(LocalContentColor provides colors.accentStrong, content = icon)
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholder ?: monarchString(
                                        labels.placeholder,
                                        R.string.monarch_aipromptinput_placeholder,
                                    ),
                                    style = MonarchTheme.typography.body,
                                    color = tokens.placeholderColor(colors),
                                )
                            }
                            field()
                        }
                        }
                    },
                )
                // The controls wrap among themselves; Send stays at the end of the same row, as on the web.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onAttach != null) {
                            AiPromptInputAttachButton(
                                name = monarchString(labels.attach, R.string.monarch_aipromptinput_attach),
                                icon = attachIcon,
                                enabled = enabled,
                                onClick = onAttach,
                            )
                        }
                        if (toolbar != null) Box(Modifier.weight(1f, fill = false)) { toolbar() }
                    }
                    AiPromptInputMeta(text, countTokens, maxLength, limit, labels)
                    if (busy) {
                        MonarchAiStopButton(
                            onStop = stop,
                            enabled = onStop != null,
                            stopLabel = monarchString(labels.stop, R.string.monarch_aipromptinput_stop),
                        )
                    } else {
                        AiPromptInputSendButton(
                            name = monarchString(labels.send, R.string.monarch_aipromptinput_send),
                            icon = sendIcon,
                            enabled = canSubmit,
                            onClick = submit,
                        )
                    }
                }
            }
            if (dragging) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    MonarchText(
                        text = monarchString(labels.dropHint, R.string.monarch_aipromptinput_drop_hint),
                        style = MonarchTextStyle.Label,
                        color = tokens.dropHintColor(colors),
                    )
                }
            }
        }
        if (!enabled && disabledReason != null) {
            MonarchText(text = disabledReason, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
        }
        footer?.invoke()
        // Always present, so the limit warnings are announced as changes of this region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/**
 * Takes files sent by the keyboard, pasted or dropped onto the box when their
 * type matches [accept], and reports whether files are being dragged over it.
 * Everything else, such as pasted text, goes on to the text field.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.aiPromptInputReceiver(
    accept: List<String>,
    onReceiveFiles: (List<Uri>) -> Unit,
    onDragging: (Boolean) -> Unit,
): Modifier = contentReceiver(
    object : ReceiveContentListener {
        override fun onDragEnter() = onDragging(true)

        override fun onDragExit() = onDragging(false)

        override fun onDragEnd() = onDragging(false)

        override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
            val accepted = MonarchAiPromptInputRules.acceptedUris(
                transferableContent.clipEntry.clipData,
                accept,
            )
            if (accepted.isEmpty()) return transferableContent
            onDragging(false)
            onReceiveFiles(accepted)
            return transferableContent.consume { item -> item.uri != null && item.uri in accepted }
        }
    },
)

/** The token estimate and the counter, each read once by TalkBack. */
@Composable
private fun AiPromptInputMeta(
    text: CharSequence,
    countTokens: ((CharSequence) -> Int)?,
    maxLength: Int?,
    limit: AiPromptInputLimit,
    labels: MonarchAiPromptInputLabels,
) {
    val resources = LocalResources.current
    val colors = MonarchTheme.colors
    if (countTokens != null) {
        val estimate = countTokens(text)
        MonarchText(
            text = labels.tokens?.invoke(estimate)
                ?: resources.getQuantityString(R.plurals.monarch_aipromptinput_tokens, estimate, estimate),
            style = MonarchTextStyle.Caption,
            color = MonarchAiPromptInputTokens.mutedColor(colors),
        )
    }
    if (maxLength != null) {
        val count = text.length
        val spoken = labels.counterLabel?.invoke(count, maxLength)
            ?: resources.getQuantityString(R.plurals.monarch_aipromptinput_counter_label, count, count, maxLength)
        MonarchText(
            text = labels.counter?.invoke(count, maxLength)
                ?: resources.getString(R.string.monarch_aipromptinput_counter, count, maxLength),
            modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            style = MonarchTextStyle.Caption,
            color = MonarchAiPromptInputTokens.counterColor(limit, colors),
        )
    }
}

/** The attach button: a named icon button, or a ghost text button until the app gives an icon. */
@Composable
private fun AiPromptInputAttachButton(
    name: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (icon != null) {
        MonarchIconButton(label = name, onClick = onClick, enabled = enabled, icon = icon)
    } else {
        MonarchButton(
            text = name,
            onClick = onClick,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}

/** The primary Send button, with the app's icon or the word Send. */
@Composable
private fun AiPromptInputSendButton(
    name: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (icon != null) {
        MonarchButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        ) {
            Box(Modifier.clearAndSetSemantics { }) { icon() }
        }
    } else {
        MonarchButton(
            text = name,
            onClick = onClick,
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}
