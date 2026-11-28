package dev.monarchui.sample.sections.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.datagrid.loadmore.MonarchLoadMore
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of Pagination: a lazy list that loads the next page at its end with MonarchLoadMore. */
object PaginationSection : CataloguePage {
    override val title = "Pagination"

    private const val PAGE = 20
    private const val TOTAL = 60
    private const val LOAD_MILLIS = 800L
    private const val LIST_HEIGHT_STEPS = 4
    private const val FIRST_INVOICE = 1000

    @Composable
    override fun Content() {
        var loaded by rememberSaveable { mutableIntStateOf(PAGE) }
        var loading by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(loading) {
            if (loading) {
                delay(LOAD_MILLIS)
                loaded = minOf(TOTAL, loaded + PAGE)
                loading = false
            }
        }
        CatalogueCaption("Invoices, twenty at a time")
        LazyColumn(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS)) {
            items((1..loaded).toList(), key = { it }) { number ->
                ListItem(headlineContent = { Text("Invoice INV-${FIRST_INVOICE + number}") })
            }
            item {
                MonarchLoadMore(
                    hasMore = loaded < TOTAL,
                    onLoadMore = { loading = true },
                    loading = loading,
                    loadedCount = loaded,
                    totalCount = TOTAL,
                )
            }
        }
    }
}
