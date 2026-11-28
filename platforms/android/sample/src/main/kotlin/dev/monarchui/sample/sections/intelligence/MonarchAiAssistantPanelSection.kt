@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiassistantpanel.MonarchAiAssistantPanel
import dev.monarchui.components.intelligence.aiassistantpanel.MonarchAiAssistantPanelPlacement
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiStreamEvent
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTransport
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTransportRequest
import dev.monarchui.components.intelligence.aiconversation.rememberMonarchAiConversation
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A transport that echoes the prompt word by word, standing in for a backend. Monarch does no networking. */
private class AssistantPanelEchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flow {
        emit(MonarchAiStreamEvent.Agent(MonarchAiAgentRef(name = "Dossier", role = "Workspace assistant")))
        val prompt = request.messages.lastOrNull()?.text.orEmpty()
        "You asked: $prompt This reply comes from a stand-in transport.".split(" ").forEach { word ->
            delay(AssistantPanelWordMillis)
            emit(MonarchAiStreamEvent.TextDelta("$word "))
        }
        emit(MonarchAiStreamEvent.Done())
    }
}

private const val AssistantPanelWordMillis = 120L

/**
 * The catalogue page of AiAssistantPanel: the panel wired to a conversation,
 * as a sheet on phones (a pane on wide windows), and an inline pane that can
 * be minimized.
 */
object MonarchAiAssistantPanelSection : CataloguePage {
    override val title = "AiAssistantPanel"

    private const val PaneHeight = 480

    private val starters = listOf(
        MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page."),
        MonarchAiStarterPrompt("week", "What changed this week?", "What changed this week?"),
        MonarchAiStarterPrompt("draft", "Draft a status update", "Draft a status update."),
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var minimized by rememberSaveable { mutableStateOf(false) }
        var context by remember {
            mutableStateOf(
                listOf(
                    MonarchAiContextItem("page", MonarchAiContextKind.Page, "Quarterly plan"),
                    MonarchAiContextItem("rows", MonarchAiContextKind.Selection, "3 rows"),
                ),
            )
        }
        val conversation = rememberMonarchAiConversation(AssistantPanelEchoTransport())
        val prompt = rememberTextFieldState()
        CatalogueCaption("A sheet on phones, a pane on wide windows")
        MonarchButton(text = "Open the assistant", onClick = { open = true }, variant = MonarchButtonVariant.Primary)
        MonarchAiAssistantPanel(
            conversation = conversation,
            title = "Assistant",
            open = open,
            onOpenChange = { open = it },
            prompt = prompt,
            context = context,
            starters = starters,
            onRemoveContext = { id -> context = context.filterNot { it.id == id } },
            footer = { MonarchAiDisclaimer() },
        )
        CatalogueCaption("A pane in the layout, with minimize")
        val paneConversation = rememberMonarchAiConversation(AssistantPanelEchoTransport())
        MonarchAiAssistantPanel(
            conversation = paneConversation,
            title = "Assistant",
            open = true,
            onOpenChange = {},
            modifier = if (minimized) Modifier else Modifier.height(PaneHeight.dp),
            placement = MonarchAiAssistantPanelPlacement.Inline,
            starters = starters,
            minimized = minimized,
            onMinimizedChange = { minimized = it },
        )
    }
}
