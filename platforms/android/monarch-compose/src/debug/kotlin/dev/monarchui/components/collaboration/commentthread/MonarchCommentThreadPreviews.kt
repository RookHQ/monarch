@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration.commentthread

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewMinute = 60_000L

private val previewComments = listOf(
    MonarchCommentThreadComment(
        id = "c1",
        author = "Priya Raman",
        body = "Should this say billed every quarter instead?",
        createdAt = PreviewNow - 50 * PreviewMinute,
    ),
    MonarchCommentThreadComment(
        id = "c2",
        author = "Tom Becker",
        body = "Yes. Monthly billing was retired.",
        createdAt = PreviewNow - 20 * PreviewMinute,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchCommentThreadPreview() {
    PreviewFrame {
        MonarchCommentThread(
            comments = previewComments,
            quote = "Billed monthly, cancel any time.",
            now = PreviewNow,
            onResolvedChange = {},
            onReply = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCommentThreadResolvedPreview() {
    PreviewFrame {
        MonarchCommentThread(
            comments = previewComments,
            resolved = true,
            now = PreviewNow,
            onResolvedChange = {},
            onReply = {},
        )
    }
}
