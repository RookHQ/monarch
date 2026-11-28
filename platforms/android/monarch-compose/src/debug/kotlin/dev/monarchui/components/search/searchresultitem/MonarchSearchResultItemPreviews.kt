package dev.monarchui.components.search.searchresultitem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchSearchResultItemPreview() {
    PreviewFrame {
        MonarchSearchResultItem(
            title = "Travel policy",
            path = listOf("Handbook", "Finance"),
            snippet = "Book travel at least two weeks ahead through the portal.",
            terms = monarchQueryTerms("travel"),
            onClick = {},
            meta = { Text("2 days ago") },
        )
        MonarchSearchResultItem(
            title = "Q3 travel budget.xlsx",
            fileName = "Q3 travel budget.xlsx",
            snippet = "Flights and <mark>hotels</mark> by team.",
            snippetMarkTags = true,
            onClick = {},
        )
        MonarchSearchResultItem(
            title = "Expense reports",
            path = listOf("Handbook"),
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchSearchResultItem(title = "Archived page", enabled = false, onClick = {})
    }
}
