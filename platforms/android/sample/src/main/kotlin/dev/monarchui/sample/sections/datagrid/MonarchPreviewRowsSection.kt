package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRow
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowIssue
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowStatus
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRows
import dev.monarchui.components.datagrid.previewrows.MonarchPreviewRowsColumn
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of PreviewRows: six rows of a contacts file, with every outcome. */
object MonarchPreviewRowsSection : CataloguePage {
    override val title = "PreviewRows"

    private const val LIST_HEIGHT_STEPS = 5

    private val columns = listOf(
        MonarchPreviewRowsColumn("name", "Name"),
        MonarchPreviewRowsColumn("email", "Email"),
        MonarchPreviewRowsColumn("seats", "Seats", numeric = true),
    )

    private val rows = listOf(
        MonarchPreviewRow(1, mapOf("name" to "Ada Lovelace", "email" to "ada@example.com", "seats" to "3")),
        MonarchPreviewRow(
            2,
            mapOf("name" to "Alan Turing", "email" to "alan@", "seats" to "1"),
            errors = listOf(MonarchPreviewRowIssue("Not a valid email address", field = "Email")),
        ),
        MonarchPreviewRow(
            3,
            mapOf("name" to "Grace Hopper", "email" to "grace@example.com", "seats" to null),
            warnings = listOf(MonarchPreviewRowIssue("Empty, so 1 seat is used", field = "Seats")),
        ),
        MonarchPreviewRow(
            4,
            mapOf("name" to "Ada Lovelace", "email" to "ada@example.com", "seats" to "3"),
            status = MonarchPreviewRowStatus.Duplicate,
            note = "Matches row 1",
        ),
        MonarchPreviewRow(5, mapOf("name" to "Katherine Johnson", "email" to "kj@example.com", "seats" to "12")),
        MonarchPreviewRow(6, mapOf("name" to "Edsger Dijkstra", "email" to "ed@example.com", "seats" to "2")),
    )

    @Composable
    override fun Content() {
        var filter by rememberSaveable { mutableStateOf<MonarchPreviewRowStatus?>(null) }
        MonarchPreviewRows(
            rows = rows,
            columns = columns,
            label = "Preview of contacts.csv",
            filter = filter,
            onFilterChange = { filter = it },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
        )
    }
}
