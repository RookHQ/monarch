@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration.linkeddocspanel

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewItems = listOf(
    MonarchLinkedDocsPanelItem(
        id = "l1",
        title = "Release runbook",
        docId = "d1",
        meta = "Updated 2 hours ago",
        status = "Published",
        spaceId = "eng",
        spaceName = "Engineering",
    ),
    MonarchLinkedDocsPanelItem(
        id = "l2",
        title = "Design file",
        kind = MonarchLinkedDocsPanelKind.Link,
        url = "https://design.example.com/file/123",
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchLinkedDocsPanelPreview() {
    PreviewFrame {
        MonarchLinkedDocsPanel(
            items = PreviewItems,
            onOpen = {},
            docOptions = listOf(MonarchLinkedDocsPanelOption("d3", "Incident review template")),
            onLinkDoc = {},
            onAddLink = { _, _ -> },
            onUnlink = {},
        )
        MonarchLinkedDocsPanel(items = emptyList(), onOpen = {}, onAddLink = { _, _ -> })
        MonarchLinkedDocsPanel(items = null, onOpen = {})
        MonarchLinkedDocsPanel(items = PreviewItems.take(1), onOpen = {}, readOnly = true)
    }
}
