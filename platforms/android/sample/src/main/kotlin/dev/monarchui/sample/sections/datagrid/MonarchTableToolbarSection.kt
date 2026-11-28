package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.datagrid.tabletoolbar.MonarchTableToolbar
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of TableToolbar: a search, a filter, a view switcher and a Columns menu over contacts. */
object MonarchTableToolbarSection : CataloguePage {
    override val title = "TableToolbar"

    private val contacts = listOf("Ada Lovelace", "Alan Turing", "Grace Hopper", "Katherine Johnson")
    private val views = listOf("Table", "Board")

    @Composable
    override fun Content() {
        var query by rememberSaveable { mutableStateOf("") }
        var onlyA by rememberSaveable { mutableStateOf(false) }
        var view by rememberSaveable { mutableIntStateOf(0) }
        var showEmail by rememberSaveable { mutableStateOf(true) }
        var columnsOpen by rememberSaveable { mutableStateOf(false) }
        var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
        val shown = contacts
            .filter { it.contains(query, ignoreCase = true) }
            .filter { !onlyA || it.startsWith("A") }
        CatalogueCaption("Search, filter and select contacts")
        MonarchTableToolbar(
            resultCount = shown.size,
            selectedCount = selected.size,
            totalCount = shown.size,
            bulkActions = listOf(MonarchBulkAction(id = "export", title = "Export") {}),
            onClearSelection = { selected = emptyList() },
            search = {
                MonarchSearchBar(query = query, onQueryChange = { query = it }, onSearch = {}, placeholder = "Search")
            },
            filters = { MonarchChip(text = "Starts with A", selected = onlyA, onSelectedChange = { onlyA = it }) },
        ) {
            MonarchSegmentedControl {
                views.forEachIndexed { index, label ->
                    Segment(
                        selected = index == view,
                        onClick = { view = index },
                        label = { Text(label) },
                    )
                }
            }
            Box {
                MonarchButton(text = "Columns", onClick = { columnsOpen = true }, size = MonarchButtonSize.Sm)
                MonarchMenu(expanded = columnsOpen, onDismissRequest = { columnsOpen = false }) {
                    MonarchMenuItem(
                        text = "Email",
                        onClick = { showEmail = !showEmail },
                        selected = showEmail,
                    )
                }
            }
        }
        shown.forEach { name ->
            MonarchCheckboxRow(
                checked = name in selected,
                onCheckedChange = { on -> selected = if (on) selected + name else selected - name },
                label = name,
                description = if (showEmail) name.substringBefore(' ').lowercase() + "@example.com" else null,
            )
        }
    }
}
