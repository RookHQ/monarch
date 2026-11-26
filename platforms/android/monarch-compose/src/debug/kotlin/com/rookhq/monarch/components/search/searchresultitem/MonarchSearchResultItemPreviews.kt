package com.rookhq.monarch.components.search.searchresultitem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
