package com.rookhq.monarch.foundation.icon

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchTheme

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
