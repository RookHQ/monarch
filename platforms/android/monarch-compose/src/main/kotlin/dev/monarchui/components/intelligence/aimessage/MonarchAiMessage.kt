package dev.monarchui.components.intelligence.aimessage

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationDefaults
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFilePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFinishReason
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditHunk
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiUiAction
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiUiRegistry
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageAction
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActions
import dev.monarchui.components.intelligence.aimessagebranch.MonarchAiMessageBranch
import dev.monarchui.components.intelligence.aiusermessageedit.MonarchAiUserMessageEdit
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

private typealias Tokens = MonarchAiMessageTokens

/**
 * One chat message: its parts in the order the model produced them (streamed text, reasoning, tool calls,
 * sources and citations, files, memories, plans, artifacts, generated UI, edit proposals and errors), with the
 * layout of its role. The person's messages are an end-aligned bubble; the assistant's are full width under its
 * avatar and name. Under the message are the time, the version switcher and the action row.
 *
 * Built by Monarch as a `Column` of the existing Monarch AI components, one per part: `MonarchAiStreamingText`,
 * `MonarchAiReasoning`, `MonarchAiToolCall` (or `MonarchAiToolApproval` while a call waits for approval and
 * [onApproveTool] or [onDenyTool] is set), `MonarchAiSourceList` at the first source, `MonarchAiCitation`,
 * `MonarchAttachmentChip`, `MonarchAiErrorMessage`, `MonarchAiMemoryChip`, `MonarchAiChainOfSteps`,
 * `MonarchAiArtifactCard`, `MonarchAiGenerativeUi` and `MonarchAiEditProposal`, followed by
 * `MonarchAiMessageBranch`, `MonarchAiMessageActions` and, while [editing], `MonarchAiUserMessageEdit`.
 * TalkBack reads "You said:", "Assistant said:" or "System message:" on the message before its parts.
 *
 * @param message the message to show.
 * @param modifier the modifier of the message.
 * @param agent the agent shown when the message has none of its own, for example the conversation's agent.
 * @param agentImage the agent's picture, loaded by the app; the avatar shows [agentIcon] or initials meanwhile.
 * @param announces whether streamed text and errors announce themselves. Pass `false` when the thread has one
 * announcer of its own.
 * @param showTimestamp shows the time the message was created.
 * @param timeZone the IANA zone of the time. `null` is the device's zone.
 * @param is24Hour shows the time on a 24-hour clock. `null` follows the device setting.
 * @param showActions shows the action row. It is hidden while the message streams, is edited or is queued.
 * @param copyable shows Copy, which copies the message's text to the clipboard.
 * @param editing shows the person's message as an edit field instead of the bubble.
 * @param speaking whether the app is reading the message aloud.
 * @param onCopy called with the text after Copy has put it on the clipboard.
 * @param onRegenerate shows Regenerate on an assistant message.
 * @param onEdit shows Edit on the person's message; usually sets [editing].
 * @param onFeedback shows thumbs on an assistant message; the pressed one is the message's feedback.
 * @param onShare shows Share.
 * @param onReadAloud shows Read aloud on an assistant message; called with `true` to start and `false` to stop.
 * @param onBranch switches between the message's versions, given the version counted from 0. The switcher shows
 * when the message has more than one version and both version icons are given.
 * @param onRetry shows Try again on an error part that is retryable.
 * @param onResume shows Resume on an error part that can be resumed.
 * @param onCancelQueued shows Cancel on a queued message of the person.
 * @param onForgetMemory shows Forget on a memory the reply saved, given its id.
 * @param onEditMemory shows Edit on a memory the reply saved, given its id and the new text.
 * @param onApproveTool approves a tool call waiting for approval, given its id and the edited input, if any.
 * @param onDenyTool denies a tool call waiting for approval, given its id and the reason typed, if any.
 * @param onAlwaysAllowTool offers "Always allow" on the approval card, given the tool's name.
 * @param onEditSubmit saves an edit of the person's message.
 * @param onEditCancel cancels an edit.
 * @param onOpenArtifact shows artifact parts as cards that call this. Without it artifact parts show nothing.
 * @param onOpenFile makes file chips open the file.
 * @param onOpenSource opens a source from the source list or a citation.
 * @param onOpenLink opens a link in the text. `null` opens it with the system.
 * @param uiRegistry shows `ui` parts with the components it lists. Without it `ui` parts show nothing: model
 * output never picks a component the app did not list.
 * @param onUiAction receives actions from generated components.
 * @param editHunks turns an edit proposal into hunks; return `null` to show nothing for it.
 * @param renderPart shows a part yourself: return content for it, `{}` for nothing, or `null` to keep the
 * default component.
 * @param agentIcon the icon of the avatar while there is no [agentImage]. Drawn by default, as on the web; `null`
 * draws none.
 * @param actionIcon the glyph of each action of the action row, given the action and whether it is active.
 * @param moreIcon the glyph of the button that opens [moreActions].
 * @param previousVersionIcon the icon of the previous-version button, mirrored for right to left.
 * @param nextVersionIcon the icon of the next-version button, mirrored for right to left.
 * @param labels replacements for the built-in strings and those of the part components.
 * @param moreActions the items of the menu of further actions, given a function that closes the menu.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList")
@Composable
public fun MonarchAiMessage(
    message: MonarchAiConversationMessage,
    modifier: Modifier = Modifier,
    agent: MonarchAiAgentRef? = null,
    agentImage: Painter? = null,
    announces: Boolean = true,
    showTimestamp: Boolean = true,
    timeZone: String? = null,
    is24Hour: Boolean? = null,
    showActions: Boolean = true,
    copyable: Boolean = true,
    editing: Boolean = false,
    speaking: Boolean = false,
    onCopy: ((String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onFeedback: ((MonarchAiMessageFeedback?) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onReadAloud: ((speaking: Boolean) -> Unit)? = null,
    onBranch: ((index: Int) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onCancelQueued: (() -> Unit)? = null,
    onForgetMemory: ((id: String) -> Unit)? = null,
    onEditMemory: ((id: String, text: String) -> Unit)? = null,
    onApproveTool: ((toolCallId: String, input: MonarchAiJSON?) -> Unit)? = null,
    onDenyTool: ((toolCallId: String, reason: String?) -> Unit)? = null,
    onAlwaysAllowTool: ((toolName: String) -> Unit)? = null,
    onEditSubmit: ((String) -> Unit)? = null,
    onEditCancel: (() -> Unit)? = null,
    onOpenArtifact: ((MonarchAiArtifactPart) -> Unit)? = null,
    onOpenFile: ((MonarchAiFilePart) -> Unit)? = null,
    onOpenSource: ((MonarchAiSourcePart) -> Unit)? = null,
    onOpenLink: ((url: String) -> Unit)? = null,
    uiRegistry: MonarchAiUiRegistry? = null,
    onUiAction: ((MonarchAiUiAction) -> Unit)? = null,
    editHunks: ((MonarchAiEditProposalPart) -> List<MonarchAiEditHunk>?)? = null,
    renderPart: ((part: MonarchAiPart, index: Int) -> (@Composable () -> Unit)?)? = null,
    agentIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    actionIcon: (@Composable (action: MonarchAiMessageAction, active: Boolean) -> Unit)? =
        MonarchDefaultIcons.AiMessageAction,
    moreIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    previousVersionIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronLeft,
    nextVersionIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronRight,
    labels: MonarchAiMessageLabels = MonarchAiMessageLabels(),
    moreActions: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    val user = message.role == MonarchAiRole.User
    val assistant = message.role == MonarchAiRole.Assistant
    val queued = Tokens.queued(message)
    val prefix = aiMessagePrefix(message.role, labels)
    val time = if (showTimestamp) aiMessageTime(message.createdAtMillis, timeZone, is24Hour) else null
    val sourceNumbers = remember(message.parts) { MonarchAiCitationDefaults.sourceNumbers(message.parts) }
    val context = AiMessagePartContext(
        announces = announces,
        lastText = Tokens.lastTextIndex(message.parts),
        replyDone = Tokens.replyDone(message),
        sources = message.parts.filterIsInstance<MonarchAiSourcePart>(),
        sourceNumbers = sourceNumbers,
        labels = labels,
        onRetry = onRetry,
        onResume = onResume,
        onForgetMemory = onForgetMemory,
        onEditMemory = onEditMemory,
        onApproveTool = onApproveTool,
        onDenyTool = onDenyTool,
        onAlwaysAllowTool = onAlwaysAllowTool,
        onOpenArtifact = onOpenArtifact,
        onOpenFile = onOpenFile,
        onOpenSource = onOpenSource,
        onOpenLink = onOpenLink,
        uiRegistry = uiRegistry,
        onUiAction = onUiAction,
        editHunks = editHunks,
    ).place(message.parts)
    val switcher = aiMessageSwitcher(message, onBranch, previousVersionIcon, nextVersionIcon, labels)
    val settled = !message.streaming && !queued
    val copy: (String) -> Unit = { text -> onCopy?.invoke(text) }
    val toolbar: (@Composable () -> Unit)? = if (showActions && !editing && settled) {
        {
            MonarchAiMessageActions(
                messageText = message.text,
                onCopy = copy.takeIf { copyable && message.text.isNotEmpty() },
                onRegenerate = onRegenerate.takeIf { assistant },
                onEdit = onEdit.takeIf { user },
                feedback = message.feedback,
                onFeedback = onFeedback.takeIf { assistant },
                onShare = onShare,
                speaking = speaking,
                onReadAloud = onReadAloud.takeIf { assistant },
                icon = actionIcon,
                moreIcon = moreIcon,
                labels = labels.actions,
                more = moreActions,
            )
        }
    } else {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = prefix },
        verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap),
        horizontalAlignment = if (user) Alignment.End else Alignment.Start,
    ) {
        if (assistant) AiMessageHeader(message.agent ?: agent, labels, time, agentImage, agentIcon)
        if (user && editing) {
            MonarchAiUserMessageEdit(
                original = message.text,
                onSubmit = { onEditSubmit?.invoke(it) },
                onCancel = { onEditCancel?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                label = labels.editLabel,
                sendLabel = labels.editSave,
                cancelLabel = labels.editCancel,
            )
        } else {
            AiMessageBody(user) {
                AiMessageParts(message.parts, renderPart, context)
                Tokens.finishNotice(message)?.let { reason -> AiMessageFinishNotice(reason, labels) }
            }
        }
        if (queued) AiMessageQueued(labels, onCancelQueued)
        AiMessageFooter(time.takeIf { !assistant }, switcher, toolbar)
    }
}

@Composable
private fun aiMessagePrefix(role: MonarchAiRole, labels: MonarchAiMessageLabels): String = when (role) {
    MonarchAiRole.User -> monarchString(labels.youSaid, R.string.monarch_aimessage_you_said)
    MonarchAiRole.Assistant -> monarchString(labels.assistantSaid, R.string.monarch_aimessage_assistant_said)
    else -> monarchString(labels.systemSaid, R.string.monarch_aimessage_system_said)
}

/** The version switcher, when the message has versions, the app switches them and both icons are given. */
private fun aiMessageSwitcher(
    message: MonarchAiConversationMessage,
    onBranch: ((Int) -> Unit)?,
    previousIcon: (@Composable () -> Unit)?,
    nextIcon: (@Composable () -> Unit)?,
    labels: MonarchAiMessageLabels,
): (@Composable () -> Unit)? {
    val branch = message.branch?.takeIf { it.count > 1 } ?: return null
    if (onBranch == null || previousIcon == null || nextIcon == null) return null
    return {
        MonarchAiMessageBranch(
            selection = branch.index,
            count = branch.count,
            onSelectionChange = onBranch,
            previousIcon = previousIcon,
            nextIcon = nextIcon,
            enabled = !message.streaming,
            label = labels.versions,
            previousLabel = labels.previousVersion,
            nextLabel = labels.nextVersion,
        )
    }
}

