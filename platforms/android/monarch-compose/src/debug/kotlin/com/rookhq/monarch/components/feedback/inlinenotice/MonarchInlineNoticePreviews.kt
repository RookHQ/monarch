package com.rookhq.monarch.components.feedback.inlinenotice

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchInlineNoticePreview() {
    PreviewFrame {
        MonarchInlineNoticeVariant.entries.forEach { variant ->
            MonarchTone.entries.forEach { tone ->
                MonarchInlineNotice(
                    title = "${tone.name}.",
                    message = "A ${variant.name.lowercase()} notice.",
                    tone = tone,
                    variant = variant,
                    icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
                )
            }
        }
        MonarchInlineNotice(message = "Saved as a draft.", tone = MonarchTone.Success, onDismiss = {})
    }
}
