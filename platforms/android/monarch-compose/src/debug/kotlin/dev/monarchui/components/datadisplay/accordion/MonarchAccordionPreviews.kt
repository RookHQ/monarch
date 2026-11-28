package dev.monarchui.components.datadisplay.accordion

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val previewItems = listOf(
    MonarchAccordionItem("billing", "How does billing work?"),
    MonarchAccordionItem("cancel", "Can I cancel at any time?"),
    MonarchAccordionItem("legacy", "Legacy plans", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchAccordionPreview() {
    PreviewFrame {
        MonarchAccordion(
            items = previewItems,
            expanded = setOf("billing"),
            onExpandedChange = {},
            trailingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        ) { item -> Text("The answer to ${item.title.lowercase()}") }
    }
}
