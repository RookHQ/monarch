package dev.monarchui.components.intelligence.aiassistantpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChips
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextChipsLabels
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversation
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationError
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationStatus
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInput
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPromptsLayout
import dev.monarchui.components.intelligence.aithread.MonarchAiThread
import dev.monarchui.components.intelligence.aiwelcome.MonarchAiWelcome
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The assistant surface any app mounts: a header with the agent and the
 * close (and, in a pane, minimize) button, chips for what the assistant can
 * see on the current screen, and a `MonarchAiThread` over a
 * `MonarchAiPromptInput`.
 *
 * A product pattern composed from Monarch AI components and Material 3. On a
 * phone (and with [MonarchAiAssistantPanelPlacement.Sheet]) it is a
 * full-height `MonarchBottomSheet`, so the drag handle, a tap outside and the
 * system back gesture close it; on a wide window it is a pane where the app
 * places it, such as the supporting pane of a `SupportingPaneScaffold`. While
 * the conversation is empty it shows a `MonarchAiWelcome` with the [starters]
 * as `MonarchAiStarterPrompts`; choosing one sends it. The title is a
 * heading and names the pane for TalkBack. The draft and the scroll position
 * are kept while the panel is closed, as long as the panel stays in the
 * composition.
 *
 * This overload takes the conversation's state; the overload that takes a
 * `MonarchAiConversation` wires it for you.
 *
 * @param title the heading of the panel, such as "Assistant". Names the pane for TalkBack.
 * @param messages the conversation, oldest first.
 * @param open whether the panel is shown.
 * @param onOpenChange called with `false` by the close button and, on a sheet, by the back gesture, a swipe
 * down or a tap outside.
 * @param onSend sends the prompt. It is called with the text as typed, or with a starter's prompt. Throw to give
 * the text back to the prompt input.
 * @param modifier the modifier of the panel.
 * @param placement a sheet on phones and a pane on wide windows, always a pane, or always a sheet.
 * @param prompt the draft. Hoist it to save and restore drafts.
 * @param status the conversation's status: the avatar shows Working and Send becomes Stop while a reply runs.
 * @param error the last turn's error, shown and announced by the thread.
 * @param offline shows the thread's offline notice.
 * @param queuedCount how many messages wait for the connection.
 * @param slow shows "taking longer than usual" while a reply runs.
 * @param agent the agent shown in the header and the welcome. Without it the avatar is named "Assistant".
 * @param agentImage the agent's picture, loaded by the app.
 * @param context what the assistant can see on the current screen, as `MonarchAiContextChips`.
 * @param starters suggested first prompts, shown while the conversation is empty. Choosing one sends it.
 * @param minimized collapses an inline pane to its header.
 * @param onMinimizedChange shows the minimize button on an inline pane. A sheet is never minimized.
 * @param onStop stops the running reply, from the Stop button and the Escape key.
 * @param onRemoveContext lets the person remove a context chip by id.
 * @param threadState the scroll position of the thread.
 * @param closeIcon the close glyph; without one the close button shows its label as text, under the title.
 * @param removeContextIcon the remove mark of the context chips. The close glyph by default, as before.
 * @param minimizeIcon a downward chevron; turned up while the pane is collapsed. Without one the button shows its
 * label as text, under the title.
 * @param headerActions buttons before minimize and close, such as "New conversation".
 * @param toolbar controls in the prompt bar, such as a model picker.
 * @param footer under the prompt input, such as `MonarchAiDisclaimer`.
 * @param empty replaces the whole empty state (the welcome and the starters).
 * @param promptFocusRequester moves focus to the prompt field, for example right after the app opens the panel.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchAiAssistantPanel(
    title: String,
    messages: List<MonarchAiConversationMessage>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSend: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    placement: MonarchAiAssistantPanelPlacement = MonarchAiAssistantPanelPlacement.Auto,
    prompt: TextFieldState = rememberTextFieldState(),
    status: MonarchAiConversationStatus = MonarchAiConversationStatus.Idle,
    error: MonarchAiConversationError? = null,
    offline: Boolean = false,
    queuedCount: Int = 0,
    slow: Boolean = false,
    agent: MonarchAiAgentRef? = null,
    agentImage: Painter? = null,
    context: List<MonarchAiContextItem> = emptyList(),
    starters: List<MonarchAiStarterPrompt> = emptyList(),
    minimized: Boolean = false,
    onMinimizedChange: ((Boolean) -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onRemoveContext: ((String) -> Unit)? = null,
    threadState: LazyListState = rememberLazyListState(),
    closeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    removeContextIcon: (@Composable () -> Unit)? = closeIcon,
    minimizeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    toolbar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    promptFocusRequester: FocusRequester? = null,
    labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
) {
    if (!open) return
    val tokens = MonarchAiAssistantPanelTokens
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val sheet = tokens.asSheet(placement, windowWidth)
    val shownAgent = agent ?: MonarchAiAgentRef(
        name = monarchString(labels.assistant, R.string.monarch_aiassistantpanel_assistant),
    )
    val header = AiAssistantPanelHeader(
        title = title,
        agent = shownAgent,
        agentLine = tokens.agentLine(agent?.name, agent?.role),
        agentImage = agentImage,
        busy = tokens.busy(status),
        minimized = minimized && !sheet,
        onMinimizedChange = onMinimizedChange.takeIf { tokens.showsMinimize(sheet, it != null) },
    )
    val body: @Composable (Modifier) -> Unit = { bodyModifier ->
        Column(modifier = bodyModifier) {
            AiAssistantPanelHeaderRow(header, labels, closeIcon, minimizeIcon, headerActions) { onOpenChange(false) }
            if (!header.minimized) {
                AiAssistantPanelBody(
                    content = AiAssistantPanelContent(
                        messages = messages,
                        status = status,
                        error = error,
                        offline = offline,
                        queuedCount = queuedCount,
                        slow = slow,
                        context = context,
                        starters = starters,
                    ),
                    prompt = prompt,
                    onSend = onSend,
                    onStop = onStop,
                    onRemoveContext = onRemoveContext,
                    removeIcon = removeContextIcon,
                    promptFocusRequester = promptFocusRequester,
                    threadState = threadState,
                    welcome = { AiAssistantPanelWelcome(agent, agentImage, starters, status, labels, onSend) },
                    empty = empty,
                    toolbar = toolbar,
                    footer = footer,
                    labels = labels,
                )
            }
        }
    }
    if (sheet) {
        MonarchBottomSheet(
            onDismissRequest = { onOpenChange(false) },
            modifier = modifier,
            fullWidthContent = true,
        ) {
            body(Modifier.fillMaxHeight().semantics { paneTitle = title })
        }
    } else {
        body(
            modifier
                .background(tokens.containerColor(MonarchTheme.colors))
                .semantics {
                    isTraversalGroup = true
                    paneTitle = title
                },
        )
    }
}

/**
 * The assistant panel wired to a `MonarchAiConversation`: it shows the
 * conversation's messages, status, error, connection and agent, sends with
 * `send` (a refused send gives the text back) and stops with `stop`. See the
 * other overload for the layout and behavior.
 *
 * @param conversation the conversation the panel shows and sends to.
 * @param title the heading of the panel, such as "Assistant". Names the pane for TalkBack.
 * @param open whether the panel is shown.
 * @param onOpenChange called with `false` by the close button and, on a sheet, by the back gesture, a swipe
 * down or a tap outside.
 * @param modifier the modifier of the panel.
 * @param placement a sheet on phones and a pane on wide windows, always a pane, or always a sheet.
 * @param prompt the draft. Hoist it to save and restore drafts.
 * @param agent replaces the conversation's agent in the header and the welcome.
 * @param agentImage the agent's picture, loaded by the app.
 * @param context what the assistant can see on the current screen, as `MonarchAiContextChips`.
 * @param starters suggested first prompts, shown while the conversation is empty. Choosing one sends it.
 * @param minimized collapses an inline pane to its header.
 * @param onMinimizedChange shows the minimize button on an inline pane. A sheet is never minimized.
 * @param onRemoveContext lets the person remove a context chip by id.
 * @param closeIcon the close glyph; without one the close button shows its label as text, under the title.
 * @param removeContextIcon the remove mark of the context chips. The close glyph by default, as before.
 * @param minimizeIcon a downward chevron; turned up while the pane is collapsed.
 * @param headerActions buttons before minimize and close, such as "New conversation".
 * @param toolbar controls in the prompt bar, such as a model picker.
 * @param footer under the prompt input, such as `MonarchAiDisclaimer`.
 * @param empty replaces the whole empty state (the welcome and the starters).
 * @param promptFocusRequester moves focus to the prompt field, for example right after the app opens the panel.
 * @param body extra request fields sent with each message from this panel, such as the selection the context
 * chips show, merged over the conversation's own `body`. Read at each send, so the conversation's `body` does not
 * have to be overwritten.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList")
@Composable
public fun MonarchAiAssistantPanel(
    conversation: MonarchAiConversation,
    title: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placement: MonarchAiAssistantPanelPlacement = MonarchAiAssistantPanelPlacement.Auto,
    prompt: TextFieldState = rememberTextFieldState(),
    agent: MonarchAiAgentRef? = null,
    agentImage: Painter? = null,
    context: List<MonarchAiContextItem> = emptyList(),
    starters: List<MonarchAiStarterPrompt> = emptyList(),
    minimized: Boolean = false,
    onMinimizedChange: ((Boolean) -> Unit)? = null,
    onRemoveContext: ((String) -> Unit)? = null,
    closeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    removeContextIcon: (@Composable () -> Unit)? = closeIcon,
    minimizeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    toolbar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    promptFocusRequester: FocusRequester? = null,
    body: Map<String, MonarchAiJSON> = emptyMap(),
    labels: MonarchAiAssistantPanelLabels = MonarchAiAssistantPanelLabels(),
) {
    val messages by conversation.messages.collectAsState()
    val status by conversation.status.collectAsState()
    val error by conversation.error.collectAsState()
    val online by conversation.online.collectAsState()
    val queued by conversation.queued.collectAsState()
    val slow by conversation.slow.collectAsState()
    val conversationAgent by conversation.agent.collectAsState()
    val sendBody by rememberUpdatedState(body)
    MonarchAiAssistantPanel(
        title = title,
        messages = messages,
        open = open,
        onOpenChange = onOpenChange,
        onSend = { text -> if (!conversation.send(text, body = sendBody)) throw AiAssistantPanelRefusedSend() },
        modifier = modifier,
        placement = placement,
        prompt = prompt,
        status = status,
        error = error,
        offline = !online,
        queuedCount = queued.size,
        slow = slow,
        agent = agent ?: conversationAgent,
        agentImage = agentImage,
        context = context,
        starters = starters,
        minimized = minimized,
        onMinimizedChange = onMinimizedChange,
        onStop = conversation::stop,
        onRemoveContext = onRemoveContext,
        closeIcon = closeIcon,
        removeContextIcon = removeContextIcon,
        minimizeIcon = minimizeIcon,
        headerActions = headerActions,
        toolbar = toolbar,
        footer = footer,
        empty = empty,
        promptFocusRequester = promptFocusRequester,
        labels = labels,
    )
}

/** Thrown inside `onSend` when the conversation refuses a message, so the prompt input gives the text back. */
internal class AiAssistantPanelRefusedSend : IllegalStateException("The conversation refused the message.")

/** What the header shows. */
private class AiAssistantPanelHeader(
    val title: String,
    val agent: MonarchAiAgentRef,
    val agentLine: String?,
    val agentImage: Painter?,
    val busy: Boolean,
    val minimized: Boolean,
    val onMinimizedChange: ((Boolean) -> Unit)?,
)

/** What the thread shows. */
private class AiAssistantPanelContent(
    val messages: List<MonarchAiConversationMessage>,
    val status: MonarchAiConversationStatus,
    val error: MonarchAiConversationError?,
    val offline: Boolean,
    val queuedCount: Int,
    val slow: Boolean,
    val context: List<MonarchAiContextItem>,
    val starters: List<MonarchAiStarterPrompt>,
)

/** A header button: the minimize toggle or close. */
private class AiAssistantPanelHeaderAction(
    val label: String,
    val icon: (@Composable () -> Unit)?,
    val turned: Boolean,
    val onClick: () -> Unit,
)

/**
 * The avatar, the title as a heading with the agent under it, the app's actions and the icon buttons; buttons
 * without an icon show their label as text in a wrapping row under the title, so they never squeeze the title
 * at large font sizes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiAssistantPanelHeaderRow(
    header: AiAssistantPanelHeader,
    labels: MonarchAiAssistantPanelLabels,
    closeIcon: (@Composable () -> Unit)?,
    minimizeIcon: (@Composable () -> Unit)?,
    headerActions: (@Composable RowScope.() -> Unit)?,
    onClose: () -> Unit,
) {
    val tokens = MonarchAiAssistantPanelTokens
    val colors = MonarchTheme.colors
    val actions = listOfNotNull(
        header.onMinimizedChange?.let { change ->
            AiAssistantPanelHeaderAction(
                label = if (header.minimized) {
                    monarchString(labels.expand, R.string.monarch_aiassistantpanel_expand)
                } else {
                    monarchString(labels.minimize, R.string.monarch_aiassistantpanel_minimize)
                },
                icon = minimizeIcon,
                turned = header.minimized,
            ) { change(!header.minimized) }
        },
        AiAssistantPanelHeaderAction(
            label = monarchString(labels.close, R.string.monarch_aiassistantpanel_close),
            icon = closeIcon,
            turned = false,
            onClick = onClose,
        ),
    )
    val (withIcon, asText) = actions.partition { it.icon != null }
    Column(modifier = Modifier.fillMaxWidth().padding(tokens.Padding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchAiAgentAvatar(
                agent = header.agent,
                image = header.agentImage,
                state = if (header.busy) MonarchAiAgentState.Working else MonarchAiAgentState.Idle,
                size = MonarchAvatarSize.Sm,
                decorative = true,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TitleGap)) {
                Text(
                    text = header.title,
                    modifier = Modifier.semantics { heading() },
                    style = MonarchTheme.typography.subheading,
                    color = tokens.titleColor(colors),
                )
                header.agentLine?.let {
                    Text(text = it, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
                }
            }
            headerActions?.invoke(this)
            withIcon.forEach { AiAssistantPanelIconButton(it) }
        }
        if (asText.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.End),
            ) {
                asText.forEach { action ->
                    MonarchButton(
                        text = action.label,
                        onClick = action.onClick,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
    MonarchDivider()
}

/** An icon button named by its label, its glyph turned up while the pane is collapsed. */
@Composable
private fun AiAssistantPanelIconButton(action: AiAssistantPanelHeaderAction) {
    val icon = action.icon ?: return
    MonarchIconButton(label = action.label, onClick = action.onClick) {
        Box(if (action.turned) Modifier.rotate(MonarchAiAssistantPanelTokens.CollapsedIconTurn) else Modifier) {
            icon()
        }
    }
}

/** The context chips, the thread and the prompt input. */
@Suppress("LongParameterList")
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiAssistantPanelBody(
    content: AiAssistantPanelContent,
    prompt: TextFieldState,
    onSend: suspend (String) -> Unit,
    onStop: (() -> Unit)?,
    onRemoveContext: ((String) -> Unit)?,
    removeIcon: (@Composable () -> Unit)?,
    promptFocusRequester: FocusRequester?,
    threadState: LazyListState,
    welcome: @Composable () -> Unit,
    empty: (@Composable () -> Unit)?,
    toolbar: (@Composable () -> Unit)?,
    footer: (@Composable () -> Unit)?,
    labels: MonarchAiAssistantPanelLabels,
) {
    val tokens = MonarchAiAssistantPanelTokens
    Column(modifier = Modifier.padding(tokens.Padding), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (content.context.isNotEmpty()) {
            MonarchAiContextChips(
                items = content.context,
                onRemove = onRemoveContext,
                removeIcon = removeIcon,
                labels = MonarchAiContextChipsLabels(
                    label = monarchString(labels.context, R.string.monarch_aiassistantpanel_context),
                ),
            )
        }
        MonarchAiThread(
            messages = content.messages,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = threadState,
            status = content.status,
            error = content.error,
            offline = content.offline,
            queuedCount = content.queuedCount,
            slow = content.slow,
            onStop = onStop,
            empty = empty ?: welcome,
        )
        MonarchAiPromptInput(
            label = monarchString(labels.inputLabel, R.string.monarch_aiassistantpanel_input_label),
            state = prompt,
            onSubmit = onSend,
            busy = tokens.busy(content.status),
            onStop = onStop,
            focusRequester = promptFocusRequester,
            toolbar = toolbar,
            footer = footer,
        )
    }
}

/** The greeting with the agent and the starters, scrolling when the panel is short. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiAssistantPanelWelcome(
    agent: MonarchAiAgentRef?,
    agentImage: Painter?,
    starters: List<MonarchAiStarterPrompt>,
    status: MonarchAiConversationStatus,
    labels: MonarchAiAssistantPanelLabels,
    onSend: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val startersLabel = monarchString(labels.starters, R.string.monarch_aiassistantpanel_starters)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        MonarchAiWelcome(
            title = monarchString(labels.welcome, R.string.monarch_aiassistantpanel_welcome),
            agent = agent,
            agentImage = agentImage,
            // As the web: no assistant tile when the app named no agent; only the question and the starters.
            icon = null,
            starters = if (starters.isEmpty()) {
                null
            } else {
                {
                    MonarchAiStarterPrompts(
                        prompts = starters,
                        onSelect = { starter ->
                            scope.launch {
                                // A refused send is not an error here: the person can type again.
                                val failure = runCatching { onSend(starter.prompt) }.exceptionOrNull()
                                if (failure is CancellationException) throw failure
                            }
                        },
                        layout = MonarchAiStarterPromptsLayout.Grid,
                        enabled = MonarchAiAssistantPanelTokens.startersEnabled(status),
                        hidePrompt = true,
                        label = startersLabel,
                    )
                }
            },
        )
    }
}
