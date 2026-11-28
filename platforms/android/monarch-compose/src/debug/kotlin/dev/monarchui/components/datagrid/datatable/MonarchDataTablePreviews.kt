package dev.monarchui.components.datagrid.datatable

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 4

private val PreviewRows = listOf(
    listOf("Ticket 42", "Open", "Ana Lima", "3"),
    listOf("Ticket 43", "Closed", "", "12"),
)

private val PreviewColumns = listOf<MonarchDataTableColumn<List<String>>>(
    MonarchDataTableColumn("Ticket") { it[0] },
    MonarchDataTableColumn("Status") { it[1] },
    MonarchDataTableColumn("Owner") { it[2] },
    MonarchDataTableColumn("Replies", numeric = true) { it[3] },
)

@MonarchPreviews
@Composable
internal fun MonarchDataTablePreview() {
    PreviewFrame {
        MonarchDataTable(
            caption = "Open tickets",
            rows = PreviewRows,
            columns = PreviewColumns,
            rowKey = { it[0] },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            sort = MonarchDataTableSort("Replies"),
            onSortChange = {},
            selection = setOf("Ticket 42"),
            onSelectionChange = {},
            layout = MonarchDataTableLayout.Table,
        )
        MonarchDataTable(
            caption = "Open tickets",
            rows = PreviewRows,
            columns = PreviewColumns,
            rowKey = { it[0] },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            layout = MonarchDataTableLayout.List,
            rowActions = { listOf(MonarchDataTableRowAction("Archive") {}) },
        )
        MonarchDataTable(
            caption = "Open tickets",
            rows = emptyList(),
            columns = PreviewColumns,
            rowKey = { it[0] },
            filtered = true,
        )
    }
}
