@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnector
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPicker
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorStatus
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorTool
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolPermission
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolRisk
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiStreamEvent
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransport
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTransportRequest
import com.rookhq.monarch.components.intelligence.aiconversation.rememberMonarchAiConversation
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeScope
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSource
import com.rookhq.monarch.components.intelligence.aiknowledgescope.MonarchAiKnowledgeSourceKind
import com.rookhq.monarch.components.intelligence.aipromptinput.MonarchAiPromptInput
import com.rookhq.monarch.components.intelligence.aithread.MonarchAiThread
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val AgentBuilderWordMillis = 120L

/** A transport that answers with the unsaved definition it received, standing in for a backend. */
private class AgentBuilderEchoTransport : MonarchAiTransport {
    override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> = flow {
        val agent = request.body["agent"] as? MonarchAiJSON.Object
        val name = (agent?.members?.get("name") as? MonarchAiJSON.Text)?.value?.ifBlank { null } ?: "The agent"
        val tools = (agent?.members?.get("tools") as? MonarchAiJSON.Array)?.values?.size ?: 0
        val prompt = request.messages.lastOrNull()?.text.orEmpty()
        "$name received: $prompt It runs your unsaved settings with $tools tools.".split(" ").forEach { word ->
            delay(AgentBuilderWordMillis)
            emit(MonarchAiStreamEvent.TextDelta("$word "))
        }
        emit(MonarchAiStreamEvent.Done())
    }
}

/**
 * The catalogue page of AiAgentBuilder: a recipe of existing components.
 * Configure and Test tabs on phones, the form and the test chat side by side
 * from 840dp.
 *
 * Apps decide the layout with currentWindowAdaptiveInfoV2, as the inventory
 * example shows. The catalogue app does not depend on the adaptive artifacts,
 * so this page measures its own width.
 */
object AiAgentBuilderSection : CataloguePage {
    override val title = "AiAgentBuilder"

    private const val HeightSteps = 6
    private const val MaxInstructions = 8000
    private val ExpandedWidth = 840.dp

    private val sources = listOf(
        MonarchAiKnowledgeSource(
            id = "handbook",
            label = "Team handbook",
            kind = MonarchAiKnowledgeSourceKind.Space,
            children = listOf(
                MonarchAiKnowledgeSource("onboarding", "Onboarding guide", MonarchAiKnowledgeSourceKind.Document),
                MonarchAiKnowledgeSource("policies", "Policies", MonarchAiKnowledgeSourceKind.Document),
            ),
        ),
        MonarchAiKnowledgeSource("tickets", "Support tickets", MonarchAiKnowledgeSourceKind.App),
    )

    private val initialConnectors = listOf(
        MonarchAiConnector(
            id = "docs",
            name = "Documents",
            status = MonarchAiConnectorStatus.Connected,
            enabled = true,
            tools = listOf(
                MonarchAiConnectorTool(
                    "search",
                    "Search documents",
                    MonarchAiConnectorToolRisk.Read,
                    MonarchAiConnectorToolPermission.Allow,
                ),
                MonarchAiConnectorTool(
                    "tasks",
                    "Create tasks",
                    MonarchAiConnectorToolRisk.Write,
                    MonarchAiConnectorToolPermission.Ask,
                ),
            ),
        ),
        MonarchAiConnector(id = "mail", name = "Mail", status = MonarchAiConnectorStatus.NeedsSignIn, enabled = false),
    )

    /** The agent being edited. An app keeps it in its view model. */
    private data class Definition(
        val name: String = "Onboarding guide",
        val role: String = "Helps new teammates find answers",
        val instructions: String = "Answer from the team handbook. Keep answers short and name the source.",
        val connectors: List<MonarchAiConnector>,
        val knowledge: List<String> = listOf("handbook"),
    ) {
        val agent: MonarchAiAgentRef
            get() = MonarchAiAgentRef(name = name.ifBlank { "New agent" }, role = role.ifBlank { null })

        fun toJson(): MonarchAiJSON = MonarchAiJSON.Object(
            mapOf(
                "name" to MonarchAiJSON.Text(name),
                "role" to MonarchAiJSON.Text(role),
                "instructions" to MonarchAiJSON.Text(instructions),
                "tools" to MonarchAiJSON.Array(
                    connectors.filter { it.enabled }.flatMap { connector ->
                        connector.tools.filter { it.permission != MonarchAiConnectorToolPermission.Never }
                            .map { MonarchAiJSON.Text(it.id) }
                    },
                ),
                "knowledge" to MonarchAiJSON.Array(knowledge.map { MonarchAiJSON.Text(it) }),
            ),
        )
    }

