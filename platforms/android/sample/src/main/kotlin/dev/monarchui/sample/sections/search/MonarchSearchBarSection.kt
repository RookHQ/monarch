package dev.monarchui.sample.sections.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of SearchInput: MonarchSearchBar. */
object MonarchSearchBarSection : CataloguePage {
    override val title = "SearchInput"

    private const val DEBOUNCE_MILLIS = 300L
    private const val CLOSE_ROTATION = 45f
    private val people = listOf("Ada Lovelace", "Alan Turing", "Grace Hopper", "Katherine Johnson")

    @Composable
    override fun Content() {
        var query by rememberSaveable { mutableStateOf("") }
        var submitted by rememberSaveable { mutableStateOf("") }
        var expanded by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Filtering a list, 300ms debounce")
        MonarchSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { submitted = it },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            debounceMillis = DEBOUNCE_MILLIS,
            placeholder = "Search people",
            // The catalogue has no close icon: a plus turned by 45 degrees.
            clearIcon = {
                MonarchIcon(
                    imageVector = SampleIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.rotate(CLOSE_ROTATION),
                )
            },
        ) {
            people.filter { it.contains(submitted, ignoreCase = true) }.forEach { MonarchText(text = it) }
        }
        MonarchText(
            text = if (submitted.isEmpty()) "Type to search" else "Results for $submitted",
            style = MonarchTextStyle.Caption,
        )
        CatalogueCaption("Pending")
        MonarchSearchBar(query = "invoices", onQueryChange = {}, onSearch = {}, pending = true)
        CatalogueCaption("Disabled")
        MonarchSearchBar(query = "", onQueryChange = {}, onSearch = {}, enabled = false)
    }
}
