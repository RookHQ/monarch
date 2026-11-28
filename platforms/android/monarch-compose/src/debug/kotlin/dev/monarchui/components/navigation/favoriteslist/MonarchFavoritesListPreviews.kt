package dev.monarchui.components.navigation.favoriteslist

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
