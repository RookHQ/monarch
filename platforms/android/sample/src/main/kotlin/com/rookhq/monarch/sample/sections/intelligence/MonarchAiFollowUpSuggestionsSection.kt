package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aifollowupsuggestions.MonarchAiFollowUpSuggestions
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiFollowUpSuggestions. */
object MonarchAiFollowUpSuggestionsSection : CataloguePage {
    override val title = "AiFollowUpSuggestions"

    private val followUps = listOf(
        "What changed since last quarter?",
        "Show it as a table",
        "Which customers are at risk of leaving this month, and why?",
    )

    @Composable
    override fun Content() {
        var sent by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("After a reply")
        MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = { sent = it })
        if (sent.isNotEmpty()) MonarchText(text = "Sent: $sent")
        CatalogueCaption("Disabled while a reply is generated")
        MonarchAiFollowUpSuggestions(suggestions = followUps.take(2), onSelect = {}, enabled = false)
    }
}
