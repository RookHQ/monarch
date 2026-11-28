package dev.monarchui.components.intelligence.aimessageactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.tooltip.MonarchTooltip
import dev.monarchui.components.overlays.tooltip.MonarchTooltipSide
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString

/**
 * The action row under a chat message: copy, regenerate, edit, rate, share,
 * read aloud and a menu of further actions. An action appears only when its
 * callback is given.
 *
 * With [icon], each action is a `MonarchIconButton` (the thumbs are toggle
 * `MonarchIconButton`s) named for TalkBack and with a `MonarchTooltip` on long
 * press, so its meaning never rests on the glyph; without [icon] (`null`) each action is a ghost
 * `MonarchButton` with its name and each thumb a `MonarchChip`. Copy is a `MonarchCopyButton`, which writes
 * [messageText] to the clipboard, plays the confirm haptic and announces the
 * copy. The menu is a `MonarchMenu` that the app fills with
 * `MonarchMenuItem`s.
 *
 * Read aloud does not speak by itself: the app owns `TextToSpeech`, passes
 * [speaking] and starts or stops speech in [onReadAloud].
 *
 * @param messageText the message's plain text: what Copy copies and what the app reads aloud.
 * @param modifier the modifier of the row.
 * @param onCopy shows Copy; called with [messageText] after it is copied to the clipboard.
 * @param onRegenerate shows Regenerate.
 * @param onEdit shows Edit, for the person's own messages.
 * @param feedback the current rating, shown as a pressed thumb; `null` for none.
 * @param onFeedback shows thumbs up and down; called with the new rating, or `null` when the pressed thumb is
 * pressed again.
 * @param onShare shows Share; start the system share sheet with `Intent.ACTION_SEND` here.
 * @param speaking whether the app is reading the message aloud; the button then reads "Stop reading".
 * @param onReadAloud shows Read aloud; called with `true` to start speaking and `false` to stop.
 * @param enabled whether the actions can be used, for example `false` while the answer streams.
 * @param icon the glyph of an action, given the action and whether it is active (copied, pressed or
 * speaking), for example a `MonarchIcon` with a `null` content description. `null` shows each action as text.
 * @param moreIcon the glyph of the button that opens [more]. `null` shows that button as text.
 * @param labels replacements for the built-in strings.
 * @param more the items of the menu of further actions, given a function that closes the menu, for example
 * `MonarchMenuItem`s. `null` hides the menu button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchAiMessageActions(
    messageText: String,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    feedback: MonarchAiMessageFeedback? = null,
    onFeedback: ((MonarchAiMessageFeedback?) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    speaking: Boolean = false,
    onReadAloud: ((speaking: Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    icon: (@Composable (action: MonarchAiMessageAction, active: Boolean) -> Unit)? =
        MonarchDefaultIcons.AiMessageAction,
    moreIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    labels: MonarchAiMessageActionsLabels = MonarchAiMessageActionsLabels(),
    more: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    val texts = aiMessageActionsTexts(labels)
    FlowRow(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = texts.toolbar
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiMessageActionsTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiMessageActionsTokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCopy != null) CopyAction(messageText, texts, enabled, icon, onCopy)
        if (onRegenerate != null) {
            ActionButton(texts.regenerate, enabled, glyphOf(icon, MonarchAiMessageAction.Regenerate), onRegenerate)
        }
        if (onEdit != null) {
            ActionButton(texts.edit, enabled, glyphOf(icon, MonarchAiMessageAction.Edit), onEdit)
        }
        if (onFeedback != null) {
            listOf(
                Triple(MonarchAiMessageFeedback.Up, MonarchAiMessageAction.Good, texts.good),
                Triple(MonarchAiMessageFeedback.Down, MonarchAiMessageAction.Bad, texts.bad),
            ).forEach { (value, action, name) ->
                val checked = feedback == value
                ToggleAction(
                    name = name,
                    checked = checked,
                    enabled = enabled,
                    icon = glyphOf(icon, action, checked),
                    onToggle = { onFeedback(MonarchAiMessageActionsTokens.nextFeedback(feedback, value)) },
                )
            }
        }
        if (onShare != null) {
            ActionButton(texts.share, enabled, glyphOf(icon, MonarchAiMessageAction.Share), onShare)
        }
        if (onReadAloud != null) {
            ActionButton(
                name = if (speaking) texts.stopReading else texts.readAloud,
                enabled = enabled,
                icon = glyphOf(icon, MonarchAiMessageAction.ReadAloud, speaking),
                onClick = { onReadAloud(!speaking) },
            )
        }
        if (more != null) MoreAction(texts.more, enabled, moreIcon, more)
    }
}

@Composable
private fun CopyAction(
    messageText: String,
    texts: AiMessageActionsTexts,
    enabled: Boolean,
    icon: (@Composable (MonarchAiMessageAction, Boolean) -> Unit)?,
    onCopy: (String) -> Unit,
) {
    if (icon == null) {
        MonarchCopyButton(
            value = messageText,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            onCopy = onCopy,
            label = texts.copy,
            copiedLabel = texts.copied,
        )
    } else {
        WithTooltip(texts.copy) {
            MonarchCopyButton(
                value = messageText,
                icon = { icon(MonarchAiMessageAction.Copy, false) },
                enabled = enabled,
                copiedIcon = { icon(MonarchAiMessageAction.Copy, true) },
                onCopy = onCopy,
                label = texts.copy,
                copiedLabel = texts.copied,
            )
        }
    }
}

/** The glyph of [action] from the app's icon slot, or `null` when the actions are text. */
private fun glyphOf(
    icon: (@Composable (MonarchAiMessageAction, Boolean) -> Unit)?,
    action: MonarchAiMessageAction,
    active: Boolean = false,
): (@Composable () -> Unit)? = icon?.let { draw -> { draw(action, active) } }

