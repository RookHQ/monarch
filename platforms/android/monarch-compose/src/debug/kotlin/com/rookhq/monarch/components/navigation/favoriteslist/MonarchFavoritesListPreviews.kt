package com.rookhq.monarch.components.navigation.favoriteslist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewItems = listOf(
    MonarchFavoriteItem("Roadmap", id = "roadmap", current = true),
    MonarchFavoriteItem("Q3 planning", id = "q3"),
    MonarchFavoriteItem("Design reviews", id = "design"),
)

@MonarchPreviews
@Composable
internal fun MonarchFavoritesListPreview() {
    PreviewFrame {
        MonarchFavoritesList(items = PreviewItems, onSelect = {}, onRemove = {}, onReorder = {})
        MonarchFavoritesList(items = emptyList(), onSelect = {}, emptyMessage = "Pin pages to find them here.")
    }
}

@MonarchPreviews
@Composable
internal fun MonarchFavoritesListEditingPreview() {
    PreviewFrame {
        MonarchFavoritesList(items = PreviewItems, onSelect = {}, editing = true, onRemove = {}, onReorder = {})
        MonarchFavoritesList(items = PreviewItems, onSelect = {}, editing = true, onRemove = {})
    }
}
