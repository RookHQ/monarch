@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.intelligence.aiassistantpanel.MonarchAiAssistantPanel
import dev.monarchui.components.intelligence.aiassistantpanel.MonarchAiAssistantPanelPlacement
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiStreamEvent
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTransport
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTransportRequest
import dev.monarchui.components.intelligence.aiconversation.rememberMonarchAiConversation
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncher
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerOrientation
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val CopilotWordMillis = 120L

/**
 * A transport that echoes the prompt and the selection it received, word by
 * word, standing in for a backend. Monarch does no networking.
 */
private class CopilotEchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flow {
        emit(MonarchAiStreamEvent.Agent(MonarchAiAgentRef(name = "Assistant", role = "Copilot")))
        val prompt = request.messages.lastOrNull()?.text.orEmpty()
        val selection = (request.body["selection"] as? MonarchAiJSON.Array)?.values.orEmpty()
            .mapNotNull { ((it as? MonarchAiJSON.Object)?.members?.get("text") as? MonarchAiJSON.Text)?.value }
        val seen = if (selection.isEmpty()) {
            "Nothing is selected on the page."
        } else {
            "I can see your selection: ${selection.joinToString()}."
        }
        "You asked: $prompt $seen This reply comes from a stand-in transport.".split(" ").forEach { word ->
            delay(CopilotWordMillis)
            emit(MonarchAiStreamEvent.TextDelta("$word "))
        }
        emit(MonarchAiStreamEvent.Done())
    }
}

/**
 * The catalogue page of AiCopilotLayout: a recipe of existing components. The
 * app content (rows to select) with MonarchAiAssistantPanel docked beside it
 * on wide windows and as a sheet below, opened by MonarchAiLauncher. The
 * selection shows as a chip and is sent with the next prompt.
 *
 * Apps use the Material SupportingPaneScaffold, as the inventory example
 * shows. The catalogue app does not depend on the adaptive artifacts, so, like
 * AiChatPageSection, this page shows the same behaviour with a Row and
 * BackHandler: docked from 840dp, a sheet below it.
 */
object AiCopilotLayoutSection : CataloguePage {
    override val title = "AiCopilotLayout"

    private const val HeightSteps = 6
    private const val PaneShare = 0.45f
    private val ExpandedWidth = 840.dp
    private val PaneWidth = 384.dp

    private val rows = listOf("Northwind renewal", "Harbor expansion", "Summit pilot", "Atlas migration")

    private val starters = listOf(
        MonarchAiStarterPrompt("sum", "Summarize the selection", "Summarize the selection."),
        MonarchAiStarterPrompt("risk", "Which one is most at risk?", "Which one is most at risk?"),
    )

    @Composable
    override fun Content() {
        val conversation = rememberMonarchAiConversation(CopilotEchoTransport())
        val prompt = rememberTextFieldState()
        var open by rememberSaveable { mutableStateOf(false) }
        var selected by remember { mutableStateOf(listOf<String>()) }
        val context = listOfNotNull(
            MonarchAiContextItem("page", MonarchAiContextKind.Page, "Opportunities", removable = false),
            if (selected.isEmpty()) {
                null
            } else {
                MonarchAiContextItem("selection", MonarchAiContextKind.Selection, "${selected.size} rows")
            },
        )
        // Sent with each message from the panel; the conversation's own body stays as it is.
        val selectionBody = if (selected.isEmpty()) {
            emptyMap()
        } else {
            mapOf(
                "selection" to MonarchAiJSON.Array(
                    listOf(
                        MonarchAiJSON.Object(
                            mapOf(
                                "label" to MonarchAiJSON.Text("Selected"),
                                "text" to MonarchAiJSON.Text(selected.joinToString("; ")),
                            ),
                        ),
                    ),
                ),
            )
        }
        CatalogueCaption("Docked from 840dp, a sheet below")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HeightSteps)) {
            val pageWidth = maxWidth
            val docked = pageWidth >= ExpandedWidth
            BackHandler(enabled = docked && open) { open = false }
            val assistant = @Composable { modifier: Modifier ->
                MonarchAiAssistantPanel(
                    conversation = conversation,
                    title = "Assistant",
                    open = open,
                    onOpenChange = { open = it },
                    modifier = modifier,
                    placement = if (docked) {
                        MonarchAiAssistantPanelPlacement.Inline
                    } else {
                        MonarchAiAssistantPanelPlacement.Sheet
                    },
                    prompt = prompt,
                    context = context,
                    starters = starters,
                    onRemoveContext = { id -> if (id == "selection") selected = emptyList() },
                    body = selectionBody,
                    footer = { MonarchAiDisclaimer() },
                )
            }
            Row {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(MonarchTheme.spacing.space4),
                        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                    ) {
                        MonarchText(text = "Opportunities", style = MonarchTextStyle.Heading)
                        MonarchText(
                            text = "Select rows, then ask the assistant about them.",
                            style = MonarchTextStyle.Caption,
                        )
                        rows.forEach { row ->
                            MonarchCheckboxRow(
                                checked = row in selected,
                                onCheckedChange = { on -> selected = if (on) selected + row else selected - row },
                                label = row,
                            )
                        }
                    }
                    if (!(docked && open)) {
                        MonarchAiLauncher(
                            open = open,
                            onOpenChange = { open = it },
                            icon = {
                                MonarchIcon(
                                    SampleIcons.Bot,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(MonarchTheme.spacing.space4),
                        )
                    }
                }
                if (docked && open) {
                    MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
                    assistant(Modifier.width(min(pageWidth * PaneShare, PaneWidth)).fillMaxHeight())
                }
            }
            if (!docked) assistant(Modifier)
        }
    }
}
