package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datagrid.cursorlist.MonarchCursorList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of CursorList: 32 tickets in pages of 10, the first and the third page fail once. */
object MonarchCursorListSection : CataloguePage {
    override val title = "CursorList"

    private const val PAGE = 10
    private const val TOTAL = 32
    private const val LOAD_MILLIS = 900L
    private const val LIST_HEIGHT_STEPS = 4
    private val FAILING_PAGES = listOf(1, 3)

    @Composable
    override fun Content() {
        var loaded by rememberSaveable { mutableIntStateOf(0) }
        var loading by rememberSaveable { mutableStateOf(true) }
        var failed by rememberSaveable { mutableStateOf(false) }
        var failures by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(loading) {
            if (loading) {
                delay(LOAD_MILLIS)
                // Pages 1 and 3 fail on their first attempt.
                if (loaded / PAGE + 1 == FAILING_PAGES.getOrNull(failures)) {
                    failures++
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
        MonarchCursorList(
            items = (1..loaded).map { "Ticket $it" },
            label = "Tickets",
            hasMore = loaded < TOTAL,
            onLoadMore = load,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            key = { it },
            loading = loading,
            failed = failed,
            totalCount = TOTAL,
        ) { MonarchText(text = it, modifier = Modifier.padding(vertical = MonarchTheme.spacing.space3)) }
        CatalogueCaption("Start again")
        MonarchButton(
            text = "Reload from the first page",
            onClick = {
                loaded = 0
                failures = 0
                load()
            },
        )
    }
}