/** One action: an icon button with a tooltip, or a ghost text button. */
@Composable
private fun ActionButton(name: String, enabled: Boolean, icon: (@Composable () -> Unit)?, onClick: () -> Unit) {
    if (icon == null) {
        MonarchButton(
            text = name,
            onClick = onClick,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
        return
    }
    WithTooltip(name) {
        MonarchIconButton(label = name, onClick = onClick, enabled = enabled) {
            Box(Modifier.clearAndSetSemantics { }) { icon() }
        }
    }
}

/** A thumb: an icon toggle button with a tooltip, or a filter chip with its name. */
@Composable
private fun ToggleAction(
    name: String,
    checked: Boolean,
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    onToggle: () -> Unit,
) {
    if (icon == null) {
        MonarchChip(text = name, selected = checked, onSelectedChange = { onToggle() }, enabled = enabled)
        return
    }
    WithTooltip(name) {
        MonarchIconButton(label = name, checked = checked, onCheckedChange = { onToggle() }, enabled = enabled) {
            Box(Modifier.clearAndSetSemantics { }) { icon() }
        }
    }
}

/** The button that opens the menu of further actions, and the menu. */
@Composable
private fun MoreAction(
    name: String,
    enabled: Boolean,
    moreIcon: (@Composable () -> Unit)?,
    more: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        ActionButton(name, enabled, moreIcon) { open = true }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) { more { open = false } }
    }
}

/** Shows [name] in a Monarch tooltip on long press, as Material 3 does for icon buttons. */
@Composable
private fun WithTooltip(name: String, content: @Composable () -> Unit) {
    MonarchTooltip(text = name, side = MonarchTooltipSide.Above, content = content)
}

/** The strings of one composition, with the labels applied. */
@Suppress("LongParameterList")
private class AiMessageActionsTexts(
    val toolbar: String,
    val copy: String,
    val copied: String,
    val regenerate: String,
    val edit: String,
    val good: String,
    val bad: String,
    val share: String,
    val readAloud: String,
    val stopReading: String,
    val more: String,
)

@Composable
private fun aiMessageActionsTexts(labels: MonarchAiMessageActionsLabels): AiMessageActionsTexts =
    AiMessageActionsTexts(
        toolbar = monarchString(labels.toolbar, R.string.monarch_aimessageactions_toolbar),
        copy = monarchString(labels.copy, R.string.monarch_aimessageactions_copy),
        copied = monarchString(labels.copied, R.string.monarch_aimessageactions_copied),
        regenerate = monarchString(labels.regenerate, R.string.monarch_aimessageactions_regenerate),
        edit = monarchString(labels.edit, R.string.monarch_aimessageactions_edit),
        good = monarchString(labels.good, R.string.monarch_aimessageactions_good),
        bad = monarchString(labels.bad, R.string.monarch_aimessageactions_bad),
        share = monarchString(labels.share, R.string.monarch_aimessageactions_share),
        readAloud = monarchString(labels.readAloud, R.string.monarch_aimessageactions_read_aloud),
        stopReading = monarchString(labels.stopReading, R.string.monarch_aimessageactions_stop_reading),
        more = monarchString(labels.more, R.string.monarch_aimessageactions_more),
    )
