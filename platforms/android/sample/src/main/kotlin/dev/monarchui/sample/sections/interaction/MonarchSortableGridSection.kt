package dev.monarchui.sample.sections.interaction

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.monarchui.components.interaction.sortablegrid.MonarchSortableGrid
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of SortableGrid: widgets reordered by a long press and drag, or the handle's actions. */
object MonarchSortableGridSection : CataloguePage {
    override val title = "SortableGrid"

    private const val HEIGHT_STEPS = 3

    @Composable
    override fun Content() {
        val widgets = remember { mutableStateListOf("Revenue", "Signups", "Churn", "Tickets", "Uptime", "Latency") }
        CatalogueCaption("Long press a tile and drag it; Uptime is locked")
        MonarchSortableGrid(
            items = widgets,
            key = { it },
            itemLabel = { it },
            onReorder = { from, to -> widgets.add(to, widgets.removeAt(from)) },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            itemEnabled = { it != "Uptime" },
        ) { widget, state ->
            MonarchText(text = widget, style = MonarchTextStyle.Label)
            MonarchText(text = "Position ${state.index + 1}", style = MonarchTextStyle.Caption)
        }
    }
}
