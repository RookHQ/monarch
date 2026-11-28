package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aidisclaimer.MonarchAiDisclaimer
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPromptsLayout
import dev.monarchui.components.intelligence.aiwelcome.MonarchAiWelcome
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SampleStarters = listOf(
    MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page."),
    MonarchAiStarterPrompt("draft", "Draft a reply", "Draft a reply to the last message."),
)

/** The catalogue page of AiWelcome. */
object MonarchAiWelcomeSection : CataloguePage {
    override val title = "AiWelcome"

    @Composable
    override fun Content() {
        var draft by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A new conversation with an agent, starters and a disclaimer")
        MonarchAiWelcome(
            title = "How can I help?",
            agent = MonarchAiAgentRef(name = "Dossier assistant", role = "Writing assistant"),
            description = "I can read the page you are on and the documents you share with me.",
            footer = { MonarchAiDisclaimer() },
        ) {
            MonarchAiStarterPrompts(
                prompts = SampleStarters,
                onSelect = { draft = it.prompt },
                layout = MonarchAiStarterPromptsLayout.List,
            )
        }
        if (draft.isNotEmpty()) MonarchText(text = "Composer: $draft")
        CatalogueCaption("Capabilities, without an agent")
        MonarchAiWelcome(
            title = "Ask about your projects",
            capabilities = listOf("Find a project by name or owner", "Summarize what changed this week"),
        )
    }
}
