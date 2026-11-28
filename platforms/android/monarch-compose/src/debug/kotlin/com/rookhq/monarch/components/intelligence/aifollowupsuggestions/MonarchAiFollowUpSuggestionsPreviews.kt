package com.rookhq.monarch.components.intelligence.aifollowupsuggestions

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val followUps = listOf(
    "What changed since last quarter?",
    "Show it as a table",
    "Which customers are at risk of leaving this month, and why?",
)

@MonarchPreviews
@Composable
internal fun MonarchAiFollowUpSuggestionsPreview() {
    PreviewFrame {
        MonarchAiFollowUpSuggestions(suggestions = followUps, onSelect = {})
        MonarchAiFollowUpSuggestions(suggestions = followUps.take(2), onSelect = {}, enabled = false)
    }
}
