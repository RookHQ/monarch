package com.rookhq.monarch.components.navigation.tableofcontents

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
