package com.rookhq.monarch.components.search.searchbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.sampleicons.SampleIcons

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
