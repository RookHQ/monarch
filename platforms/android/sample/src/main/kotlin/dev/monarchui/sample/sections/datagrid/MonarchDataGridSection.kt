package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.monarchui.components.datagrid.datagrid.MonarchDataGrid
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridColumn
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridEdit
import dev.monarchui.components.datagrid.datagrid.MonarchDataGridPin
import dev.monarchui.components.datagrid.datatable.MonarchDataTableLayout
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSort
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellEditor
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * The catalogue page of DataGrid: invoices that sort, pin, hide, resize, select and edit in place, with more rows
 * loaded at the end; in the automatic layout and as a grid.
 */
object MonarchDataGridSection : CataloguePage {
    override val title = "DataGrid"

    private const val HEIGHT_STEPS = 5
    private const val PAGE = 30
    private const val LAST = 90
    private const val AMOUNT_STEP = 7919
    private const val CENTS = 100.0
    private const val AMOUNT_RANGE = 50_000

    private val customers = listOf("Example Ltd", "Sample Co", "Demo GmbH", "Placeholder Inc")
    private val statuses = listOf("Draft", "Sent", "Paid", "Overdue")

    private data class Invoice(val id: Int, val customer: String, val status: String, val amount: Double)

    private fun invoice(i: Int) = Invoice(
        id = i,
        customer = customers[i % customers.size],
        status = statuses[i % statuses.size],
        amount = ((i * AMOUNT_STEP) % AMOUNT_RANGE) / CENTS,
    )

    private fun number(invoice: Invoice) = String.format(Locale.US, "INV-%05d", invoice.id + 1)

    @Composable
    override fun Content() {
        var rows by remember { mutableStateOf((0 until PAGE).map(::invoice)) }
        var sort by remember { mutableStateOf<MonarchDataTableSort?>(null) }
        var selection by rememberSaveable { mutableStateOf(listOf<Int>()) }
        var hidden by rememberSaveable { mutableStateOf(listOf<String>()) }
        var pinning by remember { mutableStateOf<Map<String, MonarchDataGridPin>?>(null) }
        var widths by remember { mutableStateOf(emptyMap<String, Dp>()) }
        fun update(id: Int, change: (Invoice) -> Invoice) {
            rows = rows.map { if (it.id == id) change(it) else it }
        }
        val columns = listOf<MonarchDataGridColumn<Invoice>>(
            MonarchDataGridColumn("Invoice", id = "number", hideable = false, pinned = MonarchDataGridPin.Start) {
                number(it)
            },
            MonarchDataGridColumn(
                "Customer",
                id = "customer",
                edit = MonarchDataGridEdit(
                    editor = MonarchEditableCellEditor.Text,
                    value = { it.customer },
                    onCommit = { row, value -> update(row.id) { it.copy(customer = value.orEmpty()) } },
                    required = true,
                ),
            ) { it.customer },
            MonarchDataGridColumn(
                "Status",
                id = "status",
                edit = MonarchDataGridEdit(
                    editor = MonarchEditableCellEditor.Select(statuses.map { MonarchSelectOption(it, it) }),
                    value = { it.status },
                    onCommit = { row, value -> update(row.id) { it.copy(status = value ?: it.status) } },
                    required = true,
                ),
            ) { it.status },
            MonarchDataGridColumn(
                "Amount",
                id = "amount",
                numeric = true,
                edit = MonarchDataGridEdit(
                    editor = MonarchEditableCellEditor.Number(min = 0.0),
                    value = { it.amount },
                    onCommit = { row, value -> update(row.id) { it.copy(amount = value ?: 0.0) } },
                    required = true,
                ),
            ) { String.format(Locale.US, "%.2f", it.amount) },
        )
        listOf(MonarchDataTableLayout.Automatic, MonarchDataTableLayout.Table).forEach { layout ->
            CatalogueCaption("Layout: ${layout.name}")
            MonarchDataGrid(
                label = "Invoices",
                rows = rows,
                columns = columns,
                rowKey = { it.id },
                modifier = Modifier.height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
                layout = layout,
                sort = sort,
                onSortChange = { sort = it },
                selection = selection.toSet(),
                onSelectionChange = { selection = it.toList() },
                hiddenColumns = hidden.toSet(),
                onHiddenColumnsChange = { hidden = it.toList() },
                pinning = pinning,
                onPinningChange = { pinning = it },
                columnWidths = widths,
                onColumnWidthsChange = { widths = it },
                rowLabel = ::number,
                rowActions = { invoice ->
                    listOf(
                        MonarchDataTableRowAction("Void", danger = true) {
                            rows = rows.filterNot { it.id == invoice.id }
                        },
                    )
                },
                onEndReached = {
                    // Ids continue after the highest one, so a voided row never makes a key repeat.
                    val next = (rows.maxOfOrNull { it.id } ?: -1) + 1
                    if (next < LAST) rows = rows + (next until next + PAGE).map(::invoice)
                },
            )
        }
    }
}
