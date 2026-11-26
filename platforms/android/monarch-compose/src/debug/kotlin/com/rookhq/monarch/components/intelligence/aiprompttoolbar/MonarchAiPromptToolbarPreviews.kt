package com.rookhq.monarch.components.intelligence.aiprompttoolbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiPromptToolbarPreview() {
    PreviewFrame {
        MonarchAiPromptToolbar {
            MonarchAiPromptToolbarButton(text = "Attach", onClick = {})
            MonarchAiPromptToolbarToggle(text = "Search the web", checked = true, onCheckedChange = {})
            MonarchAiPromptToolbarToggle(text = "Deep research", checked = false, onCheckedChange = {})
            MonarchAiPromptToolbarToggle(text = "Think", checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
