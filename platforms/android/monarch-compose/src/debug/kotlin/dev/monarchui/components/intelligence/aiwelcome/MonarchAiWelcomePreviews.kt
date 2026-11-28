package dev.monarchui.components.intelligence.aiwelcome

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiWelcomePreview() {
    PreviewFrame {
        MonarchAiWelcome(
            title = "How can I help?",
            agent = MonarchAiAgentRef(name = "Dossier assistant", role = "Writing assistant"),
            description = "I can read the page you are on and the documents you share with me.",
            footer = { MonarchAiDisclaimer() },
        ) {
            MonarchAiStarterPrompts(
                prompts = listOf(MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page.")),
                onSelect = {},
            )
        }
        MonarchAiWelcome(
            title = "Ask about your projects",
            capabilities = listOf("Find a project by name or owner", "Summarize what changed this week"),
        )
    }
}
