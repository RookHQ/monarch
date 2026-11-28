package com.rookhq.monarch.components.collaboration.reactionbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewReactions = listOf(
    MonarchReactionBarItem("👍", count = 3, reacted = true, label = "thumbs up"),
    MonarchReactionBarItem("🎉", count = 1, label = "party popper"),
    MonarchReactionBarItem("👀", count = 12, label = "eyes"),
)

@MonarchPreviews
@Composable
internal fun MonarchReactionBarPreview() {
    PreviewFrame {
        MonarchReactionBar(PreviewReactions, onToggle = { _, _ -> }, quickReactions = listOf("👍", "❤️"))
        MonarchReactionBar(
            PreviewReactions,
            onToggle = { _, _ -> },
            size = MonarchReactionBarSize.Md,
            quickReactions = listOf("👍"),
        )
        MonarchReactionBar(PreviewReactions)
        MonarchReactionBar(PreviewReactions, onToggle = { _, _ -> }, enabled = false, quickReactions = listOf("👍"))
    }
}
