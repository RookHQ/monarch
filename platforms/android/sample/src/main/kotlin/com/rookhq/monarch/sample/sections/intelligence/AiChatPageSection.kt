@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamEvent
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransportRequest
import com.rookhq.monarch.components.intelligence.aiconversation.rememberMonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiconversationheader.MonarchAiConversationHeader
import com.rookhq.monarch.components.intelligence.aiconversationlist.MonarchAiConversationList
import com.rookhq.monarch.components.intelligence.aiconversationlist.MonarchAiConversationSummary
import com.rookhq.monarch.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import com.rookhq.monarch.components.intelligence.aipromptinput.MonarchAiPromptInput
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPromptsLayout
import com.rookhq.monarch.components.intelligence.aithread.MonarchAiThread
import com.rookhq.monarch.components.intelligence.aiwelcome.MonarchAiWelcome
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val ChatPageWordMillis = 120L

/** A transport that echoes the prompt word by word, standing in for a backend. Monarch does no networking. */
private class ChatPageEchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flow {
        val prompt = request.messages.lastOrNull()?.text.orEmpty()
        "You asked: $prompt This reply comes from a stand-in transport.".split(" ").forEach { word ->
            delay(ChatPageWordMillis)
            emit(MonarchAiStreamEvent.TextDelta("$word "))
        }
        emit(MonarchAiStreamEvent.Done())
    }
}

/**
 * The catalogue page of AiChatPage: a recipe of existing components on the
 * list-detail pattern. The history, and the open conversation with its
 * header, thread and prompt input.
 *
 * Apps use the Material NavigableListDetailPaneScaffold, as the inventory
 * example shows. The catalogue app does not depend on the adaptive artifacts,
 * so, like AiEvalReviewSection, this page shows the same behaviour with a Row
 * and BackHandler: both panes from 600dp, one pane at a time below it.
 */
object AiChatPageSection : CataloguePage {
    override val title = "AiChatPage"

    private const val HeightSteps = 6
    private const val ListShare = 0.4f
    private const val Now = 1_794_664_800_000L
    private const val Day = 86_400_000L
    private val MediumWidth = 600.dp

    private val starters = listOf(
        MonarchAiStarterPrompt("sum", "Summarize my week", "Summarize my week."),
        MonarchAiStarterPrompt("plan", "Plan a launch", "Plan a product launch."),
    )

    @Composable
    override fun Content() {
        val conversation = rememberMonarchAiConversation(ChatPageEchoTransport())
        val prompt = rememberTextFieldState()
        var conversations by remember {
            mutableStateOf(
                listOf(
                    MonarchAiConversationSummary("c1", "Weekly summary", updatedAtMillis = Now - Day / 12),
                    MonarchAiConversationSummary(
                        "c2",
                        "Release checklist",
                        updatedAtMillis = Now - 2 * Day,
                        pinned = true,
                    ),
                    MonarchAiConversationSummary("c3", "Interview questions", updatedAtMillis = Now - 20 * Day),
                ),
            )
        }
        var active by rememberSaveable { mutableStateOf("c1") }
        var showingChat by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Two panes from 600dp, one pane below")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HeightSteps)) {
            val width = maxWidth
            val twoPanes = width >= MediumWidth
            BackHandler(enabled = !twoPanes && showingChat) { showingChat = false }
            val open = { id: String ->
                active = id
                conversation.reset()
                showingChat = true
            }
            val history = @Composable { modifier: Modifier ->
                MonarchAiConversationList(
                    conversations = conversations,
                    onSelect = open,
                    modifier = modifier,
                    activeId = active,
                    nowMillis = Now,
                    onNew = {
                        val id = "c${conversations.size + 1}"
                        conversations = listOf(MonarchAiConversationSummary(id, "", Now)) + conversations
                        open(id)
                    },
                    onRename = { id, title ->
                        conversations = conversations.map { if (it.id == id) it.copy(title = title) else it }
                    },
                    onPin = { id, pinned ->
                        conversations = conversations.map { if (it.id == id) it.copy(pinned = pinned) else it }
                    },
                    onDelete = { id -> conversations = conversations.filterNot { it.id == id } },
                )
            }
            val chat = @Composable { modifier: Modifier ->
                Chat(
                    conversation = conversation,
                    prompt = prompt,
                    title = conversations.firstOrNull { it.id == active }?.title.orEmpty(),
                    modifier = modifier,
                    onBack = if (twoPanes) null else ({ showingChat = false }),
                    onRename = { title ->
                        conversations = conversations.map { if (it.id == active) it.copy(title = title) else it }
                    },
                )
            }
            when {
                twoPanes -> Row {
                    history(Modifier.width(width * ListShare))
                    MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
                    chat(Modifier.weight(1f))
                }
                showingChat -> chat(Modifier.fillMaxSize())
                else -> history(Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun Chat(
        conversation: MonarchAiConversation,
        prompt: TextFieldState,
        title: String,
        modifier: Modifier,
        onBack: (() -> Unit)?,
        onRename: (String) -> Unit,
    ) {
        val messages by conversation.messages.collectAsState()
        val status by conversation.status.collectAsState()
        val error by conversation.error.collectAsState()
        val agent by conversation.agent.collectAsState()
        Column(modifier = modifier) {
            MonarchAiConversationHeader(
                title = title,
                agent = agent,
                onRename = onRename,
                navigationIcon = onBack?.let { back ->
                    {
                        IconButton(onClick = back) {
                            Icon(SampleIcons.History, contentDescription = "Conversation history")
                        }
                    }
                },
            )
            MonarchAiThread(
                messages = messages,
                modifier = Modifier.weight(1f).padding(horizontal = MonarchTheme.spacing.space4),
                status = status,
                error = error,
                onStop = conversation::stop,
                empty = {
                    MonarchAiWelcome(title = "How can I help?", agent = agent) {
                        MonarchAiStarterPrompts(
                            prompts = starters,
                            onSelect = { conversation.send(it.prompt) },
                            layout = MonarchAiStarterPromptsLayout.List,
                        )
                    }
                },
            )
            MonarchAiPromptInput(
                label = "Message the assistant",
                state = prompt,
                onSubmit = { text -> check(conversation.send(text)) { "Not sent" } },
                modifier = Modifier.padding(MonarchTheme.spacing.space4),
                busy = status == MonarchAiConversationStatus.Submitted ||
                    status == MonarchAiConversationStatus.Streaming,
                onStop = conversation::stop,
                footer = { MonarchAiDisclaimer() },
            )
        }
    }
}
