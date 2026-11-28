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

/** The catalogue page of CommandPalette: a recipe of existing components. */
object CommandPaletteSection : CataloguePage {
    override val title = "CommandPalette"

    @Composable
    override fun Content() {
        var ran by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Open the palette, type, and choose a command (Ctrl+K on a hardware keyboard)")
        run {
            var open by rememberSaveable { mutableStateOf(false) }
            var query by rememberSaveable { mutableStateOf("") }
            val commands = listOf(
                Triple("new", "Create ticket", "Actions"),
                Triple("assign", "Assign to me", "Actions"),
                Triple("inbox", "Go to inbox", "Go to"),
                Triple("reports", "Go to reports", "Go to"),
            )
            Box(
                modifier = Modifier.onPreviewKeyEvent { event ->
                    val shortcut = event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.K
                    if (shortcut) open = true
                    shortcut
                },
            ) {
                MonarchButton(text = "Search or run a command", onClick = { open = true })
            }
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
                            placeholder = "Type a command or search",
                        )
                        val matches = commands.filter { it.second.contains(query.trim(), ignoreCase = true) }
                        MonarchSearchResults(
                            groups = matches.groupBy { it.third }.map { (group, list) ->
                                MonarchSearchResultGroup(
                                    title = group,
                                    id = group,
                                    results = list.map { MonarchSearchResult(title = it.second, id = it.first) },
                                )
                            },
                            onSelect = { result, _ ->
                                ran = result.title
                                this@MonarchBottomSheet.dismiss()
                            },
                            modifier = Modifier.weight(1f),
                            query = query,
                        )
                    }
                }
            }
        }
        if (ran.isNotEmpty()) MonarchText(text = "Ran: $ran")
    }
}
