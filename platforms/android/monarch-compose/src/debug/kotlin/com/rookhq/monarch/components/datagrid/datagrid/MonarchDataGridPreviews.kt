package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableLayout
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSort
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCellEditor
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 3

private val PreviewRows = listOf(
    listOf("INV-00001", "Example Ltd", "Sent", "120.00"),
    listOf("INV-00002", "Sample Co", "Paid", ""),
)

private val PreviewColumns = listOf<MonarchDataGridColumn<List<String>>>(
    MonarchDataGridColumn("Invoice", pinned = MonarchDataGridPin.Start, hideable = false) { it[0] },
    MonarchDataGridColumn(
        "Customer",
        edit = MonarchDataGridEdit(
            editor = MonarchEditableCellEditor.Text,
            value = { it[1] },
            onCommit = { _, _ -> },
        ),
    ) { it[1] },
    MonarchDataGridColumn("Status") { it[2] },
    MonarchDataGridColumn("Amount", numeric = true) { it[3] },
)

@MonarchPreviews
@Composable
internal fun MonarchDataGridPreview() {
    PreviewFrame {
        MonarchDataGrid(
            label = "Invoices",
            rows = PreviewRows,
            columns = PreviewColumns,
            rowKey = { it[0] },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            layout = MonarchDataTableLayout.Table,
            sort = MonarchDataTableSort("Customer"),
            onSortChange = {},
            selection = setOf("INV-00001"),
            onSelectionChange = {},
            rowActions = { listOf(MonarchDataTableRowAction("Void", danger = true) {}) },
        )
        MonarchDataGrid(
            label = "Invoices",
            rows = PreviewRows,
            columns = PreviewColumns,
            rowKey = { it[0] },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            layout = MonarchDataTableLayout.List,
            density = MonarchDataGridDensity.Compact,
            onSortChange = {},
        )
        MonarchDataGrid(label = "Invoices", rows = emptyList(), columns = PreviewColumns, rowKey = { it[0] })
    }
}
