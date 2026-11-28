package dev.monarchui.components.feedback.inlinenotice

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
