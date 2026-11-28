package dev.monarchui.components.search.searchbar

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.sampleicons.SampleIcons

@MonarchPreviews
@Composable
internal fun MonarchSearchBarPreview() {
    PreviewFrame {
        MonarchSearchBar(
            query = "",
            onQueryChange = {},
            onSearch = {},
            leadingIcon = { MonarchIcon(SampleIcons.Search, null) },
        )
        MonarchSearchBar(
            query = "invoices",
            onQueryChange = {},
            onSearch = {},
            leadingIcon = { MonarchIcon(SampleIcons.Search, null) },
            clearIcon = { MonarchIcon(SampleIcons.Close, null) },
        )
        MonarchSearchBar(query = "invoices", onQueryChange = {}, onSearch = {}, enabled = false)
    }
}
