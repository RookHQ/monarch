package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCell
import com.rookhq.monarch.components.datagrid.editablecell.MonarchEditableCellEditor
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of EditableCell: one deal's cells, saved in memory. */
object MonarchEditableCellSection : CataloguePage {
    override val title = "EditableCell"

    private const val NEW_YEAR = 1_767_225_600_000L
    private const val AMOUNT = 1250.0
    private const val MAX_AMOUNT = 1_000_000.0

    private val stages = MonarchEditableCellEditor.Select(
        listOf(
            MonarchSelectOption("open", "Open"),
            MonarchSelectOption("won", "Won"),
            MonarchSelectOption("lost", "Lost"),
        ),
    )

    @Composable
    override fun Content() {
        var company by rememberSaveable { mutableStateOf<String?>("Acme") }
        var amount by rememberSaveable { mutableStateOf<Double?>(AMOUNT) }
        var stage by rememberSaveable { mutableStateOf<String?>("open") }
        var closes by rememberSaveable { mutableStateOf<Long?>(NEW_YEAR) }
        CatalogueCaption("Text, required")
        MonarchEditableCell(
            value = company,
            label = "Company",
            editor = MonarchEditableCellEditor.Text,
            onCommit = { company = it },
            modifier = Modifier.fillMaxWidth(),
            required = true,
        )
        CatalogueCaption("Number from 0 to 1,000,000")
        MonarchEditableCell(
            value = amount,
            label = "Amount for Acme",
            editor = MonarchEditableCellEditor.Number(min = 0.0, max = MAX_AMOUNT),
            onCommit = { amount = it },
            modifier = Modifier.fillMaxWidth(),
        )
        CatalogueCaption("Select")
        MonarchEditableCell(
            value = stage,
            label = "Stage for Acme",
            editor = stages,
            onCommit = { stage = it },
            modifier = Modifier.fillMaxWidth(),
        )
        CatalogueCaption("Date")
        MonarchEditableCell(
            value = closes,
            label = "Close date for Acme",
            editor = MonarchEditableCellEditor.Date(),
            onCommit = { closes = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "No date",
        )
        CatalogueCaption("A save that fails")
        MonarchEditableCell(
            value = "Read only on the server",
            label = "Notes",
            editor = MonarchEditableCellEditor.Text,
            onCommit = { error("This field is locked.") },
            modifier = Modifier.fillMaxWidth(),
        )
        CatalogueCaption("Disabled")
        MonarchEditableCell(
            value = "EMEA",
            label = "Region",
            editor = MonarchEditableCellEditor.Text,
            onCommit = {},
            enabled = false,
        )
    }
}