@Composable
private fun AiMessageParts(
    parts: List<MonarchAiPart>,
    renderPart: ((part: MonarchAiPart, index: Int) -> (@Composable () -> Unit)?)?,
    context: AiMessagePartContext,
) {
    parts.forEachIndexed { index, part ->
        key(index) {
            val custom = renderPart?.invoke(part, index)
            if (custom != null) custom() else AiMessagePart(part, index, context)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiMessageFooter(
    time: String?,
    switcher: (@Composable () -> Unit)?,
    toolbar: (@Composable () -> Unit)?,
) {
    if (time == null && switcher == null && toolbar == null) return
    // The 48dp targets of the switcher and the actions give the space between them, so a phone keeps both on one line.
    val gap = if (time == null) MonarchTokens.Space.space0.dp else Tokens.InlineGap
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        time?.let { AiMessageMeta(it) }
        switcher?.invoke()
        toolbar?.invoke()
    }
}

@Composable
private fun aiMessageTime(millis: Long?, timeZone: String?, is24Hour: Boolean?): String? {
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = is24Hour ?: AndroidDateFormat.is24HourFormat(LocalContext.current)
    return remember(millis, timeZone, locale, twentyFour) {
        millis?.let { Tokens.time(it, locale, timeZone, twentyFour) }
    }
}

@Composable
private fun AiMessageMeta(text: String) {
    MonarchText(text = text, style = MonarchTextStyle.Caption, color = Tokens.metaColor(MonarchTheme.colors))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiMessageHeader(
    agent: MonarchAiAgentRef?,
    labels: MonarchAiMessageLabels,
    time: String?,
    image: Painter?,
    icon: (@Composable () -> Unit)?,
) {
    val name = agent?.name ?: monarchString(labels.assistant, R.string.monarch_aimessage_assistant)
    val role = agent?.role
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.InlineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && image == null) {
            // The assistant mark is the robot on the assistant tint, as on the web, not a colour of the name.
            val colors = MonarchTheme.colors
            Box(
                modifier = Modifier
                    .size(MonarchAvatarTokens.side(MonarchAvatarSize.Sm))
                    .clip(CircleShape)
                    .background(colors.aiSubtle)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.aiStrong) { icon() }
            }
        } else {
            MonarchAvatar(image = image, name = name, size = MonarchAvatarSize.Sm, decorative = true, icon = icon)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Tokens.InlineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = name, style = MonarchTextStyle.BodyStrong)
            role?.let { AiMessageMeta(it) }
            time?.let { AiMessageMeta(it) }
        }
    }
}

