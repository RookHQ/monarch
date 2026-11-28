@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiagentcard

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
