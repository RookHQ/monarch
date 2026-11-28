package dev.monarchui.foundation.icon

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchIconPreview() {
    PreviewFrame {
        MonarchIconSize.entries.forEach { size ->
            MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = size)
        }
        MonarchIcon(
            imageVector = PreviewIcon,
            contentDescription = "Accent",
            tint = MonarchTheme.colors.accentStrong,
        )
    }
}
