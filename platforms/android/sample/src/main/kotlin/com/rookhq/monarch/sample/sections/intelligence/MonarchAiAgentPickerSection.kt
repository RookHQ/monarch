@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentTool
import com.rookhq.monarch.components.intelligence.aiagentpicker.MonarchAiAgentPicker
import com.rookhq.monarch.components.intelligence.aiagentpicker.MonarchAiAgentPickerLayout
import com.rookhq.monarch.components.intelligence.aiagentpicker.MonarchAiAgentPickerOption
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