    @Composable
    override fun Content() {
        var definition by remember { mutableStateOf(Definition(connectors = initialConnectors)) }
        var triedSave by rememberSaveable { mutableStateOf(false) }
        var saved by rememberSaveable { mutableStateOf(false) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val conversation = rememberMonarchAiConversation(AgentBuilderEchoTransport())
        val edit = { next: Definition ->
            definition = next
            saved = false
        }
        CatalogueCaption("Tabs on phones, side by side from 840dp")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HeightSteps)) {
            val form = @Composable { modifier: Modifier ->
                Form(definition, triedSave, saved, modifier, edit) {
                    triedSave = true
                    // An app saves definition.toJson() on its server here.
                    saved = definition.name.isNotBlank()
                }
            }
            val test = @Composable { modifier: Modifier -> Test(conversation, definition, modifier) }
            if (maxWidth >= ExpandedWidth) {
                Row {
                    form(Modifier.weight(1f))
                    MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
                    test(Modifier.weight(1f))
                }
            } else {
                Column {
                    PrimaryTabRow(selectedTabIndex = tab) {
                        listOf("Configure", "Test").forEachIndexed { index, title ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                        }
                    }
                    if (tab == 0) form(Modifier.weight(1f)) else test(Modifier.weight(1f))
                }
            }
        }
    }

    @Composable
    private fun Form(
        definition: Definition,
        triedSave: Boolean,
        saved: Boolean,
        modifier: Modifier,
        onChange: (Definition) -> Unit,
        onSave: () -> Unit,
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()).padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
        ) {
            MonarchText(text = "Persona", style = MonarchTextStyle.Heading)
            MonarchAiAgentAvatar(agent = definition.agent, size = MonarchAvatarSize.Lg, decorative = true)
            MonarchTextField(
                value = definition.name,
                onValueChange = { onChange(definition.copy(name = it)) },
                label = "Name",
                errorText = if (triedSave && definition.name.isBlank()) "Give the agent a name." else null,
            )
            MonarchTextField(
                value = definition.role,
                onValueChange = { onChange(definition.copy(role = it)) },
                label = "Role",
                helperText = "One line shown under the name, such as Support assistant.",
            )
            MonarchText(text = "Instructions", style = MonarchTextStyle.Heading)
            MonarchTextField(
                value = definition.instructions,
                onValueChange = { onChange(definition.copy(instructions = it.take(MaxInstructions))) },
                label = "Instructions",
                helperText = "${definition.instructions.length} / $MaxInstructions characters",
                singleLine = false,
            )
            MonarchAiConnectorPicker(
                connectors = definition.connectors,
                onConnectorsChange = { onChange(definition.copy(connectors = it)) },
                title = "Tools",
                searchable = false,
            )
            MonarchText(text = "Knowledge", style = MonarchTextStyle.Heading)
            MonarchAiKnowledgeScope(
                sources = sources,
                selection = definition.knowledge,
                onSelectionChange = { onChange(definition.copy(knowledge = it)) },
                expandIcon = {
                    MonarchIcon(
                        imageVector = SampleIcons.ChevronRight,
                        contentDescription = null,
                        size = MonarchIconSize.Sm,
                    )
                },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(
                    text = if (saved) "Agent saved." else "",
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
                MonarchButton(text = "Save agent", onClick = onSave, variant = MonarchButtonVariant.Primary)
            }
        }
    }

    @Composable
    private fun Test(conversation: MonarchAiConversation, definition: Definition, modifier: Modifier) {
        val messages by conversation.messages.collectAsState()
        val status by conversation.status.collectAsState()
        val error by conversation.error.collectAsState()
        val prompt = rememberTextFieldState()
        val busy = status == MonarchAiConversationStatus.Submitted || status == MonarchAiConversationStatus.Streaming
        Column(
            modifier = modifier.padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAiAgentAvatar(
                    agent = definition.agent,
                    state = if (busy) MonarchAiAgentState.Working else MonarchAiAgentState.Idle,
                    size = MonarchAvatarSize.Sm,
                    decorative = true,
                )
                MonarchText(text = "Test", modifier = Modifier.weight(1f), style = MonarchTextStyle.Subheading)
                MonarchButton(
                    text = "Restart test",
                    onClick = {
                        conversation.stop()
                        conversation.reset()
                    },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = messages.isNotEmpty(),
                )
            }
            MonarchText(
                text = "Try the agent with the current settings. Nothing here is saved.",
                style = MonarchTextStyle.Caption,
            )
            MonarchAiThread(
                messages = messages,
                modifier = Modifier.weight(1f),
                status = status,
                error = error,
                daySeparators = false,
                onStop = conversation::stop,
                empty = { MonarchText(text = "Send a message to try the agent.") },
            )
            MonarchAiPromptInput(
                label = "Message the agent",
                state = prompt,
                onSubmit = { text ->
                    check(conversation.send(text, body = mapOf("agent" to definition.toJson()))) { "Not sent" }
                },
                busy = busy,
                onStop = conversation::stop,
            )
        }
    }
}
