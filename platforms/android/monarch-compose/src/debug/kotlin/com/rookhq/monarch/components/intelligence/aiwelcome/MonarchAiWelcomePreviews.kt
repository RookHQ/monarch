package com.rookhq.monarch.components.intelligence.aiwelcome

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
