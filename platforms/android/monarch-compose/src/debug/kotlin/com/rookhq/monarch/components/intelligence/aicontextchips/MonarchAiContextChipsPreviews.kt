package com.rookhq.monarch.components.intelligence.aicontextchips

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiContextChipsPreview() {
    val items = listOf(
        MonarchAiContextItem("contract", MonarchAiContextKind.File, "contract.pdf", tokens = 1200),
        MonarchAiContextItem("rows", MonarchAiContextKind.Selection, "Selected rows", tokens = 300),
        MonarchAiContextItem("page", MonarchAiContextKind.Page, "Q3 planning", tokens = 2100, removable = false),
    )
    PreviewFrame {
        MonarchAiContextChips(items = items, onRemove = {})
        MonarchAiContextChips(items = items, enabled = false)
        MonarchAiContextChips(items = emptyList(), emptyText = "No context added")
    }
}
