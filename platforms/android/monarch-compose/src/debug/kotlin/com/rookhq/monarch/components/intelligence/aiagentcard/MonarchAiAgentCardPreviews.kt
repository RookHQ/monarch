@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiagentcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Analyst")

@MonarchPreviews
@Composable
internal fun MonarchAiAgentCardPreview() {
    PreviewFrame {
        MonarchAiAgentCard(
            agent = Agent,
            description = "Reads the shared drive and writes short briefs.",
            capabilities = listOf("Summarises documents", "Drafts briefs"),
            tools = listOf(MonarchAiAgentTool("Search documents", "Finds files in the shared drive")),
            dataAccess = listOf("Documents in this space"),
            owner = "Sam Rivera",
        ) {
            MonarchButton(text = "Start a chat", onClick = {})
        }
        MonarchAiAgentCard(
            agent = Agent,
            description = "Reads the shared drive and writes short briefs.",
            capabilities = listOf("Summarises documents"),
            tools = listOf(MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Write brief")),
            variant = MonarchAiAgentCardVariant.Compact,
        )
    }
}
