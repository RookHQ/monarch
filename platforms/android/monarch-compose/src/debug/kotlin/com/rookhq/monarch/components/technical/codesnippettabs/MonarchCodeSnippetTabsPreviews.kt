package com.rookhq.monarch.components.technical.codesnippettabs

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewSnippets = listOf(
    MonarchCodeSnippet("cURL", code = "curl https://api.example.com/v1/items", language = "bash"),
    MonarchCodeSnippet("Kotlin", code = "val items = client.items.list()", language = "kotlin"),
    MonarchCodeSnippet("Swift", code = "let items = try await client.items.list()", language = "swift"),
)

@MonarchPreviews
@Composable
internal fun MonarchCodeSnippetTabsPreview() {
    PreviewFrame {
        MonarchCodeSnippetTabs(snippets = PreviewSnippets)
        MonarchCodeSnippetTabs(snippets = PreviewSnippets.take(1), showLineNumbers = true)
    }
}
