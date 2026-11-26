package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPrompts
import com.rookhq.monarch.components.intelligence.aistarterprompts.MonarchAiStarterPromptsLayout
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of AiStarterPrompts. */
object MonarchAiStarterPromptsSection : CataloguePage {
    override val title = "AiStarterPrompts"

    private val starters = listOf(
        MonarchAiStarterPrompt(
            id = "summary",
            title = "Summarise a document",
            prompt = "Summarise the attached document in five points.",
            icon = { MonarchIcon(SampleIcons.Document, contentDescription = null) },
        ),
        MonarchAiStarterPrompt("reply", "Draft a reply", "Draft a short, polite reply to this email."),
        MonarchAiStarterPrompt("plan", "Plan a meeting", "Propose an agenda for a one-hour planning meeting."),
    )

    @Composable
    override fun Content() {
        var chosen by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Grid")
        MonarchAiStarterPrompts(prompts = starters, onSelect = { chosen = it.prompt })
        if (chosen.isNotEmpty()) MonarchText(text = "Sent: $chosen")
        CatalogueCaption("List, prompt hidden")
        MonarchAiStarterPrompts(
            prompts = starters,
            onSelect = { chosen = it.prompt },
            layout = MonarchAiStarterPromptsLayout.List,
            hidePrompt = true,
        )
        CatalogueCaption("Disabled while a reply is generated")
        MonarchAiStarterPrompts(prompts = starters.take(1), onSelect = {}, enabled = false)
    }
}
