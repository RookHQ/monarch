package com.rookhq.monarch.components.search.searchresults

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

private val PreviewGroups = listOf(
    MonarchSearchResultGroup(
        title = "Documents",
        id = "docs",
        total = 24,
        results = listOf(
            MonarchSearchResult(
                title = "Travel budget 2026",
                id = "d1",
                path = listOf("Finance", "Budgets"),
                snippet = "The travel budget covers flights and hotels.",
                meta = "Edited 2 days ago",
            ),
        ),
    ),
    MonarchSearchResultGroup(
        title = "People",
        id = "people",
        results = listOf(MonarchSearchResult(title = "Budget owner: Ana Lima", id = "p1")),
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchSearchResultsPreview() {
    PreviewFrame {
        MonarchSearchResults(
            groups = PreviewGroups,
            onSelect = { _, _ -> },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * 4),
            query = "budget",
            onShowAll = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchSearchResultsStatesPreview() {
    PreviewFrame {
        val height = Modifier.height(MonarchTheme.spacing.space24 * 2)
        MonarchSearchResults(
            groups = emptyList(),
            onSelect = { _, _ -> },
            modifier = height,
            query = "budget",
            loading = true,
            loadingRows = 2,
        )
        MonarchSearchResults(groups = emptyList(), onSelect = { _, _ -> }, modifier = height, query = "zebra")
        MonarchSearchResults(
            groups = emptyList(),
            onSelect = { _, _ -> },
            modifier = height,
            failed = true,
            errorMessage = "The service did not answer.",
            onRetry = {},
        )
    }
}
