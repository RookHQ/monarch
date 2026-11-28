package dev.monarchui.components.collaboration.messagecomposer

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchGlyphs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionItem
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPicker
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionQuery
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatusIndicator
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatusIndicatorLabels
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.field.monarchFieldTextStyle
import dev.monarchui.internal.monarchGhostStartAlign
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The box where people write a message: plain text with @mentions,
 * attachments, an emoji button, reply and edit modes, message types, a
 * character limit, a draft status and a send button.
 *
 * A Monarch pattern built on a Foundation `BasicTextField` with a hoisted
 * `TextFieldState`, so the app owns the text and saves and restores drafts.
 * Around it: `MonarchMentionPicker` above the box, a
 * `MonarchSegmentedControl` for [modes], `MonarchButton`s and `MonarchIconButton`s, and
 * `MonarchSaveStatusIndicator` under the box. The text grows from one to
 * eight lines and then scrolls. The on-screen keyboard's Enter adds a line
 * and the send button sends; on a hardware keyboard Enter (or Ctrl+Enter,
 * see [sendShortcut]) sends, Escape leaves editing and Up in an empty box
 * calls [onEditLast].
 *
 * [onSend] is a suspending function: the send button shows "Sending…" while
 * it runs, the text clears when it returns and stays when it throws, so the
 * app can show its own error.
 *
 * Monarch bundles no emoji picker and no icons: the emoji button appears
 * when the app gives [emojiPicker], and the buttons show their names as text
 * until the app gives icons.
 *
 * @param label the accessible name of the text field, such as "Message #design".
 * @param state the text. The app owns it, so it can save and restore drafts.
 * @param onSend receives the trimmed text, the mentions still in it and the mode. Throw to keep the text.
 * @param modifier the modifier of the composer.
 * @param enabled off, the text cannot be edited and nothing can be sent; [disabledReason] says why.
 * @param editing edits an existing message: an "Editing message" context, Save instead of Send, Cancel with
 * [onCancelEdit], and focus on the field.
 * @param sending shows the sending state, for apps that track sending themselves.
 * @param hasAttachments allows sending without text.
 * @param placeholder shown while the text is empty. A mode's placeholder wins.
 * @param replyTo the message being answered: "Replying to" the author, with a one-line excerpt.
 * @param modes message types as segmented buttons above the box. A note mode gives the box the warning look.
 * @param selectedMode the selected mode's value. The composer keeps the selection, starting with the first mode,
 * when `null`.
 * @param onModeChange receives the value of the mode the person picks.
 * @param mentions mention suggestions. The composer reports the query; the app searches and passes results.
 * @param draftStatus the app's draft save state, shown under the box: "Saving draft…", "Draft saved" or "Draft not
 * saved".
 * @param sendShortcut which hardware key sends.
 * @param maxLength the most characters. Shows the characters left; over the limit the count turns danger and
 * sending is off.
 * @param typingIntervalMillis the least time between two [onTyping] calls.
 * @param disabledReason why the composer is disabled, shown under it while [enabled] is `false`.
 * @param labels replacements for the built-in strings, including the mention picker's.
 * @param onAttach adds the attach button. Open the app's file or photo picker.
 * @param onDismissReply adds a button that removes the reply context.
 * @param onCancelEdit adds Cancel while editing; Escape on a hardware keyboard calls it too.
 * @param onEditLast called by Up on a hardware keyboard in an empty box, to edit the last message.
 * @param onTyping called at most once per [typingIntervalMillis] while the focused text changes.
 * @param onTypingStop called after [onTyping] when the text is emptied or sent, focus leaves, or the composer
 * leaves the screen.
 * @param attachIcon the attach button's icon, a `MonarchIcon` with a `null` description. Text without it.
 * @param emojiIcon the emoji button's icon. Text without it.
 * @param dismissIcon the icon of the button that removes the reply context. Text without it.
 * @param emojiPicker the app's emoji picker, shown while the emoji button is open. Call `onPick` with the emoji,
 * which is inserted at the caret, or `onDismiss` to close it. Adds the emoji button.
 * @param attachments the attachment row above the text, such as `MonarchAttachmentChip`s. Wraps onto new rows.
 * @param actions extra buttons before the send button, such as schedule or templates.
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchMessageComposer(
    label: String,
    state: TextFieldState,
    onSend: suspend (MonarchMessageComposerMessage) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    editing: Boolean = false,
    sending: Boolean = false,
    hasAttachments: Boolean = false,
    placeholder: String? = null,
    replyTo: MonarchMessageComposerReply? = null,
    modes: List<MonarchMessageComposerMode> = emptyList(),
    selectedMode: String? = null,
    onModeChange: ((String) -> Unit)? = null,
    mentions: MonarchMessageComposerMentions? = null,
    draftStatus: MonarchSaveStatus = MonarchSaveStatus.Idle,
    sendShortcut: MonarchMessageComposerSendShortcut = MonarchMessageComposerSendShortcut.Enter,
    maxLength: Int? = null,
    typingIntervalMillis: Long = MonarchMessageComposerTokens.TYPING_INTERVAL_MILLIS,
    disabledReason: String? = null,
    labels: MonarchMessageComposerLabels = MonarchMessageComposerLabels(),
    onAttach: (() -> Unit)? = null,
    onDismissReply: (() -> Unit)? = null,
    onCancelEdit: (() -> Unit)? = null,
    onEditLast: (() -> Unit)? = null,
    onTyping: (() -> Unit)? = null,
    onTypingStop: (() -> Unit)? = null,
    attachIcon: (@Composable () -> Unit)? = null,
    emojiIcon: (@Composable () -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = null,
    emojiPicker: (@Composable (onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit)? = null,
    attachments: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchMessageComposerTokens
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val picked = remember { mutableStateListOf<MessageComposerPicked>() }
    val typing = rememberMessageComposerTyping(state, typingIntervalMillis, onTyping, onTypingStop)
    var busy by remember { mutableStateOf(false) }
    var ownMode by rememberSaveable { mutableStateOf(modes.firstOrNull()?.value) }
    val mode = modes.firstOrNull { it.value == (selectedMode ?: ownMode) } ?: modes.firstOrNull()
    val showSending = sending || busy
    val text = state.text
    val canSend = tokens.canSend(text, enabled, showSending, hasAttachments, maxLength)

    val send: () -> Unit = {
        if (canSend) {
            val written = state.text.toString()
            val message = MonarchMessageComposerMessage(
                text = written.trim(),
                mentions = tokens.mentionsIn(written, picked),
                mode = mode?.value,
            )
            busy = true
            scope.launch {
                val error = runCatching { onSend(message) }.exceptionOrNull()
                busy = false
                if (error is CancellationException) throw error
                if (error == null) {
                    state.clearText()
                    picked.clear()
                    typing.stop()
                }
            }
        }
    }

    val query = messageComposerQuery(state, mentions)
    val onQueryChange by rememberUpdatedState(mentions?.onQueryChange)
    LaunchedEffect(query) { onQueryChange?.invoke(query) }
    LaunchedEffect(editing) { if (editing) focus.requestFocus() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (modes.isNotEmpty()) {
            ModeButtons(modes, mode, enabled, labels) { value ->
                ownMode = value
                onModeChange?.invoke(value)
            }
        }
        if (mentions != null) {
            MonarchMentionPicker(
                query = query,
                items = mentions.suggestions,
                onSelect = { item -> query?.let { insertMention(state, it, item, picked) } },
                status = mentions.status,
                onRetry = mentions.onRetry,
                labels = labels.mentionPicker,
            )
        }
        val shape = MonarchShapeDefaults.Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .monarchFocusRing(shape)
                .monarchSurface(tokens.boxColors(mode?.note == true, enabled, colors), shape)
                .padding(tokens.BoxPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
        ) {
            when {
                editing -> ContextBar(monarchString(labels.editing, R.string.monarch_messagecomposer_editing), null)
                replyTo != null -> ContextBar(
                    leading = MonarchGlyphs.Reply,
                    title = labels.replyingTo?.invoke(replyTo.author)
                        ?: stringResource(R.string.monarch_messagecomposer_replying_to, replyTo.author),
                    excerpt = replyTo.excerpt,
                ) {
                    if (onDismissReply != null) {
                        val name = monarchString(labels.dismissReply, R.string.monarch_messagecomposer_dismiss_reply)
                        ToolButton(name, dismissIcon, enabled = true, onClick = onDismissReply)
                    }
                }
            }
            if (attachments != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                ) { attachments() }
            }
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onFocusChanged { typing.focused(it.isFocused) }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val press = MessageComposerKey(
                            key = event.key,
                            shift = event.isShiftPressed,
                            command = event.isCtrlPressed || event.isMetaPressed,
                            hardware = event.nativeKeyEvent.flags and KeyEvent.FLAG_SOFT_KEYBOARD == 0,
                        )
                        val action = MonarchMessageComposerKeys.keyAction(
                            press = press,
                            shortcut = sendShortcut,
                            editing = editing && onCancelEdit != null,
                            empty = state.text.isEmpty(),
                            canEditLast = onEditLast != null,
                        )
                        when (action) {
                            MessageComposerKeyAction.Send -> send()
                            MessageComposerKeyAction.CancelEdit -> onCancelEdit?.invoke()
                            MessageComposerKeyAction.EditLast -> onEditLast?.invoke()
                            MessageComposerKeyAction.None -> return@onPreviewKeyEvent false
                        }
                        true
                    }
                    .semantics { contentDescription = label },
                enabled = enabled,
                textStyle = monarchFieldTextStyle().copy(
                    color = if (enabled) colors.textDefault else colors.textDisabled,
                ),
                lineLimits = TextFieldLineLimits.MultiLine(1, tokens.MAX_LINES),
                cursorBrush = SolidColor(tokens.cursorColor(colors)),
                decorator = { field ->
                    // The input has its own box inside the card, one surface step down, so it reads as a field.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = tokens.FieldMinHeight)
                            .then(
                                if (enabled) {
                                    Modifier
                                } else {
                                    Modifier.background(
                                        messageComposerFieldFill(mode?.note == true, enabled, colors),
                                        MonarchTheme.shapes.md,
                                    )
                                },
                            )
                            .padding(tokens.FieldPadding),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        val hint = mode?.placeholder ?: placeholder
                        if (text.isEmpty() && hint != null) {
                            Text(
                                text = hint,
                                style = monarchFieldTextStyle(),
                                color = tokens.placeholderColor(colors),
                            )
                        }
                        field()
                    }
                },
            )
            MonarchDivider(variant = MonarchDividerVariant.Subtle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The tools share one row that scrolls sideways when the 48dp targets do not fit.
                Row(
                    // Text buttons without icons start on the text edge of the card; icon buttons keep their inset.
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .then(
                            if (onAttach != null && attachIcon == null) Modifier.monarchGhostStartAlign() else Modifier,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onAttach != null) {
                        val name = monarchString(labels.attach, R.string.monarch_messagecomposer_attach)
                        ToolButton(name, attachIcon, enabled, onAttach)
                    }
                    if (emojiPicker != null && enabled) {
                        EmojiButton(labels, emojiIcon, emojiPicker) { emoji -> insertAtCaret(state, emoji) }
                    }
                    actions?.invoke(this)
                }
                CharacterCount(text, maxLength, labels)
                if (editing && onCancelEdit != null) {
                    MonarchButton(
                        text = monarchString(labels.cancel, R.string.monarch_messagecomposer_cancel),
                        onClick = onCancelEdit,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
                MonarchButton(
                    text = mode?.sendLabel ?: if (editing) {
                        monarchString(labels.save, R.string.monarch_messagecomposer_save)
                    } else {
                        monarchString(labels.send, R.string.monarch_messagecomposer_send)
                    },
                    onClick = send,
                    // Nothing to send yet: the outlined look with disabled text stays readable and shows the button.
                    variant = if (canSend || showSending) {
                        MonarchButtonVariant.Primary
                    } else {
                        MonarchButtonVariant.Secondary
                    },
                    size = MonarchButtonSize.Sm,
                    enabled = canSend,
                    loading = showSending,
                    loadingLabel = monarchString(labels.sending, R.string.monarch_messagecomposer_sending),
                )
            }
        }
        Footer(enabled, draftStatus, disabledReason, labels)
    }
}

