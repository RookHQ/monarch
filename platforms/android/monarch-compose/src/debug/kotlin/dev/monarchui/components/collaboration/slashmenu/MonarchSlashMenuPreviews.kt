package dev.monarchui.components.collaboration.slashmenu

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewItems = listOf(
    MonarchSlashMenuItem(id = "heading", label = "Heading", description = "Large section title", group = "Text"),
    MonarchSlashMenuItem(id = "bullets", label = "Bulleted list", description = "A simple list", group = "Lists"),
    MonarchSlashMenuItem(id = "table", label = "Table", description = "Not available in replies", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchSlashMenuPreview() {
    PreviewFrame {
        MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/"), items = PreviewItems, onSelect = {})
        MonarchSlashMenu(query = MonarchSlashMenuQuery.find("/zzz"), items = PreviewItems, onSelect = {})
    }
}
