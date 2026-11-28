package dev.monarchui.components.search.facetedfilterpanel

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewFacets = listOf(
    MonarchFacet.options(
        id = "status",
        label = "Status",
        options = listOf(
            MonarchFacetOption("open", "Open", count = 128),
            MonarchFacetOption("done", "Done", count = 1_204),
            MonarchFacetOption("archived", "Archived", count = 0, enabled = false),
        ),
        maxVisible = 2,
    ),
    MonarchFacet.range("size", "File size", bounds = 0f..100f, step = 5f, unit = "MB"),
    MonarchFacet.options("owner", "Owner", options = emptyList(), initiallyExpanded = false),
)

@MonarchPreviews
@Composable
internal fun MonarchFacetedFilterPanelPreview() {
    PreviewFrame {
        MonarchFacetedFilterPanel(
            facets = PreviewFacets,
            value = mapOf(
                "status" to MonarchFacetSelection.Options(setOf("done")),
                "size" to MonarchFacetSelection.Range(10f..50f),
            ),
            onValueChange = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchFacetedFilterPanelEmptyPreview() {
    PreviewFrame {
        MonarchFacetedFilterPanel(facets = PreviewFacets, value = emptyMap(), onValueChange = {}, showTitle = false)
    }
}