/** The mention being typed at the caret, when the composer has mentions. */
private fun messageComposerQuery(
    state: TextFieldState,
    mentions: MonarchMessageComposerMentions?,
): MonarchMentionQuery? {
    if (mentions == null) return null
    val selection = state.selection
    if (!selection.collapsed) return null
    return MonarchMentionQuery.find(state.text.toString(), selection.end, mentions.triggers)
}

/** Replaces [query] with the item's @Label and a space, and remembers the pick for the message. */
private fun insertMention(
    state: TextFieldState,
    query: MonarchMentionQuery,
    item: MonarchMentionItem,
    picked: MutableList<MessageComposerPicked>,
) {
    val pick = MessageComposerPicked(query.trigger, item)
    val (text, caret) = MonarchMessageComposerTokens.insertMention(
        state.text.toString(),
        query.start,
        query.end,
        pick.written,
    )
    state.edit {
        replace(0, length, text)
        selection = TextRange(caret)
    }
    picked += pick
}

/** Replaces the selection with [text] and puts the caret after it. */
private fun insertAtCaret(state: TextFieldState, text: String) {
    state.edit {
        val start = selection.min
        replace(start, selection.max, text)
        selection = TextRange(start + text.length)
    }
}

/** Calls onTyping at most once per interval while the focused text changes, and onTypingStop after it. */
private class MessageComposerTyping {
    var onTyping: (() -> Unit)? = null
    var onTypingStop: (() -> Unit)? = null
    var intervalMillis: Long = 0L
    private var hasFocus = false
    private var active = false
    private var lastAt = 0L

