package com.rookhq.monarch.components.collaboration.postcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewNow = 1_700_000_000_000L
private const val PreviewAge = 3 * 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchPostCardPreview() {
    PreviewFrame {
        MonarchPostCard(
            title = "How do you plan a release with two teams?",
            author = "Ana Lima",
            onClick = {},
            answered = true,
            pinned = true,
            excerpt = "We ship every two weeks and keep missing the window when both teams change the API.",
            publishedAt = PreviewNow - PreviewAge,
            now = PreviewNow,
            tags = listOf(MonarchPostCardTag("Planning"), MonarchPostCardTag("In review", MonarchTone.Warning)),
            reactionCount = 12,
            commentCount = 4,
            actions = {
                MonarchButton(
                    text = "Save",
                    onClick = {},
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        MonarchPostCard(title = "Welcome to the community", author = "Ben Okafor", onClick = {})
    }
}
