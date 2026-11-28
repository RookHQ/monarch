@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThread
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadComment
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of CommentThread: reply, resolve and reopen, unfold older replies, long press to delete. */
object MonarchCommentThreadSection : CataloguePage {
    override val title = "CommentThread"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val SAVE_MILLIS = 600L

    private val start = listOf(
        MonarchCommentThreadComment(
            id = "c1",
            author = "Priya Raman",
            body = "Should this say billed every quarter instead?",
            createdAt = NOW - 50 * MINUTE,
        ),
        MonarchCommentThreadComment(
            id = "c2",
            author = "Tom Becker",
            body = "Yes. Monthly billing was retired last year.",
            createdAt = NOW - 20 * MINUTE,
            canDelete = true,
        ),
    )

    private val replies = listOf(
        "Agreed.",
        "Updated the copy.",
        "Checked the pricing table too.",
        "Legal is fine with it.",
        "Shipping tomorrow.",
    )

    private val long = listOf(
        MonarchCommentThreadComment(
            id = "l1",
            author = "Lee Park",
            body = "Can we drop the footnote under the pricing table?",
            createdAt = NOW - 90 * MINUTE,
        ),
    ) + replies.mapIndexed { i, body ->
        MonarchCommentThreadComment(
            id = "r$i",
            author = if (i % 2 == 0) "Tom Becker" else "Lee Park",
            body = body,
            createdAt = NOW - (60L - i * 5) * MINUTE,
        )
    }

    @Composable
    override fun Content() {
        val comments = remember { mutableStateListOf<MonarchCommentThreadComment>().apply { addAll(start) } }
        var resolved by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Reply, resolve, long press your comment to delete")
        MonarchCommentThread(
            comments = comments,
            quote = "Billed monthly, cancel any time.",
            resolved = resolved,
            now = NOW,
            onResolvedChange = {
                delay(SAVE_MILLIS)
                resolved = it
            },
            onReply = { body ->
                delay(SAVE_MILLIS)
                comments += MonarchCommentThreadComment("n${comments.size}", "You", body, NOW, canDelete = true)
            },
            onDeleteComment = { id -> comments.removeAll { it.id == id } },
        )
        CatalogueCaption("Older replies folded")
        MonarchCommentThread(comments = long, now = NOW)
    }
}