    fun changed(text: CharSequence) {
        if (text.isEmpty()) {
            stop()
            return
        }
        if (!hasFocus) return
        val now = SystemClock.uptimeMillis()
        if (!active || now - lastAt >= intervalMillis) {
            active = true
            lastAt = now
            onTyping?.invoke()
        }
    }

    fun focused(focused: Boolean) {
        hasFocus = focused
        if (!focused) stop()
    }

    fun stop() {
        if (!active) return
        active = false
        onTypingStop?.invoke()
    }
}

@Composable
private fun rememberMessageComposerTyping(
    state: TextFieldState,
    intervalMillis: Long,
    onTyping: (() -> Unit)?,
    onTypingStop: (() -> Unit)?,
): MessageComposerTyping {
    val typing = remember { MessageComposerTyping() }
    typing.onTyping = onTyping
    typing.onTypingStop = onTypingStop
    typing.intervalMillis = intervalMillis
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect { typing.changed(it) }
    }
    DisposableEffect(typing) { onDispose { typing.stop() } }
    return typing
}

@Composable
private fun ModeButtons(
    modes: List<MonarchMessageComposerMode>,
    selected: MonarchMessageComposerMode?,
    enabled: Boolean,
    labels: MonarchMessageComposerLabels,
    onSelect: (String) -> Unit,
) {
    val name = monarchString(labels.modes, R.string.monarch_messagecomposer_modes)
    MonarchSegmentedControl(
        modifier = Modifier.semantics { contentDescription = name },
        size = MonarchSegmentedControlSize.Sm,
    ) {
        modes.forEach { mode ->
            Segment(
                selected = mode.value == selected?.value,
                onClick = { onSelect(mode.value) },
                label = mode.label,
                enabled = enabled,
            )
        }
    }
}

