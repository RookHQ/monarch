package com.rookhq.monarch.components.collaboration.messagerow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewMinute = 60_000L

@MonarchPreviews
@Composable
internal fun MonarchMessageRowPreview() {
    PreviewFrame {
        Column {
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Ana Ruiz"),
                time = PreviewNow - 5 * PreviewMinute,
                now = PreviewNow,
                tags = listOf("Guest"),
                reactions = listOf(MonarchReactionBarItem("👍", 3, reacted = true, label = "thumbs up")),
                replyCount = 2,
                edited = true,
                onReactionToggle = { _, _ -> },
                onOpenThread = {},
            ) { MonarchText("The build is green again.") }
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Ana Ruiz"),
                time = PreviewNow - 4 * PreviewMinute,
                now = PreviewNow,
                grouped = true,
            ) { MonarchText("Shipping after lunch.") }
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchMessageRowStatesPreview() {
    PreviewFrame {
        Column {
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Ben Okafor"),
                time = PreviewNow,
                now = PreviewNow,
                status = MonarchMessageRowStatus.Failed,
                onRetry = {},
                onDiscard = {},
            ) { MonarchText("Can you check the invoice?") }
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("Chloé Martin"),
                time = PreviewNow,
                now = PreviewNow,
                variant = MonarchMessageRowVariant.Internal,
                tags = listOf("Internal"),
            ) { MonarchText("Customer is on the old plan.") }
            MonarchMessageRow(
                author = MonarchMessageRowAuthor("System"),
                time = PreviewNow,
                now = PreviewNow,
                variant = MonarchMessageRowVariant.System,
            ) { MonarchText("Ana joined the channel") }
        }
    }
}
