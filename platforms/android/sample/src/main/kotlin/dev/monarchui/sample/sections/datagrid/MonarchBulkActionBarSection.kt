package dev.monarchui.sample.sections.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkAction
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBar
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of BulkActionBar: four invoices with checkboxes and the bar under them. */
object MonarchBulkActionBarSection : CataloguePage {
    override val title = "BulkActionBar"

    private val invoices = listOf("Invoice 1041", "Invoice 1042", "Invoice 1043", "Invoice 1044")

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
        var archived by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("Select invoices")
        invoices.filterNot { it in archived }.forEach { invoice ->
            MonarchCheckboxRow(
                checked = invoice in selected,
                onCheckedChange = { on -> selected = if (on) selected + invoice else selected - invoice },
                label = invoice,
            )
        }
        MonarchBulkActionBar(
            count = selected.size,
            onClear = { selected = emptyList() },
            totalCount = invoices.size - archived.size,
            actions = listOf(
                MonarchBulkAction(id = "archive", title = "Archive") {
                    archived = archived + selected
                    selected = emptyList()
                },
                MonarchBulkAction(id = "export", title = "Export") {},
                MonarchBulkAction(id = "tag", title = "Add tag") {},
                MonarchBulkAction(id = "delete", title = "Delete", destructive = true) {
                    archived = archived + selected
                    selected = emptyList()
                },
            ),
            maxVisibleActions = 2,
        )
        if (archived.isNotEmpty()) {
            MonarchText(text = "Removed: ${archived.joinToString()}", color = MonarchTheme.colors.textMuted)
        }
    }
}
