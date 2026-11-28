@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.search.searchresults.MonarchSearchResult
import dev.monarchui.components.search.searchresults.MonarchSearchResultGroup
import dev.monarchui.components.search.searchresults.MonarchSearchResults
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SwitcherPlaces = listOf(
    Triple("c-general", "general", "Channels"),
    Triple("c-design", "design-review", "Channels"),
    Triple("p-ada", "Ada Moss", "People"),
    Triple("d-roadmap", "Roadmap 2027", "Documents"),
)

/** The catalogue page of QuickSwitcher: a recipe of existing components. */
object QuickSwitcherSection : CataloguePage {
    override val title = "QuickSwitcher"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var query by rememberSaveable { mutableStateOf("") }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Jump to a place by name (Ctrl+K on a hardware keyboard)")
        Box(
            modifier = Modifier.onPreviewKeyEvent { event ->
                val shortcut = event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.K
                if (shortcut) open = true
                shortcut
            },
        ) {
            MonarchButton(text = "Jump to…", onClick = { open = true })
        }
        if (opened.isNotEmpty()) MonarchText(text = "Opened: $opened")
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = {
                    open = false
                    query = ""
                },
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    MonarchSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Jump to…",
                    )
                    MonarchSearchResults(
                        groups = switcherGroups(query.trim()),
                        onSelect = { result, _ ->
                            opened = result.title
                            this@MonarchBottomSheet.dismiss()
                        },
                        modifier = Modifier.weight(1f),
                        query = query,
                    )
                }
            }
        }
    }
}

/** Recent places while nothing is typed; otherwise the matches by group and the search-everywhere fallback. */
private fun switcherGroups(text: String): List<MonarchSearchResultGroup> =
    if (text.isEmpty()) {
        listOf(
            MonarchSearchResultGroup(
                "Recent",
                "recent",
                SwitcherPlaces.take(2).map { MonarchSearchResult(it.second, it.first) },
            ),
        )
    } else {
        SwitcherPlaces.filter { it.second.contains(text, ignoreCase = true) }
            .groupBy { it.third }
            .map { (group, list) ->
                MonarchSearchResultGroup(group, group, list.map { MonarchSearchResult(it.second, it.first) })
            } +
            MonarchSearchResultGroup(
                "Search",
                "search",
                listOf(MonarchSearchResult("Search for “$text”", "search")),
            )
    }
