package com.rookhq.monarch.sample.sections.interaction

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGrid
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
