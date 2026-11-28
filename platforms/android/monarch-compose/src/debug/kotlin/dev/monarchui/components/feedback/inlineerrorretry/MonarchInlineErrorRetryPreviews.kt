package dev.monarchui.components.feedback.inlineerrorretry

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchInlineErrorRetryPreview() {
    PreviewFrame {
        MonarchInlineErrorRetryVariant.entries.forEach { variant ->
            listOf(MonarchTone.Danger, MonarchTone.Warning).forEach { tone ->
                MonarchInlineErrorRetry(
                    message = "Your earnings did not load.",
                    onRetry = {},
                    tone = tone,
                    variant = variant,
                    icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
                )
            }
        }
        MonarchInlineErrorRetry(message = "Your earnings did not load.", onRetry = {}, retrying = true)
    }
}