/** The reply or edit context: one TalkBack element, with an optional button after it. */
@Composable
private fun ContextBar(
    title: String,
    excerpt: String?,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val accent = MonarchTheme.colors.accentSolid
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MonarchTheme.shapes.sm)
            .background(MonarchTheme.colors.bgSubtle)
            .drawBehind {
                drawRect(accent, size = Size(MonarchTokens.Space.space0_5.dp.toPx(), size.height))
            }
            .padding(start = MonarchTokens.Space.space3.dp)
            .heightIn(min = if (trailing != null) MonarchTouchTarget.Minimum else MonarchTokens.Space.space0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MonarchTokens.Space.space2.dp),
    ) {
        leading?.let { MonarchIcon(it, contentDescription = null, size = MonarchIconSize.Sm) }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { },
        ) {
            // Title 14sp semibold over a 12sp muted excerpt: the title is always the stronger line.
            Text(
                text = title,
                style = monarchFieldTextStyle().copy(fontWeight = FontWeight.SemiBold),
                color = MonarchTheme.colors.textDefault,
            )
            if (excerpt != null) {
                MonarchText(
                    text = excerpt,
                    style = MonarchTextStyle.Caption,
                    color = MonarchMessageComposerTokens.mutedColor(MonarchTheme.colors),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
        trailing?.invoke()
    }
}

/** A named icon button, or a ghost text button until the app gives an icon. */
@Composable
private fun ToolButton(name: String, icon: (@Composable () -> Unit)?, enabled: Boolean, onClick: () -> Unit) {
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

@Composable
private fun EmojiButton(
    labels: MonarchMessageComposerLabels,
    icon: (@Composable () -> Unit)?,
    picker: @Composable (onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(monarchString(labels.emoji, R.string.monarch_messagecomposer_emoji), icon, enabled = true) {
            open = true
        }
        if (open) {
            picker({ emoji ->
                open = false
                onPick(emoji)
            }) { open = false }
        }
    }
}

/** The characters left, read as "12 characters left" or "3 characters over the limit". */
@Composable
private fun CharacterCount(text: CharSequence, maxLength: Int?, labels: MonarchMessageComposerLabels) {
    val left = MonarchMessageComposerTokens.remaining(text, maxLength) ?: return
    val spoken = labels.remaining?.invoke(left) ?: if (left < 0) {
        pluralStringResource(R.plurals.monarch_messagecomposer_over, -left, -left)
    } else {
        pluralStringResource(R.plurals.monarch_messagecomposer_remaining, left, left)
    }
    Text(
        text = left.toString(),
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        style = MonarchTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
        color = MonarchMessageComposerTokens.countColor(left < 0, MonarchTheme.colors),
    )
}

@Composable
private fun Footer(
    enabled: Boolean,
    draftStatus: MonarchSaveStatus,
    disabledReason: String?,
    labels: MonarchMessageComposerLabels,
) {
    if (draftStatus != MonarchSaveStatus.Idle) {
        MonarchSaveStatusIndicator(
            status = draftStatus,
            labels = MonarchSaveStatusIndicatorLabels(
                saving = monarchString(labels.draftSaving, R.string.monarch_messagecomposer_draft_saving),
                saved = monarchString(labels.draftSaved, R.string.monarch_messagecomposer_draft_saved),
                error = monarchString(labels.draftError, R.string.monarch_messagecomposer_draft_error),
            ),
        )
    }
    if (!enabled && disabledReason != null) {
        MonarchText(
            text = disabledReason,
            style = MonarchTextStyle.Caption,
            color = MonarchMessageComposerTokens.mutedColor(MonarchTheme.colors),
        )
    }
}

/** The fill of the input box inside the card when it is disabled: the muted fill. */
private fun messageComposerFieldFill(note: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
    !enabled -> colors.bgMuted
    note -> colors.bgSurface
    else -> colors.bgSubtle
}
