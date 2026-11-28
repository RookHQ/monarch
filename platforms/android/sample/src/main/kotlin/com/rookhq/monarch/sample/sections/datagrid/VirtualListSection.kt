package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of VirtualList: the platform's lazy lists, which are virtualised. */
object VirtualListSection : CataloguePage {
    override val title = "VirtualList"

    private const val TICKETS = 2000
    private const val PRODUCTS = 500
    private const val LIST_HEIGHT_STEPS = 3

    private class Ticket(val id: Int)

    private val tickets = List(TICKETS) { Ticket(it + 1) }
    private val products = List(PRODUCTS) { it + 1 }

    @Composable
    override fun Content() {
        val height = MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS
        CatalogueCaption("LazyColumn, 2,000 rows with stable keys")
        LazyColumn(modifier = Modifier.fillMaxWidth().height(height)) {
            items(items = tickets, key = { it.id }, contentType = { "ticket" }) { ticket ->
                MonarchText(
                    text = "Ticket ${ticket.id}",
                    modifier = Modifier.padding(vertical = MonarchTheme.spacing.space3),
                )
                MonarchDivider()
            }
        }
        CatalogueCaption("LazyVerticalGrid, 500 cards")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(MonarchTheme.spacing.space24),
            modifier = Modifier.fillMaxWidth().height(height),
        ) {
            items(items = products, key = { it }, contentType = { "product" }) { product ->
                MonarchCard(modifier = Modifier.padding(MonarchTheme.spacing.space1)) {
                    MonarchText(text = "Item $product")
                }
            }
        }
    }
}
