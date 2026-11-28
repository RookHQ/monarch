package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooser
import dev.monarchui.components.datagrid.columnchooser.MonarchColumnChooserColumn
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ColumnChooser: switch columns, long press a row to reorder, search from 8 columns. */
object MonarchColumnChooserSection : CataloguePage {
    override val title = "ColumnChooser"

    private const val HEIGHT_STEPS = 5

    private val Columns = listOf(
        MonarchColumnChooserColumn("Name", id = "name", hideable = false),
        MonarchColumnChooserColumn("Status", id = "status"),
        MonarchColumnChooserColumn("Owner", id = "owner"),
        MonarchColumnChooserColumn("Created", id = "created"),
        MonarchColumnChooserColumn("Updated", id = "updated"),
        MonarchColumnChooserColumn("Priority", id = "priority"),
        MonarchColumnChooserColumn("Region", id = "region"),
        MonarchColumnChooserColumn("Plan", id = "plan"),
    )

    @Composable
    override fun Content() {
        var hidden by rememberSaveable { mutableStateOf(listOf("updated", "region")) }
        var order by rememberSaveable { mutableStateOf(Columns.map { it.id }) }
        CatalogueCaption("Show, hide, find and reorder")
        MonarchColumnChooser(
            columns = Columns,
            hiddenColumns = hidden.toSet(),
            onHiddenColumnsChange = { hidden = it.toList() },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            order = order,
            onOrderChange = { order = it },
            onReset = {
                hidden = listOf("updated", "region")
                order = Columns.map { it.id }
            },
        )
    }
}
