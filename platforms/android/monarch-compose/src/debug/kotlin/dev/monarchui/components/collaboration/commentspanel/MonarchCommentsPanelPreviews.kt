@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration.commentspanel

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadComment
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** 14 November 2026, 14:00 UTC. */
private const val PreviewNow = 1_794_664_800_000L
private const val PreviewMinute = 60_000L

private val previewThreads = listOf(
    MonarchCommentsPanelThread(
        id = "t1",
        comments = listOf(
            MonarchCommentThreadComment("c1", "Priya Raman", "Billed every quarter?", PreviewNow - 50 * PreviewMinute),
            MonarchCommentThreadComment("c2", "Tom Becker", "Yes, retired.", PreviewNow - 20 * PreviewMinute),
        ),
        quote = "Billed monthly, cancel any time.",
    ),
    MonarchCommentsPanelThread(
        id = "t2",
        comments = listOf(
            MonarchCommentThreadComment("c3", "Lee Park", "Can we add a summary?", PreviewNow - 180 * PreviewMinute),
        ),
    ),
)

/** A bounded height, as the lazy list needs. */
private val PreviewHeight = MonarchTokens.Space.space24.dp * 3

@MonarchPreviews
@Composable
internal fun MonarchCommentsPanelListPreview() {
    PreviewFrame {
        MonarchCommentsPanel(
            threads = previewThreads,
            filter = MonarchCommentsPanelFilter.Open,
            onFilterChange = {},
            selectedThreadId = null,
            onSelectedThreadChange = {},
            modifier = Modifier.height(PreviewHeight),
            now = PreviewNow,
            locale = Locale.US,
            onClose = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCommentsPanelThreadPreview() {
    PreviewFrame {
        MonarchCommentsPanel(
            threads = previewThreads,
            filter = MonarchCommentsPanelFilter.Open,
            onFilterChange = {},
            selectedThreadId = "t1",
            onSelectedThreadChange = {},
            modifier = Modifier.height(PreviewHeight),
            now = PreviewNow,
            locale = Locale.US,
            onReply = { _, _ -> },
            onResolvedChange = { _, _ -> },
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCommentsPanelEmptyPreview() {
    PreviewFrame {
        MonarchCommentsPanel(
            threads = emptyList(),
            filter = MonarchCommentsPanelFilter.Open,
            onFilterChange = {},
            selectedThreadId = null,
            onSelectedThreadChange = {},
            modifier = Modifier.height(PreviewHeight),
            now = PreviewNow,
        )
    }
}
