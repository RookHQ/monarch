package dev.monarchui.components.navigation.tableofcontents

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTableOfContentsPreview() {
    PreviewFrame {
        MonarchTableOfContents(
            items = listOf(
                MonarchTableOfContentsItem("Overview", "overview"),
                MonarchTableOfContentsItem("Install", "install"),
                MonarchTableOfContentsItem("With Gradle", "gradle", level = 3),
                MonarchTableOfContentsItem("Usage", "usage"),
            ),
            selectedId = "install",
            onSelect = {},
        )
    }
}
