package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.collaboration.readreceipt.MonarchReadReceipt
import com.rookhq.monarch.components.collaboration.readreceipt.MonarchReadReceiptReader
import com.rookhq.monarch.components.collaboration.typingindicator.MonarchTypingIndicator
import com.rookhq.monarch.components.collaboration.virtualmessagelist.MonarchVirtualMessageList
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of MessageTimeline: a recipe of MonarchVirtualMessageList and MonarchMessageRow. */
object MessageTimelineSection : CataloguePage {
    override val title = "MessageTimeline"

    // A fixed time, so the page and its screenshots do not change from day to day.
    private const val NOW = 1_794_664_800_000L
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE
    private const val HEIGHT_STEPS = 5

    private class Message(val id: String, val author: String, val text: String, val time: Long)

    private val messages = listOf(
        Message("m1", "Ben Okafor", "Can someone review the release notes?", NOW - DAY - 60 * MINUTE),
        Message("m2", "Ben Okafor", "They are in the shared folder.", NOW - DAY - 59 * MINUTE),
        Message("m3", "Ana Ruiz", "On it this morning.", NOW - 30 * MINUTE),
        Message("m4", "Ana Ruiz", "Done. Two small fixes.", NOW - 5 * MINUTE),
    )

    @Composable
    override fun Content() {
        CatalogueCaption("Grouped by day, with New messages, typing and seen")
        MonarchVirtualMessageList(
            items = messages,
            key = { it.id },
            time = { it.time },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            authorId = { it.author },
            firstUnreadKey = "m4",
            now = NOW,
            footer = { MonarchReadReceipt(readers = listOf(MonarchReadReceiptReader(name = "Ben Okafor"))) },
            status = { MonarchTypingIndicator(names = listOf("Ben")) },
        ) { message, context ->
            MonarchMessageRow(
                author = MonarchMessageRowAuthor(message.author),
                time = message.time,
                now = NOW,
                grouped = context.grouped,
            ) { MonarchText(message.text) }
        }
    }
}
