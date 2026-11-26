@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCard
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentCardVariant
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentTool
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiAgentCard. */
object MonarchAiAgentCardSection : CataloguePage {
    override val title = "AiAgentCard"

    private val agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Analyst")
    private val tools = listOf(
        MonarchAiAgentTool("Search documents", "Finds files in the shared drive"),
        MonarchAiAgentTool("Write brief", "Drafts a one-page brief from the sources it found"),
    )

    @Composable
    override fun Content() {
        var chats by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("The full card with actions")
        MonarchAiAgentCard(
            agent = agent,
            description = "Reads the shared drive and writes short briefs for the team.",
            capabilities = listOf("Summarises documents", "Drafts briefs", "Finds sources"),
            tools = tools,
            dataAccess = listOf("Documents in this space", "Your calendar"),
            owner = "Sam Rivera",
        ) {
            MonarchButton(text = "Start a chat", onClick = { chats += 1 }, variant = MonarchButtonVariant.Primary)
            MonarchButton(text = "Settings", onClick = {})
        }
        MonarchText(text = "Chats started: $chats", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Compact, for lists and pickers")
        MonarchAiAgentCard(
            agent = agent,
            description = "Reads the shared drive and writes short briefs.",
            capabilities = listOf("Summarises documents", "Drafts briefs"),
            tools = tools,
            state = MonarchAiAgentState.Working,
            variant = MonarchAiAgentCardVariant.Compact,
        )
        MonarchAiAgentCard(
            agent = MonarchAiAgentRef(name = "Triage agent", id = "triage", role = "Support"),
            state = MonarchAiAgentState.Waiting,
            variant = MonarchAiAgentCardVariant.Compact,
        )
    }
}
