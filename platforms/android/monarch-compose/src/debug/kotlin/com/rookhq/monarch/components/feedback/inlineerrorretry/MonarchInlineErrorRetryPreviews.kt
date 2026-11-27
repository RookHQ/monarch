package com.rookhq.monarch.components.feedback.inlineerrorretry

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
