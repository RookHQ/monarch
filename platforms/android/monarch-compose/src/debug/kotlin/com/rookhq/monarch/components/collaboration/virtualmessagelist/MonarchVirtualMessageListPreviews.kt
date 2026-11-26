package com.rookhq.monarch.components.collaboration.virtualmessagelist

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewMinute = 60_000L
private const val PreviewDay = 24 * 60 * PreviewMinute
private const val PreviewHeightSteps = 4

private class PreviewMessage(val id: String, val author: String, val text: String, val time: Long)

private val previewMessages = listOf(
    PreviewMessage("m1", "Ana Ruiz", "Morning! Is the build green?", PreviewNow - PreviewDay),
    PreviewMessage("m2", "Ben Okafor", "Yes, since last night.", PreviewNow - 40 * PreviewMinute),
    PreviewMessage("m3", "Ben Okafor", "Shipping at noon.", PreviewNow - 38 * PreviewMinute),
    PreviewMessage("m4", "Ana Ruiz", "Looks good.", PreviewNow - 5 * PreviewMinute),
)

@MonarchPreviews
@Composable
internal fun MonarchVirtualMessageListPreview() {
    PreviewFrame {
        MonarchVirtualMessageList(
            items = previewMessages,
            key = { it.id },
            time = { it.time },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            authorId = { it.author },
            firstUnreadKey = "m4",
            hasOlder = true,
            now = PreviewNow,
        ) { message, context ->
            MonarchMessageRow(
                author = MonarchMessageRowAuthor(message.author),
                time = message.time,
                now = PreviewNow,
                grouped = context.grouped,
            ) { MonarchText(message.text) }
        }
    }
}
