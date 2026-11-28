package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datagrid.datatable.MonarchDataTable
import dev.monarchui.components.datagrid.datatable.MonarchDataTableColumn
import dev.monarchui.components.datagrid.datatable.MonarchDataTableLayout
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSort
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DataTable: sort, select and use the row actions, in the list and the table layout. */
object MonarchDataTableSection : CataloguePage {
    override val title = "DataTable"

    private const val HEIGHT_STEPS = 5

    private class Ticket(val id: String, val status: String, val owner: String, val replies: String)

    private val Tickets = listOf(
        Ticket("Ticket 42", "Open", "Ana Lima", "3"),
        Ticket("Ticket 7", "Closed", "", "12"),
        Ticket("Ticket 13", "Open", "Ben Okafor", "1"),
        Ticket("Ticket 21", "Waiting", "Chen Wei", "5"),
    )

    @Composable
    override fun Content() {
        var sort by remember { mutableStateOf<MonarchDataTableSort?>(null) }
        var selection by rememberSaveable { mutableStateOf(listOf<String>()) }
        var archived by rememberSaveable { mutableStateOf(listOf<String>()) }
        val columns = remember {
            listOf<MonarchDataTableColumn<Ticket>>(
                MonarchDataTableColumn("Ticket", id = "id", hideable = false) { it.id },
                MonarchDataTableColumn("Status", id = "status") { it.status },
                MonarchDataTableColumn("Owner", id = "owner") { it.owner },
                MonarchDataTableColumn("Replies", id = "replies", numeric = true) { it.replies },
            )
        }
        val rows = Tickets.filterNot { it.id in archived }
        MonarchDataTableLayout.entries.forEach { layout ->
            CatalogueCaption("Layout: ${layout.name}")
            MonarchDataTable(
                caption = "Open tickets",
                rows = rows,
                columns = columns,
                rowKey = { it.id },
                modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
                sort = sort,
                onSortChange = { sort = it },
                selection = selection.toSet(),
                onSelectionChange = { selection = it.toList() },
                layout = layout,
                rowActions = { row ->
                    listOf(MonarchDataTableRowAction("Archive", danger = true) { archived = archived + row.id })
                },
            )
        }
    }
}
