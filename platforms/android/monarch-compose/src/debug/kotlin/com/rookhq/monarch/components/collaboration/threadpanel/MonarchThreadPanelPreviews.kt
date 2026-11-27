@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration.threadpanel

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRow
import com.rookhq.monarch.components.collaboration.messagerow.MonarchMessageRowAuthor
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale
import java.util.TimeZone

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewMinute = 60_000L

private val previewReplies = listOf(
    Triple("r1", "Ben Okafor", "I can take it."),
    Triple("r2", "Ana Ruiz", "Thanks, Ben."),
)

@Composable
private fun PreviewThread(status: MonarchThreadPanelStatus) {
    MonarchThreadPanel(
        replies = previewReplies,
        key = { it.first },
        time = { PreviewNow - 10 * PreviewMinute },
        modifier = Modifier.height(PreviewHeight),
        status = status,
        following = true,
        now = PreviewNow,
        locale = Locale.US,
        timeZone = "UTC",
        onClose = {},
        onFollowingChange = {},
        root = {
            MonarchMessageRow(author = MonarchMessageRowAuthor("Lee Park"), time = PreviewNow - 60 * PreviewMinute) {
                MonarchText("Who can review the release notes?")
            }
        },
    ) { reply, context ->
        MonarchMessageRow(
            author = MonarchMessageRowAuthor(reply.second),
            time = PreviewNow - 10 * PreviewMinute,
            now = PreviewNow,
            grouped = context.grouped,
        ) { MonarchText(reply.third) }
    }
}

/** A bounded height, as the lazy list needs. */
private val PreviewHeight = MonarchTokens.Space.space24.dp * 3

@MonarchPreviews
@Composable
internal fun MonarchThreadPanelPreview() {
    PreviewFrame { PreviewThread(MonarchThreadPanelStatus.Ready) }
}

@MonarchPreviews
@Composable
internal fun MonarchThreadPanelStatesPreview() {
    PreviewFrame {
        PreviewThread(MonarchThreadPanelStatus.Loading)
        PreviewThread(MonarchThreadPanelStatus.Deleted)
    }
}
