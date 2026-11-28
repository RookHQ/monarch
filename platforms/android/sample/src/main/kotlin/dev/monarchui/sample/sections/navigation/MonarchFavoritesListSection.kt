package dev.monarchui.sample.sections.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoriteItem
import dev.monarchui.components.navigation.favoriteslist.MonarchFavoritesList
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val Pinned = listOf("Roadmap", "Q3 planning", "Design reviews", "Hiring plan")

/** The catalogue page of FavoritesList: pinned destinations to open, reorder and remove. */
object MonarchFavoritesListSection : CataloguePage {
    override val title = "FavoritesList"

    @Composable
    override fun Content() {
        var names by rememberSaveable { mutableStateOf(Pinned) }
        var current by rememberSaveable { mutableStateOf(Pinned.first()) }
        CatalogueCaption("Open, edit, reorder and remove")
        MonarchFavoritesList(
            items = names.map { MonarchFavoriteItem(it, id = it, current = it == current) },
            onSelect = { current = it.id },
            onRemove = { item -> names = names - item.id },
            onReorder = { order -> names = order.map { it.id } },
            emptyMessage = "Pin pages to find them here.",
        )
        CatalogueCaption("Read only")
        MonarchFavoritesList(
            items = Pinned.take(2).map { MonarchFavoriteItem(it, id = it) },
            onSelect = {},
        )
    }
}
