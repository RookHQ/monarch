package com.rookhq.monarch.components.intelligence.aistarterprompts

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
