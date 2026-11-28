package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aiinlineprompt.MonarchAiInlinePrompt
import com.rookhq.monarch.components.intelligence.aiinlineprompt.MonarchAiInlinePromptArea
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val SectionParagraph =
    "The team shipped the new onboarding flow in March. Early numbers suggest more people finish setup " +
        "on their first visit, although the sample is still small and support tickets have not changed much yet."

/** The catalogue page of AiInlinePrompt. */
object MonarchAiInlinePromptSection : CataloguePage {
    override val title = "AiInlinePrompt"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Select text, then pick Ask AI in the menu; or use the button")
        MonarchAiInlinePromptArea(onAskAi = { open = true }) {
            SelectionContainer { MonarchText(text = SectionParagraph) }
        }
        MonarchButton(text = "Edit paragraph with AI", onClick = { open = true }, size = MonarchButtonSize.Sm)
        if (last.isNotEmpty()) MonarchText(text = "Sent: $last")
        if (open) {
            MonarchAiInlinePrompt(
                onDismissRequest = { open = false },
                onSubmit = { last = it },
                context = SectionParagraph,
                suggestions = listOf("Shorten", "Make it more formal", "Fix spelling and grammar"),
            )
        }
    }
}