@Composable
private fun AiMessageBody(user: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val modifier = if (user) {
        Modifier
            .fillMaxWidth(Tokens.BUBBLE_MAX_FRACTION)
            .wrapContentWidth(Alignment.End)
            .monarchSurface(Tokens.bubble(MonarchTheme.colors), MonarchTheme.shapes.xl)
            .padding(horizontal = Tokens.BubblePaddingHorizontal, vertical = Tokens.BubblePaddingVertical)
    } else {
        Modifier.fillMaxWidth()
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.PartGap), content = content)
}

@Composable
private fun AiMessageFinishNotice(reason: MonarchAiFinishReason, labels: MonarchAiMessageLabels) {
    val default = when (reason) {
        MonarchAiFinishReason.Length -> R.string.monarch_aimessage_finish_length
        MonarchAiFinishReason.ContentFilter -> R.string.monarch_aimessage_finish_content_filter
        else -> R.string.monarch_aimessage_finish_refusal
    }
    MonarchText(
        text = monarchString(labels.finishNotice?.invoke(reason), default),
        style = MonarchTextStyle.Caption,
        color = Tokens.noticeColor(MonarchTheme.colors),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiMessageQueued(labels: MonarchAiMessageLabels, onCancelQueued: (() -> Unit)?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Tokens.InlineGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AiMessageMeta(monarchString(labels.queued, R.string.monarch_aimessage_queued))
        onCancelQueued?.let {
            MonarchButton(
                text = monarchString(labels.cancelQueued, R.string.monarch_aimessage_cancel_queued),
                onClick = it,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
