package dev.monarchui.components.intelligence.aistarterprompts

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val starters = listOf(
    MonarchAiStarterPrompt("summary", "Summarise a document", "Summarise the attached document in five points."),
    MonarchAiStarterPrompt("email", "Draft a reply", "Draft a short, polite reply to this email."),
)

@MonarchPreviews
@Composable
internal fun MonarchAiStarterPromptsPreview() {
    PreviewFrame {
        MonarchAiStarterPrompts(prompts = starters, onSelect = {})
        MonarchAiStarterPrompts(prompts = starters, onSelect = {}, layout = MonarchAiStarterPromptsLayout.List)
        MonarchAiStarterPrompts(prompts = starters, onSelect = {}, enabled = false, hidePrompt = true)
    }
}
