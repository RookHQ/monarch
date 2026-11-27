package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageList
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * The catalogue page of VirtualMessageList: 120 messages loaded 20 at a
 * time from the top, a "New messages" divider, and a button that sends a new
 * message so following the bottom and the jump button can be tried.
 */
object MonarchVirtualMessageListSection : CataloguePage {
    override val title = "VirtualMessageList"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val TOTAL = 120
    private const val PAGE = 20
    private const val UNREAD_FROM = 3
    private const val LOAD_MILLIS = 800L
    private const val LIST_HEIGHT_STEPS = 5
    private const val SPACING_MINUTES = 7

    private class Message(val id: Int, val author: String, val text: String, val time: Long)

    private fun message(id: Int): Message = Message(
        id = id,
        author = if (id % 3 == 0) "Ben Okafor" else "Ana Ruiz",
        text = "Message $id of the planning thread.",
        time = NOW - (TOTAL - id) * SPACING_MINUTES * MINUTE,
    )

    @Composable
    override fun Content() {
        var oldest by rememberSaveable { mutableIntStateOf(TOTAL - PAGE) }
        var newest by rememberSaveable { mutableIntStateOf(TOTAL) }
        var loading by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(loading) {
            if (loading) {
                delay(LOAD_MILLIS)
                oldest = maxOf(0, oldest - PAGE)
                loading = false
            }
        }
        val items = (oldest until newest).map { id ->
            if (id < TOTAL) message(id) else Message(id, "Ben Okafor", "A new message.", NOW)
        }
        MonarchVirtualMessageList(
            items = items,
            key = { it.id },
            time = { it.time },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            authorId = { it.author },
            firstUnreadKey = TOTAL - UNREAD_FROM,
            hasOlder = oldest > 0,
            loadingOlder = loading,
            now = NOW,
            onLoadOlder = { loading = true },
            header = { MonarchText("This is the start of the planning thread.") },
        ) { message, context ->
            MonarchMessageRow(
                author = MonarchMessageRowAuthor(message.author),
                time = message.time,
                now = NOW,
                grouped = context.grouped,
            ) { MonarchText(message.text) }
        }
        CatalogueCaption("Scroll up, then send")
        MonarchButton(text = "Receive a message", onClick = { newest++ })
    }
}
