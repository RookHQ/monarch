package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of LoadMore: a list of 45 tickets in pages of 10; the third page fails once. */
object MonarchLoadMoreSection : CataloguePage {
    override val title = "LoadMore"

    private const val PAGE = 10
    private const val TOTAL = 45
    private const val FAILING_PAGE = 3
    private const val LOAD_MILLIS = 900L
    private const val LIST_HEIGHT_STEPS = 3

    @Composable
    override fun Content() {
        var loaded by rememberSaveable { mutableIntStateOf(PAGE) }
        var loading by rememberSaveable { mutableStateOf(false) }
        var failed by rememberSaveable { mutableStateOf(false) }
        var failedOnce by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(loading) {
            if (loading) {
                delay(LOAD_MILLIS)
                if (loaded / PAGE + 1 == FAILING_PAGE && !failedOnce) {
                    failedOnce = true
                    failed = true
                } else {
                    loaded = minOf(TOTAL, loaded + PAGE)
                }
                loading = false
            }
        }
        val load = {
            failed = false
            loading = true
        }
        CatalogueCaption("Inside a LazyColumn")
        LazyColumn(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS)) {
            items((1..loaded).toList(), key = { it }) { MonarchText(text = "Ticket $it") }
            item {
                MonarchLoadMore(
                    hasMore = loaded < TOTAL,
                    onLoadMore = load,
                    loading = loading,
                    failed = failed,
                    loadedCount = loaded,
                    totalCount = TOTAL,
                )
            }
        }
    }
}
