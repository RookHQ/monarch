@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiagentcard.MonarchAiAgentTool
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPicker
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerLayout
import dev.monarchui.components.intelligence.aiagentpicker.MonarchAiAgentPickerOption
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of AiAgentPicker. */
object MonarchAiAgentPickerSection : CataloguePage {
    override val title = "AiAgentPicker"

    private val agents = listOf(
        MonarchAiAgentPickerOption(
            agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Finds and summarizes sources"),
            description = "Reads documents in this space and answers with citations.",
            capabilities = listOf("Summarizes", "Cites sources"),
            tools = listOf(MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Read file")),
        ),
        MonarchAiAgentPickerOption(
            agent = MonarchAiAgentRef(name = "Writing agent", id = "writer", role = "Drafts and edits text"),
            description = "Drafts replies and tightens wording in the tone you choose.",
            capabilities = listOf("Drafts", "Rewrites"),
            tools = listOf(MonarchAiAgentTool("Insert text")),
            state = MonarchAiAgentState.Working,
        ),
        MonarchAiAgentPickerOption(
            agent = MonarchAiAgentRef(name = "Scheduling agent", id = "scheduler", role = "Plans meetings"),
            description = "Finds times that work and drafts invitations. Not enabled in this space.",
            enabled = false,
        ),
    )

    @Composable
    override fun Content() {
        var chosen by rememberSaveable { mutableStateOf<String?>("research") }
        var grid by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("A list of agents")
        MonarchAiAgentPicker(agents = agents, selected = chosen, onSelect = { chosen = it })
        MonarchText(text = "Chosen: ${chosen ?: "none"}", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Grid, two columns on wide windows")
        MonarchAiAgentPicker(
            agents = agents,
            selected = grid,
            onSelect = { grid = it },
            layout = MonarchAiAgentPickerLayout.Grid,
        )
        CatalogueCaption("No agents")
        MonarchAiAgentPicker(agents = emptyList(), selected = null, onSelect = {})
    }
}
