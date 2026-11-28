@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.messagecomposer.MonarchMessageComposer
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRow
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowAuthor
import dev.monarchui.components.collaboration.threadpanel.MonarchThreadPanel
import dev.monarchui.components.collaboration.threadpanel.MonarchThreadPanelStatus
import dev.monarchui.components.collaboration.typingindicator.MonarchTypingIndicator
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of ThreadPanel: reply in a thread, follow it, and see the deleted state. */
object MonarchThreadPanelSection : CataloguePage {
    override val title = "ThreadPanel"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val SAVE_MILLIS = 600L
    private const val HEIGHT_STEPS = 5

    private class Reply(val id: String, val author: String, val text: String, val time: Long)

    private val start = listOf(
        Reply("r1", "Ben Okafor", "I can take it.", NOW - 30 * MINUTE),
        Reply("r2", "Ben Okafor", "Starting after lunch.", NOW - 29 * MINUTE),
        Reply("r3", "Ana Ruiz", "Thanks, Ben.", NOW - 10 * MINUTE),
    )

    @Composable
    override fun Content() {
        val replies = remember { mutableStateListOf<Reply>().apply { addAll(start) } }
        var following by rememberSaveable { mutableStateOf(false) }
        val draft = rememberTextFieldState()
        CatalogueCaption("Reply, follow; the newest reply stays in view")
        MonarchThreadPanel(
            replies = replies,
            key = { it.id },
            time = { it.time },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            following = following,
            authorId = { it.author },
            now = NOW,
            onFollowingChange = {
                delay(SAVE_MILLIS)
                following = it
            },
            root = {
                MonarchMessageRow(author = MonarchMessageRowAuthor("Lee Park"), time = NOW - 60 * MINUTE, now = NOW) {
                    MonarchText("Who can review the release notes?")
                }
            },
            typing = { MonarchTypingIndicator(names = listOf("Ana")) },
            composer = {
                MonarchMessageComposer(
                    label = "Reply in thread",
                    state = draft,
                    onSend = { message ->
                        delay(SAVE_MILLIS)
                        replies += Reply("n${replies.size}", "You", message.text, NOW)
                    },
                    placeholder = "Reply…",
                )
            },
        ) { reply, context ->
            MonarchMessageRow(
                author = MonarchMessageRowAuthor(reply.author),
                time = reply.time,
                now = NOW,
                grouped = context.grouped,
            ) { MonarchText(reply.text) }
        }
        CatalogueCaption("Deleted")
        MonarchThreadPanel(
            replies = emptyList<Reply>(),
            key = { it.id },
            time = { it.time },
            status = MonarchThreadPanelStatus.Deleted,
            onClose = {},
        ) { _, _ -> }
    }
}
