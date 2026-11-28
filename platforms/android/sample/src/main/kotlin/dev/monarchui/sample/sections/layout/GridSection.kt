package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Grid: LazyVerticalGrid with adaptive cells, and FlowRow for short grids. */
object GridSection : CataloguePage {
    override val title = "Grid"

    private const val PROJECTS = 24
    private const val HEIGHT_STEPS = 3
    private const val COLUMNS = 2

    private val projects = List(PROJECTS) { "Project ${it + 1}" }

    @Composable
    override fun Content() {
        val gap = Arrangement.spacedBy(MonarchTheme.spacing.space4)
        CatalogueCaption("LazyVerticalGrid, GridCells.Adaptive")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = MonarchTheme.spacing.space24 * COLUMNS),
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            horizontalArrangement = gap,
            verticalArrangement = gap,
        ) {
            items(items = projects, key = { it }) { project ->
                MonarchCard { MonarchText(text = project) }
            }
        }

        CatalogueCaption("FlowRow, two columns inside a scrolling screen")
        FlowRow(maxItemsInEachRow = COLUMNS, horizontalArrangement = gap, verticalArrangement = gap) {
            listOf("Open", "Pending", "Solved", "Closed").forEach { status ->
                MonarchCard(modifier = Modifier.weight(1f)) { MonarchText(text = status) }
            }
        }
    }
}
