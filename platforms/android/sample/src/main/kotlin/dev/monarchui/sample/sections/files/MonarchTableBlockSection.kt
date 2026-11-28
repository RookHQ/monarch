package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.tableblock.MonarchTableBlock
import dev.monarchui.components.files.tableblock.MonarchTableBlockData
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of TableBlock. */
object MonarchTableBlockSection : CataloguePage {
    override val title = "TableBlock"

    private val plans = MonarchTableBlockData.of(
        listOf(
            listOf("Plan", "Seats", "Price per seat"),
            listOf("Team", "10", "12"),
            listOf("Business", "Unlimited", "30"),
        ),
    )

    @Composable
    override fun Content() {
        CatalogueCaption("Read only")
        MonarchTableBlock(data = plans, caption = "Plans")
        CatalogueCaption("Editable")
        var deleted by rememberSaveable { mutableStateOf(false) }
        var data by remember { mutableStateOf(MonarchTableBlockData.empty()) }
        if (deleted) {
            MonarchText("The table was deleted.")
        } else {
            MonarchTableBlock(
                data = data,
                onDataChange = { data = it },
                caption = "Draft",
                onDelete = { deleted = true },
            )
        }
    }
}
