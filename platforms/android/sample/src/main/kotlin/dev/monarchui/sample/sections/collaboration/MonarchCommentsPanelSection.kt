@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.commentspanel.MonarchCommentsPanel
import dev.monarchui.components.collaboration.commentspanel.MonarchCommentsPanelFilter
import dev.monarchui.components.collaboration.commentspanel.MonarchCommentsPanelThread
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadComment
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of CommentsPanel: open a thread, reply, resolve it, then find it under Resolved. */
object MonarchCommentsPanelSection : CataloguePage {
    override val title = "CommentsPanel"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val SAVE_MILLIS = 600L
    private const val HEIGHT_STEPS = 5

    private val start = listOf(
        MonarchCommentsPanelThread(
            id = "t1",
            comments = listOf(
                MonarchCommentThreadComment("c1", "Priya Raman", "Billed every quarter instead?", NOW - 50 * MINUTE),
                MonarchCommentThreadComment(
                    "c2",
                    "Tom Becker",
                    "Yes. Monthly billing was retired.",
                    NOW - 20 * MINUTE,
                    canDelete = true,
                ),
            ),
            quote = "Billed monthly, cancel any time.",
        ),
        MonarchCommentsPanelThread(
            id = "t2",
            comments = listOf(
                MonarchCommentThreadComment("c3", "Lee Park", "Can we add a summary at the top?", NOW - 180 * MINUTE),
            ),
        ),
        MonarchCommentsPanelThread(
            id = "t3",
            comments = listOf(
                MonarchCommentThreadComment("c4", "Ana Ruiz", "Typo in the heading.", NOW - 3 * 24 * 60 * MINUTE),
            ),
            quote = "Our pricinng",
            resolved = true,
        ),
    )

    @Composable
    override fun Content() {
        val threads = remember { mutableStateListOf<MonarchCommentsPanelThread>().apply { addAll(start) } }
        var filter by rememberSaveable { mutableStateOf(MonarchCommentsPanelFilter.Open) }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        fun update(id: String, change: (MonarchCommentsPanelThread) -> MonarchCommentsPanelThread) {
            val index = threads.indexOfFirst { it.id == id }
            if (index >= 0) threads[index] = change(threads[index])
        }
        CatalogueCaption("Open a thread, reply, resolve; back returns to the list")
        MonarchCommentsPanel(
            threads = threads,
            filter = filter,
            onFilterChange = { filter = it },
            selectedThreadId = selected,
            onSelectedThreadChange = { selected = it },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            now = NOW,
            onReply = { id, body ->
                delay(SAVE_MILLIS)
                update(id) { t ->
                    val added = MonarchCommentThreadComment("n${t.comments.size}", "You", body, NOW, canDelete = true)
                    MonarchCommentsPanelThread(t.id, t.comments + added, t.quote, NOW, t.resolved)
                }
            },
            onResolvedChange = { id, value ->
                delay(SAVE_MILLIS)
                update(id) { t -> MonarchCommentsPanelThread(t.id, t.comments, t.quote, t.updatedAt, value) }
            },
            onDeleteComment = { id, commentId ->
                update(id) { t ->
                    val kept = t.comments.filter { it.id != commentId }
                    MonarchCommentsPanelThread(t.id, kept, t.quote, t.updatedAt, t.resolved)
                }
            },
        )
        CatalogueCaption("Loading")
        MonarchCommentsPanel(
            threads = null,
            filter = MonarchCommentsPanelFilter.Open,
            onFilterChange = {},
            selectedThreadId = null,
            onSelectedThreadChange = {},
            now = NOW,
        )
    }
}
